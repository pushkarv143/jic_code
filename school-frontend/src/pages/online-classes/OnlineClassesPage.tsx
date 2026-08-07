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
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import VideoCameraFrontOutlinedIcon from '@mui/icons-material/VideoCameraFrontOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import EmptyState from '@/components/common/EmptyState';
import PageLoader from '@/components/common/PageLoader';
import onlineClassesApi from '@/api/onlineClassesApi';
import classesApi from '@/api/classesApi';
import { useAppSelector } from '@/store/hooks';
import type { OnlineClass, Role, SchoolClass } from '@/types';
import type { OnlineClassPayload } from '@/api/onlineClassesApi';
import OnlineClassFormDialog from './components/OnlineClassFormDialog';

const WRITE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'TEACHER', 'CLASS_TEACHER'];

function ManagementOnlineClassesView() {
  const { enqueueSnackbar } = useSnackbar();
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [rows, setRows] = useState<OnlineClass[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<OnlineClass | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<OnlineClass | null>(null);

  useEffect(() => {
    classesApi.list({ size: 200, sort: 'className,asc' }).then((res) => setClasses(res.data.content)).catch(() => undefined);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await onlineClassesApi.list({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'scheduledAt,desc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load online classes.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSave = async (values: OnlineClassPayload) => {
    setSaving(true);
    try {
      if (editing) {
        await onlineClassesApi.update(editing.id, values);
        enqueueSnackbar('Online class updated.', { variant: 'success' });
      } else {
        await onlineClassesApi.create(values);
        enqueueSnackbar('Online class scheduled.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this online class.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await onlineClassesApi.remove(deleteTarget.id);
      enqueueSnackbar('Online class removed.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not remove this online class.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<OnlineClass>[] = useMemo(
    () => [
      { field: 'title', headerName: 'Title', flex: 1.2, minWidth: 170 },
      { field: 'subjectName', headerName: 'Subject', flex: 0.8, minWidth: 120, valueGetter: (_v, row) => row.subjectName ?? `#${row.subjectId}` },
      {
        field: 'classSection',
        headerName: 'Class',
        width: 120,
        valueGetter: (_v, row) => `${row.className ?? row.classId}${row.sectionName ? `-${row.sectionName}` : ''}`,
      },
      {
        field: 'scheduledAt',
        headerName: 'Scheduled At',
        width: 170,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY, hh:mm A') : '-'),
      },
      { field: 'durationMinutes', headerName: 'Duration', width: 100, valueFormatter: (value) => `${value} min` },
      {
        field: 'meetingLink',
        headerName: 'Link',
        width: 80,
        sortable: false,
        renderCell: (params) => (
          <Tooltip title="Open meeting link">
            <IconButton size="small" component="a" href={params.row.meetingLink} target="_blank" rel="noopener noreferrer">
              <OpenInNewOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
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
        title="Online Classes"
        subtitle="Schedule and manage live online classes"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Online Classes' }]}
        action={
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
            Schedule Class
          </Button>
        }
      />

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No online classes found"
          emptyDescription="Schedule an online class to get started."
        />
      </Card>

      <OnlineClassFormDialog
        open={formOpen}
        editing={editing}
        classes={classes}
        saving={saving}
        onClose={() => { setFormOpen(false); setEditing(null); }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Remove online class"
        message={`Remove "${deleteTarget?.title ?? ''}"? This cannot be undone.`}
        confirmLabel="Remove"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

/** STUDENT/PARENT view: upcoming classes as a card list with a Join button (opens the meeting link in a new tab). */
function StudentOnlineClassesView() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const [rows, setRows] = useState<OnlineClass[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setLoading(true);
    // Scope explicitly to the logged-in student's own class/section (populated on
    // authSlice's user via /auth/login and /auth/me) rather than assuming the
    // backend auto-scopes this particular endpoint the way it does for assignments.
    onlineClassesApi
      .list({
        classId: user?.classId ?? undefined,
        sectionId: user?.sectionId ?? undefined,
        upcoming: true,
        size: 50,
        sort: 'scheduledAt,asc',
      })
      .then((res) => {
        setRows(res.data.content);
        setLoaded(true);
      })
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load online classes.', { variant: 'error' });
        setLoaded(true);
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.classId, user?.sectionId]);

  return (
    <Box>
      <PageHeader
        title="Online Classes"
        subtitle="Upcoming live classes for your section"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Online Classes' }]}
      />

      {loading ? (
        <PageLoader label="Loading online classes..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No upcoming classes" description="There are no online classes scheduled right now." />
      ) : (
        <Grid container spacing={2.5}>
          {rows.map((oc) => (
            <Grid item xs={12} sm={6} md={4} key={oc.id}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1.5 }}>
                    <Box sx={{ width: 40, height: 40, borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'action.hover', color: 'primary.main' }}>
                      <VideoCameraFrontOutlinedIcon />
                    </Box>
                    <Box>
                      <Typography variant="subtitle1" fontWeight={700}>
                        {oc.title}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {oc.subjectName ?? `Subject #${oc.subjectId}`}
                      </Typography>
                    </Box>
                  </Stack>
                  <Chip size="small" variant="outlined" label={dayjs(oc.scheduledAt).format('DD MMM YYYY, hh:mm A')} sx={{ mb: 1 }} />
                  <Typography variant="caption" color="text.secondary" display="block">
                    Duration: {oc.durationMinutes} minutes
                  </Typography>
                </CardContent>
                <Box sx={{ p: 2, pt: 0 }}>
                  <Button fullWidth variant="contained" startIcon={<OpenInNewOutlinedIcon />} component="a" href={oc.meetingLink} target="_blank" rel="noopener noreferrer">
                    Join Class
                  </Button>
                </Box>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  );
}

/** Branches by role: TEACHER/CLASS_TEACHER/admin get the management table; STUDENT/PARENT get an upcoming-classes card list. */
export function OnlineClassesPage() {
  const role = useAppSelector((state) => state.auth.user?.role);
  const isManagement = !!role && WRITE_ROLES.includes(role);
  return isManagement ? <ManagementOnlineClassesView /> : <StudentOnlineClassesView />;
}

export default OnlineClassesPage;
