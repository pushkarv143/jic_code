import axiosInstance from './axiosInstance';
import { ENDPOINTS } from './endpoints';
import type { ApiResponse, MyAccess, OrgModule, Permission } from '@/types';

/**
 * The authorization-configuration surface: what the caller may do, and — for an
 * administrator — how to change it for everyone.
 */
export const accessApi = {
  /**
   * What the signed-in user may actually do.
   *
   * <p>Preferred over the permission list cached in the login response, which goes
   * stale the moment an administrator edits a role or switches a module off.
   */
  async getMyAccess(): Promise<MyAccess> {
    const { data } = await axiosInstance.get<ApiResponse<MyAccess>>(ENDPOINTS.ME.ACCESS);
    return data.data;
  },

  async getModules(): Promise<OrgModule[]> {
    const { data } = await axiosInstance.get<ApiResponse<OrgModule[]>>(ENDPOINTS.ORG.MODULES);
    return data.data;
  },

  /**
   * Toggles modules. Partial by design — send only what changed; anything omitted
   * is left alone, so two administrators editing different modules do not clobber
   * each other.
   */
  async updateModules(modules: Record<string, boolean>): Promise<OrgModule[]> {
    const { data } = await axiosInstance.put<ApiResponse<OrgModule[]>>(ENDPOINTS.ORG.MODULES, { modules });
    return data.data;
  },
};

export interface PermissionRow {
  id: number;
  name: Permission;
  module: string;
  description: string | null;
}

export const rolesApi = {
  async list(): Promise<Array<{ id: number; name: string; description: string | null }>> {
    const { data } = await axiosInstance.get<ApiResponse<Array<{ id: number; name: string; description: string | null }>>>(
      ENDPOINTS.ROLES.BASE,
    );
    return data.data;
  },

  /** Every permission that exists, for the editor's checkbox board. */
  async permissionCatalogue(): Promise<PermissionRow[]> {
    const { data } = await axiosInstance.get<ApiResponse<PermissionRow[]>>(ENDPOINTS.ROLES.PERMISSION_CATALOGUE);
    return data.data;
  },

  async permissionsFor(roleId: number): Promise<PermissionRow[]> {
    const { data } = await axiosInstance.get<ApiResponse<PermissionRow[]>>(ENDPOINTS.ROLES.PERMISSIONS(roleId));
    return data.data;
  },

  /**
   * Replaces a role's grants with exactly `permissions`.
   *
   * Absolute, not a delta: anything left out is revoked. SUPER_ADMIN is rejected
   * by the API — it always holds everything.
   */
  async replacePermissions(roleId: number, permissions: Permission[]): Promise<PermissionRow[]> {
    const { data } = await axiosInstance.put<ApiResponse<PermissionRow[]>>(
      ENDPOINTS.ROLES.PERMISSIONS(roleId),
      { permissions },
    );
    return data.data;
  },
};

export default accessApi;
