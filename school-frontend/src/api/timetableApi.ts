import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, TimetableDay, TimetableSlot } from '@/types';

/** One period as sent to the server — no ids, since a save replaces the week. */
export interface TimetableSlotPayload {
  dayOfWeek: TimetableDay;
  periodNumber: number;
  /** "HH:mm:ss" — the API's time format throughout. */
  startTime: string;
  endTime: string;
  subjectId?: number | null;
  teacherId?: number | null;
  roomNumber?: string | null;
  label?: string | null;
}

/** Typed wrappers around /api/v1/timetable/**. */
export const timetableApi = {
  getForSection: async (
    classId: number,
    sectionId: number,
  ): Promise<ApiResponse<TimetableSlot[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TimetableSlot[]>>(
      ENDPOINTS.TIMETABLE.SECTION(classId, sectionId),
    );
    return data;
  },

  getForClass: async (classId: number): Promise<ApiResponse<TimetableSlot[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TimetableSlot[]>>(
      ENDPOINTS.TIMETABLE.CLASS(classId),
    );
    return data;
  },

  getForTeacher: async (teacherId: number): Promise<ApiResponse<TimetableSlot[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TimetableSlot[]>>(
      ENDPOINTS.TIMETABLE.TEACHER(teacherId),
    );
    return data;
  },

  /**
   * The signed-in caller's own week — the periods a teacher teaches, or the week
   * of the class a student is enrolled in. Preferred over getForTeacher/
   * getForSection on self-service screens: there is no id to get wrong, and the
   * server decides what the caller is entitled to see.
   */
  getMine: async (): Promise<ApiResponse<TimetableSlot[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TimetableSlot[]>>(ENDPOINTS.TIMETABLE.ME);
    return data;
  },

  /** One student's class week — how a parent of several children asks for one of them. */
  getForStudent: async (studentId: number): Promise<ApiResponse<TimetableSlot[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<TimetableSlot[]>>(
      ENDPOINTS.TIMETABLE.STUDENT(studentId),
    );
    return data;
  },

  /**
   * Replaces the section's whole week. Whole-week rather than per-slot because a
   * grid edit usually moves several periods at once, and a sequence of per-slot
   * calls would trip the (section, day, period) unique key mid-swap. An empty
   * list clears the section's timetable.
   */
  saveForSection: async (
    classId: number,
    sectionId: number,
    slots: TimetableSlotPayload[],
  ): Promise<ApiResponse<TimetableSlot[]>> => {
    const { data } = await axiosInstance.put<ApiResponse<TimetableSlot[]>>(
      ENDPOINTS.TIMETABLE.SECTION(classId, sectionId),
      { slots },
    );
    return data;
  },
};

export default timetableApi;
