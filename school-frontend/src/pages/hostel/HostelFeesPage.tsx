import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import dayjs from 'dayjs';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import StatusChip from '@/components/common/StatusChip';
import hostelApi from '@/api/hostelApi';
import { useAppSelector } from '@/store/hooks';
import { formatCurrencyINR } from '@/utils/format';
import type { HostelFee, HostelFeePaidStatus, Role } from '@/types';
import HostelFeeFormDialog from './components/HostelFeeFormDialog';

const WRITE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

/** Server-paginated hostel fee charges with month/year filter. Staff can add charges & mark paid; STUDENT/PARENT see their own history. */
export function HostelFeesPage() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isSelfView = user?.role === 'STUDENT' || user?.role === 'PARENT';
  const canWrite = !!user && WRITE_ROLES.includes(user.role);

  const [rows, setRows] = useState<HostelFee[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [month, setMonth] = useState<number | ''>('');
  const [year, setYear] = useState<number | ''>('');
  const [paidStatus, setPaidStatus] = useState<HostelFeePaidStatus | ''>('');
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [markingPaidId, setMarkingPaidId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await hostelApi.hostelFees.list({
        month: month || undefined,
        year: year || undefined,
        paidStatus: paidStatus || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load hostel fees.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [month, year, paidStatus, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [month, year, paidStatus]);

  const handleAdd = async (values: Parameters<typeof hostelApi.hostelFees.create>[0]) => {
    setSaving(true);
    try {
      await hostelApi.hostelFees.create(values);
      enqueueSnackbar('Hostel fee charge added.', { variant: 'success' });
      setFormOpen(false);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not add this charge.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleMarkPaid = async (fee: HostelFee) => {
    setMarkingPaidId(fee.id);
    try {
      await hostelApi.hostelFees.markPaid(fee.id);
      enqueueSnackbar('Marked as paid.', { variant: 'success' });
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not mark this fee as paid.', { variant: 'error' });
    } finally {
      setMarkingPaidId(null);
    }
  };

  const columns: GridColDef<HostelFee>[] = useMemo(
    () => [
      ...(isSelfView
        ? []
        : ([{ field: 'studentName', headerName: 'Student', flex: 1, minWidth: 160, valueGetter: (_v, row) => row.studentName ?? `#${row.studentId}` }] as GridColDef<HostelFee>[])),
      { field: 'month', headerName: 'Month', width: 120, valueGetter: (_v, row) => MONTHS[row.month - 1] ?? row.month },
      { field: 'year', headerName: 'Year', width: 90 },
      { field: 'amount', headerName: 'Amount', width: 130, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'paidStatus', headerName: 'Status', width: 110, renderCell: (params) => <StatusChip status={params.row.paidStatus} /> },
      ...(canWrite
        ? ([
            {
              field: 'actions',
              headerName: 'Actions',
              width: 90,
              sortable: false,
              filterable: false,
              renderCell: (params: any) =>
                params.row.paidStatus === 'UNPAID' ? (
                  <Tooltip title="Mark paid">
                    <IconButton size="small" color="primary" disabled={markingPaidId === params.row.id} onClick={() => handleMarkPaid(params.row)}>
                      <PaidOutlinedIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                ) : null,
            } as GridColDef<HostelFee>,
          ])
        : []),
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [isSelfView, canWrite, markingPaidId],
  );

  const years = Array.from({ length: 5 }, (_, i) => dayjs().year() - 1 + i);

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={6} sm={3} md={2}>
              <TextField select fullWidth size="small" label="Month" value={month} onChange={(e) => setMonth(e.target.value === '' ? '' : Number(e.target.value))}>
                <MenuItem value="">All months</MenuItem>
                {MONTHS.map((m, idx) => (
                  <MenuItem key={m} value={idx + 1}>
                    {m}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField select fullWidth size="small" label="Year" value={year} onChange={(e) => setYear(e.target.value === '' ? '' : Number(e.target.value))}>
                <MenuItem value="">All years</MenuItem>
                {years.map((y) => (
                  <MenuItem key={y} value={y}>
                    {y}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                select
                fullWidth
                size="small"
                label="Status"
                value={paidStatus}
                onChange={(e) => setPaidStatus(e.target.value as HostelFeePaidStatus | '')}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="PAID">Paid</MenuItem>
                <MenuItem value="UNPAID">Unpaid</MenuItem>
              </TextField>
            </Grid>
            {canWrite && (
              <Grid item xs={12} md={6}>
                <Stack direction="row" justifyContent={{ xs: 'flex-start', md: 'flex-end' }}>
                  <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => setFormOpen(true)}>
                    Add Charge
                  </Button>
                </Stack>
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No hostel fee records found"
          emptyDescription={canWrite ? 'Add a charge to get started.' : 'No hostel fee charges have been recorded for you yet.'}
        />
      </Card>

      {canWrite && (
        <HostelFeeFormDialog open={formOpen} saving={saving} onClose={() => setFormOpen(false)} onSubmit={handleAdd} />
      )}
    </Box>
  );
}

export default HostelFeesPage;
