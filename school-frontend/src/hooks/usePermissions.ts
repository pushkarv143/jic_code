import type { Permission, Role } from '@/types';
import {
  useAccess,
  MANAGEMENT_ROLES as ACCESS_MANAGEMENT_ROLES,
  TEACHING_ROLES as ACCESS_TEACHING_ROLES,
  SELF_SCOPED_ROLES as ACCESS_SELF_SCOPED_ROLES,
} from '@/access/AccessProvider';

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
  /** True for TEACHER, whose data is scoped to the students they teach. */
  isTeaching: boolean;
  /** True for STUDENT/PARENT, whose data is scoped to themselves/their children. */
  isSelfScoped: boolean;
}

export const MANAGEMENT_ROLES = ACCESS_MANAGEMENT_ROLES;
export const TEACHING_ROLES = ACCESS_TEACHING_ROLES;
export const SELF_SCOPED_ROLES = ACCESS_SELF_SCOPED_ROLES;

/**
 * Thin adapter over {@link useAccess}, kept so the pages already written against
 * this shape do not each need editing.
 *
 * <p>It used to read the permission list cached in `localStorage` at login and
 * treat an empty set as "grants unknown, so allow" — which meant two different
 * definitions of what a user could do, one of them optimistic. Everything now
 * resolves to the single live answer from `GET /api/v1/me/access`, re-read on
 * login, on token refresh and on logout, and strict: no grant, no action.
 *
 * <p>Prefer `useAccess()` in new code — it also exposes `settled`, `error`,
 * `moduleEnabled` and the homeroom assignment, none of which fit this interface.
 */
export function usePermissions(): PermissionsApi {
  const { role, permissions, can, canAny, is, isManagement, isTeaching, isSelfScoped } = useAccess();
  return { role, permissions, can, canAny, is, isManagement, isTeaching, isSelfScoped };
}

export default usePermissions;
