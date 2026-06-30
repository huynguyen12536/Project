/**
 * Profile Form Component
 *
 * Editable form for user profile information.
 * Features:
 * - Text fields (firstName, lastName, email, bio)
 * - Form validation
 * - Submit handler
 * - Error display
 * - Loading states
 * - Success/error callbacks
 *
 * Usage:
 *   <ProfileForm
 *     profile={user}
 *     isLoading={false}
 *     error={null}
 *     onSubmit={handleSubmit}
 *     onSuccess={handleSuccess}
 *     onError={handleError}
 *   />
 */

import React, { useState, useCallback } from 'react';
import { UserProfile, UserProfileUpdatePayload, ErrorResponse } from '../../types';
import { validateEmail, validateName } from '../../utils/validation';

interface ProfileFormProps {
  profile: UserProfile;
  isLoading: boolean;
  error: ErrorResponse | null;
  onSubmit: (updates: UserProfileUpdatePayload) => Promise<void>;
  onSuccess: (message: string) => void;
  onError: (error: ErrorResponse) => void;
}

export const ProfileForm: React.FC<ProfileFormProps> = ({
  profile,
  isLoading,
  error,
  onSubmit,
  onSuccess,
  onError,
}) => {
  const [formData, setFormData] = useState<UserProfileUpdatePayload>({
    firstName: profile.firstName || '',
    lastName: profile.lastName || '',
    email: profile.email || '',
    bio: profile.bio || '',
  });

  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof UserProfileUpdatePayload, string>>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  /**
   * Handle input change
   */
  const handleInputChange = useCallback(
    (field: keyof UserProfileUpdatePayload, value: string) => {
      setFormData((prev) => ({
        ...prev,
        [field]: value,
      }));
      // Clear field error on change
      setFieldErrors((prev) => ({
        ...prev,
        [field]: '',
      }));
    },
    []
  );

  /**
   * Validate form
   */
  const validateForm = (): boolean => {
    const errors: typeof fieldErrors = {};

    if (!formData.firstName.trim()) {
      errors.firstName = 'First name is required';
    } else if (!validateName(formData.firstName)) {
      errors.firstName = 'First name must be 1-50 characters';
    }

    if (!formData.lastName.trim()) {
      errors.lastName = 'Last name is required';
    } else if (!validateName(formData.lastName)) {
      errors.lastName = 'Last name must be 1-50 characters';
    }

    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!validateEmail(formData.email)) {
      errors.email = 'Please enter a valid email address';
    }

    if (formData.bio && formData.bio.length > 500) {
      errors.bio = 'Bio must not exceed 500 characters';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  /**
   * Handle form submit
   */
  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();

      if (!validateForm()) {
        onError({
          error: 'Please fix the errors above',
          code: 'VALIDATION_ERROR',
        });
        return;
      }

      setIsSubmitting(true);
      try {
        await onSubmit(formData);
        onSuccess('Profile updated successfully!');
      } catch (err: any) {
        onError(err);
      } finally {
        setIsSubmitting(false);
      }
    },
    [formData, onSubmit, onSuccess, onError]
  );

  return (
    <div className="bg-white rounded-lg shadow-sm p-8 border border-gray-200">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">Edit Profile</h2>

      {/* Error Message */}
      {error && (
        <div className="mb-6 p-4 bg-error-50 border border-error-200 rounded-lg">
          <p className="text-error-700 text-sm font-medium">{error.error}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* First Name */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            First Name <span className="text-error-500">*</span>
          </label>
          <input
            type="text"
            value={formData.firstName}
            onChange={(e) => handleInputChange('firstName', e.target.value)}
            maxLength={50}
            disabled={isLoading || isSubmitting}
            className={`w-full px-4 py-2 border rounded-lg transition ${
              fieldErrors.firstName
                ? 'border-error-300 bg-error-50'
                : 'border-gray-300 bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500'
            } disabled:opacity-50 disabled:cursor-not-allowed`}
            placeholder="Enter your first name"
          />
          {fieldErrors.firstName && (
            <p className="mt-1 text-sm text-error-600">{fieldErrors.firstName}</p>
          )}
        </div>

        {/* Last Name */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Last Name <span className="text-error-500">*</span>
          </label>
          <input
            type="text"
            value={formData.lastName}
            onChange={(e) => handleInputChange('lastName', e.target.value)}
            maxLength={50}
            disabled={isLoading || isSubmitting}
            className={`w-full px-4 py-2 border rounded-lg transition ${
              fieldErrors.lastName
                ? 'border-error-300 bg-error-50'
                : 'border-gray-300 bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500'
            } disabled:opacity-50 disabled:cursor-not-allowed`}
            placeholder="Enter your last name"
          />
          {fieldErrors.lastName && (
            <p className="mt-1 text-sm text-error-600">{fieldErrors.lastName}</p>
          )}
        </div>

        {/* Email */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Email Address <span className="text-error-500">*</span>
          </label>
          <input
            type="email"
            value={formData.email}
            onChange={(e) => handleInputChange('email', e.target.value)}
            disabled={isLoading || isSubmitting}
            className={`w-full px-4 py-2 border rounded-lg transition ${
              fieldErrors.email
                ? 'border-error-300 bg-error-50'
                : 'border-gray-300 bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500'
            } disabled:opacity-50 disabled:cursor-not-allowed`}
            placeholder="your.email@example.com"
          />
          {fieldErrors.email && (
            <p className="mt-1 text-sm text-error-600">{fieldErrors.email}</p>
          )}
        </div>

        {/* Bio */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Bio <span className="text-gray-500 text-xs">(optional)</span>
          </label>
          <textarea
            value={formData.bio}
            onChange={(e) => handleInputChange('bio', e.target.value)}
            maxLength={500}
            disabled={isLoading || isSubmitting}
            rows={4}
            className={`w-full px-4 py-2 border rounded-lg transition resize-none ${
              fieldErrors.bio
                ? 'border-error-300 bg-error-50'
                : 'border-gray-300 bg-white focus:border-primary-500 focus:ring-1 focus:ring-primary-500'
            } disabled:opacity-50 disabled:cursor-not-allowed`}
            placeholder="Tell us about yourself..."
          />
          <div className="mt-1 flex justify-between items-center">
            {fieldErrors.bio && (
              <p className="text-sm text-error-600">{fieldErrors.bio}</p>
            )}
            <p className="text-xs text-gray-500 ml-auto">
              {formData.bio.length}/500
            </p>
          </div>
        </div>

        {/* Phone (Optional) */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Phone <span className="text-gray-500 text-xs">(optional)</span>
          </label>
          <input
            type="tel"
            value={formData.phone}
            onChange={(e) => handleInputChange('phone', e.target.value)}
            disabled={isLoading || isSubmitting}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
            placeholder="+1 (555) 000-0000"
          />
        </div>

        {/* Location (Optional) */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Location <span className="text-gray-500 text-xs">(optional)</span>
          </label>
          <input
            type="text"
            value={formData.location}
            onChange={(e) => handleInputChange('location', e.target.value)}
            disabled={isLoading || isSubmitting}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
            placeholder="City, Country"
          />
        </div>

        {/* Submit Button */}
        <div className="flex gap-4 pt-6">
          <button
            type="submit"
            disabled={isLoading || isSubmitting}
            className="flex-1 px-6 py-3 bg-primary-600 text-white font-medium rounded-lg hover:bg-primary-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {isSubmitting && <span className="animate-spin">⟳</span>}
            {isSubmitting ? 'Saving...' : 'Save Changes'}
          </button>

          <button
            type="button"
            disabled={isLoading || isSubmitting}
            className="px-6 py-3 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};
