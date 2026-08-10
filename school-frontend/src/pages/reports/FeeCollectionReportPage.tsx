import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import StatCard from '@/components/common/StatCard';
import DataTable from '@/components/common/DataTable';
import PieChartCard from '@/components/charts/PieChartCard';
import BarChartCard from '@/components/charts/BarChartCard';
import reportsApi from '@/api/reportsApi';
import academicYearsApi from '@/api/academicYearsApi';
import { exportRowsToCsv } from '@/utils/csvExport';
import { formatCurrencyINR } from '@/utils/format';
import type { AcademicYear, CategoryCollectedBreakdown, FeeCollectionReport } from '@/types';

/** Fee collection analytics — academic-year filter -> due/collected/outstanding stat cards, byCategory pie, byMonth bar, exportable table. */
export function FeeCollectionReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');
  const [report, setReport] = useState<FeeCollectionReport | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    academicYearsApi.list().then((res) => setYears(res.data)).catch(() => undefined);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reportsApi.getFeeCollection({ academicYearId: academicYearId || undefined });
      setReport(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the fee collection report.', { variant: 'error' });
      setReport(null);
    } finally {
      setLoading(false);
    }
  }, [academicYearId, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: GridColDef<CategoryCollectedBreakdown>[] = useMemo(
    () => [
      { field: 'categoryName', headerName: 'Fee Category', flex: 1 },
      { field: 'collected', headerName: 'Collected', flex: 1, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
    ],
    [],
  );

  const handleExport = () => {
    if (!report) return;
    exportRowsToCsv(
      report.byCategory as unknown as Record<string, unknown>[],
      [
        { field: 'categoryName', header: 'Fee Category' },
        { field: 'collected', header: 'Collected' },
      ],
      'fee-collection-by-category-report',
    );
  };

  const pieData = (report?.byCategory ?? []).map((row) => ({ name: row.categoryName, value: row.collected }));
  const barData = (report?.byMonth ?? []).map((row) => ({ month: String(row.month), Collected: row.collected }));

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Academic Year"
                value={academicYearId}
                onChange={(e) => setAcademicYearId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All years</MenuItem>
                {years.map((y) => (
                  <MenuItem key={y.id} value={y.id}>
                    {y.yearName}
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
            <StatCard icon={<AccountBalanceWalletOutlinedIcon />} label="Total Due" value={formatCurrencyINR(report.totalDue)} color="primary" />
          </Grid>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<PaidOutlinedIcon />} label="Total Collected" value={formatCurrencyINR(report.totalCollected)} color="success" />
          </Grid>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<WarningAmberOutlinedIcon />} label="Outstanding" value={formatCurrencyINR(report.totalOutstanding)} color="error" />
          </Grid>
        </Grid>
      )}

      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={12} md={5}>
          <PieChartCard title="Collection by Category" data={pieData} valueFormatter={(v) => formatCurrencyINR(v)} />
        </Grid>
        <Grid item xs={12} md={7}>
          <BarChartCard
            title="Collection by Month"
            data={barData}
            xKey="month"
            series={[{ key: 'Collected', label: 'Collected' }]}
            valueFormatter={(v) => formatCurrencyINR(v)}
          />
        </Grid>
      </Grid>

      <Card>
        <DataTable
          rows={report?.byCategory ?? []}
          columns={columns}
          loading={loading}
          getRowId={(row) => row.categoryName}
          onExport={handleExport}
          emptyTitle="No fee collection data found"
          mobileVisibleFields={['categoryName', 'collected']}
        />
      </Card>
    </Box>
  );
}

export default FeeCollectionReportPage;
