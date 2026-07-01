import axios, { AxiosError } from 'axios';
import { multipartUploadApi } from './multipartUploadApi';
import {
  clearMultipartUploadDraft,
  getMultipartUploadDraft,
  saveMultipartUploadDraft,
} from './multipartUploadStore';
import type {
  CompleteMultipartUploadResponse,
  MultipartUploadDraft,
  MultipartUploadProgressSnapshot,
  MultipartUploadSession,
  PresignedPart,
  UploadAssetType,
  UploadedPartSummary,
} from '../types';

type UploadContext = {
  assetType: UploadAssetType;
  courseId: string;
  lectureId?: string;
  contextKey: string;
  durationSeconds?: number;
};

type UploadCallbacks = {
  onProgress?: (snapshot: MultipartUploadProgressSnapshot) => void;
};

type UploadPartResult = {
  partNumber: number;
  eTag: string;
};

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function buildFingerprint(file: File) {
  return `${file.name}:${file.size}:${file.type}:${file.lastModified}`;
}

function buildEmptySnapshot(file: File): MultipartUploadProgressSnapshot {
  return {
    phase: 'idle',
    fileName: file.name,
    totalBytes: file.size,
    uploadedBytes: 0,
    overallProgress: 0,
    uploadedParts: [],
    partProgress: {},
  };
}

export async function readVideoDurationSeconds(file: File): Promise<number | undefined> {
  if (typeof document === 'undefined' || !file.type.startsWith('video/')) {
    return undefined;
  }

  return new Promise((resolve) => {
    const video = document.createElement('video');
    const objectUrl = URL.createObjectURL(file);

    video.preload = 'metadata';
    video.onloadedmetadata = () => {
      const duration = Number.isFinite(video.duration) ? Math.round(video.duration) : undefined;
      URL.revokeObjectURL(objectUrl);
      resolve(duration);
    };
    video.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(undefined);
    };

    video.src = objectUrl;
  });
}

export class MultipartVideoUploadTask {
  private readonly callbacks?: UploadCallbacks;
  private readonly context: UploadContext;
  private readonly file: File;
  private readonly fileFingerprint: string;
  private session: MultipartUploadSession | null = null;
  private snapshot: MultipartUploadProgressSnapshot;
  private controllers = new Map<number, AbortController>();
  private partLoadedBytes = new Map<number, number>();
  private completedParts = new Map<number, UploadedPartSummary>();
  private cancelled = false;

  constructor(file: File, context: UploadContext, callbacks?: UploadCallbacks) {
    this.file = file;
    this.context = context;
    this.callbacks = callbacks;
    this.fileFingerprint = buildFingerprint(file);
    this.snapshot = buildEmptySnapshot(file);
  }

  async start(): Promise<CompleteMultipartUploadResponse> {
    this.cancelled = false;
    this.emit({ phase: 'starting', message: 'Dang khoi tao upload session' });

    await this.ensureSession();
    await this.hydrateCompletedParts();

    const activeSession = this.requireSession();
    const partSize = activeSession.chunkSizeBytes;
    const totalParts = Math.ceil(this.file.size / partSize);
    const pendingPartNumbers = Array.from({ length: totalParts }, (_, index) => index + 1).filter(
      (partNumber) => !this.completedParts.has(partNumber)
    );

    if (pendingPartNumbers.length > 0) {
      this.emit({
        phase: 'uploading',
        message: pendingPartNumbers.length === totalParts ? 'Dang upload video truc tiep len object storage' : 'Dang tiep tuc upload cac phan con thieu',
      });

      await this.uploadPendingParts(pendingPartNumbers);
    }

    if (this.cancelled) {
      throw new Error('Upload cancelled');
    }

    this.emit({ phase: 'completing', message: 'Dang hoan tat multipart upload' });

    const completed = await multipartUploadApi.complete({
      uploadId: activeSession.uploadId,
      objectKey: activeSession.objectKey,
      parts: Array.from(this.completedParts.values())
        .sort((a, b) => a.partNumber - b.partNumber)
        .map((part) => ({
          partNumber: part.partNumber,
          eTag: part.eTag,
        })),
      durationSeconds: this.context.durationSeconds,
    });

    clearMultipartUploadDraft(this.context.contextKey);
    this.emit({
      phase: 'completed',
      message: 'Upload hoan tat',
      uploadId: completed.uploadId,
      objectKey: completed.objectKey,
      uploadedBytes: this.file.size,
      overallProgress: 100,
    });

    return completed;
  }

