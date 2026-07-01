import React, { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { cn } from '../lib/cn';
import { useUIStore } from '../stores/uiStore';
import type { Toast as ToastType } from '../types/auth';

const variantClasses: Record<ToastType['variant'], string> = {
  success: 'bg-success-600 text-white',
  error: 'bg-error-600 text-white',
  warning: 'bg-amber-500 text-white',
  info: 'bg-gray-800 text-white',
};

const ToastItem: React.FC<{ toast: ToastType }> = ({ toast }) => {
  const removeToast = useUIStore((s) => s.removeToast);

  useEffect(() => {
    if (toast.duration && toast.duration > 0) {
      const t = setTimeout(() => removeToast(toast.id), toast.duration);
      return () => clearTimeout(t);
    }
  }, [toast.id, toast.duration, removeToast]);

  return (
    <motion.div
      role="status"
      initial={{ opacity: 0, x: 32, scale: 0.98 }}
      animate={{ opacity: 1, x: 0, scale: 1 }}
      exit={{ opacity: 0, x: 24, scale: 0.98 }}
      transition={{ duration: 0.22, ease: 'easeOut' }}
      className={cn(
        'pointer-events-auto flex items-center justify-between gap-4 rounded-lg px-4 py-3 shadow-lg',
        variantClasses[toast.variant]
      )}
    >
      <span className="text-sm">{toast.message}</span>
      <button
        type="button"
        onClick={() => removeToast(toast.id)}
        aria-label="Dismiss"
        className="text-white/80 hover:text-white"
      >
        ✕
      </button>
    </motion.div>
  );
};

/**
 * Mount once near the app root. Renders toasts from the UI store.
 */
export const ToastContainer: React.FC = () => {
  const toasts = useUIStore((s) => s.toasts);
  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-80 flex-col gap-2">
      <AnimatePresence initial={false}>
        {toasts.map((t) => (
          <ToastItem key={t.id} toast={t} />
        ))}
      </AnimatePresence>
    </div>
  );
};

export default ToastContainer;
