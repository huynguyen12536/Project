import React, { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  CheckCircle2,
  CheckSquare,
  ChevronDown,
  ChevronRight,
  FileText,
  FolderPlus,
  GripVertical,
  ImagePlus,
  Layers3,
  PanelLeftClose,
  PanelLeftOpen,
  Save,
  Sparkles,
  Trash2,
  Video,
} from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import InstructorWorkspaceLayout from '../../components/layouts/InstructorWorkspaceLayout';
import { QuizBuilder, type QuizQuestion } from '../../components/QuizBuilder';
import { courseApi } from '../../services/courseApi';
import { fileUploadApi } from '../../services/fileUploadApi';
import { multipartUploadApi } from '../../services/multipartUploadApi';
import {
  MultipartVideoUploadTask,
  readVideoDurationSeconds,
} from '../../services/multipartUploadService';
import { clearMultipartUploadDraft, getMultipartUploadDraft } from '../../services/multipartUploadStore';
import { cn } from '../../lib/cn';
import { useUIStore } from '../../stores/uiStore';
import {
  Badge,
  Button,
  FileDropzone,
  Input,
  ProgressBar,
  RichTextEditor,
  Select,
  SkeletonCard,
  Textarea,
  Toggle,
} from '../../ui-kit';
import type {
  Course,
  CourseLecture,
  CourseSection,
  CourseTaxonomyBundle,
  CreateCourseRequest,
  MultipartUploadDraft,
  MultipartUploadProgressSnapshot,
  UpdateCourseRequest,
} from '../../types';
import { LectureType, UploadAssetType } from '../../types';

type Step = 1 | 2 | 3;

type SelectedLectureRef = {
  sectionId: string;
  lectureId: string;
};

type LectureDraft = {
  title: string;
  type: LectureType;
  content: string;
  videoUrl: string;
  durationSeconds: string;
  isFreePreview: boolean;
};

type LectureEditorDraftState = {
  lectureDraft: LectureDraft;
  quizQuestions: QuizQuestion[];
  dirty: boolean;
};

type LectureVideoPreviewState = {
  posterUrl?: string;
  videoUrl?: string;
  fileName?: string;
  fileSize?: number;
  durationSeconds?: number;
  isLocal?: boolean;
};

const steps: Array<{ id: Step; label: string; detail: string; icon: React.ComponentType<{ className?: string }> }> = [
  { id: 1, label: 'Nen tang khoa hoc', detail: 'Noi dung co ban va taxonomy', icon: BookOpen },
  { id: 2, label: 'Curriculum studio', detail: 'Chuong, bai giang, upload va noi dung', icon: Layers3 },
  { id: 3, label: 'Kiem tra va gui duyet', detail: 'Rafat lai chat luong truoc khi gui', icon: CheckSquare },
];

const emptyTaxonomy: CourseTaxonomyBundle = {
  categories: [],
  subcategories: [],
  levels: [],
  languages: [],
  tags: [],
};

const initialBasicFormData: CreateCourseRequest = {
  title: '',
  subtitle: '',
  description: '',
  thumbnailUrl: '',
  promoVideoUrl: '',
  categoryId: '',
  subcategoryId: '',
  levelId: '',
  languageId: '',
};

const lectureTypeLabels: Record<LectureType, string> = {
  [LectureType.VIDEO]: 'Video',
  [LectureType.ARTICLE]: 'Bai viet',
  [LectureType.QUIZ]: 'Quiz',
};

const courseStatusVariant: Record<string, 'default' | 'warning' | 'info' | 'success' | 'error'> = {
  DRAFT: 'default',
  PENDING_REVIEW: 'warning',
  PUBLISHED: 'success',
  REJECTED: 'error',
};

const courseStatusLabel: Record<string, string> = {
  DRAFT: 'Ban nhap',
  PENDING_REVIEW: 'Cho duyet',
  PUBLISHED: 'Da xuat ban',
  REJECTED: 'Can chinh sua',
};

function createDefaultLectureDraft(lecture: CourseLecture): LectureDraft {
  return {
    title: lecture.title,
    type: lecture.type,
    content: lecture.type === LectureType.QUIZ ? '' : lecture.content ?? '',
    videoUrl: lecture.videoUrl ?? '',
    durationSeconds: lecture.durationSeconds ? String(lecture.durationSeconds) : '',
    isFreePreview: lecture.isFreePreview,
  };
}

function parseQuizContent(content?: string): QuizQuestion[] {
  if (!content) return [];

  try {
    const parsed = JSON.parse(content);
    if (Array.isArray(parsed?.questions)) {
      return parsed.questions;
    }
  } catch {
    return [];
  }

  return [];
}

function serializeQuizContent(questions: QuizQuestion[]): string {
  return JSON.stringify({
    version: 1,
    questions,
  });
}

function buildNextDisplayOrder(items: Array<{ displayOrder: number }>) {
  const maxValue = items.reduce((maxOrder, item) => Math.max(maxOrder, item.displayOrder ?? 0), 0);
  return maxValue + 10;
}

function formatSeconds(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const remainSeconds = seconds % 60;
  if (minutes <= 0) return `${remainSeconds}s`;
  if (remainSeconds === 0) return `${minutes} phut`;
  return `${minutes} phut ${remainSeconds}s`;
}

function formatBytes(value: number) {
  if (!Number.isFinite(value) || value <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  const precision = size >= 100 || unitIndex === 0 ? 0 : 1;
  return `${size.toFixed(precision)} ${units[unitIndex]}`;
}

function getFileNameFromUrl(url: string) {
  try {
    const pathname = new URL(url).pathname;
    const rawSegment = pathname.split('/').filter(Boolean).pop();
    return rawSegment ? decodeURIComponent(rawSegment) : 'video-da-tai-len';
  } catch {
    return 'video-da-tai-len';
  }
}

async function createVideoPosterFrame(file: File): Promise<string | null> {
  if (typeof document === 'undefined' || !file.type.startsWith('video/')) {
    return null;
  }

  return new Promise((resolve) => {
    const captureUrl = URL.createObjectURL(file);
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.muted = true;
    video.playsInline = true;

    const cleanup = () => {
      URL.revokeObjectURL(captureUrl);
    };

    const fail = () => {
      cleanup();
      resolve(null);
    };

    video.onerror = fail;
    video.onloadedmetadata = () => {
      const targetTime = Number.isFinite(video.duration) && video.duration > 1 ? Math.min(1, video.duration / 3) : 0;
      video.currentTime = targetTime;
    };

    video.onseeked = () => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth || 1280;
        canvas.height = video.videoHeight || 720;

        const context = canvas.getContext('2d');
        if (!context) {
          fail();
          return;
        }

        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        canvas.toBlob((blob) => {
          cleanup();
          if (!blob) {
            resolve(null);
            return;
          }

          resolve(URL.createObjectURL(blob));
        }, 'image/jpeg', 0.86);
      } catch {
        fail();
      }
    };

    video.src = captureUrl;
  });
}

function computeCurriculumStats(sections: CourseSection[]) {
  return sections.reduce(
    (acc, section) => {
      acc.sectionCount += 1;
      acc.lectureCount += section.lectures.length;
      acc.videoSeconds += section.lectures.reduce(
        (sum, lecture) => sum + (lecture.durationSeconds ?? 0),
        0
      );
      return acc;
    },
    { sectionCount: 0, lectureCount: 0, videoSeconds: 0 }
  );
}

function getFirstLecture(sections: CourseSection[]): SelectedLectureRef | null {
  for (const section of sections) {
    if (section.lectures[0]) {
      return {
        sectionId: section.id,
        lectureId: section.lectures[0].id,
      };
    }
  }
  return null;
}

function buildLectureUploadContextKey(lectureId: string) {
  return `course-video:${lectureId}`;
}