  async cancel() {
    this.cancelled = true;
    this.controllers.forEach((controller) => controller.abort());
    this.controllers.clear();

    if (this.session) {
      try {
        await multipartUploadApi.abort(this.session.uploadId, this.session.objectKey);
      } catch {
        /* ignore abort failures during client cancellation */
      }
    }

    clearMultipartUploadDraft(this.context.contextKey);
    this.emit({ phase: 'cancelled', message: 'Da huy upload' });
  }

  getSnapshot() {
    return this.snapshot;
  }

  private async ensureSession() {
    const existingDraft = getMultipartUploadDraft(this.context.contextKey);

    if (existingDraft && existingDraft.fileFingerprint === this.fileFingerprint) {
      const resumedSession = await multipartUploadApi.getStatus(existingDraft.uploadId, existingDraft.objectKey);
      this.session = resumedSession;
      this.persistDraft(resumedSession);
      return;
    }

    if (existingDraft && existingDraft.fileFingerprint !== this.fileFingerprint) {
      try {
        await multipartUploadApi.abort(existingDraft.uploadId, existingDraft.objectKey);
      } catch {
        /* ignore stale draft abort */
      }
      clearMultipartUploadDraft(this.context.contextKey);
    }

    const createdSession = await multipartUploadApi.start({
      fileName: this.file.name,
      contentType: this.file.type || 'application/octet-stream',
      size: this.file.size,
      assetType: this.context.assetType,
      courseId: this.context.courseId,
      lectureId: this.context.lectureId,
    });

    this.session = createdSession;
    this.persistDraft(createdSession);
  }

  private async hydrateCompletedParts() {
    const session = this.requireSession();
    const latestStatus = await multipartUploadApi.getStatus(session.uploadId, session.objectKey);
    this.session = latestStatus;

    this.completedParts = new Map(
      latestStatus.uploadedParts.map((part) => [part.partNumber, part])
    );

    for (const uploadedPart of latestStatus.uploadedParts) {
      this.partLoadedBytes.set(uploadedPart.partNumber, uploadedPart.size);
    }

    this.emit({
      uploadId: latestStatus.uploadId,
      objectKey: latestStatus.objectKey,
      uploadedParts: latestStatus.uploadedParts.map((part) => part.partNumber),
    });
  }

  private async uploadPendingParts(partNumbers: number[]) {
    const session = this.requireSession();
    const concurrency = Math.max(1, Math.min(session.maxConcurrency, partNumbers.length));
    const queue = [...partNumbers];

    const workers = Array.from({ length: concurrency }, async () => {
      while (queue.length > 0 && !this.cancelled) {
        const partNumber = queue.shift();
        if (!partNumber) return;
        const result = await this.uploadPartWithRetry(partNumber, session.maxRetries);
        this.completedParts.set(result.partNumber, {
          partNumber: result.partNumber,
          eTag: result.eTag,
          size: this.getPartSize(result.partNumber),
        });
        this.partLoadedBytes.set(result.partNumber, this.getPartSize(result.partNumber));
        this.emit({
          uploadedParts: Array.from(this.completedParts.keys()).sort((a, b) => a - b),
        });
      }
    });

    await Promise.all(workers);
  }

  private async uploadPartWithRetry(partNumber: number, maxRetries: number): Promise<UploadPartResult> {
    for (let attempt = 1; attempt <= maxRetries; attempt += 1) {
      try {
        return await this.uploadPart(partNumber);
      } catch (error) {
        if (this.cancelled) {
          throw error;
        }

        if (attempt >= maxRetries) {
          this.emit({
            phase: 'failed',
            message: `Upload part ${partNumber} that bai sau ${maxRetries} lan thu`,
          });
          throw error;
        }

        this.partLoadedBytes.set(partNumber, 0);
        this.emit({
          message: `Part ${partNumber} loi, dang thu lai lan ${attempt + 1}/${maxRetries}`,
        });
        await delay(500 * 2 ** attempt);
      }
    }

    throw new Error(`Unable to upload part ${partNumber}`);
  }

