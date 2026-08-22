import { Navigate, Outlet } from 'react-router-dom';
import PageLoader from '@/components/common/PageLoader';
import { useAccess } from '@/access/AccessProvider';
import type { Permission } from '@/types';

export interface PermissionRouteProps {
  /** Renders the nested routes when the user holds at least one of these. */
  anyOf?: Permission[];
  /** Renders the nested routes only when the user holds every one of these. */
  allOf?: Permission[];
  /** Additionally require the named org module to be switched on. */
  module?: string;
  /** Additionally require an actual homeroom assignment (for My Class). */
  requiresHomeroom?: boolean;
}

/**
 * Route guard keyed on permissions rather than on a hardcoded role list.
 *
 * <p>Companion to {@link RoleBasedRoute}, not a replacement: a role list is still
 * the honest guard for a screen that is genuinely about a role. But anything an
 * administrator can now reconfigure at runtime has to be guarded by the thing they
 * reconfigure — a route pinned to `['SUPER_ADMIN','PRINCIPAL']` cannot be opened up
 * by granting a permission, which defeats the point of making the grants editable.
 *
 * <p>Waits for `/me/access` to settle before deciding. Without that, every guarded
 * route would bounce to /403 on a hard refresh, because the permission set is empty
 * for the moment before the fetch resolves.
 */
export function PermissionRoute({ anyOf, allOf, module, requiresHomeroom }: PermissionRouteProps) {
  const { can, canAny, moduleEnabled, isClassTeacherOfOwnSection, settled, role } = useAccess();

  if (!settled) return <PageLoader />;
  if (!role) return <Navigate to="/403" replace />;

  if (module && !moduleEnabled(module)) return <Navigate to="/403" replace />;
  if (requiresHomeroom && !isClassTeacherOfOwnSection) return <Navigate to="/403" replace />;
  if (anyOf?.length && !canAny(...anyOf)) return <Navigate to="/403" replace />;
  if (allOf?.length && !can(...allOf)) return <Navigate to="/403" replace />;

  return <Outlet />;
}

export default PermissionRoute;
