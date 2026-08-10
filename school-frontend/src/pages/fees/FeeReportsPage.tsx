import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import StatCard from '@/components/common/StatCard';
import BarChartCard from '@/components/charts/BarChartCard';
import DataTable from '@/components/common/DataTable';
import StatusChip from '@/components/common/StatusChip';
import feesApi from '@/api/feesApi';
import classesApi from '@/api/classesApi';
import academicYearsApi from '@/api/academicYearsApi';
import reportsApi from '@/api/reportsApi';
import { exportRowsToCsv } from '@/utils/csvExport';
import { downloadBlob } from '@/utils/downloadBlob';
import { formatCurrencyINR } from '@/utils/format';
import type { AcademicYear, DuesSummary, SchoolClass, StudentFee } from '@/types';

/**
 * ASSUMPTION (reconcile with backend): there is no dedicated "collection by category"
 * aggregation endpoint in the contract, so this page pulls a bounded page of
 * /student-fees rows for the selected class/year and aggregates due/collected by
 * feeCategoryName client-side. Works well for a single class; if the backend later
 * adds a proper aggregate endpoint, swap this out.
 */
const AGGREGATION_PAGE_SIZE = 500;

/** Class + academic year filters -> dues summary tiles, a per-category bar chart, and an outstanding-dues table. */
export function FeeReportsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');

  const [summary, setSummary] = useState<DuesSummary | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);

  const [allFees, setAllFees] = useState<StudentFee[]>([]);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [exportingExcel, setExportingExcel] = useState(false);

  useEffect(() => {
    classesApi.list({ size: 200, sort: 'className,asc' }).then((res) => setClasses(res.data.content)).catch(() => undefined);
    academicYearsApi.list().then((res) => setYears(res.data)).catch(() => undefined);
  }, []);

  useEffect(() => {
    setSummaryLoading(true);
    feesApi.dues
      .getSummary({ classId: classId || undefined, academicYearId: academicYearId || undefined })
      .then((res) => setSummary(res.data))
      .catch(() => setSummary(null))
      .finally(() => setSummaryLoading(false));
  }, [classId, academicYearId]);

  const loadFees = useCallback(async () => {
    setLoading(true);
    try {
      const res = await feesApi.studentFees.list({
        classId: classId || undefined,
        academicYearId: academicYearId || undefined,
        size: AGGREGATION_PAGE_SIZE,
        sort: 'dueDate,desc',
      });
      setAllFees(res.data.content);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load fee data for reports.', { variant: 'error' });
      setAllFees([]);
    } finally {
      setLoading(false);
    }
  }, [classId, academicYearId, enqueueSnackbar]);

  useEffect(() => {
    loadFees();
  }, [loadFees]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [classId, academicYearId]);

  const categoryChartData = useMemo(() => {
    const byCategory = new Map<string, { due: number; collected: number }>();
    allFees.forEach((f) => {
      const key = f.feeCategoryName ?? 'Uncategorised';
      const entry = byCategory.get(key) ?? { due: 0, collected: 0 };
      entry.due += f.amountDue;
      entry.collected += f.amountPaid;
      byCategory.set(key, entry);
    });
    return Array.from(byCategory.entries()).map(([category, v]) => ({ category, Due: v.due, Collected: v.collected }));
  }, [allFees]);

  const duesRows = useMemo(() => allFees.filter((f) => f.amountDue - f.amountPaid > 0), [allFees]);

  const handleExport = () => {
    exportRowsToCsv(
      duesRows as unknown as Record<string, unknown>[],
      [
        { field: 'studentName', header: 'Student' },
        { field: 'admissionNumber', header: 'Admission No.' },
        { field: 'className', header: 'Class' },
        { field: 'sectionName', header: 'Section' },
        { field: 'feeCategoryName', header: 'Fee Category' },
        { field: 'amountDue', header: 'Amount Due' },
        { field: 'amountPaid', header: 'Amount Paid' },
        {
          field: 'balance',
          header: 'Balance',
          value: (row) => Math.max(Number(row.amountDue) - Number(row.amountPaid), 0),
        },
        { field: 'status', header: 'Status' },
        { field: 'dueDate', header: 'Due Date' },
      ],
      'fee-dues-report',
    );
  };

  const handleExportExcel = async () => {
    setExportingExcel(true);
    try {
      const blob = await reportsApi.exportFeeCollectionExcel({ academicYearId: academicYearId || undefined });
      downloadBlob(blob, 'fee-collection-report.xlsx');
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not export the fee collection report to Excel.', {
        variant: 'error',
      });
    } finally {
      setExportingExcel(false);
    }
  };

  const columns: GridColDef<StudentFee>[] = useMemo(
    () => [
      { field: 'studentName', headerName: 'Student', flex: 1, minWidth: 160, valueGetter: (_v, row) => row.studentName ?? `#${row.studentId}` },
      { field: 'className', headerName: 'Class / Section', flex: 0.9, minWidth: 130, valueGetter: (_v, row) => (row.className ? `${row.className}${row.sectionName ? ` - ${row.sectionName}` : ''}` : '-') },
      { field: 'feeCategoryName', headerName: 'Fee Category', flex: 0.9, minWidth: 130, valueGetter: (_v, row) => row.feeCategoryName ?? '-' },
      {
        field: 'dueDate',
        headerName: 'Due Date',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      {
        field: 'balance',
        headerName: 'Balance',
        width: 130,
        valueGetter: (_v, row) => Math.max(row.amountDue - row.amountPaid, 0),
        valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)),
      },
      { field: 'status', headerName: 'Status', width: 110, renderCell: (params) => <StatusChip status={params.row.status} /> },
    ],
    [],
  );

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
                label="Class"
                value={classId}
                onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All classes</MenuItem>
                {classes.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.className}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
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

      {!summaryLoading && summary && (
        <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<AccountBalanceWalletOutlinedIcon />} label="Total Due" value={formatCurrencyINR(summary.totalDue)} color="primary" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<PaidOutlinedIcon />} label="Total Collected" value={formatCurrencyINR(summary.totalCollected)} color="success" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<WarningAmberOutlinedIcon />} label="Outstanding" value={formatCurrencyINR(summary.totalOutstanding)} color="error" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<GroupsOutlinedIcon />} label="Students" value={summary.studentCount} color="info" />
          </Grid>
        </Grid>
      )}

      <Box sx={{ mb: 2.5 }}>
        <BarChartCard
          title="Collection by Fee Category"
          subtitle="Amount due vs. collected, aggregated across the filtered students"
          data={categoryChartData}
          xKey="category"
          series={[
            { key: 'Due', label: 'Due' },
            { key: 'Collected', label: 'Collected' },
          ]}
          valueFormatter={(v) => formatCurrencyINR(v)}
        />
      </Box>

      <Card>
        <DataTable
          rows={duesRows}
          columns={columns}
          loading={loading}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          mobileVisibleFields={['studentName', 'balance']}
          onExport={handleExport}
          toolbarExtra={
            <Button
              size="small"
              variant="outlined"
              startIcon={exportingExcel ? <CircularProgress size={14} color="inherit" /> : <FileDownloadOutlinedIcon />}
              onClick={handleExportExcel}
              disabled={exportingExcel}
            >
              Export Excel
            </Button>
          }
          emptyTitle="No outstanding dues"
          emptyDescription="Every fee in this selection has been fully collected."
        />
      </Card>
    </Box>
  );
}

export default FeeReportsPage;