  private async uploadPart(partNumber: number): Promise<UploadPartResult> {
    const session = this.requireSession();
    const presigned = await multipartUploadApi.presignParts(session.uploadId, session.objectKey, [partNumber]);
    const part = this.findPresignedPart(presigned.parts, partNumber);
    const controller = new AbortController();
    this.controllers.set(partNumber, controller);
    this.partLoadedBytes.set(partNumber, 0);

    try {
      const response = await axios.put(part.url, this.slicePart(partNumber), {
        headers: {
          'Content-Type': this.file.type || 'application/octet-stream',
        },
        signal: controller.signal,
        onUploadProgress: (event) => {
          this.partLoadedBytes.set(partNumber, event.loaded);
          this.emit();
        },
      });

      const etag =
        response.headers.etag ||
        response.headers.ETag ||
        response.headers['etag'] ||
        response.headers['ETag'];

      if (!etag) {
        throw new Error(`MinIO did not return ETag for part ${partNumber}`);
      }

      return { partNumber, eTag: String(etag) };
    } catch (error) {
      if (axios.isCancel(error) || (error as AxiosError).code === 'ERR_CANCELED') {
        throw new Error('Upload cancelled');
      }
      throw error;
    } finally {
      this.controllers.delete(partNumber);
    }
  }

  private slicePart(partNumber: number) {
    const session = this.requireSession();
    const start = (partNumber - 1) * session.chunkSizeBytes;
    const end = Math.min(start + session.chunkSizeBytes, this.file.size);
    return this.file.slice(start, end);
  }

  private getPartSize(partNumber: number) {
    const session = this.requireSession();
    const start = (partNumber - 1) * session.chunkSizeBytes;
    const end = Math.min(start + session.chunkSizeBytes, this.file.size);
    return end - start;
  }

  private findPresignedPart(parts: PresignedPart[], partNumber: number) {
    const part = parts.find((item) => item.partNumber === partNumber);
    if (!part) {
      throw new Error(`Missing presigned URL for part ${partNumber}`);
    }
    return part;
  }

  private requireSession() {
    if (!this.session) {
      throw new Error('Upload session has not been initialized');
    }
    return this.session;
  }

  private persistDraft(session: MultipartUploadSession) {
    const draft: MultipartUploadDraft = {
      contextKey: this.context.contextKey,
      fileFingerprint: this.fileFingerprint,
      fileName: this.file.name,
      size: this.file.size,
      contentType: this.file.type || 'application/octet-stream',
      assetType: this.context.assetType,
      courseId: this.context.courseId,
      lectureId: this.context.lectureId,
      uploadId: session.uploadId,
      objectKey: session.objectKey,
      chunkSizeBytes: session.chunkSizeBytes,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    saveMultipartUploadDraft(draft);
  }

  private emit(patch: Partial<MultipartUploadProgressSnapshot> = {}) {
    const uploadedBytes = Array.from(this.partLoadedBytes.values()).reduce((total, bytes) => total + bytes, 0);
    const overallProgress = this.file.size > 0 ? Math.min(100, Math.round((uploadedBytes / this.file.size) * 100)) : 0;

    this.snapshot = {
      ...this.snapshot,
      ...patch,
      fileName: this.file.name,
      totalBytes: this.file.size,
      uploadedBytes,
      overallProgress,
      uploadedParts: patch.uploadedParts ?? Array.from(this.completedParts.keys()).sort((a, b) => a - b),
      partProgress: Object.fromEntries(
        Array.from(this.partLoadedBytes.entries()).map(([partNumber, bytes]) => [
          partNumber,
          Math.min(100, Math.round((bytes / this.getPartSize(partNumber)) * 100)),
        ])
      ),
      uploadId: patch.uploadId ?? this.session?.uploadId,
      objectKey: patch.objectKey ?? this.session?.objectKey,
    };

    this.callbacks?.onProgress?.(this.snapshot);
  }
}
