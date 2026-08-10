import { useCallback, useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import StatusChip from '@/components/common/StatusChip';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import leaveApi from '@/api/leaveApi';
import { useAppSelector } from '@/store/hooks';
import type { LeaveApplicantType, LeaveApplication, LeaveStatus, Role } from '@/types';
import LeaveFormDialog from './components/LeaveFormDialog';

const APPROVER_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'CLASS_TEACHER'];

const TYPE_OPTIONS: Array<{ label: string; value: LeaveApplicantType | '' }> = [
  { label: 'All types', value: '' },
  { label: 'Teacher', value: 'TEACHER' },
  { label: 'Staff', value: 'STAFF' },
  { label: 'Student', value: 'STUDENT' },
];

const STATUS_OPTIONS: Array<{ label: string; value: LeaveStatus | '' }> = [
  { label: 'All statuses', value: '' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Rejected', value: 'REJECTED' },
];

/** Role-aware leave applications screen: approver management table + everyone's own applications. */
export function LeaveListPage() {
  const { enqueueSnackbar } = useSnackbar();
  const role = useAppSelector((state) => state.auth.user?.role);
  const isApprover = !!role && APPROVER_ROLES.includes(role);

  const [scope, setScope] = useState<'all' | 'mine'>(isApprover ? 'all' : 'mine');

  const [rows, setRows] = useState<LeaveApplication[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [applicantType, setApplicantType] = useState<LeaveApplicantType | ''>('');
  const [status, setStatus] = useState<LeaveStatus | ''>('');

  const [applyOpen, setApplyOpen] = useState(false);
  const [applying, setApplying] = useState(false);
  const [actionTarget, setActionTarget] = useState<{ row: LeaveApplication; action: 'approve' | 'reject' | 'withdraw' } | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      if (scope === 'all') {
        const res = await leaveApi.list({
          applicantType: applicantType || undefined,
          status: status || undefined,
          page: paginationModel.page,
          size: paginationModel.pageSize,
        });
        setRows(res.data.content);
        setRowCount(res.data.totalElements);
      } else {
        const res = await leaveApi.listMine({ page: paginationModel.page, size: paginationModel.pageSize });
        setRows(res.data.content);
        setRowCount(res.data.totalElements);
      }
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load leave applications.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [scope, applicantType, status, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [scope, applicantType, status]);

  const handleApply = async (payload: Parameters<typeof leaveApi.create>[0]) => {
    setApplying(true);
    try {
      await leaveApi.create(payload);
      enqueueSnackbar('Leave application submitted.', { variant: 'success' });
      setApplyOpen(false);
      if (scope === 'mine') load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not submit the leave application.', { variant: 'error' });
    } finally {
      setApplying(false);
    }
  };

  const handleAction = async () => {
    if (!actionTarget) return;
    setActionLoading(true);
    try {
      if (actionTarget.action === 'approve') {
        await leaveApi.approve(actionTarget.row.id);
        enqueueSnackbar('Leave application approved.', { variant: 'success' });
      } else if (actionTarget.action === 'reject') {
        await leaveApi.reject(actionTarget.row.id);
        enqueueSnackbar('Leave application rejected.', { variant: 'success' });
      } else {
        await leaveApi.remove(actionTarget.row.id);
        enqueueSnackbar('Leave application withdrawn.', { variant: 'success' });
      }
      setActionTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not complete this action.', { variant: 'error' });
    } finally {
      setActionLoading(false);
    }
  };

  const columns: GridColDef<LeaveApplication>[] = useMemo(() => {
    const base: GridColDef<LeaveApplication>[] = [
      ...(scope === 'all'
        ? ([
            { field: 'applicantName', headerName: 'Applicant', flex: 1, minWidth: 160, valueGetter: (_v, row) => row.applicantName ?? `User #${row.applicantId}` },
            { field: 'applicantType', headerName: 'Type', width: 110 },
          ] as GridColDef<LeaveApplication>[])
        : []),
      { field: 'leaveType', headerName: 'Leave Type', width: 130 },
      {
        field: 'startDate',
        headerName: 'From',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      {
        field: 'endDate',
        headerName: 'To',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      { field: 'reason', headerName: 'Reason', flex: 1.2, minWidth: 180, sortable: false },
      {
        field: 'status',
        headerName: 'Status',
        width: 120,
        renderCell: (params) => <StatusChip status={params.row.status} />,
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: scope === 'all' ? 130 : 100,
        sortable: false,
        filterable: false,
        renderCell: (params) => {
          if (scope === 'all') {
            if (params.row.status !== 'PENDING') return null;
            return (
              <Stack direction="row" spacing={0.5}>
                <Tooltip title="Approve">
                  <IconButton size="small" color="success" onClick={() => setActionTarget({ row: params.row, action: 'approve' })}>
                    <CheckCircleOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Reject">
                  <IconButton size="small" color="error" onClick={() => setActionTarget({ row: params.row, action: 'reject' })}>
                    <CancelOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Stack>
            );
          }
          if (params.row.status !== 'PENDING') return null;
          return (
            <Tooltip title="Withdraw">
              <IconButton size="small" onClick={() => setActionTarget({ row: params.row, action: 'withdraw' })}>
                <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
              </IconButton>
            </Tooltip>
          );
        },
      },
    ];
    return base;
  }, [scope]);

  return (
    <Box>
      <PageHeader
        title="Leave Applications"
        subtitle="Apply for leave and track approval status"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Leave' }]}
        action={
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => setApplyOpen(true)}>
            Apply for Leave
          </Button>
        }
      />

      {isApprover && (
        <Card sx={{ mb: 2.5 }}>
          <Tabs value={scope} onChange={(_e: SyntheticEvent, v: 'all' | 'mine') => setScope(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
            <Tab value="all" label="All Applications" />
            <Tab value="mine" label="My Applications" />
          </Tabs>
        </Card>
      )}

      {scope === 'all' && (
        <Card sx={{ mb: 2.5 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  select
                  fullWidth
                  size="small"
                  label="Applicant Type"
                  value={applicantType}
                  onChange={(e) => setApplicantType(e.target.value as LeaveApplicantType | '')}
                >
                  {TYPE_OPTIONS.map((opt) => (
                    <MenuItem key={opt.label} value={opt.value}>
                      {opt.label}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  select
                  fullWidth
                  size="small"
                  label="Status"
                  value={status}
                  onChange={(e) => setStatus(e.target.value as LeaveStatus | '')}
                >
                  {STATUS_OPTIONS.map((opt) => (
                    <MenuItem key={opt.label} value={opt.value}>
                      {opt.label}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
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
          mobileVisibleFields={['applicantName', 'leaveType', 'status']}
          emptyTitle="No leave applications found"
          emptyDescription={scope === 'mine' ? 'Apply for leave using the button above.' : 'Try adjusting the filters.'}
        />
      </Card>

      <LeaveFormDialog open={applyOpen} saving={applying} onClose={() => setApplyOpen(false)} onSubmit={handleApply} />

      <ConfirmDialog
        open={!!actionTarget}
        title={
          actionTarget?.action === 'approve'
            ? 'Approve leave application'
            : actionTarget?.action === 'reject'
              ? 'Reject leave application'
              : 'Withdraw leave application'
        }
        message={
          actionTarget?.action === 'approve'
            ? 'Approve this leave application?'
            : actionTarget?.action === 'reject'
              ? 'Reject this leave application?'
              : 'Withdraw your pending leave application? This cannot be undone.'
        }
        confirmLabel={actionTarget?.action === 'approve' ? 'Approve' : actionTarget?.action === 'reject' ? 'Reject' : 'Withdraw'}
        destructive={actionTarget?.action !== 'approve'}
        loading={actionLoading}
        onConfirm={handleAction}
        onCancel={() => setActionTarget(null)}
      />
    </Box>
  );
}

export default LeaveListPage;
