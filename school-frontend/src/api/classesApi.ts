import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  ClassOfficial,
  ClassOfficialRole,
  ClassOverview,
  ClassSubjectTeacher,
  ClassTeacherAvailability,
  PageResponse,
  SchoolClass,
  Section,
  Subject,
  TeacherWorkload,
} from '@/types';

export interface ClassListParams {
  academicYearId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ClassPayload {
  className: string;
  academicYearId: number;
}

export interface SectionPayload {
  sectionName: string;
  roomNumber?: string | null;
  capacity?: number | null;
  classTeacherId?: number | null;
}

export interface SubjectPayload {
  subjectName: string;
  subjectCode: string;
  isElective?: boolean;
}

export interface ClassSubjectTeacherPayload {
  classId: number;
  sectionId: number;
  subjectId: number;
  teacherId: number;
}

export interface ClassOfficialPayload {
  studentId: number;
  role: ClassOfficialRole;
  /** Optional: scopes a post to one section instead of the whole class. */
  sectionId?: number | null;
  /** Defaults to today server-side when omitted. */
  fromDate?: string;
  remarks?: string | null;
}

export interface ClassSubjectTeacherParams {
  sectionId?: number;
  subjectId?: number;
  teacherId?: number;
}

/**
 * Typed wrappers around /api/v1/classes/**, /api/v1/sections/**,
 * /api/v1/subjects/** and /api/v1/class-subject-teacher/** from the schema
 * contract. Grouped in one file because the Classes module UI treats them as
 * one tightly-coupled screen (class -> sections/subjects -> teacher mapping).
 */
export const classesApi = {
  list: async (
    params: ClassListParams = {},
  ): Promise<ApiResponse<PageResponse<SchoolClass>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<SchoolClass>>>(
      ENDPOINTS.CLASSES.BASE,
      { params },
    );
    return data;
  },

  getById: async (id: number): Promise<ApiResponse<SchoolClass>> => {
    const { data } = await axiosInstance.get<ApiResponse<SchoolClass>>(
      ENDPOINTS.CLASSES.BY_ID(id),
    );
    return data;
  },

  create: async (payload: ClassPayload): Promise<ApiResponse<SchoolClass>> => {
    const { data } = await axiosInstance.post<ApiResponse<SchoolClass>>(
      ENDPOINTS.CLASSES.BASE,
      payload,
    );
    return data;
  },

  update: async (id: number, payload: ClassPayload): Promise<ApiResponse<SchoolClass>> => {
    const { data } = await axiosInstance.put<ApiResponse<SchoolClass>>(
      ENDPOINTS.CLASSES.BY_ID(id),
      payload,
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.CLASSES.BY_ID(id));
    return data;
  },

  listSections: async (classId: number): Promise<ApiResponse<Section[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<Section[]>>(
      ENDPOINTS.CLASSES.SECTIONS(classId),
    );
    return data;
  },

  createSection: async (
    classId: number,
    payload: SectionPayload,
  ): Promise<ApiResponse<Section>> => {
    const { data } = await axiosInstance.post<ApiResponse<Section>>(
      ENDPOINTS.CLASSES.SECTIONS(classId),
      payload,
    );
    return data;
  },

  updateSection: async (id: number, payload: SectionPayload): Promise<ApiResponse<Section>> => {
    const { data } = await axiosInstance.put<ApiResponse<Section>>(
      ENDPOINTS.SECTIONS.BY_ID(id),
      payload,
    );
    return data;
  },

