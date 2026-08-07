import { Navigate, Outlet } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';
import type { Role } from '@/types';

export interface RoleBasedRouteProps {
  allowedRoles: Role[];
}

/** Renders nested routes only if the current user's role is in allowedRoles, else redirects to /403. */
export function RoleBasedRoute({ allowedRoles }: RoleBasedRouteProps) {
  const role = useAppSelector((state) => state.auth.user?.role);

  if (!role || !allowedRoles.includes(role)) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}

export default RoleBasedRoute;
