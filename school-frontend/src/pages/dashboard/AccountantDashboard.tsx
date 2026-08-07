import { useEffect, useState } from 'react';
import Grid from '@mui/material/Grid';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import { Link as RouterLink } from 'react-router-dom';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import LineChartCard from '@/components/charts/LineChartCard';
import PieChartCard from '@/components/charts/PieChartCard';
import NotificationsCard from './widgets/NotificationsCard';
import feesApi from '@/api/feesApi';
import payrollApi from '@/api/payrollApi';
import reportsApi from '@/api/reportsApi';
import { formatCurrencyINR } from '@/utils/format';
import type { DuesSummary, FeeCollectionReport, FeePayment, PayrollDashboardSummary } from '@/types';

const PAYMENT_MODE_COLOR: Record<string, 'default' | 'primary' | 'info' | 'success'> = {
  CASH: 'default',
  ONLINE: 'primary',
  CARD: 'info',
  CHEQUE: 'success',
};

/** Fee-collection-focused dashboard for ACCOUNTANT, built from live dues/payments/payroll data. */
export function AccountantDashboard() {
  const { enqueueSnackbar } = useSnackbar();
  const [dues, setDues] = useState<DuesSummary | null>(null);
  const [feeCollection, setFeeCollection] = useState<FeeCollectionReport | null>(null);
  const [payrollDashboard, setPayrollDashboard] = useState<PayrollDashboardSummary | null>(null);
  const [recentPayments, setRecentPayments] = useState<FeePayment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    const now = new Date();
    Promise.allSettled([
      feesApi.dues.getSummary(),
      reportsApi.getFeeCollection(),
      payrollApi.payroll.getDashboard({ month: now.getMonth() + 1, year: now.getFullYear() }),
      feesApi.feePayments.list({ size: 5, sort: 'paymentDate,desc' }),
    ])
      .then(([duesRes, feeRes, payrollRes, paymentsRes]) => {
        if (duesRes.status === 'fulfilled') setDues(duesRes.value.data);
        else enqueueSnackbar('Could not load the fee dues summary.', { variant: 'error' });
        if (feeRes.status === 'fulfilled') setFeeCollection(feeRes.value.data);
        if (payrollRes.status === 'fulfilled') setPayrollDashboard(payrollRes.value.data);
        if (paymentsRes.status === 'fulfilled') setRecentPayments(paymentsRes.value.data.content);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading dashboard..." />;

  const feeMonthChartData = (feeCollection?.byMonth ?? []).map((row) => ({
    month: String(row.month),
    Collected: row.collected,
  }));
  const feeCategoryPieData = (feeCollection?.byCategory ?? []).map((row) => ({
    name: row.categoryName,
    value: row.collected,
  }));

  return (
    <Box>
      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<PaidOutlinedIcon />}
            label="Fees Collected"
            value={dues ? formatCurrencyINR(dues.totalCollected) : '-'}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<WarningAmberOutlinedIcon />}
            label="Fees Outstanding"
            value={dues ? formatCurrencyINR(dues.totalOutstanding) : '-'}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<GroupsOutlinedIcon />}
            label="Students With Dues"
            value={dues ? dues.studentCount : '-'}
            color="info"
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
          <PieChartCard
            title="Fee Collection by Category"
            subtitle="Current academic year"
            data={feeCategoryPieData}
            valueFormatter={(v) => formatCurrencyINR(v)}
            height={320}
          />
        </Grid>

        <Grid item xs={12} lg={7}>
          <Card sx={{ height: '100%' }}>
            <CardHeader
              title="Recent Payments"
              subheader="Latest fee payments recorded"
              action={
                <Button size="small" component={RouterLink} to="/app/fees">
                  View All
                </Button>
              }
            />
            <CardContent sx={{ pt: 0 }}>
              {recentPayments.length === 0 ? (
                <EmptyState
                  icon={<ReceiptLongOutlinedIcon fontSize="large" />}
                  title="No payments recorded yet"
                  description="Payments you record will show up here."
                />
              ) : (
                <List disablePadding>
                  {recentPayments.map((payment, idx) => (
                    <Box key={payment.id}>
                      <ListItem disableGutters sx={{ py: 1.25 }}>
                        <ListItemText
                          primary={`Receipt ${payment.receiptNumber} · ${formatCurrencyINR(payment.amount)}`}
                          secondary={`${dayjs(payment.paymentDate).format('DD MMM YYYY')}${payment.collectedByName ? ` · Collected by ${payment.collectedByName}` : ''}`}
                          primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                          secondaryTypographyProps={{ fontSize: '0.75rem' }}
                        />
                        <Chip
                          label={payment.paymentMode}
                          size="small"
                          color={PAYMENT_MODE_COLOR[payment.paymentMode] ?? 'default'}
                          variant="outlined"
                        />
                      </ListItem>
                      {idx < recentPayments.length - 1 && <Divider />}
                    </Box>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Quick Actions" subheader="Jump to what you use most" />
            <CardContent sx={{ pt: 0 }}>
              <Stack spacing={1.5}>
                <Button variant="outlined" fullWidth component={RouterLink} to="/app/fees">
                  Record a Fee Payment
                </Button>
                <Button variant="outlined" fullWidth component={RouterLink} to="/app/payroll/runs">
                  Run Payroll
                </Button>
                <Button variant="outlined" fullWidth component={RouterLink} to="/app/reports/overview">
                  View Reports
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <NotificationsCard />
        </Grid>
      </Grid>
    </Box>
  );
}

export default AccountantDashboard;
