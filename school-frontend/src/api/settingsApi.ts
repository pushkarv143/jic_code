import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, PermissionInfo, RoleInfo, SchoolInfo, SystemSetting } from '@/types';

/** Typed wrappers around every /api/v1/settings/**, /api/v1/roles/** endpoint from the schema contract. */
export const settingsApi = {
  getSchoolInfo: async (): Promise<ApiResponse<SchoolInfo>> => {
    const { data } = await axiosInstance.get<ApiResponse<SchoolInfo>>(ENDPOINTS.SETTINGS.SCHOOL_INFO);
    return data;
  },

  updateSchoolInfo: async (payload: SchoolInfo): Promise<ApiResponse<SchoolInfo>> => {
    const { data } = await axiosInstance.put<ApiResponse<SchoolInfo>>(ENDPOINTS.SETTINGS.SCHOOL_INFO, payload);
    return data;
  },

  getSystemSettings: async (): Promise<ApiResponse<SystemSetting[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<SystemSetting[]>>(ENDPOINTS.SETTINGS.SYSTEM);
    return data;
  },

  updateSystemSettings: async (payload: SystemSetting[]): Promise<ApiResponse<SystemSetting[]>> => {
    const { data } = await axiosInstance.put<ApiResponse<SystemSetting[]>>(ENDPOINTS.SETTINGS.SYSTEM, payload);
    return data;
  },

  listRoles: async (): Promise<ApiResponse<RoleInfo[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<RoleInfo[]>>(ENDPOINTS.ROLES.BASE);
    return data;
  },

  getRolePermissions: async (roleId: number): Promise<ApiResponse<PermissionInfo[]>> => {
    const { data } = await axiosInstance.get<ApiResponse<PermissionInfo[]>>(ENDPOINTS.ROLES.PERMISSIONS(roleId));
    return data;
  },

  exportBackup: async (): Promise<Blob> => {
    const { data } = await axiosInstance.get<Blob>(ENDPOINTS.SETTINGS.BACKUP_EXPORT, { responseType: 'blob' });
    return data;
  },
};

export default settingsApi;
