import { useAppSelector } from '@/store/hooks';
import AccountantDashboard from './AccountantDashboard';
import LibrarianDashboard from './LibrarianDashboard';
import ReceptionistDashboard from './ReceptionistDashboard';
import SecurityGuardDashboard from './SecurityGuardDashboard';

/**
 * Dispatches to the right operational dashboard for ACCOUNTANT / LIBRARIAN /
 * RECEPTIONIST / SECURITY_GUARD. These four roles have very different daily
 * jobs (money, books, front-desk, premises) so each gets its own focused
 * variant rather than one generic "staff" screen with fabricated numbers.
 */
export function StaffDashboard() {
  const role = useAppSelector((state) => state.auth.user?.role);

  switch (role) {
    case 'ACCOUNTANT':
      return <AccountantDashboard />;
    case 'LIBRARIAN':
      return <LibrarianDashboard />;
    case 'RECEPTIONIST':
      return <ReceptionistDashboard />;
    default:
      return <SecurityGuardDashboard />;
  }
}

export default StaffDashboard;
