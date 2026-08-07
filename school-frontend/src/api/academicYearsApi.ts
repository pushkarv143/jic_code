import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { AcademicYear, ApiResponse } from '@/types';

export interface AcademicYearPayload {
  yearName: string;
  startDate: string;
  endDate: string;
}

/** Typed wrappers around every /api/v1/academic-years/** endpoint from the schema contract. */
export const academicYearsApi = {
  list: async (): Promise<ApiResponse<AcademicYear[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<AcademicYear[]>>(
      ENDPOINTS.ACADEMIC_YEARS.BASE,
    );
    return data;
  },

  create: async (payload: AcademicYearPayload): Promise<ApiResponse<AcademicYear>> => {
    const { data } = await axiosInstance.post<ApiResponse<AcademicYear>>(
      ENDPOINTS.ACADEMIC_YEARS.BASE,
      payload,
    );
    return data;
  },

  update: async (
    id: number,
    payload: AcademicYearPayload,
  ): Promise<ApiResponse<AcademicYear>> => {
    const { data } = await axiosInstance.put<ApiResponse<AcademicYear>>(
      ENDPOINTS.ACADEMIC_YEARS.BY_ID(id),
      payload,
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.ACADEMIC_YEARS.BY_ID(id),
    );
    return data;
  },

  setCurrent: async (id: number): Promise<ApiResponse<AcademicYear>> => {
    const { data } = await axiosInstance.patch<ApiResponse<AcademicYear>>(
      ENDPOINTS.ACADEMIC_YEARS.SET_CURRENT(id),
    );
    return data;
  },
};

export default academicYearsApi;
