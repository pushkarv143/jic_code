import { useMemo, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import DirectionsBusOutlinedIcon from '@mui/icons-material/DirectionsBusOutlined';
import PageHeader from '@/components/common/PageHeader';

const TABS = [
  { value: 'overview', label: 'Overview', icon: <DashboardOutlinedIcon fontSize="small" /> },
  { value: 'students', label: 'Students', icon: <SchoolOutlinedIcon fontSize="small" /> },
  { value: 'teachers', label: 'Teachers', icon: <BadgeOutlinedIcon fontSize="small" /> },
  { value: 'attendance', label: 'Attendance', icon: <EventAvailableOutlinedIcon fontSize="small" /> },
  { value: 'fees', label: 'Fees', icon: <PaidOutlinedIcon fontSize="small" /> },
  { value: 'payroll', label: 'Payroll', icon: <RequestQuoteOutlinedIcon fontSize="small" /> },
  { value: 'library', label: 'Library', icon: <MenuBookOutlinedIcon fontSize="small" /> },
  { value: 'transport', label: 'Transport', icon: <DirectionsBusOutlinedIcon fontSize="small" /> },
];

/** Shared Tabs sub-nav for the /app/reports/* module — Overview + one tab per module's analytics. */
export function ReportsLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const current = TABS.find((t) => location.pathname.includes(`/reports/${t.value}`))?.value ?? TABS[0].value;

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/reports/${value}`);
  };

  const tabs = useMemo(() => TABS, []);

  return (
    <Box>
      <PageHeader
        title="Reports & Analytics"
        subtitle="School-wide analytics, breakdowns and downloadable reports"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Reports' }]}
      />
      <Card sx={{ mb: 2.5 }}>
        <Tabs
          value={current}
          onChange={handleChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          {tabs.map((t) => (
            <Tab key={t.value} value={t.value} label={t.label} icon={t.icon} iconPosition="start" sx={{ minHeight: 48 }} />
          ))}
        </Tabs>
      </Card>
      <Outlet />
    </Box>
  );
}

export default ReportsLayout;
