import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import CircularProgress from '@mui/material/CircularProgress';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import MeetingRoomOutlinedIcon from '@mui/icons-material/MeetingRoomOutlined';
import dayjs from 'dayjs';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import EmptyState from '@/components/common/EmptyState';
import StatusChip from '@/components/common/StatusChip';
import hostelApi from '@/api/hostelApi';
import { useAppSelector } from '@/store/hooks';
import type { Hostel, HostelStudent, Role } from '@/types';
import AllocateResidentDialog from './components/AllocateResidentDialog';
import VacateDialog from './components/VacateDialog';

const WRITE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];

function MyResidencyCard() {
  const { enqueueSnackbar } = useSnackbar();
  const [record, setRecord] = useState<HostelStudent | null>(null);
  const [loading, setLoading] = useState(true);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setLoading(true);
    hostelApi.hostelStudents
      .list({ size: 5 })
      .then((res) => {
        setRecord(res.data.content[0] ?? null);
        setLoaded(true);
      })
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load your hostel allocation.', { variant: 'error' });
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
    return <EmptyState title="No hostel allocation" description="You are not currently allocated to a hostel room." />;
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
            <MeetingRoomOutlinedIcon />
          </Box>
          <Box>
            <Typography variant="subtitle1" fontWeight={700}>
              {record.hostelName ?? 'Hostel'} — Room {record.roomNumber ?? `#${record.roomId}`}
            </Typography>
            <StatusChip status={record.status} />
          </Box>
        </Stack>
        <Divider sx={{ mb: 2 }} />
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <Typography variant="caption" color="text.secondary">
              Allocation Date
            </Typography>
            <Typography variant="body1">{dayjs(record.allocationDate).format('DD MMM YYYY')}</Typography>
          </Grid>
          {record.vacateDate && (
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" color="text.secondary">
                Vacate Date
              </Typography>
              <Typography variant="body1">{dayjs(record.vacateDate).format('DD MMM YYYY')}</Typography>
            </Grid>
          )}
        </Grid>
      </CardContent>
    </Card>
  );
}

/** Server-paginated hostel residents directory. Staff can allocate/vacate; STUDENT/PARENT see a read-only "My Residency" card. */
export function ResidentsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isSelfView = user?.role === 'STUDENT' || user?.role === 'PARENT';
  const canWrite = !!user && WRITE_ROLES.includes(user.role);

  const [rows, setRows] = useState<HostelStudent[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [hostels, setHostels] = useState<Hostel[]>([]);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [allocateOpen, setAllocateOpen] = useState(false);
  const [allocating, setAllocating] = useState(false);
  const [vacateTarget, setVacateTarget] = useState<HostelStudent | null>(null);
  const [vacating, setVacating] = useState(false);

  useEffect(() => {
    if (isSelfView) return;
    hostelApi.hostels.list().then((res) => setHostels(res.data)).catch(() => undefined);
  }, [isSelfView]);

  const load = useCallback(async () => {
    if (isSelfView) return;
    setLoading(true);
    try {
      const res = await hostelApi.hostelStudents.list({ page: paginationModel.page, size: paginationModel.pageSize, sort: 'allocationDate,desc' });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load hostel residents.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [isSelfView, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleAllocate = async (values: { studentId: number; roomId: number; allocationDate: string }) => {
    setAllocating(true);
    try {
      await hostelApi.hostelStudents.create(values);
      enqueueSnackbar('Student allocated to hostel room.', { variant: 'success' });
      setAllocateOpen(false);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not allocate this room.', { variant: 'error' });
    } finally {
      setAllocating(false);
    }
  };

  const handleVacate = async (vacateDate: string) => {
    if (!vacateTarget) return;
    setVacating(true);
    try {
      await hostelApi.hostelStudents.vacate(vacateTarget.id, { vacateDate });
      enqueueSnackbar('Room vacated.', { variant: 'success' });
      setVacateTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not vacate this room.', { variant: 'error' });
    } finally {
      setVacating(false);
    }
  };

  const columns: GridColDef<HostelStudent>[] = useMemo(
    () => [
      {
        field: 'studentName',
        headerName: 'Student',
        flex: 1,
        minWidth: 170,
        valueGetter: (_v, row) => (row.studentName ? `${row.studentName} (${row.admissionNumber ?? ''})` : `#${row.studentId}`),
      },
      {
        field: 'roomNumber',
        headerName: 'Room',
        flex: 0.8,
        minWidth: 130,
        valueGetter: (_v, row) => `${row.hostelName ? `${row.hostelName} - ` : ''}${row.roomNumber ?? `#${row.roomId}`}`,
      },
      {
        field: 'allocationDate',
        headerName: 'Allocated',
        width: 130,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      {
        field: 'vacateDate',
        headerName: 'Vacated',
        width: 130,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      { field: 'status', headerName: 'Status', width: 110, renderCell: (params) => <StatusChip status={params.row.status} /> },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 90,
        sortable: false,
        filterable: false,
        renderCell: (params) =>
          params.row.status === 'ACTIVE' ? (
            <Tooltip title="Vacate room">
              <IconButton size="small" color="warning" onClick={() => setVacateTarget(params.row)}>
                <LogoutOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : null,
      },
    ],
    [],
  );

  if (isSelfView) {
    return <MyResidencyCard />;
  }

  return (
    <Box>
      {canWrite && (
        <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => setAllocateOpen(true)}>
            Allocate
          </Button>
        </Stack>
      )}

      <Card>
        <DataTable
          rows={rows}
          columns={canWrite ? columns : columns.filter((c) => c.field !== 'actions')}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          mobileVisibleFields={['studentName', 'status']}
          emptyTitle="No hostel residents found"
          emptyDescription={canWrite ? 'Allocate a student to a room to get started.' : 'No students are currently residing in hostel.'}
        />
      </Card>

      {canWrite && (
        <>
          <AllocateResidentDialog
            open={allocateOpen}
            hostels={hostels}
            saving={allocating}
            onClose={() => setAllocateOpen(false)}
            onSubmit={handleAllocate}
          />
          <VacateDialog
            open={!!vacateTarget}
            resident={vacateTarget}
            saving={vacating}
            onClose={() => setVacateTarget(null)}
            onSubmit={handleVacate}
          />
        </>
      )}
    </Box>
  );
}

export default ResidentsPage;
