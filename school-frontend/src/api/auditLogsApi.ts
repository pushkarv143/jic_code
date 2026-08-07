import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, AuditLog, PageResponse } from '@/types';

export interface AuditLogListParams {
  userId?: number;
  entityName?: string;
  action?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

/** Typed wrapper around GET /api/v1/audit-logs (paginated, SUPER_ADMIN/PRINCIPAL only). */
export const auditLogsApi = {
  list: async (params: AuditLogListParams = {}): Promise<ApiResponse<PageResponse<AuditLog>>> => {
    const { data } = await axiosInstance.get<ApiResponse<PageResponse<AuditLog>>>(ENDPOINTS.AUDIT_LOGS.BASE, {
      params,
    });
    return data;
  },
};

export default auditLogsApi;
