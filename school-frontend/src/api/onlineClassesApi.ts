import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, OnlineClass, PageResponse } from '@/types';

export interface OnlineClassListParams {
  classId?: number;
  sectionId?: number;
  subjectId?: number;
  teacherId?: number;
  upcoming?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface OnlineClassPayload {
  classId: number;
  sectionId: number;
  subjectId: number;
  title: string;
  meetingLink: string;
  scheduledAt: string;
  durationMinutes: number;
}

/** Typed wrappers around every /api/v1/online-classes/** endpoint. */
export const onlineClassesApi = {
  list: async (params: OnlineClassListParams = {}): Promise<ApiResponse<PageResponse<OnlineClass>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<OnlineClass>>>(ENDPOINTS.ONLINE_CLASSES.BASE, {
      params,
    });
    return data;
  },
  create: async (payload: OnlineClassPayload): Promise<ApiResponse<OnlineClass>> => {
    const { data } = await axiosInstance.post<ApiResponse<OnlineClass>>(ENDPOINTS.ONLINE_CLASSES.BASE, payload);
    return data;
  },
  update: async (id: number, payload: OnlineClassPayload): Promise<ApiResponse<OnlineClass>> => {
    const { data } = await axiosInstance.put<ApiResponse<OnlineClass>>(ENDPOINTS.ONLINE_CLASSES.BY_ID(id), payload);
    return data;
  },
  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.ONLINE_CLASSES.BY_ID(id));
    return data;
  },
};

export default onlineClassesApi;
