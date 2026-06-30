/**
 * Zustand course-player store.
 *
 * Tracks the active lesson, completed lessons, notes-panel visibility,
 * and per-lesson video progress for the course player.
 */

import { create } from 'zustand';

interface CoursePlayerState {
  currentLessonId: string | null;
  completedLessons: string[];
  notesPanelOpen: boolean;
  videoProgress: Record<string, number>; // lessonId -> seconds watched

  setCurrentLesson: (lessonId: string) => void;
  markLessonComplete: (lessonId: string) => void;
  toggleNotesPanel: () => void;
  setNotesPanelOpen: (open: boolean) => void;
  setVideoProgress: (lessonId: string, seconds: number) => void;
  reset: () => void;
}

export const useCoursePlayerStore = create<CoursePlayerState>((set) => ({
  currentLessonId: null,
  completedLessons: [],
  notesPanelOpen: false,
  videoProgress: {},

  setCurrentLesson: (lessonId) => set({ currentLessonId: lessonId }),

  markLessonComplete: (lessonId) =>
    set((s) =>
      s.completedLessons.includes(lessonId)
        ? s
        : { completedLessons: [...s.completedLessons, lessonId] }
    ),

  toggleNotesPanel: () => set((s) => ({ notesPanelOpen: !s.notesPanelOpen })),
  setNotesPanelOpen: (open) => set({ notesPanelOpen: open }),

  setVideoProgress: (lessonId, seconds) =>
    set((s) => ({ videoProgress: { ...s.videoProgress, [lessonId]: seconds } })),

  reset: () =>
    set({
      currentLessonId: null,
      completedLessons: [],
      notesPanelOpen: false,
      videoProgress: {},
    }),
}));
