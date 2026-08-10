import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PlaylistAddOutlinedIcon from '@mui/icons-material/PlaylistAddOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined';
import HourglassEmptyOutlinedIcon from '@mui/icons-material/HourglassEmptyOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import StatCard from '@/components/common/StatCard';
import StatusChip from '@/components/common/StatusChip';
import payrollApi from '@/api/payrollApi';
import { formatCurrencyINR } from '@/utils/format';
import type { PayrollDashboardSummary, PayrollEmployeeType, PayrollRun, PayrollStatus } from '@/types';
import GeneratePayrollDialog from './components/GeneratePayrollDialog';
import MarkPaidDialog from './components/MarkPaidDialog';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

/** Month/year/employee-type filters -> payroll dashboard tiles, a payroll runs table, Generate/Mark Paid/View Slip actions. */
export function PayrollRunsPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [month, setMonth] = useState(dayjs().month() + 1);
  const [year, setYear] = useState(dayjs().year());
  const [employeeType, setEmployeeType] = useState<PayrollEmployeeType | ''>('');
  const [status, setStatus] = useState<PayrollStatus | ''>('');

  const [dashboard, setDashboard] = useState<PayrollDashboardSummary | null>(null);
  const [dashboardLoading, setDashboardLoading] = useState(false);

  const [rows, setRows] = useState<PayrollRun[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [generateOpen, setGenerateOpen] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [markPaidTarget, setMarkPaidTarget] = useState<PayrollRun | null>(null);
  const [markingPaid, setMarkingPaid] = useState(false);

  const years = useMemo(() => Array.from({ length: 5 }, (_, i) => dayjs().year() - 3 + i), []);

  const loadDashboard = useCallback(async () => {
    setDashboardLoading(true);
    try {
      const res = await payrollApi.payroll.getDashboard({ month, year });
      setDashboard(res.data);
    } catch {
      setDashboard(null);
    } finally {
      setDashboardLoading(false);
    }
  }, [month, year]);

  const loadRows = useCallback(async () => {
    setLoading(true);
    try {
      const res = await payrollApi.payroll.list({
        month,
        year,
        employeeType: employeeType || undefined,
        status: status || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'employeeId,asc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load payroll runs.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [month, year, employeeType, status, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  useEffect(() => {
    loadRows();
  }, [loadRows]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [month, year, employeeType, status]);

  const handleGenerate = async (values: { employeeType: PayrollEmployeeType; month: number; year: number }) => {
    setGenerating(true);
    try {
      const res = await payrollApi.payroll.generate(values);
      enqueueSnackbar(
        `Payroll generated for ${res.data.generatedCount} employee${res.data.generatedCount === 1 ? '' : 's'}` +
          (res.data.skippedCount ? ` (${res.data.skippedCount} skipped — already existed).` : '.'),
        { variant: 'success' },
      );
      setGenerateOpen(false);
      setMonth(values.month);
      setYear(values.year);
      loadDashboard();
      loadRows();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not generate payroll.', { variant: 'error' });
    } finally {
      setGenerating(false);
    }
  };

  const handleMarkPaid = async (paymentDate: string) => {
    if (!markPaidTarget) return;
    setMarkingPaid(true);
    try {
      await payrollApi.payroll.markPaid(markPaidTarget.id, { paymentDate });
      enqueueSnackbar('Payroll marked as paid.', { variant: 'success' });
      setMarkPaidTarget(null);
      loadDashboard();
      loadRows();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not mark this payroll as paid.', { variant: 'error' });
    } finally {
      setMarkingPaid(false);
    }
  };

  const columns: GridColDef<PayrollRun>[] = useMemo(
    () => [
      { field: 'employeeName', headerName: 'Employee', flex: 1, minWidth: 170, valueGetter: (_v, row) => row.employeeName ?? `Employee #${row.employeeId}` },
      { field: 'employeeType', headerName: 'Type', width: 100 },
      {
        field: 'period',
        headerName: 'Period',
        width: 130,
        valueGetter: (_v, row) => dayjs(`${row.year}-${row.month}-01`).format('MMM YYYY'),
      },
      { field: 'basicSalary', headerName: 'Basic', width: 110, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'allowances', headerName: 'Allowances', width: 120, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'deductions', headerName: 'Deductions', width: 120, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'netSalary', headerName: 'Net Salary', width: 130, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'status', headerName: 'Status', width: 110, renderCell: (params) => <StatusChip status={params.row.status} /> },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 130,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            {params.row.status !== 'PAID' && (
              <Tooltip title="Mark paid">
                <IconButton size="small" color="success" onClick={() => setMarkPaidTarget(params.row)}>
                  <PaidOutlinedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            <Tooltip title="View salary slip">
              <IconButton size="small" onClick={() => navigate(`/app/payroll/slip/${params.row.id}`)}>
                <ReceiptLongOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    [navigate],
  );

  return (
    <Box>
      <PageHeader
        title="Payroll Runs"
        subtitle="Generate and track monthly payroll for teachers and staff"
        action={
          <Button variant="contained" startIcon={<PlaylistAddOutlinedIcon />} onClick={() => setGenerateOpen(true)}>
            Generate Payroll
          </Button>
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField select fullWidth size="small" label="Month" value={month} onChange={(e) => setMonth(Number(e.target.value))}>
                {MONTHS.map((m, idx) => (
                  <MenuItem key={m} value={idx + 1}>
                    {m}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField select fullWidth size="small" label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
                {years.map((y) => (
                  <MenuItem key={y} value={y}>
                    {y}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Employee Type"
                value={employeeType}
                onChange={(e) => setEmployeeType(e.target.value as PayrollEmployeeType | '')}
              >
                <MenuItem value="">All types</MenuItem>
                <MenuItem value="TEACHER">Teacher</MenuItem>
                <MenuItem value="STAFF">Staff</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Status"
                value={status}
                onChange={(e) => setStatus(e.target.value as PayrollStatus | '')}
              >
                <MenuItem value="">All statuses</MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="PAID">Paid</MenuItem>
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {!dashboardLoading && dashboard && (
        <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<AccountBalanceWalletOutlinedIcon />} label="Total Paid" value={formatCurrencyINR(dashboard.totalPaid)} color="success" />
          </Grid>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<HourglassEmptyOutlinedIcon />} label="Total Pending" value={formatCurrencyINR(dashboard.totalPending)} color="warning" />
          </Grid>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<GroupsOutlinedIcon />} label="Employees" value={dashboard.employeeCount} color="info" />
          </Grid>
        </Grid>
      )}

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No payroll runs found"
          emptyDescription="Generate payroll for this month to see rows here."
          mobileVisibleFields={['employeeName', 'status']}
        />
      </Card>

      <GeneratePayrollDialog
        open={generateOpen}
        generating={generating}
        onClose={() => setGenerateOpen(false)}
        onGenerate={handleGenerate}
      />

      <MarkPaidDialog
        open={!!markPaidTarget}
        payrollRow={markPaidTarget}
        saving={markingPaid}
        onClose={() => setMarkPaidTarget(null)}
        onConfirm={handleMarkPaid}
      />
    </Box>
  );
}

export default PayrollRunsPage;
