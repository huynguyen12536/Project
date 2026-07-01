import apiClient from '../lib/api';
import type {
    Course,
    CourseSection,
    CourseLecture,
    CourseTaxonomyBundle,
    CreateCourseRequest,
    UpdateCourseRequest,
    CreateSectionRequest,
    UpdateSectionRequest,
    CreateLectureRequest,
    UpdateLectureRequest,
    RejectCourseRequest,
} from '../types';

export const courseApi = {
    // Instructor Course APIs
    getInstructorCourses: async (): Promise<Course[]> => {
        const response = await apiClient.get<Course[]>('/v1/instructor/courses');
        return response.data;
    },

    getInstructorCourse: async (courseId: string): Promise<Course> => {
        const response = await apiClient.get<Course>(`/v1/instructor/courses/${courseId}`);
        return response.data;
    },

    createCourse: async (request: CreateCourseRequest): Promise<Course> => {
        const response = await apiClient.post<Course>('/v1/instructor/courses', request);
        return response.data;
    },

    updateCourse: async (courseId: string, request: UpdateCourseRequest): Promise<Course> => {
        const response = await apiClient.put<Course>(`/v1/instructor/courses/${courseId}`, request);
        return response.data;
    },

    submitForReview: async (courseId: string): Promise<Course> => {
        const response = await apiClient.post<Course>(`/v1/instructor/courses/${courseId}/submit-review`);
        return response.data;
    },

    getTaxonomyOptions: async (): Promise<CourseTaxonomyBundle> => {
        const response = await apiClient.get<CourseTaxonomyBundle>('/v1/taxonomy/options');
        return response.data;
    },

    // Course Section APIs
    getCourseSections: async (courseId: string): Promise<CourseSection[]> => {
        const response = await apiClient.get<CourseSection[]>(`/v1/instructor/courses/${courseId}/sections`);
        return response.data;
    },

    createCourseSection: async (courseId: string, request: CreateSectionRequest): Promise<CourseSection> => {
        const response = await apiClient.post<CourseSection>(`/v1/instructor/courses/${courseId}/sections`, request);
        return response.data;
    },

    updateCourseSection: async (
        courseId: string,
        sectionId: string,
        request: UpdateSectionRequest
    ): Promise<CourseSection> => {
        const response = await apiClient.put<CourseSection>(
            `/v1/instructor/courses/${courseId}/sections/${sectionId}`,
            request
        );
        return response.data;
    },

    deleteCourseSection: async (courseId: string, sectionId: string): Promise<void> => {
        await apiClient.delete(`/v1/instructor/courses/${courseId}/sections/${sectionId}`);
    },

    // Course Lecture APIs
    createCourseLecture: async (
        courseId: string,
        sectionId: string,
        request: CreateLectureRequest
    ): Promise<CourseLecture> => {
        const response = await apiClient.post<CourseLecture>(
            `/v1/instructor/courses/${courseId}/sections/${sectionId}/lectures`,
            request
        );
        return response.data;
    },

    updateCourseLecture: async (
        courseId: string,
        sectionId: string,
        lectureId: string,
        request: UpdateLectureRequest
    ): Promise<CourseLecture> => {
        const response = await apiClient.put<CourseLecture>(
            `/v1/instructor/courses/${courseId}/sections/${sectionId}/lectures/${lectureId}`,
            request
        );
        return response.data;
    },

    deleteCourseLecture: async (courseId: string, sectionId: string, lectureId: string): Promise<void> => {
        await apiClient.delete(`/v1/instructor/courses/${courseId}/sections/${sectionId}/lectures/${lectureId}`);
    },

    // Admin Course APIs
    getPendingCourses: async (): Promise<Course[]> => {
        const response = await apiClient.get<Course[]>('/v1/admin/courses/pending');
        return response.data;
    },

    approveCourse: async (courseId: string): Promise<Course> => {
        const response = await apiClient.post<Course>(`/v1/admin/courses/${courseId}/approve`);
        return response.data;
    },

    rejectCourse: async (courseId: string, request: RejectCourseRequest): Promise<Course> => {
        const response = await apiClient.post<Course>(`/v1/admin/courses/${courseId}/reject`, request);
        return response.data;
    },
};
