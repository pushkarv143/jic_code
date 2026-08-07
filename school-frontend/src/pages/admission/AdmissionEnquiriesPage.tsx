import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import StatusChip from '@/components/common/StatusChip';
import admissionApi from '@/api/admissionApi';
import type { AdmissionEnquiry, AdmissionEnquiryStatus } from '@/types';
import EnquiryDetailDialog from './components/EnquiryDetailDialog';

const STATUS_OPTIONS: AdmissionEnquiryStatus[] = ['PENDING', 'APPROVED', 'REJECTED'];

/** Staff-only admission enquiries directory: status filter, approve/reject, detail view, delete. */
export function AdmissionEnquiriesPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [status, setStatus] = useState<AdmissionEnquiryStatus | ''>('');
  const [search, setSearch] = useState('');

  const [rows, setRows] = useState<AdmissionEnquiry[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [detailTarget, setDetailTarget] = useState<AdmissionEnquiry | null>(null);
  const [statusTarget, setStatusTarget] = useState<{ enquiry: AdmissionEnquiry; next: AdmissionEnquiryStatus } | null>(null);
  const [updating, setUpdating] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<AdmissionEnquiry | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await admissionApi.list({
        status: status || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'appliedAt,desc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load admission enquiries.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [status, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [status]);

  const visibleRows = useMemo(() => {
    if (!search.trim()) return rows;
    const q = search.trim().toLowerCase();
    return rows.filter(
      (r) =>
        r.studentName.toLowerCase().includes(q) ||
        r.parentName.toLowerCase().includes(q) ||
        r.phone.includes(q) ||
        r.email.toLowerCase().includes(q),
    );
  }, [rows, search]);

  const handleStatusChange = async () => {
    if (!statusTarget) return;
    setUpdating(true);
    try {
      await admissionApi.updateStatus(statusTarget.enquiry.id, statusTarget.next);
      enqueueSnackbar(`Enquiry ${statusTarget.next === 'APPROVED' ? 'approved' : 'rejected'}.`, { variant: 'success' });
      setStatusTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not update this enquiry.', { variant: 'error' });
    } finally {
      setUpdating(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await admissionApi.remove(deleteTarget.id);
      enqueueSnackbar('Enquiry deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this enquiry.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<AdmissionEnquiry>[] = useMemo(
    () => [
      { field: 'studentName', headerName: 'Student', flex: 1, minWidth: 150 },
      { field: 'classApplying', headerName: 'Class', width: 110 },
      { field: 'parentName', headerName: 'Parent', flex: 1, minWidth: 140 },
      { field: 'phone', headerName: 'Phone', width: 120 },
      {
        field: 'appliedAt',
        headerName: 'Applied On',
        width: 130,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      { field: 'status', headerName: 'Status', width: 110, renderCell: (params) => <StatusChip status={params.row.status} /> },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 170,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="View details">
              <IconButton size="small" onClick={() => setDetailTarget(params.row)}>
                <VisibilityOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {params.row.status === 'PENDING' && (
              <>
                <Tooltip title="Approve">
                  <IconButton size="small" color="success" onClick={() => setStatusTarget({ enquiry: params.row, next: 'APPROVED' })}>
                    <CheckCircleOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Reject">
                  <IconButton size="small" color="error" onClick={() => setStatusTarget({ enquiry: params.row, next: 'REJECTED' })}>
                    <CancelOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            )}
            <Tooltip title="Delete">
              <IconButton size="small" onClick={() => setDeleteTarget(params.row)}>
                <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    [],
  );

  return (
    <Box>
      <PageHeader
        title="Admission Enquiries"
        subtitle="Review, approve or reject admission enquiries submitted from the public site"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Admission Enquiries' }]}
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Status"
                value={status}
                onChange={(e) => setStatus(e.target.value as AdmissionEnquiryStatus | '')}
              >
                <MenuItem value="">All statuses</MenuItem>
                {STATUS_OPTIONS.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s.charAt(0) + s.slice(1).toLowerCase()}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                size="small"
                label="Search"
                placeholder="Name, phone or email"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={visibleRows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No admission enquiries found"
          emptyDescription="Enquiries submitted from the public admission form will appear here."
        />
      </Card>

      <EnquiryDetailDialog open={!!detailTarget} enquiry={detailTarget} onClose={() => setDetailTarget(null)} />

      <ConfirmDialog
        open={!!statusTarget}
        title={statusTarget?.next === 'APPROVED' ? 'Approve enquiry' : 'Reject enquiry'}
        message={`Mark ${statusTarget?.enquiry.studentName ?? 'this enquiry'} as ${statusTarget?.next === 'APPROVED' ? 'approved' : 'rejected'}?`}
        confirmLabel={statusTarget?.next === 'APPROVED' ? 'Approve' : 'Reject'}
        destructive={statusTarget?.next === 'REJECTED'}
        loading={updating}
        onConfirm={handleStatusChange}
        onCancel={() => setStatusTarget(null)}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete enquiry"
        message={`Delete the enquiry for ${deleteTarget?.studentName ?? ''}? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

export default AdmissionEnquiriesPage;
