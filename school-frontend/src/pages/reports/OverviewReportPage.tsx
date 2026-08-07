import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import { useSnackbar } from 'notistack';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import BarChartCard from '@/components/charts/BarChartCard';
import LineChartCard from '@/components/charts/LineChartCard';
import reportsApi from '@/api/reportsApi';
import payrollApi from '@/api/payrollApi';
import { formatCurrencyINR, formatNumber } from '@/utils/format';
import type { AnalyticsDashboard, AttendanceSummaryReport, FeeCollectionReport, PayrollDashboardSummary } from '@/types';

/** /analytics/dashboard rendered as a polished StatCard grid plus two headline trend charts pulled from the attendance and fee-collection summaries. */
export function OverviewReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [dashboard, setDashboard] = useState<AnalyticsDashboard | null>(null);
  const [attendance, setAttendance] = useState<AttendanceSummaryReport | null>(null);
  const [feeCollection, setFeeCollection] = useState<FeeCollectionReport | null>(null);
  const [payrollDashboard, setPayrollDashboard] = useState<PayrollDashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    const now = new Date();
    Promise.allSettled([
      reportsApi.getAnalyticsDashboard(),
      reportsApi.getAttendanceSummary(),
      reportsApi.getFeeCollection(),
      payrollApi.payroll.getDashboard({ month: now.getMonth() + 1, year: now.getFullYear() }),
    ])
      .then(([dashRes, attRes, feeRes, payrollRes]) => {
        if (dashRes.status === 'fulfilled') setDashboard(dashRes.value.data);
        else enqueueSnackbar('Could not load the analytics overview.', { variant: 'error' });
        if (attRes.status === 'fulfilled') setAttendance(attRes.value.data);
        if (feeRes.status === 'fulfilled') setFeeCollection(feeRes.value.data);
        if (payrollRes.status === 'fulfilled') setPayrollDashboard(payrollRes.value.data);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading analytics overview..." />;

  const attendanceChartData = (attendance?.byClass ?? []).map((row) => ({ className: row.className, Attendance: row.percentage }));
  const feeChartData = (feeCollection?.byMonth ?? []).map((row) => ({ month: String(row.month), Collected: row.collected }));

  return (
    <Box>
      <Grid container spacing={2.5} sx={{ mb: 0.5 }}>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<SchoolOutlinedIcon />} label="Active Students" value={formatNumber(dashboard?.totalActiveStudents ?? 0)} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<BadgeOutlinedIcon />} label="Active Teachers" value={formatNumber(dashboard?.totalActiveTeachers ?? 0)} color="info" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<EventAvailableOutlinedIcon />}
            label="Average Attendance"
            value={dashboard ? `${dashboard.averageAttendancePercentage.toFixed(1)}%` : '-'}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<PaidOutlinedIcon />}
            label="Total Fees Collected"
            value={dashboard ? formatCurrencyINR(dashboard.totalFeeCollected) : '-'}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<WarningAmberOutlinedIcon />}
            label="Fees Outstanding"
            value={dashboard ? formatCurrencyINR(dashboard.totalFeeOutstanding) : '-'}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<RequestQuoteOutlinedIcon />}
            label="Payroll Pending (This Month)"
            value={payrollDashboard ? formatCurrencyINR(payrollDashboard.totalPending) : '-'}
            color="warning"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
        <Grid item xs={12} lg={6}>
          <BarChartCard
            title="Attendance by Class"
            subtitle="Average attendance percentage"
            data={attendanceChartData}
            xKey="className"
            series={[{ key: 'Attendance', label: 'Attendance %' }]}
            valueFormatter={(v) => `${v.toFixed(1)}%`}
          />
        </Grid>
        <Grid item xs={12} lg={6}>
          <LineChartCard
            title="Fee Collection Trend"
            subtitle="Amount collected by month"
            data={feeChartData}
            xKey="month"
            series={[{ key: 'Collected', label: 'Collected' }]}
            valueFormatter={(v) => formatCurrencyINR(v)}
          />
        </Grid>
      </Grid>
    </Box>
  );
}

export default OverviewReportPage;
