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
import {
  MultipartVideoUploadTask,
  readVideoDurationSeconds,
} from '../../services/multipartUploadService';
import { getMultipartUploadDraft } from '../../services/multipartUploadStore';
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
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([]);
  const [thumbnailProgress, setThumbnailProgress] = useState(0);
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

  useEffect(() => {
    void bootstrapPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [courseId]);

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

    setLectureDraft(createDefaultLectureDraft(selectedLecture));
    setQuizQuestions(parseQuizContent(selectedLecture.content));
    setPendingUploadDraft(getMultipartUploadDraft(buildLectureUploadContextKey(selectedLecture.id)));
  }, [selectedLecture?.id]);

  const bootstrapPage = async () => {
    setIsBooting(true);
    try {
      const taxonomyData = await courseApi.getTaxonomyOptions();
      setTaxonomy(taxonomyData);

      if (!courseId) {
        setCourse(null);
        setSections([]);
        setBasicFormData(initialBasicFormData);
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
    try {
      const url = await fileUploadApi.uploadThumbnail(file, setThumbnailProgress);
      setBasicFormData((previous) => ({ ...previous, thumbnailUrl: url }));
      addToast('Da tai thumbnail len', 'success');
    } catch (error: any) {
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
      await courseApi.deleteCourseSection(course.id, sectionId);
      const nextSections = sections.filter((section) => section.id !== sectionId);
      syncSections(nextSections);
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

    if (videoUploadTasksRef.current[lectureId]) {
      await videoUploadTasksRef.current[lectureId].cancel();
      delete videoUploadTasksRef.current[lectureId];
    }

    const durationSeconds = await readVideoDurationSeconds(file);
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
      setLectureDraft((previous) =>
        previous
          ? {
              ...previous,
              videoUrl: completed.publicUrl,
              durationSeconds:
                previous.durationSeconds.trim().length > 0
                  ? previous.durationSeconds
                  : completed.durationSeconds
                    ? String(completed.durationSeconds)
                    : previous.durationSeconds,
            }
          : previous
      );
      setPendingUploadDraft(null);
      addToast('Da tai video len object storage. Bam Luu bai giang de gan video vao bai hoc.', 'success');
    } catch (error: any) {
      if (error?.message === 'Upload cancelled') {
        addToast('Da huy upload video', 'warning');
      } else {
        addToast(error?.response?.data?.message || error?.message || 'Khong tai duoc video', 'error');
      }
      setPendingUploadDraft(getMultipartUploadDraft(contextKey));
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

        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.08 }}
          className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm"
        >
          <p className="text-sm font-semibold text-gray-900">Thumbnail khoa hoc</p>
          <p className="mt-1 text-sm text-gray-500">
            Nen co anh nen doc ro o tile 16:9, uu tien du an that hoac mockup khoa hoc.
          </p>

          <div className="mt-5">
            {basicFormData.thumbnailUrl ? (
              <div className="overflow-hidden rounded-3xl border border-gray-200">
                <img
                  src={basicFormData.thumbnailUrl}
                  alt="Thumbnail khoa hoc"
                  className="aspect-video w-full object-cover"
                />
              </div>
            ) : (
              <FileDropzone
                accept="image/*"
                progress={isUploadingThumbnail ? thumbnailProgress : undefined}
                onFiles={handleThumbnailUpload}
                className="min-h-[220px] rounded-3xl"
              />
            )}
          </div>

          {basicFormData.thumbnailUrl ? (
            <div className="mt-4 flex flex-wrap gap-3">
              <Button type="button" variant="secondary" onClick={() => setBasicFormData((previous) => ({ ...previous, thumbnailUrl: '' }))}>
                <ImagePlus className="h-4 w-4" />
                Chon anh khac
              </Button>
            </div>
          ) : null}
        </motion.div>
      </div>
    </div>
  );

  const renderLectureEditor = () => {
    if (!selectedLecture || !lectureDraft) {
	    return (
        <div className="flex min-h-[480px] flex-col items-center justify-center rounded-3xl border border-dashed border-gray-300 bg-white px-6 py-12 text-center shadow-sm">
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
        className="rounded-3xl border border-gray-200 bg-white p-6 shadow-sm"
      >
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <p className="text-base font-semibold text-gray-900">{selectedLecture.title}</p>
              <Badge variant={selectedLecture.type === 'VIDEO' ? 'info' : selectedLecture.type === 'ARTICLE' ? 'default' : 'warning'}>
                {lectureTypeLabels[selectedLecture.type]}
              </Badge>
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

        <div className="mt-6 grid gap-5 xl:grid-cols-2">
          <Input
            label="Ten bai giang"
            value={lectureDraft.title}
            onChange={(event) =>
              setLectureDraft((previous) =>
                previous ? { ...previous, title: event.target.value } : previous
              )
            }
            placeholder="Dat ten theo ket qua hoc tap cu the"
          />

          <Input
            label="Thoi luong (giay)"
            value={lectureDraft.durationSeconds}
            type="number"
            onChange={(event) =>
              setLectureDraft((previous) =>
                previous ? { ...previous, durationSeconds: event.target.value } : previous
              )
            }
            placeholder="600"
          />
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-4 rounded-2xl border border-gray-200 bg-gray-50 px-4 py-4">
          <Toggle
            checked={lectureDraft.isFreePreview}
            onChange={(checked) =>
              setLectureDraft((previous) =>
                previous ? { ...previous, isFreePreview: checked } : previous
              )
            }
            label="Cho phep hoc vien xem thu bai nay"
          />
          <Badge variant="default">Loai: {lectureTypeLabels[lectureDraft.type]}</Badge>
        </div>

	        {lectureDraft.type === LectureType.VIDEO ? (
	          <div className="mt-6 space-y-5">
            <Input
              label="Video URL"
              value={lectureDraft.videoUrl}
              onChange={(event) =>
                setLectureDraft((previous) =>
                  previous ? { ...previous, videoUrl: event.target.value } : previous
                )
              }
              placeholder="https://cdn.learnhub/... hoac link embed"
            />

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
                        Video duoc chia chunk 10MB va tai truc tiep len MinIO, backend chi cap session va presigned URL.
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
                      Phat hien session upload chua hoan tat cho bai giang nay. Chon lai dung file video cu de tiep tuc, he thong chi upload cac part con thieu.
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
                          {videoUploadState[selectedLecture.id].uploadedBytes.toLocaleString()} /{' '}
                          {videoUploadState[selectedLecture.id].totalBytes.toLocaleString()} bytes
                        </span>
                        <span>
                          {videoUploadState[selectedLecture.id].uploadedParts.length} part da xong
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
          <div className="mt-6">
            <p className="mb-2 text-sm font-medium text-gray-700">Noi dung bai viet</p>
            <RichTextEditor
              value={lectureDraft.content}
              onChange={(value) =>
                setLectureDraft((previous) =>
                  previous ? { ...previous, content: value } : previous
                )
              }
            />
          </div>
        ) : null}

        {lectureDraft.type === LectureType.QUIZ ? (
          <div className="mt-6">
            <QuizBuilder value={quizQuestions} onChange={setQuizQuestions} />
          </div>
        ) : null}
      </motion.div>
    );
  };

  const renderStepTwo = () => (
    <div className="grid gap-6 xl:grid-cols-[420px_minmax(0,1fr)]">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold text-gray-900">Curriculum</p>
            <p className="mt-1 text-sm text-gray-500">
              Sap xep chuong, tao bai giang va giu mot flow hoc tap ro rang.
            </p>
          </div>
          <Button type="button" variant="secondary" onClick={() => void handleAddSection()} loading={isCreatingSection}>
            <FolderPlus className="h-4 w-4" />
            Them chuong
          </Button>
        </div>

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

        <div className="mt-5 space-y-4">
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
                                  <p className="truncate text-sm font-medium text-gray-900">{lecture.title}</p>
                                  <p className="mt-1 text-xs text-gray-500">
                                    {lectureTypeLabels[lecture.type]}
                                    {lecture.durationSeconds ? ` • ${formatSeconds(lecture.durationSeconds)}` : ''}
                                  </p>
                                </div>
                                {lecture.isFreePreview ? (
                                  <Badge variant="info">Preview</Badge>
                                ) : null}
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
      </motion.div>

      <AnimatePresence mode="wait">{renderLectureEditor()}</AnimatePresence>
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
                  ? 'Moi bai giang can duoc luu rieng de backend cap nhat thoi luong va lecture count.'
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
