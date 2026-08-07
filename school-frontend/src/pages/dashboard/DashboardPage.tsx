import type { ReactNode } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import dayjs from 'dayjs';
import { useAppSelector } from '@/store/hooks';
import { formatRoleLabel } from '@/utils/format';
import AdminDashboard from './AdminDashboard';
import TeacherDashboard from './TeacherDashboard';
import StudentParentDashboard from './StudentParentDashboard';
import StaffDashboard from './StaffDashboard';
import type { Role } from '@/types';

const MANAGEMENT_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
const TEACHER_ROLES: Role[] = ['TEACHER', 'CLASS_TEACHER'];
const LEARNER_ROLES: Role[] = ['STUDENT', 'PARENT'];

/**
 * Role-aware dashboard entry point. Picks one of four visibly distinct
 * dashboard variants based on the signed-in user's role.
 */
export function DashboardPage() {
  const user = useAppSelector((state) => state.auth.user);
  const role = user?.role;

  let content: ReactNode;
  if (role && MANAGEMENT_ROLES.includes(role)) {
    content = <AdminDashboard />;
  } else if (role && TEACHER_ROLES.includes(role)) {
    content = <TeacherDashboard />;
  } else if (role && LEARNER_ROLES.includes(role)) {
    content = <StudentParentDashboard />;
  } else {
    content = <StaffDashboard />;
  }

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          alignItems: { xs: 'flex-start', sm: 'center' },
          justifyContent: 'space-between',
          gap: 1,
          mb: 3,
        }}
      >
        <Box>
          <Typography variant="h4" component="h1">
            Welcome back, {user?.firstName ?? 'there'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            {dayjs().format('dddd, DD MMMM YYYY')} · Here&apos;s what&apos;s happening today.
          </Typography>
        </Box>
        {role && <Chip label={formatRoleLabel(role)} color="primary" />}
      </Box>
      {content}
    </Box>
  );
}

export default DashboardPage;
