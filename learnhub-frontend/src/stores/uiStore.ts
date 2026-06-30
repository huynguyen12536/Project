/**
 * Zustand UI store.
 *
 * Global UI chrome state: sidebar collapse, the currently open modal,
 * and a transient toast queue.
 */

import { create } from 'zustand';
import type { Toast, ToastVariant } from '../types/auth';

interface UIState {
  sidebarCollapsed: boolean;
  activeModal: string | null;
  toasts: Toast[];

  toggleSidebar: () => void;
  setSidebarCollapsed: (collapsed: boolean) => void;

  openModal: (id: string) => void;
  closeModal: () => void;

  addToast: (message: string, variant?: ToastVariant, duration?: number) => string;
  removeToast: (id: string) => void;
  clearToasts: () => void;
}

let toastCounter = 0;
function nextToastId(): string {
  toastCounter += 1;
  return `toast-${Date.now()}-${toastCounter}`;
}

export const useUIStore = create<UIState>((set) => ({
  sidebarCollapsed: false,
  activeModal: null,
  toasts: [],

  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
  setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),

  openModal: (id) => set({ activeModal: id }),
  closeModal: () => set({ activeModal: null }),

  addToast: (message, variant = 'info', duration = 4000) => {
    const id = nextToastId();
    set((s) => ({ toasts: [...s.toasts, { id, message, variant, duration }] }));
    return id;
  },
  removeToast: (id) =>
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
  clearToasts: () => set({ toasts: [] }),
}));
