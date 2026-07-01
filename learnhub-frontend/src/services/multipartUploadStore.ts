import type { MultipartUploadDraft } from '../types';

const STORAGE_KEY = 'learnhub.multipart-upload-drafts';

type DraftMap = Record<string, MultipartUploadDraft>;

function readDraftMap(): DraftMap {
  if (typeof window === 'undefined') return {};

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return {};
    return JSON.parse(raw) as DraftMap;
  } catch {
    return {};
  }
}

function writeDraftMap(map: DraftMap) {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(map));
}

export function getMultipartUploadDraft(contextKey: string): MultipartUploadDraft | null {
  return readDraftMap()[contextKey] ?? null;
}

export function saveMultipartUploadDraft(draft: MultipartUploadDraft) {
  const map = readDraftMap();
  map[draft.contextKey] = draft;
  writeDraftMap(map);
}

export function clearMultipartUploadDraft(contextKey: string) {
  const map = readDraftMap();
  delete map[contextKey];
  writeDraftMap(map);
}
