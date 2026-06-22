/**
 * Validation Utilities
 *
 * Helper functions for form validation.
 */

/**
 * Validate email address
 */
export const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Validate name fields (firstName, lastName)
 * - Must be 1-50 characters
 * - Can contain letters, numbers, spaces, hyphens
 */
export const validateName = (name: string): boolean => {
  if (!name || name.length < 1 || name.length > 50) {
    return false;
  }
  // Allow letters, numbers, spaces, hyphens, apostrophes
  const nameRegex = /^[a-zA-Z0-9\s'-]+$/;
  return nameRegex.test(name);
};

/**
 * Validate bio field
 * - Max 500 characters
 */
export const validateBio = (bio: string): boolean => {
  return bio.length <= 500;
};

/**
 * Validate phone number (basic)
 * - Allows various formats
 */
export const validatePhone = (phone: string): boolean => {
  if (!phone) return true; // Optional field
  const phoneRegex = /^[\d+\-\s()]+$/;
  return phoneRegex.test(phone) && phone.length >= 10;
};

/**
 * Check if field is required and not empty
 */
export const isRequired = (value: string | undefined | null): boolean => {
  return Boolean(value && value.trim().length > 0);
};

/**
 * Trim whitespace from object values
 */
export const trimObjectValues = (obj: Record<string, any>) => {
  const trimmed: Record<string, any> = {};
  for (const key in obj) {
    if (typeof obj[key] === 'string') {
      trimmed[key] = obj[key].trim();
    } else {
      trimmed[key] = obj[key];
    }
  }
  return trimmed;
};
