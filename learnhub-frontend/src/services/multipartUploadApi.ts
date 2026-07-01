import { apiClient } from '../lib/api';
import type {
  CompleteMultipartUploadRequestPayload,
  CompleteMultipartUploadResponse,
  MultipartUploadPresignResponse,
  MultipartUploadSession,
  StartMultipartUploadRequest,
} from '../types';

export const multipartUploadApi = {
  start: async (request: StartMultipartUploadRequest): Promise<MultipartUploadSession> => {
    const response = await apiClient.post<MultipartUploadSession>('/v1/uploads/multipart/start', request);
    return response.data;
  },

  getStatus: async (uploadId: string, objectKey: string): Promise<MultipartUploadSession> => {
    const response = await apiClient.get<MultipartUploadSession>('/v1/uploads/multipart/status', {
      params: { uploadId, objectKey },
    });
    return response.data;
  },

  presignParts: async (
    uploadId: string,
    objectKey: string,
    partNumbers: number[]
  ): Promise<MultipartUploadPresignResponse> => {
    const response = await apiClient.post<MultipartUploadPresignResponse>('/v1/uploads/multipart/presign', {
      uploadId,
      objectKey,
      partNumbers,
    });
    return response.data;
  },

  complete: async (
    request: CompleteMultipartUploadRequestPayload
  ): Promise<CompleteMultipartUploadResponse> => {
    const response = await apiClient.post<CompleteMultipartUploadResponse>('/v1/uploads/multipart/complete', request);
    return response.data;
  },

  abort: async (uploadId: string, objectKey: string): Promise<void> => {
    await apiClient.post('/v1/uploads/multipart/abort', { uploadId, objectKey });
  },
};
