import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, ParentChild } from '@/types';

/** Typed wrappers around every /api/v1/parents/** endpoint from the schema contract. */
export const parentApi = {
  getMyChildren: async (): Promise<ApiResponse<ParentChild[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<ParentChild[]>>(ENDPOINTS.PARENTS.MY_CHILDREN);
    return data;
  },
};

export default parentApi;
