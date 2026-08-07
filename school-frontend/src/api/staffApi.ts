import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, PageResponse, Staff } from '@/types';

export interface StaffListParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
}

/** Read-only staff directory (GET /staff) — powers Payroll's STAFF employee search. */
export const staffApi = {
  list: async (params: StaffListParams = {}): Promise<ApiResponse<PageResponse<Staff>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<Staff>>>(ENDPOINTS.STAFF.BASE, { params });
    return data;
  },
};

export default staffApi;
