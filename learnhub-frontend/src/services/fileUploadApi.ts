import apiClient from '../lib/api';

export interface FileUploadResult {
    url: string;
    checksum: string;
    checksumAlgorithm: string;
    fileSize: number;
    mimeType: string;
}

export const fileUploadApi = {
    uploadThumbnail: async (file: File, onProgress?: (progress: number) => void): Promise<FileUploadResult> => {
        const formData = new FormData();
        formData.append('file', file);

        const response = await apiClient.post<FileUploadResult>('/v1/files/upload-thumbnail', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            onUploadProgress: (progressEvent) => {
                if (onProgress && progressEvent.total) {
                    const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    onProgress(progress);
                }
            },
        });

        return response.data;
    },

    uploadVideo: async (file: File, onProgress?: (progress: number) => void): Promise<FileUploadResult> => {
        const formData = new FormData();
        formData.append('file', file);

        const response = await apiClient.post<FileUploadResult>('/v1/files/upload-video', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            onUploadProgress: (progressEvent) => {
                if (onProgress && progressEvent.total) {
                    const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    onProgress(progress);
                }
            },
        });

        return response.data;
    },
};
