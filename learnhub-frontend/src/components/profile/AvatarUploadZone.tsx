/**
 * Avatar Upload Zone Component
 *
 * Drag-and-drop file upload for user avatars.
 * Features:
 * - Drag-drop zone
 * - File picker button
 * - Image preview
 * - File validation (size, type)
 * - Upload progress
 * - Error handling
 *
 * Usage:
 *   <AvatarUploadZone
 *     userId={userId}
 *     currentAvatarUrl={url}
 *     isLoading={false}
 *     onUpload={handleUpload}
 *     onError={handleError}
 *   />
 */

import React, { useState, useRef, useCallback } from 'react';
import { ErrorResponse } from '../../types';

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

  /**
   * Validate file before upload
   */
  const validateFile = (file: File): ErrorResponse | null => {
    if (file.size > MAX_FILE_SIZE) {
      return {
        error: 'File is too large. Maximum size is 5MB.',
        code: 'FILE_TOO_LARGE',
      };
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      return {
        error: 'Invalid file type. Please upload JPEG, PNG, or WebP.',
        code: 'INVALID_FILE_TYPE',
      };
    }

    return null;
  };

  /**
   * Handle file selection (from input or drag-drop)
   */
  const handleFileSelect = useCallback(
    async (file: File) => {
      // Validate
      const validationError = validateFile(file);
      if (validationError) {
        onError(validationError);
        return;
      }

      // Show preview
      const reader = new FileReader();
      reader.onload = (e) => {
        setPreview(e.target?.result as string);
      };
      reader.readAsDataURL(file);

      // Upload
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

  /**
   * Handle drag-over
   */
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  /**
   * Handle drag-leave
   */
  const handleDragLeave = () => {
    setIsDragging(false);
  };

  /**
   * Handle drop
   */
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFileSelect(files[0]);
    }
  };

  /**
   * Handle input change
   */
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.currentTarget.files;
    if (files && files.length > 0) {
      handleFileSelect(files[0]);
    }
  };

  /**
   * Trigger file picker
   */
  const triggerFilePicke = () => {
    fileInputRef.current?.click();
  };

  return (
    <div className="space-y-4">
      {/* Drop Zone */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
          isDragging
            ? 'border-primary-500 bg-primary-50'
            : 'border-gray-300 bg-gray-50 hover:border-primary-400'
        } ${isUploading ? 'opacity-50 pointer-events-none' : ''}`}
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
            <div className="animate-spin text-primary-600 text-3xl">⟳</div>
            <p className="text-gray-600 text-sm">Uploading...</p>
          </div>
        ) : (
          <div className="space-y-3">
            <div className="text-4xl">📸</div>
            <p className="text-gray-700 font-medium">Drag and drop your avatar</p>
            <p className="text-gray-600 text-sm">or</p>
            <button
              onClick={triggerFilePicke}
              className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition font-medium text-sm"
            >
              Choose File
            </button>
            <p className="text-gray-600 text-xs">
              JPEG, PNG, or WebP · Max 5MB
            </p>
          </div>
        )}
      </div>

      {/* Preview */}
      {(preview || currentAvatarUrl) && (
        <div className="space-y-3">
          <p className="text-sm font-medium text-gray-700">Preview:</p>
          <img
            src={preview || currentAvatarUrl!}
            alt="Avatar preview"
            className="w-full h-48 object-cover rounded-lg border border-gray-200"
          />
        </div>
      )}

      {/* Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-blue-700 text-sm">
          💡 <strong>Tip:</strong> Use a square image (e.g., 200×200px) for best results.
        </p>
      </div>
    </div>
  );
};
