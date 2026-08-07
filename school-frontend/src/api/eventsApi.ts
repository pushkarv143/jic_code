import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, BirthdaysResponse, CalendarEvent, CalendarEventType } from '@/types';

export interface EventListParams {
  startDate?: string;
  endDate?: string;
  eventType?: CalendarEventType;
}

export interface EventPayload {
  title: string;
  description?: string;
  eventDate: string;
  eventType: CalendarEventType;
}

/** Typed wrappers around every /api/v1/events/** and /calendar/** endpoint. */
export const eventsApi = {
  list: async (params: EventListParams = {}): Promise<ApiResponse<CalendarEvent[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<CalendarEvent[]>>(ENDPOINTS.EVENTS.BASE, { params });
    return data;
  },
  create: async (payload: EventPayload): Promise<ApiResponse<CalendarEvent>> => {
    const { data } = await axiosInstance.post<ApiResponse<CalendarEvent>>(ENDPOINTS.EVENTS.BASE, payload);
    return data;
  },
  update: async (id: number, payload: EventPayload): Promise<ApiResponse<CalendarEvent>> => {
    const { data } = await axiosInstance.put<ApiResponse<CalendarEvent>>(ENDPOINTS.EVENTS.BY_ID(id), payload);
    return data;
  },
  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.EVENTS.BY_ID(id));
    return data;
  },
  birthdays: async (month: number): Promise<ApiResponse<BirthdaysResponse>> => {
    const { data } = await axiosInstance.get<ApiResponse<BirthdaysResponse>>(ENDPOINTS.CALENDAR.BIRTHDAYS, {
      params: { month },
    });
    return data;
  },
};

export default eventsApi;
