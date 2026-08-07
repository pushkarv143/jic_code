import { useEffect, useState } from 'react';
import Grid from '@mui/material/Grid';
import Box from '@mui/material/Box';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import LineChartCard from '@/components/charts/LineChartCard';
import BarChartCard from '@/components/charts/BarChartCard';
import PieChartCard from '@/components/charts/PieChartCard';
import UpcomingEventsCard from './widgets/UpcomingEventsCard';
import RecentActivitiesCard from './widgets/RecentActivitiesCard';
import NotificationsCard from './widgets/NotificationsCard';
import reportsApi from '@/api/reportsApi';
import payrollApi from '@/api/payrollApi';
import noticesApi from '@/api/noticesApi';
import eventsApi from '@/api/eventsApi';
import { formatCurrencyINR, formatNumber } from '@/utils/format';
import { useTranslation } from '@/i18n/LanguageProvider';
import type {
  AnalyticsDashboard,
  AttendanceSummaryReport,
  CalendarEvent,
  FeeCollectionReport,
  Notice,
  PayrollDashboardSummary,
} from '@/types';

/** School-wide dashboard for SUPER_ADMIN / PRINCIPAL / VICE_PRINCIPAL, built entirely from live reporting/analytics endpoints. */
export function AdminDashboard() {
  const { enqueueSnackbar } = useSnackbar();
  const t = useTranslation();
  const [dashboard, setDashboard] = useState<AnalyticsDashboard | null>(null);
  const [payrollDashboard, setPayrollDashboard] = useState<PayrollDashboardSummary | null>(null);
  const [attendance, setAttendance] = useState<AttendanceSummaryReport | null>(null);
  const [feeCollection, setFeeCollection] = useState<FeeCollectionReport | null>(null);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    const now = new Date();
    const today = dayjs().format('YYYY-MM-DD');
    Promise.allSettled([
      reportsApi.getAnalyticsDashboard(),
      payrollApi.payroll.getDashboard({ month: now.getMonth() + 1, year: now.getFullYear() }),
      reportsApi.getAttendanceSummary(),
      reportsApi.getFeeCollection(),
      noticesApi.list({ size: 5, sort: 'publishedAt,desc' }),
      eventsApi.list({ startDate: today }),
    ])
      .then(([dashRes, payrollRes, attRes, feeRes, noticesRes, eventsRes]) => {
        if (dashRes.status === 'fulfilled') setDashboard(dashRes.value.data);
        else enqueueSnackbar('Could not load the school-wide analytics overview.', { variant: 'error' });
        if (payrollRes.status === 'fulfilled') setPayrollDashboard(payrollRes.value.data);
        if (attRes.status === 'fulfilled') setAttendance(attRes.value.data);
        if (feeRes.status === 'fulfilled') setFeeCollection(feeRes.value.data);
        if (noticesRes.status === 'fulfilled') setNotices(noticesRes.value.data.content);
        if (eventsRes.status === 'fulfilled') setEvents(eventsRes.value.data);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading dashboard..." />;

  const attendanceChartData = (attendance?.byClass ?? []).map((row) => ({
    className: row.className,
    Attendance: row.percentage,
  }));
  const feeMonthChartData = (feeCollection?.byMonth ?? []).map((row) => ({
    month: String(row.month),
    Collected: row.collected,
  }));
  const feeCategoryPieData = (feeCollection?.byCategory ?? []).map((row) => ({
    name: row.categoryName,
    value: row.collected,
  }));

  const upcomingEvents = events
    .filter((e) => !dayjs(e.eventDate).isBefore(dayjs(), 'day'))
    .sort((a, b) => dayjs(a.eventDate).diff(dayjs(b.eventDate)))
    .slice(0, 5)
    .map((e) => ({ title: e.title, date: e.eventDate, type: e.eventType }));

  const recentNotices = notices.map((n) => ({
    title: n.title,
    detail: n.description,
    time: dayjs(n.publishedAt).format('DD MMM YYYY, hh:mm A'),
  }));

  return (
    <Box>
      <Grid container spacing={2.5} sx={{ mb: 0.5 }}>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <StatCard
            icon={<SchoolOutlinedIcon />}
            label={t('dashboard.activeStudents')}
            value={formatNumber(dashboard?.totalActiveStudents ?? 0)}
            color="primary"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <StatCard
            icon={<BadgeOutlinedIcon />}
            label={t('dashboard.activeTeachers')}
            value={formatNumber(dashboard?.totalActiveTeachers ?? 0)}
            color="info"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <StatCard
            icon={<EventAvailableOutlinedIcon />}
            label={t('dashboard.averageAttendance')}
            value={dashboard ? `${dashboard.averageAttendancePercentage.toFixed(1)}%` : '-'}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <StatCard
            icon={<PaidOutlinedIcon />}
            label={t('dashboard.feesCollected')}
            value={dashboard ? formatCurrencyINR(dashboard.totalFeeCollected) : '-'}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <StatCard
            icon={<WarningAmberOutlinedIcon />}
            label={t('dashboard.feesOutstanding')}
            value={dashboard ? formatCurrencyINR(dashboard.totalFeeOutstanding) : '-'}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <StatCard
            icon={<RequestQuoteOutlinedIcon />}
            label={t('dashboard.payrollPending')}
            value={payrollDashboard ? formatCurrencyINR(payrollDashboard.totalPending) : '-'}
            color="warning"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
        <Grid item xs={12} lg={7}>
          <BarChartCard
            title="Attendance by Class"
            subtitle="Average attendance percentage, current term"
            data={attendanceChartData}
            xKey="className"
            series={[{ key: 'Attendance', label: 'Attendance %' }]}
            valueFormatter={(v) => `${v.toFixed(1)}%`}
            height={300}
          />
        </Grid>
        <Grid item xs={12} lg={5}>
          <PieChartCard
            title="Fee Collection by Category"
            subtitle="Current academic year"
            data={feeCategoryPieData}
            valueFormatter={(v) => formatCurrencyINR(v)}
            height={300}
          />
        </Grid>
        <Grid item xs={12} lg={7}>
          <LineChartCard
            title="Fee Collection Trend"
            subtitle="Amount collected by month"
            data={feeMonthChartData}
            xKey="month"
            series={[{ key: 'Collected', label: 'Collected' }]}
            valueFormatter={(v) => formatCurrencyINR(v)}
            height={320}
          />
        </Grid>
        <Grid item xs={12} lg={5}>
          <UpcomingEventsCard events={upcomingEvents} />
        </Grid>
        <Grid item xs={12} lg={7}>
          <RecentActivitiesCard
            activities={recentNotices}
            title="Recent Notices"
            subheader="Latest circulars from the notice board"
          />
        </Grid>
        <Grid item xs={12} lg={5}>
          <NotificationsCard />
        </Grid>
      </Grid>
    </Box>
  );
}

export default AdminDashboard;
