import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, Designation } from '@/types';

export interface DesignationPayload {
  name: string;
  description?: string | null;
}

/** Typed wrappers around every /api/v1/designations/** endpoint from the schema contract. */
export const designationsApi = {
  list: async (): Promise<ApiResponse<Designation[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<Designation[]>>(
      ENDPOINTS.DESIGNATIONS.BASE,
    );
    return data;
  },

  create: async (payload: DesignationPayload): Promise<ApiResponse<Designation>> => {
    const { data } = await axiosInstance.post<ApiResponse<Designation>>(
      ENDPOINTS.DESIGNATIONS.BASE,
      payload,
    );
    return data;
  },

  update: async (id: number, payload: DesignationPayload): Promise<ApiResponse<Designation>> => {
    const { data } = await axiosInstance.put<ApiResponse<Designation>>(
      ENDPOINTS.DESIGNATIONS.BY_ID(id),
      payload,
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.DESIGNATIONS.BY_ID(id),
    );
    return data;
  },
};

export default designationsApi;
