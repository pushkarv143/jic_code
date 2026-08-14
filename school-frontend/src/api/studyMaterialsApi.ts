import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, MaterialType, PageResponse, StudyMaterial } from '@/types';

export interface StudyMaterialListParams {
  page?: number;
  size?: number;
  classId?: number;
  sectionId?: number;
  subjectId?: number;
  materialType?: MaterialType;
  search?: string;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

/** Mirrors the backend's StudyMaterialRequest. */
export interface StudyMaterialPayload {
  classId: number;
  /** Omit to share with every section of the class (management only). */
  sectionId?: number | null;
  subjectId: number;
  title: string;
  description?: string;
  materialType: MaterialType;
  externalUrl?: string;
  published?: boolean;
}

/**
 * Create/update are multipart: the JSON payload rides in a `material` part and the
 * optional upload in a `file` part, matching the @RequestPart names on the
 * controller. The JSON part needs an explicit content type or Spring reads it as
 * text/plain and fails to bind.
 */
function toFormData(payload: StudyMaterialPayload, file?: File | null): FormData {
  const formData = new FormData();
  formData.append('material', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  if (file) formData.append('file', file);
  return formData;
}

export const studyMaterialsApi = {
  list: async (
    params: StudyMaterialListParams = {},
  ): Promise<ApiResponse<PageResponse<StudyMaterial>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<StudyMaterial>>>(
      ENDPOINTS.STUDY_MATERIALS.BASE,
      { params },
    );
    return data;
  },

  getById: async (id: number): Promise<ApiResponse<StudyMaterial>> => {
    const { data } = await axiosInstance.get<ApiResponse<StudyMaterial>>(
      ENDPOINTS.STUDY_MATERIALS.BY_ID(id),
    );
    return data;
  },

  /** Re-checks visibility server-side before handing back the file/external URL. */
  resolveDownload: async (id: number): Promise<ApiResponse<StudyMaterial>> => {
    const { data } = await axiosInstance.get<ApiResponse<StudyMaterial>>(
      ENDPOINTS.STUDY_MATERIALS.DOWNLOAD(id),
    );
    return data;
  },

  create: async (
    payload: StudyMaterialPayload,
    file?: File | null,
  ): Promise<ApiResponse<StudyMaterial>> => {
    const { data } = await axiosInstance.post<ApiResponse<StudyMaterial>>(
      ENDPOINTS.STUDY_MATERIALS.BASE,
      toFormData(payload, file),
    );
    return data;
  },

  update: async (
    id: number,
    payload: StudyMaterialPayload,
    file?: File | null,
  ): Promise<ApiResponse<StudyMaterial>> => {
    const { data } = await axiosInstance.put<ApiResponse<StudyMaterial>>(
      ENDPOINTS.STUDY_MATERIALS.BY_ID(id),
      toFormData(payload, file),
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.STUDY_MATERIALS.BY_ID(id),
    );
    return data;
  },
};

export default studyMaterialsApi;
