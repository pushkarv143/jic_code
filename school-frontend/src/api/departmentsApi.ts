import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, Department } from '@/types';

export interface DepartmentPayload {
  name: string;
  description?: string | null;
}

/** Typed wrappers around every /api/v1/departments/** endpoint from the schema contract. */
export const departmentsApi = {
  list: async (): Promise<ApiResponse<Department[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<Department[]>>(
      ENDPOINTS.DEPARTMENTS.BASE,
    );
    return data;
  },

  create: async (payload: DepartmentPayload): Promise<ApiResponse<Department>> => {
    const { data } = await axiosInstance.post<ApiResponse<Department>>(
      ENDPOINTS.DEPARTMENTS.BASE,
      payload,
    );
    return data;
  },

  update: async (id: number, payload: DepartmentPayload): Promise<ApiResponse<Department>> => {
    const { data } = await axiosInstance.put<ApiResponse<Department>>(
      ENDPOINTS.DEPARTMENTS.BY_ID(id),
      payload,
    );
    return data;
  },

  remove: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.delete<ApiResponse<null>>(
      ENDPOINTS.DEPARTMENTS.BY_ID(id),
    );
    return data;
  },
};

export default departmentsApi;
