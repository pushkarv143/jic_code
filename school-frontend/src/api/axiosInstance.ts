import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import { store } from '@/store/store';
import { logout, setTokens } from '@/store/authSlice';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, AuthResponse } from '@/types';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const axiosInstance = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Separate plain client (no interceptors) used for the refresh call itself,
// to avoid recursive interception.
const refreshClient = axios.create({ baseURL, timeout: 15000 });

axiosInstance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = store.getState().auth.accessToken;
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let pendingQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function flushQueue(error: unknown, token: string | null) {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (token) resolve(token);
    else reject(error);
  });
  pendingQueue = [];
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.assign('/login');
  }
}

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;

    const status = error.response?.status;

    if (status !== 401 || !originalRequest || originalRequest._retry) {
      return Promise.reject(error);
    }

    // Never try to refresh on the auth endpoints themselves.
    if (
      originalRequest.url?.includes(ENDPOINTS.AUTH.LOGIN) ||
      originalRequest.url?.includes(ENDPOINTS.AUTH.REFRESH_TOKEN)
    ) {
      store.dispatch(logout());
      redirectToLogin();
      return Promise.reject(error);
    }

    const refreshToken = store.getState().auth.refreshToken;
    if (!refreshToken) {
      store.dispatch(logout());
      redirectToLogin();
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push({
          resolve: (token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            resolve(axiosInstance(originalRequest));
          },
          reject,
        });
      });
    }

    isRefreshing = true;

    try {
      const { data: envelope } = await refreshClient.post<ApiResponse<AuthResponse>>(
        ENDPOINTS.AUTH.REFRESH_TOKEN,
        { refreshToken },
      );
      const data = envelope.data;
      store.dispatch(
        setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken }),
      );
      flushQueue(null, data.accessToken);
      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      }
      return axiosInstance(originalRequest);
    } catch (refreshError) {
      flushQueue(refreshError, null);
      store.dispatch(logout());
      redirectToLogin();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  },
);

export function apiRequest<T>(config: AxiosRequestConfig): Promise<T> {
  return axiosInstance.request<T>(config).then((res) => res.data);
}

export default axiosInstance;
