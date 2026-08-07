import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, AuthResponse, User } from '@/types';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  username: string;
  password: string;
  role: 'STUDENT' | 'PARENT';
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

/** Typed wrappers around every /api/v1/auth/** endpoint from the schema contract. */
export const authApi = {
  login: async (payload: LoginPayload): Promise<AuthResponse> => {
    const { data } = await axiosInstance.post<ApiResponse<AuthResponse>>(
      ENDPOINTS.AUTH.LOGIN,
      payload,
    );
    return data.data;
  },

  register: async (payload: RegisterPayload): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.post<ApiResponse<null>>(
      ENDPOINTS.AUTH.REGISTER,
      payload,
    );
    return data;
  },

  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const { data } = await axiosInstance.post<ApiResponse<AuthResponse>>(
      ENDPOINTS.AUTH.REFRESH_TOKEN,
      { refreshToken },
    );
    return data.data;
  },

  logout: async (refreshToken: string): Promise<void> => {
    await axiosInstance.post(ENDPOINTS.AUTH.LOGOUT, { refreshToken });
  },

  forgotPassword: async (payload: ForgotPasswordPayload): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.post<ApiResponse<null>>(
      ENDPOINTS.AUTH.FORGOT_PASSWORD,
      payload,
    );
    return data;
  },

  resetPassword: async (payload: ResetPasswordPayload): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.post<ApiResponse<null>>(
      ENDPOINTS.AUTH.RESET_PASSWORD,
      payload,
    );
    return data;
  },

  changePassword: async (payload: ChangePasswordPayload): Promise<ApiResponse<null>> => {
    const { data } = await axiosInstance.post<ApiResponse<null>>(
      ENDPOINTS.AUTH.CHANGE_PASSWORD,
      payload,
    );
    return data;
  },

  me: async (): Promise<User> => {
    const { data } = await axiosInstance.get<ApiResponse<User>>(ENDPOINTS.AUTH.ME);
    return data.data;
  },
};

export default authApi;
