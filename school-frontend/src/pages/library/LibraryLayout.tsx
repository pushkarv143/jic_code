import { useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Grid from '@mui/material/Grid';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import LibraryBooksOutlinedIcon from '@mui/icons-material/LibraryBooksOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import AssignmentReturnOutlinedIcon from '@mui/icons-material/AssignmentReturnOutlined';
import ReportProblemOutlinedIcon from '@mui/icons-material/ReportProblemOutlined';
import AutoStoriesOutlinedIcon from '@mui/icons-material/AutoStoriesOutlined';
import PageHeader from '@/components/common/PageHeader';
import StatCard from '@/components/common/StatCard';
import libraryApi from '@/api/libraryApi';
import { formatNumber } from '@/utils/format';
import type { LibraryDashboard } from '@/types';

const TABS = [
  { value: 'books', label: 'Books', icon: <AutoStoriesOutlinedIcon fontSize="small" /> },
  { value: 'issue-return', label: 'Issue / Return', icon: <AssignmentReturnOutlinedIcon fontSize="small" /> },
  { value: 'overdue', label: 'Overdue', icon: <ReportProblemOutlinedIcon fontSize="small" /> },
];

/** Shared Tabs sub-nav for the /app/library/* module (staff-only: Librarian + management), plus a KPI strip. */
export function LibraryLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<LibraryDashboard | null>(null);

  useEffect(() => {
    libraryApi.dashboard
      .get()
      .then((res) => setDashboard(res.data))
      .catch(() => undefined);
  }, []);

  const current = useMemo(
    () => TABS.find((t) => location.pathname.includes(`/library/${t.value}`))?.value ?? 'books',
    [location.pathname],
  );

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/library/${value}`);
  };

  return (
    <Box>
      <PageHeader
        title="Library"
        subtitle="Manage the book catalog, issues, returns and fines"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Library' }]}
      />

      {dashboard && (
        <Grid container spacing={2} sx={{ mb: 2.5 }}>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<MenuBookOutlinedIcon />} label="Titles" value={formatNumber(dashboard.totalBooks)} />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<LibraryBooksOutlinedIcon />} label="Total Copies" value={formatNumber(dashboard.totalCopies)} color="secondary" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<CheckCircleOutlinedIcon />} label="Available Copies" value={formatNumber(dashboard.availableCopies)} color="success" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<ReportProblemOutlinedIcon />} label="Overdue" value={formatNumber(dashboard.overdueCount)} color="error" />
          </Grid>
        </Grid>
      )}

      <Card sx={{ mb: 2.5 }}>
        <Tabs
          value={current}
          onChange={handleChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          {TABS.map((t) => (
            <Tab key={t.value} value={t.value} label={t.label} icon={t.icon} iconPosition="start" sx={{ minHeight: 48 }} />
          ))}
        </Tabs>
      </Card>
      <Outlet />
    </Box>
  );
}

export default LibraryLayout;
