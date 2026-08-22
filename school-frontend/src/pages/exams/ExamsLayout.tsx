import { useMemo, type ReactElement, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import SettingsSuggestOutlinedIcon from '@mui/icons-material/SettingsSuggestOutlined';
import EditNoteOutlinedIcon from '@mui/icons-material/EditNoteOutlined';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import PageHeader from '@/components/common/PageHeader';
import { useAppSelector } from '@/store/hooks';
import type { Role } from '@/types';

const SETUP_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'TEACHER'];

interface ExamTab {
  value: string;
  label: string;
  icon: ReactElement;
  roles?: Role[];
}

const TABS: ExamTab[] = [
  { value: 'setup', label: 'Exam Setup', icon: <SettingsSuggestOutlinedIcon fontSize="small" />, roles: SETUP_ROLES },
  { value: 'marks-entry', label: 'Marks Entry', icon: <EditNoteOutlinedIcon fontSize="small" />, roles: SETUP_ROLES },
  { value: 'results', label: 'Results & Report Cards', icon: <AssessmentOutlinedIcon fontSize="small" /> },
];

/** Shared Tabs sub-nav for the /app/exams/* module. Setup/Marks Entry are staff-only; Results branches by role internally. */
export function ExamsLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const role = useAppSelector((state) => state.auth.user?.role);

  const visibleTabs = useMemo(() => TABS.filter((t) => !t.roles || (role && t.roles.includes(role))), [role]);

  const current =
    visibleTabs.find((t) => location.pathname.includes(`/exams/${t.value}`))?.value ?? visibleTabs[0]?.value ?? 'results';

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/exams/${value}`);
  };

  return (
    <Box>
      <PageHeader
        title="Exams & Marks"
        subtitle="Schedule exams, enter marks and publish results"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Exams & Marks' }]}
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

export default ExamsLayout;
