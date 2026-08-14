import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  TeacherAssignment,
  EmploymentType,
  Gender,
  PageResponse,
  StaffStatus,
  Teacher,
} from '@/types';

export interface TeacherListParams {
  page?: number;
  size?: number;
  search?: string;
  departmentId?: number;
  designationId?: number;
  status?: StaffStatus;
  sort?: string;
}

/** Fields for both create (username/email/password required) and edit (password omitted). */
export interface TeacherPayload {
  username: string;
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  phone: string;
  gender: Gender;
  departmentId: number;
  designationId: number;
  qualification?: string;
  experienceYears?: number;
  joiningDate: string;
  dateOfBirth?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  bloodGroup?: string;
  emergencyContact?: string;
  salary?: number;
  employmentType: EmploymentType;
}

/**
 * The narrow slice of their own profile a teacher may edit. Mirrors the backend's
 * TeacherSelfUpdateRequest exactly — department, designation, salary, employment
 * type and status are absent from both, since those are HR decisions.
 */
export interface TeacherSelfPayload {
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  bloodGroup?: string;
  emergencyContact?: string;
  qualification?: string;
}

/** Typed wrappers around every /api/v1/teachers/** endpoint from the schema contract. */
export const teachersApi = {
  list: async (params: TeacherListParams = {}): Promise<ApiResponse<PageResponse<Teacher>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<Teacher>>>(
      ENDPOINTS.TEACHERS.BASE,
      { params },
    );
    return data;
  },

  getById: async (id: number): Promise<ApiResponse<Teacher>> => {
    const { data } = await axiosInstance.get<ApiResponse<Teacher>>(ENDPOINTS.TEACHERS.BY_ID(id));
    return data;
  },

  /** The signed-in teacher's own record, unredacted. TEACHER/CLASS_TEACHER only. */
  getOwnProfile: async (): Promise<ApiResponse<Teacher>> => {
    const { data } = await axiosInstance.get<ApiResponse<Teacher>>(ENDPOINTS.TEACHERS.ME);
    return data;
  },

  /** Updates only the contact/qualification fields a teacher may maintain themselves. */
  updateOwnProfile: async (payload: TeacherSelfPayload): Promise<ApiResponse<Teacher>> => {
    const { data } = await axiosInstance.patch<ApiResponse<Teacher>>(ENDPOINTS.TEACHERS.ME, payload);
    return data;
  },

  /** The classes, sections and subjects the signed-in teacher is assigned to. */
  getOwnAssignments: async (): Promise<ApiResponse<TeacherAssignment[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TeacherAssignment[]>>(
      ENDPOINTS.TEACHERS.ME_ASSIGNMENTS,
    );
    return data;
  },

  create: async (payload: TeacherPayload): Promise<ApiResponse<Teacher>> => {
    const { data } = await axiosInstance.post<ApiResponse<Teacher>>(
      ENDPOINTS.TEACHERS.BASE,
      payload,
    );
    return data;
  },

  update: async (id: number, payload: Partial<TeacherPayload>): Promise<ApiResponse<Teacher>> => {
    const { data } = await axiosInstance.put<ApiResponse<Teacher>>(
      ENDPOINTS.TEACHERS.BY_ID(id),
      payload,
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.TEACHERS.BY_ID(id));
    return data;
  },

  updateStatus: async (id: number, status: StaffStatus): Promise<ApiResponse<Teacher>> => {
    const { data } = await axiosInstance.patch<ApiResponse<Teacher>>(
      ENDPOINTS.TEACHERS.STATUS(id),
      { status },
    );
    return data;
  },

  getIdCardPdf: async (id: number): Promise<Blob> => {
    const { data } = await axiosInstance.get<Blob>(ENDPOINTS.TEACHERS.ID_CARD_PDF(id), {
      responseType: 'blob',
    });
    return data;
  },

  exportExcel: async (): Promise<Blob> => {
    const { data } = await axiosInstance.get<Blob>(ENDPOINTS.TEACHERS.EXPORT_EXCEL, {
      responseType: 'blob',
    });
    return data;
  },
};

export default teachersApi;
