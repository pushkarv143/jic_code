import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, LeaveApplicantType, LeaveApplication, LeaveStatus, PageResponse } from '@/types';

export interface LeaveListParams {
  applicantType?: LeaveApplicantType;
  status?: LeaveStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface MyLeaveListParams {
  page?: number;
  size?: number;
}

export interface LeaveApplicationPayload {
  leaveType: string;
  startDate: string;
  endDate: string;
  reason: string;
}

/** Typed wrappers around every /api/v1/leave-applications/** endpoint from the schema contract. */
export const leaveApi = {
  list: async (params: LeaveListParams = {}): Promise<ApiResponse<PageResponse<LeaveApplication>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<LeaveApplication>>>(
      ENDPOINTS.LEAVE_APPLICATIONS.BASE,
      { params },
    );
    return data;
  },

  listMine: async (
    params: MyLeaveListParams = {},
  ): Promise<ApiResponse<PageResponse<LeaveApplication>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<LeaveApplication>>>(
      ENDPOINTS.LEAVE_APPLICATIONS.MY,
      { params },
    );
    return data;
  },

  create: async (payload: LeaveApplicationPayload): Promise<ApiResponse<LeaveApplication>> => {
    const { data } = await axiosInstance.post<ApiResponse<LeaveApplication>>(
      ENDPOINTS.LEAVE_APPLICATIONS.BASE,
      payload,
    );
    return data;
  },

  approve: async (id: number): Promise<ApiResponse<LeaveApplication>> => {
    const { data } = await axiosInstance.patch<ApiResponse<LeaveApplication>>(
      ENDPOINTS.LEAVE_APPLICATIONS.APPROVE(id),
    );
    return data;
  },

  reject: async (id: number): Promise<ApiResponse<LeaveApplication>> => {
    const { data } = await axiosInstance.patch<ApiResponse<LeaveApplication>>(
      ENDPOINTS.LEAVE_APPLICATIONS.REJECT(id),
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.LEAVE_APPLICATIONS.BY_ID(id),
    );
    return data;
  },
};

export default leaveApi;
