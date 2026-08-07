import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, NotificationRecord, PageResponse, SendNotificationPayload } from '@/types';

export interface MyNotificationsParams {
  page?: number;
  size?: number;
}

/** Typed wrappers around every /api/v1/notifications/** endpoint from the schema contract. */
export const notificationsApi = {
  listMy: async (params: MyNotificationsParams = {}): Promise<ApiResponse<PageResponse<NotificationRecord>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<NotificationRecord>>>(
      ENDPOINTS.NOTIFICATIONS.MY,
      { params },
    );
    return data;
  },

  send: async (payload: SendNotificationPayload): Promise<ApiResponse<NotificationRecord>> => {
    const { data } = await axiosInstance.post<ApiResponse<NotificationRecord>>(
      ENDPOINTS.NOTIFICATIONS.SEND,
      payload,
    );
    return data;
  },
};

export default notificationsApi;
