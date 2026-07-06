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

type PresignedPartBatch = {
  batchIndex: number;
  totalBatches: number;
  partNumbers: number[];
};

const MIN_PRESIGN_BATCH_SIZE = 8;
const NETWORK_RECOVERY_RETRY_DELAY_MS = 1500;

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function getNow() {
  return typeof performance !== 'undefined' ? performance.now() : Date.now();
}

function chunkPartNumbers(partNumbers: number[], batchSize: number) {
  const chunks: number[][] = [];
  for (let index = 0; index < partNumbers.length; index += batchSize) {
    chunks.push(partNumbers.slice(index, index + batchSize));
  }
  return chunks;
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
    speedBytesPerSecond: 0,
    chunkSizeBytes: 0,
    completedPartCount: 0,
    totalPartCount: 0,
    activePartCount: 0,
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
  private activePartNumbers = new Set<number>();
  private lastMeasuredBytes = 0;
  private lastMeasuredAt = getNow();
  private smoothedSpeedBytesPerSecond = 0;

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

    this.emit({
      chunkSizeBytes: activeSession.chunkSizeBytes,
      maxConcurrency: activeSession.maxConcurrency,
      totalPartCount: totalParts,
      completedPartCount: this.completedParts.size,
    });

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

    const completed = await this.runWithReconnectRetry(
      () =>
        multipartUploadApi.complete({
          uploadId: activeSession.uploadId,
          objectKey: activeSession.objectKey,
          parts: Array.from(this.completedParts.values())
            .sort((a, b) => a.partNumber - b.partNumber)
            .map((part) => ({
              partNumber: part.partNumber,
              eTag: part.eTag,
            })),
          durationSeconds: this.context.durationSeconds,
        }),
      'Mat ket noi trong luc hoan tat upload. Dang cho mang quay lai de tiep tuc.'
    );

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
      const resumedSession = await this.runWithReconnectRetry(
        () => multipartUploadApi.getStatus(existingDraft.uploadId, existingDraft.objectKey),
        'Mat ket noi khi khoi phuc upload session. Dang cho mang quay lai.'
      );
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

    const createdSession = await this.runWithReconnectRetry(
      () =>
        multipartUploadApi.start({
          fileName: this.file.name,
          contentType: this.file.type || 'application/octet-stream',
          size: this.file.size,
          assetType: this.context.assetType,
          courseId: this.context.courseId,
          lectureId: this.context.lectureId,
        }),
      'Mat ket noi khi tao upload session. Dang cho mang quay lai.'
    );

    this.session = createdSession;
    this.persistDraft(createdSession);
  }

  private async hydrateCompletedParts() {
    const session = this.requireSession();
    const latestStatus = await this.runWithReconnectRetry(
      () => multipartUploadApi.getStatus(session.uploadId, session.objectKey),
      'Mat ket noi khi dong bo trang thai upload. Dang cho mang quay lai.'
    );
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
    const presignBatchSize = Math.max(MIN_PRESIGN_BATCH_SIZE, concurrency * 4);
    const batches = chunkPartNumbers(partNumbers, presignBatchSize);

    for (let index = 0; index < batches.length; index += 1) {
      if (this.cancelled) {
        break;
      }

      const batch: PresignedPartBatch = {
        batchIndex: index + 1,
        totalBatches: batches.length,
        partNumbers: batches[index],
      };

      this.emit({
        message: `Dang lay presigned URL cho lo ${batch.batchIndex}/${batch.totalBatches} (${batch.partNumbers.length} part)`,
      });

      const presigned = await this.runWithReconnectRetry(
        () => multipartUploadApi.presignParts(session.uploadId, session.objectKey, batch.partNumbers),
        `Mat ket noi khi lay presigned URL cho lo ${batch.batchIndex}/${batch.totalBatches}. Dang cho mang quay lai.`
      );
      const presignedMap = new Map(
        presigned.parts.map((part) => [part.partNumber, part])
      );
      const queue = [...batch.partNumbers];

      const workers = Array.from({ length: Math.min(concurrency, batch.partNumbers.length) }, async () => {
        while (queue.length > 0 && !this.cancelled) {
          const partNumber = queue.shift();
          if (!partNumber) return;
          const part = presignedMap.get(partNumber);
          if (!part) {
            throw new Error(`Missing presigned URL for part ${partNumber}`);
          }

          const result = await this.uploadPartWithRetry(partNumber, part, session.maxRetries);
          this.completedParts.set(result.partNumber, {
            partNumber: result.partNumber,
            eTag: result.eTag,
            size: this.getPartSize(result.partNumber),
          });
          this.partLoadedBytes.set(result.partNumber, this.getPartSize(result.partNumber));
          this.emit({
            uploadedParts: Array.from(this.completedParts.keys()).sort((a, b) => a - b),
            completedPartCount: this.completedParts.size,
          });
        }
      });

      await Promise.all(workers);
    }
  }

  private async uploadPartWithRetry(partNumber: number, part: PresignedPart, maxRetries: number): Promise<UploadPartResult> {
    let attempt = 1;

    while (attempt <= maxRetries) {
      try {
        return await this.uploadPart(partNumber, part);
      } catch (error) {
        if (this.cancelled) {
          throw error;
        }

        if (this.isRecoverableNetworkError(error)) {
          this.partLoadedBytes.set(partNumber, 0);
          await this.waitForNetworkRecovery(
            `Mat ket noi trong luc tai part ${partNumber}. Se tu tiep tuc khi mang quay lai.`
          );
          continue;
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
        attempt += 1;
      }
    }

    throw new Error(`Unable to upload part ${partNumber}`);
  }

  private async uploadPart(partNumber: number, part: PresignedPart): Promise<UploadPartResult> {
    const controller = new AbortController();
    this.controllers.set(partNumber, controller);
    this.partLoadedBytes.set(partNumber, 0);
    this.activePartNumbers.add(partNumber);
    this.emit();

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
      this.activePartNumbers.delete(partNumber);
      this.controllers.delete(partNumber);
      this.emit();
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

  private async runWithReconnectRetry<T>(operation: () => Promise<T>, waitingMessage: string): Promise<T> {
    for (;;) {
      try {
        return await operation();
      } catch (error) {
        if (this.cancelled) {
          throw error;
        }

        if (!this.isRecoverableNetworkError(error)) {
          throw error;
        }

        await this.waitForNetworkRecovery(waitingMessage);
      }
    }
  }

  private isRecoverableNetworkError(error: unknown) {
    if (typeof navigator !== 'undefined' && navigator.onLine === false) {
      return true;
    }

    if (!axios.isAxiosError(error)) {
      return false;
    }

    if (!error.response) {
      return true;
    }

    return error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT';
  }

  private async waitForNetworkRecovery(message: string) {
    this.emit({
      phase: 'uploading',
      message,
      speedBytesPerSecond: 0,
    });

    if (typeof window === 'undefined') {
      await delay(NETWORK_RECOVERY_RETRY_DELAY_MS);
      return;
    }

    if (navigator.onLine) {
      await delay(NETWORK_RECOVERY_RETRY_DELAY_MS);
      this.emit({
        phase: 'uploading',
        message: 'Da ket noi lai. Dang tiep tuc upload cac phan con thieu.',
      });
      return;
    }

    await new Promise<void>((resolve, reject) => {
      const handleOnline = () => {
        cleanup();
        resolve();
      };

      const pollTimer = window.setInterval(() => {
        if (this.cancelled) {
          cleanup();
          reject(new Error('Upload cancelled'));
          return;
        }

        if (navigator.onLine) {
          cleanup();
          resolve();
        }
      }, 1000);

      const cleanup = () => {
        window.removeEventListener('online', handleOnline);
        window.clearInterval(pollTimer);
      };

      window.addEventListener('online', handleOnline);
    });

    await delay(NETWORK_RECOVERY_RETRY_DELAY_MS);
    this.emit({
      phase: 'uploading',
      message: 'Da ket noi lai. Dang tiep tuc upload cac phan con thieu.',
    });
  }

  private emit(patch: Partial<MultipartUploadProgressSnapshot> = {}) {
    const uploadedBytes = Array.from(this.partLoadedBytes.values()).reduce((total, bytes) => total + bytes, 0);
    const overallProgress = this.file.size > 0 ? Math.min(100, Math.round((uploadedBytes / this.file.size) * 100)) : 0;
    const now = getNow();
    const elapsedMs = Math.max(1, now - this.lastMeasuredAt);
    const deltaBytes = Math.max(0, uploadedBytes - this.lastMeasuredBytes);

    if (deltaBytes > 0 || elapsedMs >= 1000 || patch.phase === 'completed' || patch.phase === 'failed' || patch.phase === 'cancelled') {
      const instantaneousSpeed = elapsedMs > 0 ? (deltaBytes * 1000) / elapsedMs : 0;
      this.smoothedSpeedBytesPerSecond =
        this.smoothedSpeedBytesPerSecond === 0
          ? instantaneousSpeed
          : this.smoothedSpeedBytesPerSecond * 0.65 + instantaneousSpeed * 0.35;
      this.lastMeasuredBytes = uploadedBytes;
      this.lastMeasuredAt = now;
    }

    const session = this.session;
    const totalPartCount = patch.totalPartCount ?? (session ? Math.ceil(this.file.size / session.chunkSizeBytes) : this.snapshot.totalPartCount);
    const completedPartCount = patch.completedPartCount ?? this.completedParts.size;

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
      speedBytesPerSecond:
        patch.speedBytesPerSecond ??
        (patch.phase === 'completed' || patch.phase === 'failed' || patch.phase === 'cancelled'
          ? 0
          : Math.max(0, Math.round(this.smoothedSpeedBytesPerSecond))),
      chunkSizeBytes: patch.chunkSizeBytes ?? session?.chunkSizeBytes ?? this.snapshot.chunkSizeBytes,
      completedPartCount,
      totalPartCount,
      activePartCount: this.activePartNumbers.size,
      maxConcurrency: patch.maxConcurrency ?? session?.maxConcurrency ?? this.snapshot.maxConcurrency,
      uploadId: patch.uploadId ?? this.session?.uploadId,
      objectKey: patch.objectKey ?? this.session?.objectKey,
    };

    this.callbacks?.onProgress?.(this.snapshot);
  }
}
