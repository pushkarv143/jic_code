import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import DirectionsBusFilledOutlinedIcon from '@mui/icons-material/DirectionsBusFilledOutlined';
import PinDropOutlinedIcon from '@mui/icons-material/PinDropOutlined';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import transportApi from '@/api/transportApi';
import { useAppSelector } from '@/store/hooks';
import { formatCurrencyINR } from '@/utils/format';
import type { Role, Route, StudentTransport } from '@/types';
import StudentTransportFormDialog from './components/StudentTransportFormDialog';

const WRITE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'RECEPTIONIST'];

function MyTransportCard() {
  const { enqueueSnackbar } = useSnackbar();
  const [record, setRecord] = useState<StudentTransport | null>(null);
  const [loading, setLoading] = useState(true);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setLoading(true);
    transportApi.studentTransport
      .list({ size: 5 })
      .then((res) => {
        setRecord(res.data.content[0] ?? null);
        setLoaded(true);
      })
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load your transport details.', { variant: 'error' });
        setLoaded(true);
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={32} />
      </Box>
    );
  }

  if (loaded && !record) {
    return <EmptyState title="No transport assigned" description="You are not currently allocated to a bus route." />;
  }

  if (!record) return null;

  return (
    <Card>
      <CardContent>
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: 2.5,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: 'action.hover',
              color: 'primary.main',
            }}
          >
            <DirectionsBusFilledOutlinedIcon />
          </Box>
          <Box>
            <Typography variant="subtitle1" fontWeight={700}>
              {record.routeName ?? `Route #${record.routeId}`}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              My Transport Allocation
            </Typography>
          </Box>
        </Stack>
        <Divider sx={{ mb: 2 }} />
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <Typography variant="caption" color="text.secondary">
              Pickup Point
            </Typography>
            <Typography variant="body1" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <PinDropOutlinedIcon fontSize="small" color="action" />
              {record.pickupPointName ?? `#${record.pickupPointId}`}
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography variant="caption" color="text.secondary">
              Monthly Fee
            </Typography>
            <Typography variant="body1" fontWeight={600}>
              {formatCurrencyINR(record.monthlyFee)}
            </Typography>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}

/** Server-paginated student transport allocations. Staff get full CRUD; STUDENT/PARENT see a read-only "My Transport" card. */
export function StudentAssignmentsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isSelfView = user?.role === 'STUDENT' || user?.role === 'PARENT';
  const canWrite = !!user && WRITE_ROLES.includes(user.role);

  const [rows, setRows] = useState<StudentTransport[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [routes, setRoutes] = useState<Route[]>([]);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<StudentTransport | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<StudentTransport | null>(null);

  useEffect(() => {
    if (isSelfView) return;
    transportApi.routes.list().then((res) => setRoutes(res.data)).catch(() => undefined);
  }, [isSelfView]);

  const load = useCallback(async () => {
    if (isSelfView) return;
    setLoading(true);
    try {
      const res = await transportApi.studentTransport.list({ page: paginationModel.page, size: paginationModel.pageSize });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load transport assignments.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [isSelfView, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSave = async (values: Parameters<typeof transportApi.studentTransport.create>[0]) => {
    setSaving(true);
    try {
      if (editing) {
        await transportApi.studentTransport.update(editing.id, values);
        enqueueSnackbar('Transport assignment updated.', { variant: 'success' });
      } else {
        await transportApi.studentTransport.create(values);
        enqueueSnackbar('Student assigned to transport.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this assignment.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await transportApi.studentTransport.remove(deleteTarget.id);
      enqueueSnackbar('Transport assignment removed.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not remove this assignment.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<StudentTransport>[] = useMemo(
    () => [
      {
        field: 'studentName',
        headerName: 'Student',
        flex: 1,
        minWidth: 170,
        valueGetter: (_v, row) => (row.studentName ? `${row.studentName} (${row.admissionNumber ?? ''})` : `#${row.studentId}`),
      },
      { field: 'routeName', headerName: 'Route', flex: 0.9, minWidth: 140, valueGetter: (_v, row) => row.routeName ?? `#${row.routeId}` },
      {
        field: 'pickupPointName',
        headerName: 'Pickup Point',
        flex: 0.9,
        minWidth: 140,
        valueGetter: (_v, row) => row.pickupPointName ?? `#${row.pickupPointId}`,
      },
      {
        field: 'monthlyFee',
        headerName: 'Monthly Fee',
        width: 130,
        renderCell: (params) => <Chip size="small" label={formatCurrencyINR(params.row.monthlyFee)} variant="outlined" />,
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 110,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => { setEditing(params.row); setFormOpen(true); }}>
                <EditOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Remove">
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

  if (isSelfView) {
    return <MyTransportCard />;
  }

  return (
    <Box>
      {canWrite && (
        <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
            Assign Transport
          </Button>
        </Stack>
      )}

      <Card>
        <DataTable
          rows={rows}
          columns={canWrite ? columns : columns.filter((c) => c.field !== 'actions')}
          mobileVisibleFields={['studentName', 'monthlyFee']}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No transport assignments found"
          emptyDescription={canWrite ? 'Assign a student to a route to get started.' : 'No students have been assigned transport yet.'}
        />
      </Card>

      {canWrite && (
        <>
          <StudentTransportFormDialog
            open={formOpen}
            editing={editing}
            routes={routes}
            saving={saving}
            onClose={() => { setFormOpen(false); setEditing(null); }}
            onSubmit={handleSave}
          />
          <ConfirmDialog
            open={!!deleteTarget}
            title="Remove transport assignment"
            message="This will unassign the student from this route and pickup point."
            confirmLabel="Remove"
            destructive
            onConfirm={handleDelete}
            onCancel={() => setDeleteTarget(null)}
          />
        </>
      )}
    </Box>
  );
}

export default StudentAssignmentsPage;
