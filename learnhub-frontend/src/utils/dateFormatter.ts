/**
 * Date Formatting Utilities
 *
 * Helper functions for date/time formatting.
 */

/**
 * Format ISO timestamp to readable date
 * Example: "2026-06-19T14:30:00Z" → "June 19, 2026"
 */
export const formatDate = (dateString: string | null): string => {
  if (!dateString) return 'Unknown';

  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return 'Invalid date';
  }
};

/**
 * Format ISO timestamp to readable date + time
 * Example: "2026-06-19T14:30:00Z" → "June 19, 2026 · 2:30 PM"
 */
export const formatDateTime = (dateString: string | null): string => {
  if (!dateString) return 'Unknown';

  try {
    const date = new Date(dateString);
    const dateStr = date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
    const timeStr = date.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
    return `${dateStr} · ${timeStr}`;
  } catch {
    return 'Invalid date';
  }
};

/**
 * Get relative time (e.g., "2 hours ago")
 */
export const getRelativeTime = (dateString: string | null): string => {
  if (!dateString) return 'Unknown';

  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return 'Just now';
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`;
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
    if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)} days ago`;

    return formatDate(dateString);
  } catch {
    return 'Unknown';
  }
};
