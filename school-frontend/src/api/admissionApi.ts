import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { AdmissionEnquiry, AdmissionEnquiryStatus, ApiResponse, PageResponse } from '@/types';

export interface PublicAdmissionEnquiryPayload {
  studentName: string;
  parentName: string;
  phone: string;
  email: string;
  classApplying: string;
  dob: string;
  address: string;
}

export interface AdmissionEnquiryListParams {
  status?: AdmissionEnquiryStatus;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Typed wrappers around /api/v1/public/admission-enquiries (no auth required) and the
 * staff-only /api/v1/admission-enquiries/** endpoints. The public submission uses the
 * same axiosInstance as everything else — its request interceptor only *attaches* a
 * token when one exists in the store, and its response interceptor only reacts to a
 * 401 *response*; an anonymous visitor has no token to attach and this endpoint won't
 * return a 401, so nothing about the shared client's auth handling misfires here.
 */
export const admissionApi = {
  submitEnquiry: async (payload: PublicAdmissionEnquiryPayload): Promise<ApiResponse<AdmissionEnquiry>> => {
    const { data } = await axiosInstance.post<ApiResponse<AdmissionEnquiry>>(
      ENDPOINTS.ADMISSION_ENQUIRIES.PUBLIC_CREATE,
      payload,
    );
    return data;
  },

  list: async (params: AdmissionEnquiryListParams = {}): Promise<ApiResponse<PageResponse<AdmissionEnquiry>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<AdmissionEnquiry>>>(
      ENDPOINTS.ADMISSION_ENQUIRIES.BASE,
      { params },
    );
    return data;
  },

  updateStatus: async (id: number, status: AdmissionEnquiryStatus): Promise<ApiResponse<AdmissionEnquiry>> => {
    const { data } = await axiosInstance.patch<ApiResponse<AdmissionEnquiry>>(
      ENDPOINTS.ADMISSION_ENQUIRIES.STATUS(id),
      { status },
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(ENDPOINTS.ADMISSION_ENQUIRIES.BY_ID(id));
    return data;
  },
};

export default admissionApi;
