import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type {
  ApiResponse,
  Gender,
  Guardian,
  ImportResult,
  MedicalDetails,
  PageResponse,
  Student,
  StudentDocument,
  StudentStatus,
} from '@/types';

export interface StudentListParams {
  page?: number;
  size?: number;
  search?: string;
  classId?: number;
  sectionId?: number;
  status?: StudentStatus;
  sort?: string;
}

export interface GuardianPayload {
  name: string;
  relation: string;
  occupation?: string;
  phone: string;
  email?: string;
  address?: string;
  isPrimary: boolean;
}

/** Personal/address/academic fields for create + edit; guardians are nested on create. */
export interface StudentPayload {
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  classId: number;
  sectionId: number;
  academicYearId: number;
  rollNumber: string;
  admissionDate: string;
  dateOfBirth: string;
  gender: Gender;
  bloodGroup?: string;
  religion?: string;
  category?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  guardians?: GuardianPayload[];
}

export interface MedicalDetailsPayload {
  heightCm?: number | null;
  weightKg?: number | null;
  allergies?: string | null;
  medicalConditions?: string | null;
  doctorName?: string | null;
  doctorContact?: string | null;
}

export interface PromotePayload {
  studentIds: number[];
  toClassId: number;
  toSectionId: number;
  academicYearId: number;
}

export interface TransferPayload {
  remarks: string;
}

export interface StudentExportParams {
  classId?: number;
  sectionId?: number;
  status?: StudentStatus;
}

/** Typed wrappers around every /api/v1/students/** endpoint from the schema contract. */
export const studentsApi = {
  list: async (params: StudentListParams = {}): Promise<ApiResponse<PageResponse<Student>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<Student>>>(
      ENDPOINTS.STUDENTS.BASE,
      { params },
    );
    return data;
  },

  getById: async (id: number): Promise<ApiResponse<Student>> => {
    const { data } = await axiosInstance.get<ApiResponse<Student>>(ENDPOINTS.STUDENTS.BY_ID(id));
    return data;
  },

  create: async (payload: StudentPayload): Promise<ApiResponse<Student>> => {
    const { data } = await axiosInstance.post<ApiResponse<Student>>(
      ENDPOINTS.STUDENTS.BASE,
      payload,
    );
    return data;
  },

  update: async (id: number, payload: Partial<StudentPayload>): Promise<ApiResponse<Student>> => {
    const { data } = await axiosInstance.put<ApiResponse<Student>>(
      ENDPOINTS.STUDENTS.BY_ID(id),
      payload,
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.STUDENTS.BY_ID(id));
    return data;
  },

  updateStatus: async (id: number, status: StudentStatus): Promise<ApiResponse<Student>> => {
    const { data } = await axiosInstance.patch<ApiResponse<Student>>(
      ENDPOINTS.STUDENTS.STATUS(id),
      { status },
    );
    return data;
  },

  uploadPhoto: async (id: number, file: File): Promise<ApiResponse<Student>> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<ApiResponse<Student>>(
      ENDPOINTS.STUDENTS.PHOTO(id),
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
  },

  listGuardians: async (id: number): Promise<ApiResponse<Guardian[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<Guardian[]>>(
      ENDPOINTS.STUDENTS.GUARDIANS(id),
    );
    return data;
  },

  addGuardian: async (id: number, payload: GuardianPayload): Promise<ApiResponse<Guardian>> => {
    const { data } = await axiosInstance.post<ApiResponse<Guardian>>(
      ENDPOINTS.STUDENTS.GUARDIANS(id),
      payload,
    );
    return data;
  },

  updateGuardian: async (
    id: number,
    guardianId: number,
    payload: GuardianPayload,
  ): Promise<ApiResponse<Guardian>> => {
    const { data } = await axiosInstance.put<ApiResponse<Guardian>>(
      ENDPOINTS.STUDENTS.GUARDIAN_BY_ID(id, guardianId),
      payload,
    );
    return data;
  },

  removeGuardian: async (id: number, guardianId: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.STUDENTS.GUARDIAN_BY_ID(id, guardianId),
    );
    return data;
  },

  getMedicalDetails: async (id: number): Promise<ApiResponse<MedicalDetails>> => {
    const { data } = await axiosInstance.get<ApiResponse<MedicalDetails>>(
      ENDPOINTS.STUDENTS.MEDICAL_DETAILS(id),
    );
    return data;
  },

  updateMedicalDetails: async (
    id: number,
    payload: MedicalDetailsPayload,
  ): Promise<ApiResponse<MedicalDetails>> => {
    const { data } = await axiosInstance.put<ApiResponse<MedicalDetails>>(
      ENDPOINTS.STUDENTS.MEDICAL_DETAILS(id),
      payload,
    );
    return data;
  },

  listDocuments: async (id: number): Promise<ApiResponse<StudentDocument[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<StudentDocument[]>>(
      ENDPOINTS.STUDENTS.DOCUMENTS(id),
    );
    return data;
  },

  uploadDocument: async (
    id: number,
    file: File,
    documentType: string,
  ): Promise<ApiResponse<StudentDocument>> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    const { data } = await axiosInstance.post<ApiResponse<StudentDocument>>(
      ENDPOINTS.STUDENTS.DOCUMENTS(id),
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
  },

  removeDocument: async (id: number, docId: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.STUDENTS.DOCUMENT_BY_ID(id, docId),
    );
    return data;
  },

  promote: async (payload: PromotePayload): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.post<ApiResponse<null>>(
      ENDPOINTS.STUDENTS.PROMOTE,
      payload,
    );
    return data;
  },

  transfer: async (id: number, payload: TransferPayload): Promise<ApiResponse<Student>> => {
    const { data } = await axiosInstance.post<ApiResponse<Student>>(
      ENDPOINTS.STUDENTS.TRANSFER(id),
      payload,
    );
    return data;
  },

  markAlumni: async (id: number): Promise<ApiResponse<Student>> => {
    const { data } = await axiosInstance.post<ApiResponse<Student>>(
      ENDPOINTS.STUDENTS.MARK_ALUMNI(id),
    );
    return data;
  },

  getIdCardPdf: async (id: number): Promise<Blob> => {
    const { data } = await axiosInstance.get<Blob>(ENDPOINTS.STUDENTS.ID_CARD_PDF(id), {
      responseType: 'blob',
    });
    return data;
  },

  exportExcel: async (params: StudentExportParams = {}): Promise<Blob> => {
    const { data } = await axiosInstance.get<Blob>(ENDPOINTS.STUDENTS.EXPORT_EXCEL, {
      params,
      responseType: 'blob',
    });
    return data;
  },

  importExcel: async (file: File): Promise<ApiResponse<ImportResult>> => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await axiosInstance.post<ApiResponse<ImportResult>>(
      ENDPOINTS.STUDENTS.IMPORT_EXCEL,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
  },
};

export default studentsApi;
