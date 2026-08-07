import { useMemo, type ReactElement, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import MeetingRoomOutlinedIcon from '@mui/icons-material/MeetingRoomOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import PageHeader from '@/components/common/PageHeader';
import { useAppSelector } from '@/store/hooks';
import type { Role } from '@/types';

const ROOMS_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];

interface HostelTab {
  value: string;
  label: string;
  icon: ReactElement;
  roles?: Role[];
}

const TABS: HostelTab[] = [
  { value: 'rooms', label: 'Rooms', icon: <MeetingRoomOutlinedIcon fontSize="small" />, roles: ROOMS_ROLES },
  { value: 'residents', label: 'Residents', icon: <GroupsOutlinedIcon fontSize="small" /> },
  { value: 'visitors', label: 'Visitors', icon: <HowToRegOutlinedIcon fontSize="small" /> },
  { value: 'fees', label: 'Hostel Fees', icon: <PaidOutlinedIcon fontSize="small" /> },
];

/** Shared Tabs sub-nav for the /app/hostel/* module. Rooms management is restricted; other tabs branch to self-view internally. */
export function HostelLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const role = useAppSelector((state) => state.auth.user?.role);

  const visibleTabs = useMemo(() => TABS.filter((t) => !t.roles || (role && t.roles.includes(role))), [role]);

  const current =
    visibleTabs.find((t) => location.pathname.includes(`/hostel/${t.value}`))?.value ?? visibleTabs[0]?.value ?? 'residents';

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/hostel/${value}`);
  };

  return (
    <Box>
      <PageHeader
        title="Hostel"
        subtitle="Manage hostels, room allocation, visitors and hostel fees"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Hostel' }]}
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

export default HostelLayout;
