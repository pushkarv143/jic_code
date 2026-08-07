import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined';
import HourglassEmptyOutlinedIcon from '@mui/icons-material/HourglassEmptyOutlined';
import StatCard from '@/components/common/StatCard';
import DataTable from '@/components/common/DataTable';
import BarChartCard from '@/components/charts/BarChartCard';
import reportsApi from '@/api/reportsApi';
import { exportRowsToCsv } from '@/utils/csvExport';
import { formatCurrencyINR } from '@/utils/format';
import type { MonthPaidBreakdown, PayrollSummaryReport } from '@/types';

/** Payroll analytics — year filter -> paid/pending stat cards, byMonth bar chart, exportable table. */
export function PayrollReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [year, setYear] = useState(dayjs().year());
  const [report, setReport] = useState<PayrollSummaryReport | null>(null);
  const [loading, setLoading] = useState(false);

  const years = useMemo(() => Array.from({ length: 5 }, (_, i) => dayjs().year() - 3 + i), []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reportsApi.getPayrollSummary({ year });
      setReport(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the payroll report.', { variant: 'error' });
      setReport(null);
    } finally {
      setLoading(false);
    }
  }, [year, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: GridColDef<MonthPaidBreakdown>[] = useMemo(
    () => [
      { field: 'month', headerName: 'Month', flex: 1 },
      { field: 'paidAmount', headerName: 'Paid Amount', flex: 1, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
    ],
    [],
  );

  const handleExport = () => {
    if (!report) return;
    exportRowsToCsv(
      report.byMonth as unknown as Record<string, unknown>[],
      [
        { field: 'month', header: 'Month' },
        { field: 'paidAmount', header: 'Paid Amount' },
      ],
      'payroll-by-month-report',
    );
  };

  const chartData = (report?.byMonth ?? []).map((row) => ({ month: String(row.month), Paid: row.paidAmount }));

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField select fullWidth size="small" label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
                {years.map((y) => (
                  <MenuItem key={y} value={y}>
                    {y}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {report && (
        <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<AccountBalanceWalletOutlinedIcon />} label="Total Paid" value={formatCurrencyINR(report.totalPaidAmount)} color="success" />
          </Grid>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<HourglassEmptyOutlinedIcon />} label="Total Pending" value={formatCurrencyINR(report.totalPendingAmount)} color="warning" />
          </Grid>
        </Grid>
      )}

      <Box sx={{ mb: 2.5 }}>
        <BarChartCard
          title="Payroll Paid by Month"
          data={chartData}
          xKey="month"
          series={[{ key: 'Paid', label: 'Paid' }]}
          valueFormatter={(v) => formatCurrencyINR(v)}
        />
      </Box>

      <Card>
        <DataTable
          rows={report?.byMonth ?? []}
          columns={columns}
          loading={loading}
          getRowId={(row) => String(row.month)}
          onExport={handleExport}
          emptyTitle="No payroll data found"
        />
      </Card>
    </Box>
  );
}

export default PayrollReportPage;