  removeSection: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.SECTIONS.BY_ID(id));
    return data;
  },

  assignClassTeacher: async (id: number, teacherId: number): Promise<ApiResponse<Section>> => {
    const { data } = await axiosInstance.patch<ApiResponse<Section>>(
      ENDPOINTS.SECTIONS.ASSIGN_CLASS_TEACHER(id),
      { teacherId },
    );
    return data;
  },

  listSubjects: async (classId: number): Promise<ApiResponse<Subject[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<Subject[]>>(
      ENDPOINTS.CLASSES.SUBJECTS(classId),
    );
    return data;
  },

  createSubject: async (
    classId: number,
    payload: SubjectPayload,
  ): Promise<ApiResponse<Subject>> => {
    const { data } = await axiosInstance.post<ApiResponse<Subject>>(
      ENDPOINTS.CLASSES.SUBJECTS(classId),
      payload,
    );
    return data;
  },

  updateSubject: async (id: number, payload: SubjectPayload): Promise<ApiResponse<Subject>> => {
    const { data } = await axiosInstance.put<ApiResponse<Subject>>(
      ENDPOINTS.SUBJECTS.BY_ID(id),
      payload,
    );
    return data;
  },

  removeSubject: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.SUBJECTS.BY_ID(id));
    return data;
  },

  listTeacherMappings: async (
    params: ClassSubjectTeacherParams = {},
  ): Promise<ApiResponse<ClassSubjectTeacher[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<ClassSubjectTeacher[]>>(
      ENDPOINTS.CLASS_SUBJECT_TEACHER.BASE,
      { params },
    );
    return data;
  },

  /**
   * The signed-in caller's own slice of the mapping grid: a teacher's assigned
   * subjects, or the subjects and teachers of a student's own class. Students and
   * parents are only permitted this and listTeacherMappingsForStudent — not the
   * unfiltered listTeacherMappings above.
   */
  listMyTeacherMappings: async (): Promise<ApiResponse<ClassSubjectTeacher[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<ClassSubjectTeacher[]>>(
      ENDPOINTS.CLASS_SUBJECT_TEACHER.ME,
    );
    return data;
  },

  /** Subjects and their teachers for one student's class. */
  listTeacherMappingsForStudent: async (
    studentId: number,
  ): Promise<ApiResponse<ClassSubjectTeacher[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<ClassSubjectTeacher[]>>(
      ENDPOINTS.CLASS_SUBJECT_TEACHER.STUDENT(studentId),
    );
    return data;
  },

  assignTeacherMapping: async (
    payload: ClassSubjectTeacherPayload,
  ): Promise<ApiResponse<ClassSubjectTeacher>> => {
    const { data } = await axiosInstance.post<ApiResponse<ClassSubjectTeacher>>(
      ENDPOINTS.CLASS_SUBJECT_TEACHER.BASE,
      payload,
    );
    return data;
  },

  removeTeacherMapping: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.CLASS_SUBJECT_TEACHER.BY_ID(id),
    );
    return data;
  },

  /* ---- class module: overview, officials, workload ---------------------- */

  /**
   * Strength, people, stats and warnings in one call. The date range bounds the
   * stats only; omit it and the backend uses the current month to date.
   */
  getOverview: async (
    classId: number,
    params: { startDate?: string; endDate?: string } = {},
  ): Promise<ApiResponse<ClassOverview>> => {
    const { data } = await axiosInstance.get<ApiResponse<ClassOverview>>(
      ENDPOINTS.CLASSES.OVERVIEW(classId),
      { params },
    );
    return data;
  },

  listOfficials: async (classId: number): Promise<ApiResponse<ClassOfficial[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<ClassOfficial[]>>(
      ENDPOINTS.CLASSES.OFFICIALS(classId),
    );
    return data;
  },

  listOfficialHistory: async (classId: number): Promise<ApiResponse<ClassOfficial[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<ClassOfficial[]>>(
      ENDPOINTS.CLASSES.OFFICIALS_HISTORY(classId),
    );
    return data;
  },

  /** Appointing ends the sitting holder's tenure server-side; no separate call needed. */
  appointOfficial: async (
    classId: number,
    payload: ClassOfficialPayload,
  ): Promise<ApiResponse<ClassOfficial>> => {
    const { data } = await axiosInstance.post<ApiResponse<ClassOfficial>>(
      ENDPOINTS.CLASSES.OFFICIALS(classId),
      payload,
    );
    return data;
  },

  /** Ends a tenure, leaving the post vacant. The record is kept, not deleted. */
  endOfficial: async (classId: number, officialId: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.CLASSES.OFFICIAL_BY_ID(classId, officialId),
    );
    return data;
  },

  /**
   * Teachers who already have a homeroom, and where. Returned for assigned
   * teachers only, so anyone absent from the list is free — the assignment UI
   * greys out the rest instead of offering a pick the server would reject.
   */
  getClassTeacherAvailability: async (): Promise<ApiResponse<ClassTeacherAvailability[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<ClassTeacherAvailability[]>>(
      ENDPOINTS.CLASSES.CLASS_TEACHER_AVAILABILITY,
    );
    return data;
  },

  getTeacherWorkload: async (): Promise<ApiResponse<TeacherWorkload[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TeacherWorkload[]>>(
      ENDPOINTS.CLASSES.TEACHER_WORKLOAD,
    );
    return data;
  },
};

export default classesApi;
