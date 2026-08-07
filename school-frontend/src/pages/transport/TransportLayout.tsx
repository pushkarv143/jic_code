import { useMemo, type ReactElement, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import DirectionsBusOutlinedIcon from '@mui/icons-material/DirectionsBusOutlined';
import GroupOutlinedIcon from '@mui/icons-material/GroupOutlined';
import PageHeader from '@/components/common/PageHeader';
import { useAppSelector } from '@/store/hooks';
import type { Role } from '@/types';

const FLEET_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];

interface TransportTab {
  value: string;
  label: string;
  icon: ReactElement;
  roles?: Role[];
}

const TABS: TransportTab[] = [
  { value: 'fleet', label: 'Buses & Routes', icon: <DirectionsBusOutlinedIcon fontSize="small" />, roles: FLEET_ROLES },
  { value: 'assignments', label: 'Student Assignments', icon: <GroupOutlinedIcon fontSize="small" /> },
];

/** Shared Tabs sub-nav for the /app/transport/* module. Fleet management is restricted; assignments is open to all. */
export function TransportLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const role = useAppSelector((state) => state.auth.user?.role);

  const visibleTabs = useMemo(() => TABS.filter((t) => !t.roles || (role && t.roles.includes(role))), [role]);

  const current =
    visibleTabs.find((t) => location.pathname.includes(`/transport/${t.value}`))?.value ??
    visibleTabs[0]?.value ??
    'assignments';

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/transport/${value}`);
  };

  return (
    <Box>
      <PageHeader
        title="Transport"
        subtitle="Manage buses, routes, drivers and student transport allocation"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Transport' }]}
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

export default TransportLayout;
