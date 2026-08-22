import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, AuthResponse, User } from '@/types';

export interface LoginPayload {
  username: string;
  password: string;
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
  /** Validated as mandatory by the API; omitting it fails the request outright. */
  confirmPassword: string;
}

export type OtpPurpose = 'PASSWORD_RESET' | 'LOGIN' | 'PHONE_VERIFY';

export interface SendOtpPayload {
  /** An email address or a phone number — the server works out which. */
  destination: string;
  purpose: OtpPurpose;
}

export interface VerifyOtpPayload {
  destination: string;
  purpose: OtpPurpose;
  code: string;
}

/** Carries no hint of whether the account exists; only what the countdown needs. */
export interface OtpSendResult {
  expiresInSeconds: number;
  resendAfterSeconds: number;
}

export interface OtpVerifyResult {
  /** Present for PASSWORD_RESET — hand straight to resetPassword. */
  resetToken?: string;
  auth?: AuthResponse;
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

  /**
   * Asks for a one-time passcode. Resolves the same way whether or not the
   * destination is registered, so a caller must not treat success as proof the
   * account exists.
   */
  requestOtp: async (payload: SendOtpPayload): Promise<OtpSendResult> => {
    const { data } = await axiosInstance.post<ApiResponse<OtpSendResult>>(
      ENDPOINTS.AUTH.OTP_REQUEST,
      payload,
    );
    return data.data;
  },

  /** Exchanges a correct code for a single-use password-reset token. */
  verifyOtp: async (payload: VerifyOtpPayload): Promise<OtpVerifyResult> => {
    const { data } = await axiosInstance.post<ApiResponse<OtpVerifyResult>>(
      ENDPOINTS.AUTH.OTP_VERIFY,
      payload,
    );
    return data.data;
  },

  me: async (): Promise<User> => {
    const { data } = await axiosInstance.get<ApiResponse<User>>(ENDPOINTS.AUTH.ME);
    return data.data;
  },
};

export default authApi;
