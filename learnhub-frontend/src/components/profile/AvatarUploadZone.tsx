/**
 * Avatar Upload Zone Component
 */

import React, { useState, useRef, useCallback } from 'react';
import { ErrorResponse } from '../../types';
import { Upload, Image } from 'lucide-react';
import { Button } from '../../ui-kit';

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

interface AvatarUploadZoneProps {
  userId: string;
  currentAvatarUrl: string | null;
  isLoading: boolean;
  onUpload: (userId: string, file: File) => Promise<string>;
  onError: (error: ErrorResponse) => void;
}

export const AvatarUploadZone: React.FC<AvatarUploadZoneProps> = ({
  userId,
  currentAvatarUrl,
  isLoading,
  onUpload,
  onError,
}) => {
  const [isDragging, setIsDragging] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): ErrorResponse | null => {
    if (file.size > MAX_FILE_SIZE) {
      return {
        error: 'Tệp quá lớn. Kích thước tối đa là 5MB.',
        code: 'FILE_TOO_LARGE',
      };
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      return {
        error: 'Loại tệp không hợp lệ. Vui lòng tải lên JPEG, PNG hoặc WebP.',
        code: 'INVALID_FILE_TYPE',
      };
    }

    return null;
  };

  const handleFileSelect = useCallback(
    async (file: File) => {
      const validationError = validateFile(file);
      if (validationError) {
        onError(validationError);
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        setPreview(e.target?.result as string);
      };
      reader.readAsDataURL(file);

      setIsUploading(true);
      try {
        await onUpload(userId, file);
      } catch (error: any) {
        onError(error);
      } finally {
        setIsUploading(false);
      }
    },
    [userId, onUpload, onError]
  );

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFileSelect(files[0]);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.currentTarget.files;
    if (files && files.length > 0) {
      handleFileSelect(files[0]);
    }
  };

  const triggerFilePicker = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-6 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
      <div className="mb-4">
        <h3 className="text-lg font-bold tracking-[-0.03em] text-[#111827]">Thay đổi ảnh đại diện</h3>
        <p className="mt-1 text-sm text-[#6B7280]">Tải lên ảnh đại diện mới của bạn</p>
      </div>

      <div className="space-y-4">
        {/* Drop Zone */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`border-2 border-dashed rounded-2xl p-8 text-center transition-all ${isDragging
              ? 'border-lh-blue bg-[#EEF2FF]'
              : 'border-[#E7E9F2] bg-[#F7F8FC] hover:border-lh-blue/50'
            } ${isUploading || isLoading ? 'opacity-50 pointer-events-none' : ''}`}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={handleInputChange}
            className="hidden"
          />

          {isUploading ? (
            <div className="space-y-3">
              <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-[#EEF2FF] border-t-lh-blue"></div>
              <p className="text-sm font-semibold text-[#6B7280]">Đang tải lên...</p>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#EEF2FF]">
                <Upload className="h-6 w-6 text-lh-blue" />
              </div>
              <div>
                <p className="text-sm font-semibold text-[#111827]">Kéo thả ảnh vào đây</p>
                <p className="text-xs text-[#6B7280] mt-1">hoặc</p>
              </div>
              <Button type="button" size="sm" onClick={triggerFilePicker}>
                Chọn tệp
              </Button>
              <p className="text-xs text-[#9CA3AF]">
                JPEG, PNG hoặc WebP · Tối đa 5MB
              </p>
            </div>
          )}
        </div>

        {/* Preview */}
        {(preview || currentAvatarUrl) && (
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Image className="h-4 w-4 text-[#9CA3AF]" />
              <span className="text-sm font-semibold text-[#111827]">Xem trước</span>
            </div>
            <div className="flex justify-center">
              <img
                src={preview || currentAvatarUrl!}
                alt="Xem trước ảnh đại diện"
                className="h-40 w-40 rounded-full border-4 border-[#E7E9F2] object-cover"
              />
            </div>
          </div>
        )}

        {/* Tip */}
        <div className="rounded-2xl bg-[#EEF2FF] p-4">
          <p className="text-sm font-medium text-lh-blue">
            💡 <strong>Mẹo:</strong> Sử dụng ảnh vuông (vd: 200×200px) để có kết quả tốt nhất.
          </p>
        </div>
      </div>
    </div>
  );
};
