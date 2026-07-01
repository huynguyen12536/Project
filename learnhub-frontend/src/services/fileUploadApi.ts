import apiClient from '../lib/api';

interface FileUploadResponse {
    url: string;
}

export const fileUploadApi = {
    uploadThumbnail: async (file: File, onProgress?: (progress: number) => void): Promise<string> => {
        const formData = new FormData();
        formData.append('file', file);

        const response = await apiClient.post<FileUploadResponse>('/v1/files/upload-thumbnail', formData, {
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

        return response.data.url;
    },
};
