import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import InputAdornment from '@mui/material/InputAdornment';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Avatar from '@mui/material/Avatar';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import CircularProgress from '@mui/material/CircularProgress';
import type { GridColDef, GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import StatusChip from '@/components/common/StatusChip';
import LookupManagerDialog from '@/components/common/LookupManagerDialog';
import teachersApi from '@/api/teachersApi';
import departmentsApi from '@/api/departmentsApi';
import designationsApi from '@/api/designationsApi';
import type { Department, Designation, StaffStatus, Teacher } from '@/types';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { exportRowsToCsv } from '@/utils/csvExport';
import { downloadBlob } from '@/utils/downloadBlob';

const STATUS_OPTIONS: Array<{ label: string; value: StaffStatus | '' }> = [
  { label: 'All', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Resigned', value: 'RESIGNED' },
  { label: 'Terminated', value: 'TERMINATED' },
];

/** Server-paginated teacher directory: filter by department/designation/status, search, deactivate/delete. */
export function TeacherListPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [rows, setRows] = useState<Teacher[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const [departments, setDepartments] = useState<Department[]>([]);
  const [designations, setDesignations] = useState<Designation[]>([]);
  const [departmentId, setDepartmentId] = useState<number | ''>('');
  const [designationId, setDesignationId] = useState<number | ''>('');
  const [status, setStatus] = useState<StaffStatus | ''>('');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 400);

  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [sortModel, setSortModel] = useState<GridSortModel>([{ field: 'employeeId', sort: 'asc' }]);

  const [deleteTarget, setDeleteTarget] = useState<Teacher | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<Teacher | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [lookupDialog, setLookupDialog] = useState<'departments' | 'designations' | null>(null);
  const [exportingExcel, setExportingExcel] = useState(false);

  const loadLookups = useCallback(() => {
    departmentsApi
      .list()
      .then((res) => setDepartments(res.data))
      .catch(() => enqueueSnackbar('Could not load departments.', { variant: 'error' }));
    designationsApi
      .list()
      .then((res) => setDesignations(res.data))
      .catch(() => enqueueSnackbar('Could not load designations.', { variant: 'error' }));
  }, [enqueueSnackbar]);

  useEffect(() => {
    loadLookups();
  }, [loadLookups]);

  const loadTeachers = useCallback(async () => {
    setLoading(true);
    try {
      const sort = sortModel[0] ? `${sortModel[0].field},${sortModel[0].sort}` : undefined;
      const res = await teachersApi.list({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        search: debouncedSearch || undefined,
        departmentId: departmentId || undefined,
        designationId: designationId || undefined,
        status: status || undefined,
        sort,
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load teachers. Please try again.', {
        variant: 'error',
      });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [paginationModel, sortModel, debouncedSearch, departmentId, designationId, status, enqueueSnackbar]);

  useEffect(() => {
    loadTeachers();
  }, [loadTeachers]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [debouncedSearch, departmentId, designationId, status]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setActionLoading(true);
    try {
      await teachersApi.remove(deleteTarget.id);
      enqueueSnackbar('Teacher deleted.', { variant: 'success' });
      setDeleteTarget(null);
      loadTeachers();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this teacher.', { variant: 'error' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeactivate = async () => {
    if (!deactivateTarget) return;
    setActionLoading(true);
    const nextStatus: StaffStatus = deactivateTarget.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await teachersApi.updateStatus(deactivateTarget.id, nextStatus);
      enqueueSnackbar(`Teacher marked as ${nextStatus.toLowerCase()}.`, { variant: 'success' });
      setDeactivateTarget(null);
      loadTeachers();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not update this teacher’s status.', {
        variant: 'error',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleExport = () => {
    exportRowsToCsv(
      rows as unknown as Record<string, unknown>[],
      [
        { field: 'employeeId', header: 'Employee ID' },
        { field: 'firstName', header: 'First Name' },
        { field: 'lastName', header: 'Last Name' },
        { field: 'departmentName', header: 'Department' },
        { field: 'designationName', header: 'Designation' },
        { field: 'email', header: 'Email' },
        { field: 'phone', header: 'Phone' },
        { field: 'status', header: 'Status' },
      ],
      `teachers-page-${paginationModel.page + 1}`,
    );
  };

  const handleExportExcel = async () => {
    setExportingExcel(true);
    try {
      const blob = await teachersApi.exportExcel();
      downloadBlob(blob, 'teachers-export.xlsx');
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not export teachers to Excel.', { variant: 'error' });
    } finally {
      setExportingExcel(false);
    }
  };

  const columns: GridColDef<Teacher>[] = useMemo(
    () => [
      {
        field: 'name',
        headerName: 'Teacher',
        flex: 1.3,
        minWidth: 200,
        sortable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={1.25} alignItems="center" sx={{ height: '100%' }}>
            <Avatar sx={{ width: 32, height: 32 }}>{params.row.firstName?.[0] ?? params.row.username?.[0]}</Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Box sx={{ fontWeight: 600, fontSize: '0.85rem', lineHeight: 1.3, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {params.row.firstName ?? params.row.username} {params.row.lastName ?? ''}
              </Box>
              <Box sx={{ fontSize: '0.75rem', lineHeight: 1.3, color: 'text.secondary' }}>{params.row.employeeId}</Box>
            </Box>
          </Stack>
        ),
      },
      { field: 'departmentName', headerName: 'Department', flex: 0.9, minWidth: 130 },
      { field: 'designationName', headerName: 'Designation', flex: 0.9, minWidth: 130 },
      { field: 'email', headerName: 'Email', flex: 1, minWidth: 160, sortable: false },
      { field: 'phone', headerName: 'Phone', width: 130, sortable: false },
      {
        field: 'status',
        headerName: 'Status',
        width: 130,
        renderCell: (params) => <StatusChip status={params.row.status} />,
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 160,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="View profile">
              <IconButton size="small" onClick={() => navigate(`/app/teachers/${params.row.id}`)}>
                <VisibilityOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => navigate(`/app/teachers/${params.row.id}/edit`)}>
                <EditOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title={params.row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}>
              <IconButton size="small" onClick={() => setDeactivateTarget(params.row)}>
                <BlockOutlinedIcon fontSize="small" color={params.row.status === 'ACTIVE' ? 'warning' : 'success'} />
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
    [navigate],
  );

  return (
    <Box>
      <PageHeader
        title="Teachers"
        subtitle="Manage teaching staff records, qualifications and status"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Teachers' }]}
        action={
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => navigate('/app/teachers/new')}>
            Add Teacher
          </Button>
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                fullWidth
                size="small"
                placeholder="Search by name, employee ID or email"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchOutlinedIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                select
                fullWidth
                size="small"
                label="Department"
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All departments</MenuItem>
                {departments.map((d) => (
                  <MenuItem key={d.id} value={d.id}>
                    {d.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={6} sm={3} md={2}>
              <TextField
                select
                fullWidth
                size="small"
                label="Designation"
                value={designationId}
                onChange={(e) => setDesignationId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All designations</MenuItem>
                {designations.map((d) => (
                  <MenuItem key={d.id} value={d.id}>
                    {d.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={5}>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
                {STATUS_OPTIONS.map((opt) => (
                  <Chip
                    key={opt.label}
                    label={opt.label}
                    size="small"
                    color={status === opt.value && opt.value !== '' ? 'primary' : 'default'}
                    variant={status === opt.value ? 'filled' : 'outlined'}
                    onClick={() => setStatus(opt.value)}
                  />
                ))}
                <Tooltip title="Manage departments">
                  <IconButton size="small" onClick={() => setLookupDialog('departments')}>
                    <TuneOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Manage designations">
                  <IconButton size="small" onClick={() => setLookupDialog('designations')}>
                    <TuneOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          mobileVisibleFields={['name', 'status']}
          paginationMode="server"
          sortingMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          sortModel={sortModel}
          onSortModelChange={setSortModel}
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
          emptyTitle="No teachers found"
          emptyDescription="Try adjusting the filters, or add a new teacher to get started."
        />
      </Card>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete teacher"
        message={`Are you sure you want to delete ${deleteTarget?.firstName ?? deleteTarget?.username ?? ''} ${
          deleteTarget?.lastName ?? ''
        }? This action cannot be undone.`}
        confirmLabel="Delete"
        destructive
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <ConfirmDialog
        open={!!deactivateTarget}
        title={deactivateTarget?.status === 'ACTIVE' ? 'Deactivate teacher' : 'Activate teacher'}
        message={`Are you sure you want to ${deactivateTarget?.status === 'ACTIVE' ? 'deactivate' : 'activate'} ${
          deactivateTarget?.firstName ?? deactivateTarget?.username ?? ''
        }?`}
        confirmLabel="Confirm"
        loading={actionLoading}
        onConfirm={handleDeactivate}
        onCancel={() => setDeactivateTarget(null)}
      />

      <LookupManagerDialog
        open={lookupDialog === 'departments'}
        title="Departments"
        onClose={() => setLookupDialog(null)}
        fetchAll={async () => (await departmentsApi.list()).data}
        create={(p) => departmentsApi.create(p).then((r) => r.data)}
        update={(id, p) => departmentsApi.update(id, p).then((r) => r.data)}
        remove={(id) => departmentsApi.remove(id).then(() => undefined)}
        onChanged={loadLookups}
      />

      <LookupManagerDialog
        open={lookupDialog === 'designations'}
        title="Designations"
        onClose={() => setLookupDialog(null)}
        fetchAll={async () => (await designationsApi.list()).data}
        create={(p) => designationsApi.create(p).then((r) => r.data)}
        update={(id, p) => designationsApi.update(id, p).then((r) => r.data)}
        remove={(id) => designationsApi.remove(id).then(() => undefined)}
        onChanged={loadLookups}
      />
    </Box>
  );
}

export default TeacherListPage;
