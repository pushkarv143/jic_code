import { useMemo, type ReactElement, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import EditCalendarOutlinedIcon from '@mui/icons-material/EditCalendarOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined';
import PageHeader from '@/components/common/PageHeader';
import { useAppSelector } from '@/store/hooks';
import type { Role } from '@/types';

const STAFF_MARKING_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'TEACHER'];

interface AttendanceTab {
  value: string;
  label: string;
  icon: ReactElement;
  roles?: Role[];
}

const TABS: AttendanceTab[] = [
  { value: 'mark', label: 'Mark Attendance', icon: <EditCalendarOutlinedIcon fontSize="small" />, roles: STAFF_MARKING_ROLES },
  { value: 'reports', label: 'Reports', icon: <FactCheckOutlinedIcon fontSize="small" /> },
  { value: 'monthly', label: 'Monthly Register', icon: <CalendarMonthOutlinedIcon fontSize="small" /> },
  { value: 'teachers', label: 'Teacher Attendance', icon: <HowToRegOutlinedIcon fontSize="small" />, roles: STAFF_MARKING_ROLES },
];

/** Shared Tabs sub-nav for the /app/attendance/* module, so mark/reports/monthly/teachers read as one screen. */
export function AttendanceLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const role = useAppSelector((state) => state.auth.user?.role);

  const visibleTabs = useMemo(() => TABS.filter((t) => !t.roles || (role && t.roles.includes(role))), [role]);

  const current = visibleTabs.find((t) => location.pathname.includes(`/attendance/${t.value}`))?.value
    ?? visibleTabs[0]?.value
    ?? 'reports';

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/attendance/${value}`);
  };

  return (
    <Box>
      <PageHeader
        title="Attendance"
        subtitle="Mark and review daily attendance for students and staff"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Attendance' }]}
      />
      <Card sx={{ mb: 2.5 }}>
        <Tabs
          value={current}
          onChange={handleChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          {visibleTabs.map((t) => (
            <Tab key={t.value} value={t.value} label={t.label} icon={t.icon} iconPosition="start" sx={{ minHeight: 48 }} />
          ))}
        </Tabs>
      </Card>
      <Outlet />
    </Box>
  );
}

export default AttendanceLayout;
