import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, Notice, PageResponse, Role } from '@/types';

export interface NoticeListParams {
  page?: number;
  size?: number;
  sort?: string;
  targetRole?: Role;
  includeExpired?: boolean;
}

export interface NoticePayload {
  title: string;
  description: string;
  targetRole?: Role;
  expiryDate?: string;
  file?: File | null;
}

function toNoticeFormData(payload: NoticePayload): FormData {
  const formData = new FormData();
  formData.append('title', payload.title);
  formData.append('description', payload.description);
  if (payload.targetRole) formData.append('targetRole', payload.targetRole);
  if (payload.expiryDate) formData.append('expiryDate', payload.expiryDate);
  if (payload.file) formData.append('file', payload.file);
  return formData;
}

/** Typed wrappers around every /api/v1/notices/** endpoint. */
export const noticesApi = {
  list: async (params: NoticeListParams = {}): Promise<ApiResponse<PageResponse<Notice>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<Notice>>>(ENDPOINTS.NOTICES.BASE, { params });
    return data;
  },
  create: async (payload: NoticePayload): Promise<ApiResponse<Notice>> => {
    const { data } = await axiosInstance.post<ApiResponse<Notice>>(ENDPOINTS.NOTICES.BASE, toNoticeFormData(payload), {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },
  update: async (id: number, payload: NoticePayload): Promise<ApiResponse<Notice>> => {
    const { data } = await axiosInstance.put<ApiResponse<Notice>>(
      ENDPOINTS.NOTICES.BY_ID(id),
      toNoticeFormData(payload),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
  },
  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.NOTICES.BY_ID(id));
    return data;
  },
};

export default noticesApi;
