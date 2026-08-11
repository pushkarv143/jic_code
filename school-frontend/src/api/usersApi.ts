import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, PageResponse, Role, User } from '@/types';

export interface UserListParams {
  page?: number;
  size?: number;
  search?: string;
  role?: Role;
  sort?: string;
}

/** Typed wrappers around every /api/v1/users/** endpoint from the schema contract. */
export const usersApi = {
  list: async (params: UserListParams = {}): Promise<ApiResponse<PageResponse<User>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<User>>>(ENDPOINTS.USERS.BASE, {
      params,
    });
    return data;
  },

  getById: async (id: number): Promise<ApiResponse<User>> => {
    const { data } = await axiosInstance.get<ApiResponse<User>>(ENDPOINTS.USERS.BY_ID(id));
    return data;
  },

  activate: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.patch<ApiResponse<null>>(ENDPOINTS.USERS.ACTIVATE(id));
    return data;
  },

  deactivate: async (id: number): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.patch<ApiResponse<null>>(ENDPOINTS.USERS.DEACTIVATE(id));
    return data;
  },
};

export default usersApi;
