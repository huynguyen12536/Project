/**
 * Zustand admin store.
 *
 * Manages multi-row selection and bulk-action state for admin tables.
 */

import { create } from 'zustand';

interface AdminState {
  selectedUserIds: string[];
  bulkActionPending: boolean;

  toggleSelection: (id: string) => void;
  selectAll: (ids: string[]) => void;
  clearSelection: () => void;
  setBulkActionPending: (pending: boolean) => void;
}

export const useAdminStore = create<AdminState>((set) => ({
  selectedUserIds: [],
  bulkActionPending: false,

  toggleSelection: (id) =>
    set((s) => ({
      selectedUserIds: s.selectedUserIds.includes(id)
        ? s.selectedUserIds.filter((x) => x !== id)
        : [...s.selectedUserIds, id],
    })),

  selectAll: (ids) => set({ selectedUserIds: ids }),
  clearSelection: () => set({ selectedUserIds: [] }),
  setBulkActionPending: (pending) => set({ bulkActionPending: pending }),
}));