export default function InstructorCourseWizardPage() {
  const navigate = useNavigate();
  const { courseId } = useParams<{ courseId?: string }>();
  const [searchParams] = useSearchParams();
  const addToast = useUIStore((state) => state.addToast);

  const [currentStep, setCurrentStep] = useState<Step>(1);
  const [course, setCourse] = useState<Course | null>(null);
  const [taxonomy, setTaxonomy] = useState<CourseTaxonomyBundle>(emptyTaxonomy);
  const [sections, setSections] = useState<CourseSection[]>([]);
  const [basicFormData, setBasicFormData] = useState<CreateCourseRequest>(initialBasicFormData);
  const [selectedLectureRef, setSelectedLectureRef] = useState<SelectedLectureRef | null>(null);
  const [sectionDrafts, setSectionDrafts] = useState<Record<string, string>>({});
  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>({});
  const [lectureDraft, setLectureDraft] = useState<LectureDraft | null>(null);
  const [lectureEditorDrafts, setLectureEditorDrafts] = useState<Record<string, LectureEditorDraftState>>({});
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([]);
  const [isCurriculumCollapsed, setIsCurriculumCollapsed] = useState(false);
  const [thumbnailProgress, setThumbnailProgress] = useState(0);
  const [thumbnailPreviewUrl, setThumbnailPreviewUrl] = useState<string | null>(null);
  const [lectureVideoPreviews, setLectureVideoPreviews] = useState<Record<string, LectureVideoPreviewState>>({});
  const [videoUploadState, setVideoUploadState] = useState<Record<string, MultipartUploadProgressSnapshot>>({});
  const [pendingUploadDraft, setPendingUploadDraft] = useState<MultipartUploadDraft | null>(null);
  const [isBooting, setIsBooting] = useState(true);
  const [isSavingBasics, setIsSavingBasics] = useState(false);
  const [isCreatingSection, setIsCreatingSection] = useState(false);
  const [savingSectionId, setSavingSectionId] = useState<string | null>(null);
  const [savingLectureId, setSavingLectureId] = useState<string | null>(null);
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [deletingTarget, setDeletingTarget] = useState<string | null>(null);
  const [isUploadingThumbnail, setIsUploadingThumbnail] = useState(false);
  const videoUploadTasksRef = useRef<Record<string, MultipartVideoUploadTask>>({});
  const lectureVideoPreviewsRef = useRef<Record<string, LectureVideoPreviewState>>({});

  const filteredSubcategories = useMemo(
    () =>
      basicFormData.categoryId
        ? taxonomy.subcategories.filter((subcategory) => subcategory.categoryId === basicFormData.categoryId)
        : [],
    [taxonomy.subcategories, basicFormData.categoryId]
  );

  const selectedLecture = useMemo(() => {
    if (!selectedLectureRef) return null;
    return sections
      .find((section) => section.id === selectedLectureRef.sectionId)
      ?.lectures.find((lecture) => lecture.id === selectedLectureRef.lectureId) ?? null;
  }, [sections, selectedLectureRef]);

  const selectedLectureEditorDraft = useMemo(
    () => (selectedLecture ? lectureEditorDrafts[selectedLecture.id] ?? null : null),
    [lectureEditorDrafts, selectedLecture]
  );

  const selectedLectureVideoPreview = useMemo(() => {
    if (!selectedLecture || !lectureDraft) return null;

    const localPreview = lectureVideoPreviews[selectedLecture.id];
    if (localPreview) {
      return localPreview;
    }

    if (!lectureDraft.videoUrl) {
      return null;
    }

    return {
      videoUrl: lectureDraft.videoUrl,
      fileName: getFileNameFromUrl(lectureDraft.videoUrl),
      durationSeconds:
        lectureDraft.durationSeconds.trim().length > 0 ? Number(lectureDraft.durationSeconds) : undefined,
      isLocal: false,
    } satisfies LectureVideoPreviewState;
  }, [lectureDraft, lectureVideoPreviews, selectedLecture]);

  const curriculumStats = useMemo(() => computeCurriculumStats(sections), [sections]);

  const basicCompletion = useMemo(() => {
    const checks = [
      Boolean(basicFormData.title?.trim()),
      Boolean(basicFormData.description?.trim()),
      Boolean(basicFormData.thumbnailUrl),
      Boolean(basicFormData.categoryId),
      Boolean(basicFormData.levelId),
      Boolean(basicFormData.languageId),
    ];

    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  }, [basicFormData]);

  const reviewChecks = useMemo(
    () => [
      {
        label: 'Thong tin nen tang',
        detail: 'Tieu de, mo ta, thumbnail va taxonomy can day du.',
        passed:
          Boolean(basicFormData.title?.trim()) &&
          Boolean(basicFormData.description?.trim()) &&
          Boolean(basicFormData.thumbnailUrl) &&
          Boolean(basicFormData.categoryId) &&
          Boolean(basicFormData.levelId) &&
          Boolean(basicFormData.languageId),
      },
      {
        label: 'It nhat 1 chuong',
        detail: `${curriculumStats.sectionCount} chuong hien co trong curriculum.`,
        passed: curriculumStats.sectionCount > 0,
      },
      {
        label: 'It nhat 5 bai giang',
        detail: `${curriculumStats.lectureCount} bai giang hien co.`,
        passed: curriculumStats.lectureCount >= 5,
      },
      {
        label: 'It nhat 30 phut noi dung',
        detail: `Tong thoi luong video hien tai la ${formatSeconds(curriculumStats.videoSeconds)}.`,
        passed: curriculumStats.videoSeconds >= 1800,
      },
    ],
    [basicFormData, curriculumStats]
  );

  const reviewCompletion = Math.round(
    (reviewChecks.filter((item) => item.passed).length / reviewChecks.length) * 100
  );

  const updateLectureEditorDraft = (
    lectureId: string,
    nextLectureDraft: LectureDraft,
    nextQuizQuestions: QuizQuestion[],
    dirty = true
  ) => {
    setLectureEditorDrafts((previous) => ({
      ...previous,
      [lectureId]: {
        lectureDraft: nextLectureDraft,
        quizQuestions: nextQuizQuestions,
        dirty,
      },
    }));
  };

  const removeLectureEditorDraft = (lectureId: string) => {
    setLectureEditorDrafts((previous) => {
      if (!previous[lectureId]) return previous;
      const nextState = { ...previous };
      delete nextState[lectureId];
      return nextState;
    });
  };

  const revokeLectureVideoPreview = (preview?: LectureVideoPreviewState) => {
    if (!preview?.isLocal) return;
    if (preview.posterUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(preview.posterUrl);
    }
    if (preview.videoUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(preview.videoUrl);
    }
  };

  const setLectureVideoPreview = (lectureId: string, nextPreview: LectureVideoPreviewState | null) => {
    setLectureVideoPreviews((previous) => {
      const current = previous[lectureId];
      if (current) {
        revokeLectureVideoPreview(current);
      }

      if (!nextPreview) {
        const nextState = { ...previous };
        delete nextState[lectureId];
        lectureVideoPreviewsRef.current = nextState;
        return nextState;
      }

      const nextState = {
        ...previous,
        [lectureId]: nextPreview,
      };
      lectureVideoPreviewsRef.current = nextState;
      return nextState;
    });
  };

  const clearAllLectureVideoPreviews = () => {
    Object.values(lectureVideoPreviewsRef.current).forEach((preview) => {
      revokeLectureVideoPreview(preview);
    });
    lectureVideoPreviewsRef.current = {};
    setLectureVideoPreviews({});
  };

  const updateActiveLectureDraft = (updater: (current: LectureDraft) => LectureDraft) => {
    if (!selectedLecture) return;

    setLectureDraft((previous) => {
      const baseDraft =
        previous ??
        lectureEditorDrafts[selectedLecture.id]?.lectureDraft ??
        createDefaultLectureDraft(selectedLecture);
      const nextDraft = updater(baseDraft);
      updateLectureEditorDraft(
        selectedLecture.id,
        nextDraft,
        lectureEditorDrafts[selectedLecture.id]?.quizQuestions ?? quizQuestions,
        true
      );
      return nextDraft;
    });
  };

  const updateActiveQuizQuestions = (nextQuestions: QuizQuestion[]) => {
    setQuizQuestions(nextQuestions);

    if (!selectedLecture) return;

    const baseDraft =
      lectureDraft ??
      lectureEditorDrafts[selectedLecture.id]?.lectureDraft ??
      createDefaultLectureDraft(selectedLecture);

    updateLectureEditorDraft(selectedLecture.id, baseDraft, nextQuestions, true);
  };

  useEffect(() => {
    void bootstrapPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [courseId]);

  useEffect(() => {
    return () => {
      if (thumbnailPreviewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(thumbnailPreviewUrl);
      }
    };
  }, [thumbnailPreviewUrl]);

  useEffect(() => {
    lectureVideoPreviewsRef.current = lectureVideoPreviews;
  }, [lectureVideoPreviews]);

  useEffect(() => {
    return () => {
      Object.values(lectureVideoPreviewsRef.current).forEach((preview) => {
        revokeLectureVideoPreview(preview);
      });
    };
  }, []);

  useEffect(() => {
    if (!courseId) {
      setCurrentStep(1);
      return;
    }

    const requestedStep = Number(searchParams.get('step'));
    if (requestedStep === 1 || requestedStep === 2 || requestedStep === 3) {
      setCurrentStep(requestedStep as Step);
    }
  }, [courseId, searchParams]);

  useEffect(() => {
    if (!selectedLectureRef) {
      const firstLecture = getFirstLecture(sections);
      if (firstLecture) {
        setSelectedLectureRef(firstLecture);
      }
      return;
    }

    const lectureStillExists = sections.some(
      (section) =>
        section.id === selectedLectureRef.sectionId &&
        section.lectures.some((lecture) => lecture.id === selectedLectureRef.lectureId)
    );

    if (!lectureStillExists) {
      setSelectedLectureRef(getFirstLecture(sections));
    }
  }, [sections, selectedLectureRef]);

  useEffect(() => {
    if (!selectedLecture) {
      setLectureDraft(null);
      setQuizQuestions([]);
      setPendingUploadDraft(null);
      return;
    }

    const localDraft = lectureEditorDrafts[selectedLecture.id];
    setLectureDraft(localDraft?.lectureDraft ?? createDefaultLectureDraft(selectedLecture));
    setQuizQuestions(localDraft?.quizQuestions ?? parseQuizContent(selectedLecture.content));
    const contextKey = buildLectureUploadContextKey(selectedLecture.id);
    const draft = getMultipartUploadDraft(contextKey);

    if (!draft) {
      setPendingUploadDraft(null);
      return;
    }

    let disposed = false;
    setPendingUploadDraft(draft);

    void (async () => {
      try {
        await multipartUploadApi.getStatus(draft.uploadId, draft.objectKey);
        if (!disposed) {
          setPendingUploadDraft(getMultipartUploadDraft(contextKey));
        }
      } catch {
        clearMultipartUploadDraft(contextKey);
        if (!disposed) {
          setPendingUploadDraft(null);
        }
      }
    })();

    return () => {
      disposed = true;
    };
  }, [selectedLecture?.id, lectureEditorDrafts]);

  const bootstrapPage = async () => {
    setIsBooting(true);
    try {
      const taxonomyData = await courseApi.getTaxonomyOptions();
      setTaxonomy(taxonomyData);

      if (!courseId) {
        setCourse(null);
        setSections([]);
        setBasicFormData(initialBasicFormData);
        setThumbnailPreviewUrl(null);
        clearAllLectureVideoPreviews();
        setSelectedLectureRef(null);
        setSectionDrafts({});
        setExpandedSections({});
        return;
      }

      const [courseData, curriculumData] = await Promise.all([
        courseApi.getInstructorCourse(courseId),
        courseApi.getCourseSections(courseId),
      ]);

      setCourse(courseData);
      setBasicFormData({
        title: courseData.title,
        subtitle: courseData.subtitle ?? '',
        description: courseData.description ?? '',
        thumbnailUrl: courseData.thumbnailUrl ?? '',
        promoVideoUrl: courseData.promoVideoUrl ?? '',
        categoryId: courseData.categoryId ?? '',
        subcategoryId: courseData.subcategoryId ?? '',
        levelId: courseData.levelId ?? '',
        languageId: courseData.languageId ?? '',
      });
      setThumbnailPreviewUrl(null);
      clearAllLectureVideoPreviews();
      setSections(curriculumData);
      setSectionDrafts(
        Object.fromEntries(curriculumData.map((section) => [section.id, section.title]))
      );
      setExpandedSections(
        Object.fromEntries(curriculumData.map((section) => [section.id, true]))
      );
      setSelectedLectureRef(getFirstLecture(curriculumData));
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong tai duoc du lieu khoa hoc', 'error');
    } finally {
      setIsBooting(false);
    }
  };

  const syncSections = (nextSections: CourseSection[]) => {
    const nextMetrics = computeCurriculumStats(nextSections);
    setSections(nextSections);
    setCourse((currentCourse) =>
      currentCourse
        ? {
            ...currentCourse,
            lectureCount: nextMetrics.lectureCount,
            totalVideoDurationSeconds: nextMetrics.videoSeconds,
          }
        : currentCourse
    );
  };

  const handleBasicFieldChange = (key: keyof CreateCourseRequest, value: string) => {
    setBasicFormData((previous) => ({ ...previous, [key]: value }));
  };

  const handleSaveBasics = async (moveToStepTwo = false) => {
    if (!basicFormData.title?.trim() || !basicFormData.description?.trim()) {
      addToast('Hay dien tieu de va mo ta khoa hoc truoc', 'warning');
      return;
    }

    setIsSavingBasics(true);
    try {
      if (!course) {
        const createdCourse = await courseApi.createCourse(basicFormData);
        setCourse(createdCourse);
        navigate(
          `/instructor/courses/${createdCourse.id}${moveToStepTwo ? '?step=2' : ''}`,
          { replace: true }
        );
        addToast('Da tao khung khoa hoc moi', 'success');
      } else {
        const updatedCourse = await courseApi.updateCourse(course.id, basicFormData as UpdateCourseRequest);
        setCourse(updatedCourse);
        addToast('Da cap nhat thong tin khoa hoc', 'success');
      }

      if (moveToStepTwo) {
        setCurrentStep(2);
      }
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong luu duoc thong tin khoa hoc', 'error');
    } finally {
      setIsSavingBasics(false);
    }
  };

  const handleThumbnailUpload = async (files: File[]) => {
    const [file] = files;
    if (!file) return;

    setIsUploadingThumbnail(true);
    const localPreviewUrl = URL.createObjectURL(file);
    setThumbnailPreviewUrl((current) => {
      if (current?.startsWith('blob:')) {
        URL.revokeObjectURL(current);
      }
      return localPreviewUrl;
    });
    try {
      const result = await fileUploadApi.uploadThumbnail(file, setThumbnailProgress);
      setBasicFormData((previous) => ({ ...previous, thumbnailUrl: result.url }));
      addToast('Da tai thumbnail len', 'success');
    } catch (error: any) {
      setThumbnailPreviewUrl((current) => {
        if (current?.startsWith('blob:')) {
          URL.revokeObjectURL(current);
        }
        return null;
      });
      addToast(error?.response?.data?.message || 'Khong tai duoc thumbnail', 'error');
    } finally {
      setIsUploadingThumbnail(false);
    }
  };

  const ensureCourseCreated = () => {
    if (course) return true;
    addToast('Hay luu thong tin nen tang truoc khi tao curriculum', 'warning');
    return false;
  };

  const handleAddSection = async () => {
    if (!ensureCourseCreated() || !course) return;

    setIsCreatingSection(true);
    try {
      const nextIndex = sections.length + 1;
      const newSection = await courseApi.createCourseSection(course.id, {
        title: `Chuong ${nextIndex}`,
        displayOrder: buildNextDisplayOrder(sections),
      });

      const nextSections = [...sections, newSection];
      syncSections(nextSections);
      setSectionDrafts((previous) => ({ ...previous, [newSection.id]: newSection.title }));
      setExpandedSections((previous) => ({ ...previous, [newSection.id]: true }));
      addToast('Da tao chuong moi', 'success');
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong tao duoc chuong', 'error');
    } finally {
      setIsCreatingSection(false);
    }
  };

  const handleSaveSection = async (sectionId: string) => {
    if (!course) return;

    const targetSection = sections.find((section) => section.id === sectionId);
    if (!targetSection) return;

    setSavingSectionId(sectionId);
    try {
      const updatedSection = await courseApi.updateCourseSection(course.id, sectionId, {
        title: sectionDrafts[sectionId]?.trim() || targetSection.title,
        displayOrder: targetSection.displayOrder,
      });

      const nextSections = sections.map((section) =>
        section.id === sectionId ? { ...updatedSection, lectures: section.lectures } : section
      );
      syncSections(nextSections);
      setSectionDrafts((previous) => ({ ...previous, [sectionId]: updatedSection.title }));
      addToast('Da luu chuong', 'success');
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong luu duoc chuong', 'error');
    } finally {
      setSavingSectionId(null);
    }
  };

  const handleDeleteSection = async (sectionId: string) => {
    if (!course) return;

    setDeletingTarget(sectionId);
    try {
      const removedSection = sections.find((section) => section.id === sectionId);
      await courseApi.deleteCourseSection(course.id, sectionId);
      const nextSections = sections.filter((section) => section.id !== sectionId);
      syncSections(nextSections);
      if (removedSection) {
        setLectureEditorDrafts((previous) => {
          const nextState = { ...previous };
          removedSection.lectures.forEach((lecture) => {
            delete nextState[lecture.id];
          });
          return nextState;
        });
        removedSection.lectures.forEach((lecture) => setLectureVideoPreview(lecture.id, null));
      }
      addToast('Da xoa chuong', 'success');
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong xoa duoc chuong', 'error');
    } finally {
      setDeletingTarget(null);
    }
  };

  const handleAddLecture = async (sectionId: string, type: LectureType) => {
    if (!course) return;

    const targetSection = sections.find((section) => section.id === sectionId);
    if (!targetSection) return;

    setSavingSectionId(sectionId);
    try {
      const newLecture = await courseApi.createCourseLecture(course.id, sectionId, {
        title:
          type === LectureType.VIDEO
            ? 'Bai video moi'
            : type === LectureType.ARTICLE
              ? 'Bai viet moi'
              : 'Quiz moi',
        type,
        displayOrder: buildNextDisplayOrder(targetSection.lectures),
        isFreePreview: false,
      });

      const nextSections = sections.map((section) =>
        section.id === sectionId
          ? { ...section, lectures: [...section.lectures, newLecture] }
          : section
      );
      syncSections(nextSections);
      setExpandedSections((previous) => ({ ...previous, [sectionId]: true }));
      setSelectedLectureRef({ sectionId, lectureId: newLecture.id });
      updateLectureEditorDraft(newLecture.id, createDefaultLectureDraft(newLecture), parseQuizContent(newLecture.content), false);
      addToast('Da tao bai giang moi', 'success');
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong tao duoc bai giang', 'error');
    } finally {
      setSavingSectionId(null);
    }
  };

  const handleUploadVideo = async (files: File[]) => {
    const [file] = files;
    if (!file || !selectedLecture || !lectureDraft || !course) return;

    const lectureId = selectedLecture.id;
    const contextKey = buildLectureUploadContextKey(lectureId);
    const durationSeconds = await readVideoDurationSeconds(file);
    const localVideoUrl = URL.createObjectURL(file);
    const localPosterUrl = await createVideoPosterFrame(file);

    setLectureVideoPreview(lectureId, {
      videoUrl: localVideoUrl,
      posterUrl: localPosterUrl ?? undefined,
      fileName: file.name,
      fileSize: file.size,
      durationSeconds,
      isLocal: true,
    });

    if (videoUploadTasksRef.current[lectureId]) {
      await videoUploadTasksRef.current[lectureId].cancel();
      delete videoUploadTasksRef.current[lectureId];
    }

    const task = new MultipartVideoUploadTask(
      file,
      {
        assetType: UploadAssetType.COURSE_VIDEO,
        courseId: course.id,
        lectureId,
        contextKey,
        durationSeconds,
      },
      {
        onProgress: (snapshot) => {
          setVideoUploadState((previous) => ({ ...previous, [lectureId]: snapshot }));
        },
      }
    );

    videoUploadTasksRef.current[lectureId] = task;

    try {
      const completed = await task.start();
      updateActiveLectureDraft((previous) => ({
        ...previous,
        videoUrl: completed.publicUrl,
        durationSeconds:
          previous.durationSeconds.trim().length > 0
            ? previous.durationSeconds
            : completed.durationSeconds
              ? String(completed.durationSeconds)
              : previous.durationSeconds,
      }));
      setPendingUploadDraft(null);
      addToast('Da tai video len object storage. Bam Luu bai giang de gan video vao bai hoc.', 'success');
    } catch (error: any) {
      const status = error?.response?.status;
      if (status === 404 || status === 409 || status === 410 || status === 502) {
        clearMultipartUploadDraft(contextKey);
        setPendingUploadDraft(null);
      }

      if (error?.message === 'Upload cancelled') {
        addToast('Da huy upload video', 'warning');
      } else {
        addToast(error?.response?.data?.message || error?.message || 'Khong tai duoc video', 'error');
      }
      if (!(status === 404 || status === 409 || status === 410 || status === 502)) {
        setPendingUploadDraft(getMultipartUploadDraft(contextKey));
      }
    } finally {
      delete videoUploadTasksRef.current[lectureId];
    }
  };

  const handleCancelVideoUpload = async () => {
    if (!selectedLecture) return;
    const currentTask = videoUploadTasksRef.current[selectedLecture.id];
    if (!currentTask) return;

    await currentTask.cancel();
    delete videoUploadTasksRef.current[selectedLecture.id];
    setPendingUploadDraft(null);
  };

  const handleSaveLecture = async () => {
    if (!course || !selectedLecture || !selectedLectureRef || !lectureDraft) return;
    if (!lectureDraft.title.trim()) {
      addToast('Ten bai giang khong duoc de trong', 'warning');
      return;
    }

    setSavingLectureId(selectedLecture.id);

    try {
      const updatedLecture = await courseApi.updateCourseLecture(
        course.id,
        selectedLectureRef.sectionId,
        selectedLecture.id,
        {
          title: lectureDraft.title.trim(),
          type: lectureDraft.type,
          content:
            lectureDraft.type === LectureType.QUIZ
              ? serializeQuizContent(quizQuestions)
              : lectureDraft.content,
          videoUrl: lectureDraft.type === LectureType.VIDEO ? lectureDraft.videoUrl : undefined,
          durationSeconds:
            lectureDraft.durationSeconds.trim().length > 0
              ? Number(lectureDraft.durationSeconds)
              : undefined,
          displayOrder: selectedLecture.displayOrder,
          isFreePreview: lectureDraft.isFreePreview,
        }
      );

      const nextSections = sections.map((section) =>
        section.id === selectedLectureRef.sectionId
          ? {
              ...section,
              lectures: section.lectures.map((lecture) =>
                lecture.id === updatedLecture.id ? updatedLecture : lecture
              ),
            }
          : section
      );
      syncSections(nextSections);
      updateLectureEditorDraft(
        updatedLecture.id,
        createDefaultLectureDraft(updatedLecture),
        parseQuizContent(updatedLecture.content),
        false
      );
      addToast('Da luu bai giang', 'success');
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong luu duoc bai giang', 'error');
    } finally {
      setSavingLectureId(null);
    }
  };

  const handleDeleteLecture = async () => {
    if (!course || !selectedLectureRef || !selectedLecture) return;

    setDeletingTarget(selectedLecture.id);
    try {
      await courseApi.deleteCourseLecture(course.id, selectedLectureRef.sectionId, selectedLecture.id);

      const nextSections = sections.map((section) =>
        section.id === selectedLectureRef.sectionId
          ? {
              ...section,
              lectures: section.lectures.filter((lecture) => lecture.id !== selectedLecture.id),
            }
          : section
      );
      syncSections(nextSections);
      removeLectureEditorDraft(selectedLecture.id);
      setLectureVideoPreview(selectedLecture.id, null);
      addToast('Da xoa bai giang', 'success');
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong xoa duoc bai giang', 'error');
    } finally {
      setDeletingTarget(null);
    }
  };

  const handleSubmitForReview = async () => {
    if (!course) {
      addToast('Hay tao khoa hoc truoc khi gui duyet', 'warning');
      return;
    }

    setIsSubmittingReview(true);
    try {
      const updatedCourse = await courseApi.submitForReview(course.id);
      setCourse(updatedCourse);
      addToast('Da gui khoa hoc de duyet', 'success');
      navigate('/instructor/courses');
    } catch (error: any) {
      addToast(error?.response?.data?.message || 'Khong gui duyet duoc', 'error');
    } finally {
      setIsSubmittingReview(false);
    }
  };

  const renderStepOne = () => (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm"
      >
        <div className="mb-6 flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold text-gray-900">Thong tin nen tang</p>
            <p className="mt-1 text-sm text-gray-500">
              Hoan thien profile khoa hoc de hoc vien hieu ngay khoa hoc nay danh cho ai.
            </p>
          </div>
          {course ? (
            <Badge variant={courseStatusVariant[course.status] ?? 'default'}>
              {courseStatusLabel[course.status] ?? course.status}
            </Badge>
          ) : null}
        </div>

        <div className="grid gap-5 md:grid-cols-2">
          <div className="md:col-span-2">
            <Input
              label="Ten khoa hoc"
              value={basicFormData.title}
              onChange={(event) => handleBasicFieldChange('title', event.target.value)}
              placeholder="Vi du: React tu co ban den production"
            />
          </div>

          <div className="md:col-span-2">
            <Input
              label="Phu de"
              value={basicFormData.subtitle}
              onChange={(event) => handleBasicFieldChange('subtitle', event.target.value)}
              placeholder="Goi ro ket qua hoc tap va doi tuong phu hop"
            />
          </div>

          <div className="md:col-span-2">
            <Textarea
              label="Mo ta khoa hoc"
              value={basicFormData.description}
              onChange={(event) => handleBasicFieldChange('description', event.target.value)}
              placeholder="Giai thich khoa hoc nay giai quyet van de gi, hoc vien se lam duoc gi sau khi hoc xong."
              rows={6}
              maxLength={1600}
              showCount
            />
          </div>

          <Select
            label="Danh muc chinh"
            value={basicFormData.categoryId || undefined}
            placeholder="Chon danh muc"
            onChange={(value) =>
              setBasicFormData((previous) => ({
                ...previous,
                categoryId: value,
                subcategoryId: '',
              }))
            }
            options={taxonomy.categories.map((category) => ({
              value: category.id,
              label: category.name,
            }))}
          />

          <Select
            label="Phan loai con"
            value={basicFormData.subcategoryId || undefined}
            placeholder="Chon phan loai con"
            disabled={!basicFormData.categoryId}
            onChange={(value) => handleBasicFieldChange('subcategoryId', value)}
            options={filteredSubcategories.map((subcategory) => ({
              value: subcategory.id,
              label: subcategory.name,
            }))}
          />

          <Select
            label="Trinh do"
            value={basicFormData.levelId || undefined}
            placeholder="Chon trinh do"
            onChange={(value) => handleBasicFieldChange('levelId', value)}
            options={taxonomy.levels.map((level) => ({
              value: level.id,
              label: level.label,
            }))}
          />

          <Select
            label="Ngon ngu"
            value={basicFormData.languageId || undefined}
            placeholder="Chon ngon ngu"
            onChange={(value) => handleBasicFieldChange('languageId', value)}
            options={taxonomy.languages.map((language) => ({
              value: language.id,
              label: language.label,
            }))}
          />

          <div className="md:col-span-2">
            <Input
              label="Video gioi thieu"
              value={basicFormData.promoVideoUrl}
              onChange={(event) => handleBasicFieldChange('promoVideoUrl', event.target.value)}
              placeholder="https://youtube.com/..."
            />
          </div>

          <div className="md:col-span-2 rounded-3xl border border-gray-200 bg-gray-50 p-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-gray-900">Thumbnail khoa hoc</p>
                <p className="mt-1 text-sm text-gray-500">
                  Nen co anh nen doc ro o tile 16:9, uu tien du an that hoac mockup khoa hoc.
                </p>
              </div>
              {basicFormData.thumbnailUrl || thumbnailPreviewUrl ? (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setThumbnailPreviewUrl((current) => {
                      if (current?.startsWith('blob:')) {
                        URL.revokeObjectURL(current);
                      }
                      return null;
                    });
                    setBasicFormData((previous) => ({ ...previous, thumbnailUrl: '' }));
                  }}
                >
                  <ImagePlus className="h-4 w-4" />
                  Chon anh khac
                </Button>
              ) : null}
            </div>

            <div className="mt-5">
              {basicFormData.thumbnailUrl || thumbnailPreviewUrl ? (
                <div className="overflow-hidden rounded-3xl border border-gray-200 bg-white">
                  <img
                    src={thumbnailPreviewUrl ?? basicFormData.thumbnailUrl}
                    alt="Thumbnail khoa hoc"
                    className="aspect-video w-full object-cover"
                  />
                </div>
              ) : (
                <FileDropzone
                  accept="image/*"
                  progress={isUploadingThumbnail ? thumbnailProgress : undefined}
                  onFiles={handleThumbnailUpload}
                  className="min-h-[220px] rounded-3xl bg-white"
                />
              )}
            </div>
          </div>
        </div>

        <div className="mt-8 flex flex-wrap gap-3">
          <Button type="button" variant="secondary" onClick={() => navigate('/instructor/courses')}>
            <ArrowLeft className="h-4 w-4" />
            Ve danh sach khoa hoc
          </Button>
          <Button type="button" onClick={() => void handleSaveBasics(true)} loading={isSavingBasics}>
            <Save className="h-4 w-4" />
            Luu va sang curriculum
          </Button>
        </div>
      </motion.div>

      <div className="space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.04 }}
          className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm"
        >
          <div className="flex items-center gap-3">
            <div className="rounded-2xl bg-primary-50 p-3 text-primary-700">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-900">Muc do hoan thien</p>
              <p className="text-sm text-gray-500">Builder se khoe hon khi phan mo ta va taxonomy ro rang.</p>
            </div>
          </div>

          <div className="mt-5">
            <ProgressBar value={basicCompletion} showLabel />
          </div>

          <div className="mt-5 space-y-3">
            {[
              ['Tieu de', Boolean(basicFormData.title?.trim())],
              ['Mo ta', Boolean(basicFormData.description?.trim())],
              ['Thumbnail', Boolean(basicFormData.thumbnailUrl)],
              ['Danh muc', Boolean(basicFormData.categoryId)],
              ['Trinh do', Boolean(basicFormData.levelId)],
              ['Ngon ngu', Boolean(basicFormData.languageId)],
            ].map(([label, done]) => (
              <div key={String(label)} className="flex items-center justify-between rounded-2xl bg-gray-50 px-4 py-3">
                <span className="text-sm text-gray-600">{label}</span>
                <CheckCircle2 className={cn('h-4 w-4', done ? 'text-emerald-600' : 'text-gray-300')} />
              </div>
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );

  const renderLectureEditor = () => {
    if (!selectedLecture || !lectureDraft) {
	    return (
        <div className="flex min-h-[480px] flex-col items-center justify-center rounded-3xl border border-dashed border-gray-300 bg-white px-6 py-12 text-center shadow-sm xl:h-full">
          <Layers3 className="h-10 w-10 text-gray-300" />
          <p className="mt-4 text-sm font-semibold text-gray-800">Chon mot bai giang de chinh sua</p>
          <p className="mt-1 max-w-sm text-sm text-gray-500">
            Moi bai giang se co editor rieng cho video, bai viet hoac quiz. Moi thao tac luu se co phan hoi ngay.
          </p>
        </div>
      );
    }

    return (
      <motion.div
        key={selectedLecture.id}
        initial={{ opacity: 0, x: 24 }}
        animate={{ opacity: 1, x: 0 }}
        exit={{ opacity: 0, x: -12 }}
        className="flex min-h-[560px] flex-col rounded-3xl border border-gray-200 bg-white p-6 shadow-sm xl:h-full xl:min-h-0"
      >
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <p className="text-base font-semibold text-gray-900">{selectedLecture.title}</p>
              <Badge variant={selectedLecture.type === 'VIDEO' ? 'info' : selectedLecture.type === 'ARTICLE' ? 'default' : 'warning'}>
                {lectureTypeLabels[selectedLecture.type]}
              </Badge>
              {selectedLectureEditorDraft?.dirty ? <Badge variant="warning">Chua luu</Badge> : null}
            </div>
            <p className="mt-1 text-sm text-gray-500">
              Cap nhat noi dung, asset va cach hoc vien tiep can bai hoc nay.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <Button type="button" variant="danger" onClick={() => void handleDeleteLecture()} loading={deletingTarget === selectedLecture.id}>
              <Trash2 className="h-4 w-4" />
              Xoa bai giang
            </Button>
            <Button type="button" onClick={() => void handleSaveLecture()} loading={savingLectureId === selectedLecture.id}>
              <Save className="h-4 w-4" />
              Luu bai giang
            </Button>
          </div>
        </div>

        <div className="mt-6 flex-1 space-y-6 overflow-y-auto pr-1">
          <div className="grid gap-5 xl:grid-cols-2">
            <Input
              label="Ten bai giang"
              value={lectureDraft.title}
              onChange={(event) => updateActiveLectureDraft((previous) => ({ ...previous, title: event.target.value }))}
              placeholder="Dat ten theo ket qua hoc tap cu the"
            />

            <Input
              label="Thoi luong (giay)"
              value={lectureDraft.durationSeconds}
              type="number"
              onChange={(event) =>
                updateActiveLectureDraft((previous) => ({ ...previous, durationSeconds: event.target.value }))
              }
              placeholder="600"
            />
          </div>

          <div className="flex flex-wrap items-center gap-4 rounded-2xl border border-gray-200 bg-gray-50 px-4 py-4">
            <Toggle
              checked={lectureDraft.isFreePreview}
              onChange={(checked) =>
                updateActiveLectureDraft((previous) => ({ ...previous, isFreePreview: checked }))
              }
              label="Cho phep hoc vien xem thu bai nay"
            />
            <Badge variant="default">Loai: {lectureTypeLabels[lectureDraft.type]}</Badge>
          </div>

	        {lectureDraft.type === LectureType.VIDEO ? (
	          <div className="space-y-5">
              <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-gray-900">Media bai giang</p>
                    <p className="mt-1 text-sm text-gray-500">
                      Sau khi upload, giao dien giu lai preview de ban kiem tra nhanh truoc khi luu bai giang.
                    </p>
                  </div>
                  {lectureDraft.videoUrl ? (
                    <a
                      href={lectureDraft.videoUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center rounded-xl border border-gray-200 bg-white px-3 py-2 text-sm font-medium text-gray-700 transition hover:bg-gray-50"
                    >
                      Mo tep video
                    </a>
                  ) : null}
                </div>

                <div className="mt-4 overflow-hidden rounded-[28px] border border-gray-200 bg-gray-950">
                  <div className="relative aspect-video w-full">
                    {selectedLectureVideoPreview?.posterUrl ? (
                      <img
                        src={selectedLectureVideoPreview.posterUrl}
                        alt="Thumbnail bai giang"
                        className="h-full w-full object-cover"
                      />
                    ) : selectedLectureVideoPreview?.videoUrl ? (
                      <video
                        src={selectedLectureVideoPreview.videoUrl}
                        controls
                        preload="metadata"
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-950">
                        <div className="text-center">
                          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl border border-white/10 bg-white/5 text-white">
                            <Video className="h-6 w-6" />
                          </div>
                          <p className="mt-4 text-sm font-semibold text-white">Chua co media duoc gan</p>
                          <p className="mt-1 text-xs text-gray-300">
                            Upload video de xem cover va thong tin media ngay tai day.
                          </p>
                        </div>
                      </div>
                    )}

                    <div className="pointer-events-none absolute inset-x-0 bottom-0 flex items-end justify-between bg-gradient-to-t from-black/70 via-black/20 to-transparent p-4">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-white/70">Lecture media</p>
                        <p className="mt-1 max-w-[70%] truncate text-sm font-semibold text-white">
                          {selectedLectureVideoPreview?.fileName || lectureDraft.title || 'Video bai giang'}
                        </p>
                      </div>
                      <Badge variant={lectureDraft.videoUrl ? 'success' : 'default'}>
                        {lectureDraft.videoUrl ? 'Da gan video' : 'Cho upload'}
                      </Badge>
                    </div>
                  </div>
                </div>

                <div className="mt-4 grid gap-3 sm:grid-cols-3">
                  <div className="rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-gray-400">Tep</p>
                    <p className="mt-2 truncate text-sm font-semibold text-gray-900">
                      {selectedLectureVideoPreview?.fileName || 'Chua co'}
                    </p>
                  </div>
                  <div className="rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-gray-400">Dung luong</p>
                    <p className="mt-2 text-sm font-semibold text-gray-900">
                      {selectedLectureVideoPreview?.fileSize ? formatBytes(selectedLectureVideoPreview.fileSize) : 'Dang cho'}
                    </p>
                  </div>
                  <div className="rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-gray-400">Thoi luong</p>
                    <p className="mt-2 text-sm font-semibold text-gray-900">
                      {selectedLectureVideoPreview?.durationSeconds
                        ? formatSeconds(selectedLectureVideoPreview.durationSeconds)
                        : lectureDraft.durationSeconds.trim().length > 0
                          ? formatSeconds(Number(lectureDraft.durationSeconds))
                          : 'Dang do'}
                    </p>
                  </div>
                </div>
              </div>

	            <div>
	              <p className="mb-2 text-sm font-medium text-gray-700">Tai video truc tiep</p>
	              <FileDropzone
	                accept="video/mp4,video/webm,video/quicktime"
	                progress={videoUploadState[selectedLecture.id]?.overallProgress}
	                onFiles={handleUploadVideo}
	                className="rounded-3xl"
	              />
                  <div className="mt-3 space-y-3 rounded-2xl border border-gray-200 bg-gray-50 p-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-gray-900">Direct multipart upload</p>
                        <p className="mt-1 text-xs text-gray-500">
                          Video duoc chia chunk lon va presign theo lo de giam request vao backend, sau do upload truc tiep len MinIO.
                        </p>
                      </div>
                      {videoUploadState[selectedLecture.id]?.phase === 'uploading' ||
                      videoUploadState[selectedLecture.id]?.phase === 'starting' ||
                      videoUploadState[selectedLecture.id]?.phase === 'completing' ? (
                        <Button type="button" variant="secondary" onClick={() => void handleCancelVideoUpload()}>
                          Huy upload
                        </Button>
                      ) : null}
                    </div>

                    {pendingUploadDraft ? (
                      <div className="rounded-2xl border border-amber-200 bg-amber-50 px-3 py-3 text-xs text-amber-800">
                        Phat hien session upload chua hoan tat cho bai giang nay. Neu tab van mo, he thong se tu tiep tuc khi mang quay lai. Neu da tai lai trang, chon lai dung file video cu de resume, he thong chi upload cac part con thieu.
                      </div>
                    ) : null}

                    {videoUploadState[selectedLecture.id] ? (
                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-xs text-gray-600">
                          <span>{videoUploadState[selectedLecture.id].message || 'San sang upload'}</span>
                          <span>{videoUploadState[selectedLecture.id].overallProgress}%</span>
                        </div>
                        <ProgressBar value={videoUploadState[selectedLecture.id].overallProgress} />
                        <div className="flex flex-wrap gap-3 text-xs text-gray-500">
                          <span>
                            {formatBytes(videoUploadState[selectedLecture.id].uploadedBytes)} /{' '}
                            {formatBytes(videoUploadState[selectedLecture.id].totalBytes)}
                          </span>
                          <span>
                            {videoUploadState[selectedLecture.id].completedPartCount}/
                            {videoUploadState[selectedLecture.id].totalPartCount} part da xong
                          </span>
                          <span>
                            Toc do: {formatBytes(videoUploadState[selectedLecture.id].speedBytesPerSecond)}/s
                          </span>
                          <span>
                            Chunk: {formatBytes(videoUploadState[selectedLecture.id].chunkSizeBytes)}
                          </span>
                          <span>
                            Luong song song: {videoUploadState[selectedLecture.id].activePartCount}/
                            {videoUploadState[selectedLecture.id].maxConcurrency ?? 0}
                          </span>
                          <span>Trang thai: {videoUploadState[selectedLecture.id].phase}</span>
                        </div>
                      </div>
                    ) : null}
                  </div>
	            </div>
	          </div>
	        ) : null}

          {lectureDraft.type === LectureType.ARTICLE ? (
            <div>
              <p className="mb-2 text-sm font-medium text-gray-700">Noi dung bai viet</p>
              <RichTextEditor
                value={lectureDraft.content}
                onChange={(value) => updateActiveLectureDraft((previous) => ({ ...previous, content: value }))}
              />
            </div>
          ) : null}

          {lectureDraft.type === LectureType.QUIZ ? (
            <div>
              <QuizBuilder value={quizQuestions} onChange={updateActiveQuizQuestions} />
            </div>
          ) : null}
        </div>
      </motion.div>
    );
  };

  const renderStepTwo = () => (
    <div
      className={cn(
        'grid gap-6 xl:min-h-[calc(100vh-10rem)]',
        isCurriculumCollapsed
          ? 'xl:grid-cols-[96px_minmax(0,1fr)]'
          : 'xl:grid-cols-[360px_minmax(0,1fr)]'
      )}
    >
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex min-h-[420px] flex-col overflow-hidden rounded-3xl border border-gray-200 bg-white p-5 shadow-sm xl:h-[calc(100vh-10rem)]"
      >
        <div className={cn('flex items-start gap-3', isCurriculumCollapsed ? 'justify-center' : 'justify-between')}>
          {isCurriculumCollapsed ? (
            <div className="flex flex-col items-center gap-3">
              <Button
                type="button"
                variant="secondary"
                className="w-full px-0"
                onClick={() => setIsCurriculumCollapsed(false)}
                aria-label="Mo rong curriculum"
              >
                <PanelLeftOpen className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="secondary"
                className="w-full px-0"
                onClick={() => void handleAddSection()}
                loading={isCreatingSection}
                aria-label="Them chuong"
              >
                <FolderPlus className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <>
              <div>
                <p className="text-sm font-semibold text-gray-900">Curriculum</p>
                <p className="mt-1 text-sm text-gray-500">
                  Sap xep chuong, tao bai giang va giu mot flow hoc tap ro rang.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button type="button" variant="secondary" onClick={() => void handleAddSection()} loading={isCreatingSection}>
                  <FolderPlus className="h-4 w-4" />
                  Them chuong
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  className="px-3"
                  onClick={() => setIsCurriculumCollapsed(true)}
                  aria-label="Thu gon curriculum"
                >
                  <PanelLeftClose className="h-4 w-4" />
                </Button>
              </div>
            </>
          )}
        </div>

        {isCurriculumCollapsed ? (
          <div className="mt-5 flex flex-1 flex-col items-center gap-3 overflow-y-auto">
            {[
              ['C', curriculumStats.sectionCount],
              ['B', curriculumStats.lectureCount],
              ['P', `${Math.floor(curriculumStats.videoSeconds / 60)}`],
            ].map(([label, value]) => (
              <div key={String(label)} className="flex w-full flex-col items-center rounded-2xl border border-gray-200 bg-gray-50 px-2 py-3">
                <span className="text-[11px] font-semibold uppercase tracking-[0.14em] text-gray-400">{label}</span>
                <span className="mt-2 text-sm font-semibold text-gray-900">{value}</span>
              </div>
            ))}

            {selectedLecture ? (
              <div className="w-full rounded-2xl border border-primary-200 bg-primary-50 px-3 py-4 text-center">
                <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-primary-600">Dang mo</p>
                <p className="mt-2 text-xs font-semibold text-gray-900">{lectureTypeLabels[selectedLecture.type]}</p>
                <p className="mt-1 line-clamp-3 text-xs text-gray-600">{lectureDraft?.title ?? selectedLecture.title}</p>
                {selectedLectureEditorDraft?.dirty ? (
                  <Badge variant="warning" className="mt-3">Chua luu</Badge>
                ) : null}
              </div>
            ) : (
              <div className="w-full rounded-2xl border border-dashed border-gray-300 bg-gray-50 px-3 py-6 text-center">
                <Layers3 className="mx-auto h-4 w-4 text-gray-400" />
              </div>
            )}
          </div>
        ) : (
          <>
            <div className="mt-5 grid grid-cols-3 gap-3">
              {[
                ['Chuong', curriculumStats.sectionCount],
                ['Bai giang', curriculumStats.lectureCount],
                ['Thoi luong', `${Math.floor(curriculumStats.videoSeconds / 60)} ph`],
              ].map(([label, value]) => (
                <div key={String(label)} className="rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.14em] text-gray-400">{label}</p>
                  <p className="mt-2 text-lg font-semibold text-gray-900">{value}</p>
                </div>
              ))}
            </div>

            <div className="mt-5 min-h-0 flex-1 space-y-4 overflow-y-auto pr-1">
              {sections.length === 0 ? (
                <div className="rounded-3xl border border-dashed border-gray-300 bg-gray-50 px-6 py-12 text-center">
                  <BookOpen className="mx-auto h-10 w-10 text-gray-300" />
                  <p className="mt-4 text-sm font-semibold text-gray-800">Chua co chuong nao</p>
                  <p className="mt-1 text-sm text-gray-500">
                    Tao chuong dau tien de bat dau dung hanh trinh hoc tap.
                  </p>
                </div>
              ) : null}

              {sections.map((section, sectionIndex) => {
                const isExpanded = expandedSections[section.id] ?? true;
                return (
                  <motion.div
                    key={section.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="rounded-3xl border border-gray-200 bg-white"
                  >
                    <button
                      type="button"
                      onClick={() =>
                        setExpandedSections((previous) => ({
                          ...previous,
                          [section.id]: !isExpanded,
                        }))
                      }
                      className="flex w-full items-center justify-between gap-3 px-5 py-4 text-left"
                    >
                      <div className="flex items-center gap-3">
                        <div className="rounded-2xl bg-gray-100 p-2 text-gray-500">
                          <GripVertical className="h-4 w-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold uppercase tracking-[0.14em] text-gray-400">
                            Chuong {sectionIndex + 1}
                          </p>
                          <p className="text-sm font-semibold text-gray-900">{section.title}</p>
                        </div>
                      </div>
                      {isExpanded ? (
                        <ChevronDown className="h-4 w-4 text-gray-400" />
                      ) : (
                        <ChevronRight className="h-4 w-4 text-gray-400" />
                      )}
                    </button>

                    <AnimatePresence initial={false}>
                      {isExpanded ? (
                        <motion.div
                          initial={{ height: 0, opacity: 0 }}
                          animate={{ height: 'auto', opacity: 1 }}
                          exit={{ height: 0, opacity: 0 }}
                          className="overflow-hidden border-t border-gray-100"
                        >
                          <div className="space-y-4 px-5 py-4">
                            <div className="flex gap-3">
                              <Input
                                value={sectionDrafts[section.id] ?? section.title}
                                onChange={(event) =>
                                  setSectionDrafts((previous) => ({
                                    ...previous,
                                    [section.id]: event.target.value,
                                  }))
                                }
                                placeholder="Ten chuong"
                              />
                              <Button
                                type="button"
                                variant="secondary"
                                onClick={() => void handleSaveSection(section.id)}
                                loading={savingSectionId === section.id}
                              >
                                <Save className="h-4 w-4" />
                              </Button>
                              <Button
                                type="button"
                                variant="danger"
                                onClick={() => void handleDeleteSection(section.id)}
                                loading={deletingTarget === section.id}
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>

                            <div className="flex flex-wrap gap-2">
                              <Button type="button" size="sm" variant="secondary" onClick={() => void handleAddLecture(section.id, LectureType.VIDEO)}>
                                <Video className="h-4 w-4" />
                                Video
                              </Button>
                              <Button type="button" size="sm" variant="secondary" onClick={() => void handleAddLecture(section.id, LectureType.ARTICLE)}>
                                <FileText className="h-4 w-4" />
                                Bai viet
                              </Button>
                              <Button type="button" size="sm" variant="secondary" onClick={() => void handleAddLecture(section.id, LectureType.QUIZ)}>
                                <CheckSquare className="h-4 w-4" />
                                Quiz
                              </Button>
                            </div>

                            <div className="space-y-2">
                              {section.lectures.map((lecture) => {
                                const active =
                                  selectedLectureRef?.sectionId === section.id &&
                                  selectedLectureRef.lectureId === lecture.id;
                                const localLectureDraft = lectureEditorDrafts[lecture.id];
                                return (
                                  <button
                                    type="button"
                                    key={lecture.id}
                                    onClick={() =>
                                      setSelectedLectureRef({ sectionId: section.id, lectureId: lecture.id })
                                    }
                                    className={cn(
                                      'flex w-full items-center gap-3 rounded-2xl border px-4 py-3 text-left transition',
                                      active
                                        ? 'border-primary-200 bg-primary-50'
                                        : 'border-gray-200 bg-gray-50 hover:border-primary-200 hover:bg-white'
                                    )}
                                  >
                                    <div className="rounded-2xl bg-white p-2 text-primary-700 shadow-sm">
                                      {lecture.type === LectureType.VIDEO ? (
                                        <Video className="h-4 w-4" />
                                      ) : lecture.type === LectureType.ARTICLE ? (
                                        <FileText className="h-4 w-4" />
                                      ) : (
                                        <CheckSquare className="h-4 w-4" />
                                      )}
                                    </div>
                                    <div className="min-w-0 flex-1">
                                      <p className="truncate text-sm font-medium text-gray-900">
                                        {localLectureDraft?.lectureDraft.title || lecture.title}
                                      </p>
                                      <p className="mt-1 text-xs text-gray-500">
                                        {lectureTypeLabels[lecture.type]}
                                        {lecture.durationSeconds ? ` • ${formatSeconds(lecture.durationSeconds)}` : ''}
                                      </p>
                                    </div>
                                    <div className="flex items-center gap-2">
                                      {localLectureDraft?.dirty ? <Badge variant="warning">Chua luu</Badge> : null}
                                      {lecture.isFreePreview ? (
                                        <Badge variant="info">Preview</Badge>
                                      ) : null}
                                    </div>
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        </motion.div>
                      ) : null}
                    </AnimatePresence>
                  </motion.div>
                );
              })}
            </div>
          </>
        )}
      </motion.div>

      <div className="min-h-0 xl:h-[calc(100vh-10rem)]">
        <AnimatePresence mode="wait">{renderLectureEditor()}</AnimatePresence>
      </div>
    </div>
  );

  const renderStepThree = () => (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold text-gray-900">Kiem tra chat luong khoa hoc</p>
            <p className="mt-1 text-sm text-gray-500">
              Day la checkpoint cuoi de tranh gui mot khoa hoc thieu metadata hoac thieu noi dung.
            </p>
          </div>
          {course ? (
            <Badge variant={courseStatusVariant[course.status] ?? 'default'}>
              {courseStatusLabel[course.status] ?? course.status}
            </Badge>
          ) : null}
        </div>

        <div className="mt-6 space-y-4">
          {reviewChecks.map((item) => (
            <div key={item.label} className="flex items-start gap-4 rounded-2xl border border-gray-200 px-5 py-4">
              <div className={cn('mt-0.5 rounded-full p-1', item.passed ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-400')}>
                <CheckCircle2 className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-gray-900">{item.label}</p>
                <p className="mt-1 text-sm text-gray-500">{item.detail}</p>
              </div>
            </div>
          ))}
        </div>

        {course?.rejectionReason ? (
          <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 px-5 py-4">
            <p className="text-sm font-semibold text-red-700">Ly do can chinh sua tu dot duyet truoc</p>
            <p className="mt-1 text-sm text-red-600">{course.rejectionReason}</p>
          </div>
        ) : null}

        <div className="mt-8 flex flex-wrap gap-3">
          <Button type="button" variant="secondary" onClick={() => setCurrentStep(2)}>
            <ArrowLeft className="h-4 w-4" />
            Quay lai curriculum
          </Button>
          <Button
            type="button"
            onClick={() => void handleSubmitForReview()}
            loading={isSubmittingReview}
            disabled={reviewCompletion < 100}
          >
            <Save className="h-4 w-4" />
            Gui de duyet
          </Button>
        </div>
      </motion.div>

      <div className="space-y-6">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm"
        >
          <p className="text-sm font-semibold text-gray-900">Do san sang</p>
          <p className="mt-1 text-sm text-gray-500">Tien do thuc te dua tren cac yeu cau review cua backend.</p>
          <div className="mt-5">
            <ProgressBar value={reviewCompletion} showLabel />
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.04 }}
          className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm"
        >
          <p className="text-sm font-semibold text-gray-900">Tom tat curriculum</p>
          <div className="mt-4 space-y-3">
            {[
              ['Chuong', curriculumStats.sectionCount],
              ['Bai giang', curriculumStats.lectureCount],
              ['Tong video', formatSeconds(curriculumStats.videoSeconds)],
            ].map(([label, value]) => (
              <div key={String(label)} className="flex items-center justify-between rounded-2xl bg-gray-50 px-4 py-3">
                <span className="text-sm text-gray-600">{label}</span>
                <span className="text-sm font-semibold text-gray-900">{value}</span>
              </div>
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );

  return (
    <InstructorWorkspaceLayout
      title={course ? `Builder: ${course.title}` : 'Tao khoa hoc moi'}
      description="Khung tao khoa hoc duoc thiet ke lai theo huong workspace: nhap metadata, dung curriculum va gui review trong cung mot flow co phan hoi ro rang."
      actions={
        <div className="flex flex-wrap gap-3">
          <Button type="button" variant="secondary" onClick={() => navigate('/instructor/courses')}>
            <ArrowLeft className="h-4 w-4" />
            Danh sach khoa hoc
          </Button>
          <Button
            type="button"
            onClick={() => void handleSaveBasics(currentStep === 1)}
            loading={isSavingBasics}
            disabled={currentStep !== 1}
          >
            <Save className="h-4 w-4" />
            Luu va sang curriculum
          </Button>
        </div>
      }
    >
      <div className="space-y-6">
        <div className="rounded-3xl border border-gray-200 bg-white p-4 shadow-sm">
          <div className="grid gap-4 md:grid-cols-3">
            {steps.map((step) => {
              const Icon = step.icon;
              const isActive = currentStep === step.id;
              const isCompleted = currentStep > step.id;
              return (
                <button
                  key={step.id}
                  type="button"
                  onClick={() => setCurrentStep(step.id)}
                  className={cn(
                    'flex items-center gap-4 rounded-2xl border px-4 py-4 text-left transition',
                    isActive
                      ? 'border-primary-200 bg-primary-50'
                      : 'border-gray-200 bg-gray-50 hover:bg-white',
                    isCompleted && 'border-emerald-200 bg-emerald-50'
                  )}
                >
                  <div
                    className={cn(
                      'flex h-11 w-11 items-center justify-center rounded-2xl',
                      isActive
                        ? 'bg-primary-600 text-white'
                        : isCompleted
                          ? 'bg-emerald-600 text-white'
                          : 'bg-white text-gray-500 shadow-sm'
                    )}
                  >
                    {isCompleted ? <CheckCircle2 className="h-5 w-5" /> : <Icon className="h-5 w-5" />}
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-gray-900">{step.label}</p>
                    <p className="mt-1 text-sm text-gray-500">{step.detail}</p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {isBooting ? (
          <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
            <SkeletonCard className="h-[520px] rounded-3xl" lines={5} />
            <SkeletonCard className="h-[520px] rounded-3xl" lines={4} />
          </div>
        ) : currentStep === 1 ? (
          renderStepOne()
        ) : currentStep === 2 ? (
          renderStepTwo()
        ) : (
          renderStepThree()
        )}

        {!isBooting ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-3xl border border-gray-200 bg-white px-5 py-4 shadow-sm">
            <p className="text-sm text-gray-500">
              {currentStep === 1
                ? 'Khi metadata on dinh, builder se mo duong sang curriculum.'
                : currentStep === 2
                  ? 'Draft tung bai giang duoc giu lai ngay trong workspace; bam Luu bai giang khi muon day xuong backend va cap nhat lecture count.'
                  : 'Neu chua dat 100%, backend se tu choi submit va tra lai thong diep loi cu the.'}
            </p>
            <div className="flex flex-wrap gap-3">
              {currentStep > 1 ? (
                <Button type="button" variant="secondary" onClick={() => setCurrentStep((currentStep - 1) as Step)}>
                  <ArrowLeft className="h-4 w-4" />
                  Quay lai
                </Button>
              ) : null}

              {currentStep === 1 ? (
                <Button type="button" onClick={() => void handleSaveBasics(true)} loading={isSavingBasics}>
                  <ArrowRight className="h-4 w-4" />
                  Sang curriculum
                </Button>
              ) : null}

              {currentStep === 2 ? (
                <Button
                  type="button"
                  onClick={() => {
                    if (curriculumStats.lectureCount === 0) {
                      addToast('Hay tao it nhat mot bai giang truoc khi review', 'warning');
                      return;
                    }
                    setCurrentStep(3);
                  }}
                >
                  <ArrowRight className="h-4 w-4" />
                  Sang review
                </Button>
              ) : null}
            </div>
          </div>
        ) : null}
      </div>
    </InstructorWorkspaceLayout>
  );
}
