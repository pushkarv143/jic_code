import { useMemo } from 'react';
import { useAppSelector } from '@/store/hooks';
import type { Permission, Role } from '@/types';

export interface PermissionsApi {
  role: Role | undefined;
  permissions: Set<Permission>;
  /** True when the signed-in user's role grants every one of `names`. */
  can: (...names: Permission[]) => boolean;
  /** True when the signed-in user's role grants at least one of `names`. */
  canAny: (...names: Permission[]) => boolean;
  /** True when the signed-in user holds one of `roles`. */
  is: (...roles: Role[]) => boolean;
  /** True for the roles that see the whole school rather than a slice of it. */
  isManagement: boolean;
  /** True for TEACHER/CLASS_TEACHER, whose data is scoped to what they teach. */
  isTeaching: boolean;
  /** True for STUDENT/PARENT, whose data is scoped to themselves/their children. */
  isSelfScoped: boolean;
}

export const MANAGEMENT_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
export const TEACHING_ROLES: Role[] = ['TEACHER', 'CLASS_TEACHER'];
export const SELF_SCOPED_ROLES: Role[] = ['STUDENT', 'PARENT'];

/**
 * Reads the role and permission grants the backend returned at login.
 *
 * These drive what the UI *offers*, not what the user may actually do — the API
 * enforces the same grants independently, so hiding a button here is a usability
 * choice and never the thing standing between a student and someone else's data.
 */
export function usePermissions(): PermissionsApi {
  const user = useAppSelector((state) => state.auth.user);

  return useMemo(() => {
    const role = user?.role;
    const permissions = new Set<Permission>(user?.permissions ?? []);

    const can = (...names: Permission[]) => names.every((name) => permissions.has(name));
    const canAny = (...names: Permission[]) => names.some((name) => permissions.has(name));
    const is = (...roles: Role[]) => Boolean(role && roles.includes(role));

    return {
      role,
      permissions,
      can,
      canAny,
      is,
      isManagement: is(...MANAGEMENT_ROLES),
      isTeaching: is(...TEACHING_ROLES),
      isSelfScoped: is(...SELF_SCOPED_ROLES),
    };
  }, [user]);
}

export default usePermissions;
