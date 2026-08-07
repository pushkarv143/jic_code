import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, Assignment, AssignmentSubmission, PageResponse } from '@/types';

export interface AssignmentListParams {
  classId?: number;
  sectionId?: number;
  subjectId?: number;
  teacherId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface AssignmentPayload {
  title: string;
  description?: string;
  classId: number;
  sectionId: number;
  subjectId: number;
  assignedDate: string;
  dueDate: string;
  file?: File | null;
}

export interface GradeSubmissionPayload {
  marksObtained: number;
  feedback?: string;
}

function toAssignmentFormData(payload: AssignmentPayload): FormData {
  const formData = new FormData();
  formData.append('title', payload.title);
  if (payload.description) formData.append('description', payload.description);
  formData.append('classId', String(payload.classId));
  formData.append('sectionId', String(payload.sectionId));
  formData.append('subjectId', String(payload.subjectId));
  formData.append('assignedDate', payload.assignedDate);
  formData.append('dueDate', payload.dueDate);
  if (payload.file) formData.append('file', payload.file);
  return formData;
}

/** Typed wrappers around every /api/v1/assignments/**, /assignment-submissions/** endpoint. */
export const assignmentsApi = {
  list: async (params: AssignmentListParams = {}): Promise<ApiResponse<PageResponse<Assignment>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<Assignment>>>(ENDPOINTS.ASSIGNMENTS.BASE, {
      params,
    });
    return data;
  },

  create: async (payload: AssignmentPayload): Promise<ApiResponse<Assignment>> => {
    const { data } = await axiosInstance.post<ApiResponse<Assignment>>(
      ENDPOINTS.ASSIGNMENTS.BASE,
      toAssignmentFormData(payload),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
  },

  update: async (id: number, payload: AssignmentPayload): Promise<ApiResponse<Assignment>> => {
    const { data } = await axiosInstance.put<ApiResponse<Assignment>>(
      ENDPOINTS.ASSIGNMENTS.BY_ID(id),
      toAssignmentFormData(payload),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.ASSIGNMENTS.BY_ID(id));
    return data;
  },

  listSubmissions: async (assignmentId: number): Promise<ApiResponse<AssignmentSubmission[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<AssignmentSubmission[]>>(
      ENDPOINTS.ASSIGNMENTS.SUBMISSIONS(assignmentId),
    );
    return data;
  },

  /** `data` may be `null` when the student hasn't submitted yet. */
  getMySubmission: async (assignmentId: number): Promise<ApiResponse<AssignmentSubmission | null>> => {
    const { data } = await axiosInstance.get<ApiResponse<AssignmentSubmission | null>>(
      ENDPOINTS.ASSIGNMENTS.MY_SUBMISSION(assignmentId),
    );
    return data;
  },

  submit: async (assignmentId: number, file: File): Promise<ApiResponse<AssignmentSubmission>> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<ApiResponse<AssignmentSubmission>>(
      ENDPOINTS.ASSIGNMENTS.SUBMIT(assignmentId),
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
  },

  gradeSubmission: async (
    submissionId: number,
    payload: GradeSubmissionPayload,
  ): Promise<ApiResponse<AssignmentSubmission>> => {
    const { data } = await axiosInstance.patch<ApiResponse<AssignmentSubmission>>(
      ENDPOINTS.ASSIGNMENT_SUBMISSIONS.GRADE(submissionId),
      payload,
    );
    return data;
  },
};

export default assignmentsApi;
