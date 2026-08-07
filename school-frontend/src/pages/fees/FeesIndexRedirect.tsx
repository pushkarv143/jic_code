import { Navigate } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';

/** Lands STUDENT/PARENT on "My Fees" and everyone else on the Collect Fees tab. */
export function FeesIndexRedirect() {
  const role = useAppSelector((state) => state.auth.user?.role);
  const isSelfView = role === 'STUDENT' || role === 'PARENT';
  return <Navigate to={isSelfView ? 'my' : 'collect'} replace />;
}

export default FeesIndexRedirect;
