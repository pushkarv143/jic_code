import { useCallback, useEffect, useMemo, useState } from 'react';
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
import Alert from '@mui/material/Alert';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import dayjs from 'dayjs';
import type { GridColDef, GridPaginationModel, GridRowSelectionModel, GridSortModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import StatusChip from '@/components/common/StatusChip';
import usersApi from '@/api/usersApi';
import type { Role, User } from '@/types';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';

const ROLE_OPTIONS: Array<{ label: string; value: Role | '' }> = [
  { label: 'All roles', value: '' },
  { label: 'Super Admin', value: 'SUPER_ADMIN' },
  { label: 'Principal', value: 'PRINCIPAL' },
  { label: 'Vice Principal', value: 'VICE_PRINCIPAL' },
  { label: 'Teacher', value: 'TEACHER' },
  { label: 'Accountant', value: 'ACCOUNTANT' },
  { label: 'Librarian', value: 'LIBRARIAN' },
  { label: 'Receptionist', value: 'RECEPTIONIST' },
  { label: 'Student', value: 'STUDENT' },
  { label: 'Parent', value: 'PARENT' },
  { label: 'Security Guard', value: 'SECURITY_GUARD' },
];

const STATUS_OPTIONS: Array<{ label: string; value: '' | 'ACTIVE' | 'INACTIVE' }> = [
  { label: 'Pending / Inactive', value: 'INACTIVE' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'All', value: '' },
];

/** Self-registration (AuthServiceImpl.register) always creates inactive accounts that have
 * never logged in. Admin-created accounts (UserServiceImpl.createUser) start active, so the
 * only way an account is both inactive AND has no lastLogin is a self-signup awaiting approval. */
const isPendingSignup = (u: User) => !u.active && !u.lastLogin;

/**
 * Admin directory of every system user account, including self-registered STUDENT/PARENT
 * signups (created inactive, awaiting approval - see AuthServiceImpl.register). Lets
 * SUPER_ADMIN/PRINCIPAL activate or deactivate any account from one screen instead of
 * calling the backend API directly. Defaults to the pending/inactive view since that's
 * the primary reason to open this screen; use the status filter to find active users.
 */
export function UserListPage() {
  const { enqueueSnackbar } = useSnackbar();

  const [rows, setRows] = useState<User[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const [role, setRole] = useState<Role | ''>('');
  const [statusFilter, setStatusFilter] = useState<'' | 'ACTIVE' | 'INACTIVE'>('INACTIVE');
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 400);

  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [sortModel, setSortModel] = useState<GridSortModel>([{ field: 'id', sort: 'desc' }]);
  const [selectionModel, setSelectionModel] = useState<GridRowSelectionModel>([]);

  const [toggleTarget, setToggleTarget] = useState<User | null>(null);
  const [bulkConfirmOpen, setBulkConfirmOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // Global count of pending accounts, independent of the role/search filters above, so the
  // banner still makes sense while an admin is looking at the Active/All view.
  const [pendingCount, setPendingCount] = useState<number | null>(null);

  const loadPendingCount = useCallback(async () => {
    try {
      // No dedicated "count of inactive users" endpoint - pull a large batch and count
      // client-side. Fine at this app's scale; would need a real backend count past ~1000 users.
      const res = await usersApi.list({ page: 0, size: 1000, sort: 'id,desc' });
      setPendingCount(res.data.content.filter((u) => !u.active).length);
    } catch {
      setPendingCount(null);
    }
  }, []);

  useEffect(() => {
    loadPendingCount();
  }, [loadPendingCount]);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const sort = sortModel[0] ? `${sortModel[0].field},${sortModel[0].sort}` : undefined;
      if (statusFilter === '') {
        // No active/inactive filter: normal server-side pagination.
        const res = await usersApi.list({
          page: paginationModel.page,
          size: paginationModel.pageSize,
          search: debouncedSearch || undefined,
          role: role || undefined,
          sort,
        });
        setRows(res.data.content);
        setRowCount(res.data.totalElements);
      } else {
        // The list endpoint has no server-side active/inactive filter, so pull every
        // matching row (role/search still applied server-side) and paginate client-side -
        // filtering only the current page would silently miss/miscount matches elsewhere.
        const res = await usersApi.list({
          page: 0,
          size: 1000,
          search: debouncedSearch || undefined,
          role: role || undefined,
          sort,
        });
        const filtered = res.data.content.filter((u) => (statusFilter === 'ACTIVE' ? u.active : !u.active));
        setRows(filtered);
        setRowCount(filtered.length);
      }
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load users. Please try again.', {
        variant: 'error',
      });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [paginationModel, sortModel, debouncedSearch, role, statusFilter, enqueueSnackbar]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
    setSelectionModel([]);
  }, [debouncedSearch, role, statusFilter]);

  const handleToggleActive = async () => {
    if (!toggleTarget) return;
    setActionLoading(true);
    try {
      if (toggleTarget.active) {
        await usersApi.deactivate(toggleTarget.id);
        enqueueSnackbar('User deactivated.', { variant: 'success' });
      } else {
        await usersApi.activate(toggleTarget.id);
        enqueueSnackbar('User activated.', { variant: 'success' });
      }
      setToggleTarget(null);
      loadUsers();
      loadPendingCount();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not update this user’s status.', {
        variant: 'error',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const selectedIds = selectionModel as number[];
  const selectedRows = useMemo(() => rows.filter((r) => selectedIds.includes(r.id)), [rows, selectedIds]);
  const allSelectedInactive = selectedRows.length > 0 && selectedRows.every((r) => !r.active);
  const allSelectedActive = selectedRows.length > 0 && selectedRows.every((r) => r.active);
  const bulkActionAvailable = allSelectedInactive || allSelectedActive;

  const handleBulkApply = async () => {
    if (selectedRows.length === 0 || !bulkActionAvailable) return;
    setActionLoading(true);
    try {
      if (allSelectedInactive) {
        await Promise.all(selectedRows.map((r) => usersApi.activate(r.id)));
        enqueueSnackbar(`${selectedRows.length} user(s) activated.`, { variant: 'success' });
      } else {
        await Promise.all(selectedRows.map((r) => usersApi.deactivate(r.id)));
        enqueueSnackbar(`${selectedRows.length} user(s) deactivated.`, { variant: 'success' });
      }
      setBulkConfirmOpen(false);
      setSelectionModel([]);
      loadUsers();
      loadPendingCount();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not update the selected users.', {
        variant: 'error',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const viewPendingOnly = () => {
    setRole('');
    setSearch('');
    setStatusFilter('INACTIVE');
  };

  const columns: GridColDef<User>[] = useMemo(
    () => [
      {
        field: 'name',
        headerName: 'User',
        flex: 1.3,
        minWidth: 220,
        sortable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={1.25} alignItems="center" sx={{ height: '100%' }}>
            <Avatar sx={{ width: 32, height: 32 }}>{params.row.firstName?.[0] ?? params.row.username?.[0]}</Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Box
                sx={{
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  lineHeight: 1.3,
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {params.row.firstName} {params.row.lastName ?? ''}
              </Box>
              <Box sx={{ fontSize: '0.75rem', lineHeight: 1.3, color: 'text.secondary' }}>
                @{params.row.username}
              </Box>
              {isPendingSignup(params.row) && (
                <Chip
                  label="New signup"
                  size="small"
                  color="info"
                  variant="outlined"
                  sx={{ height: 18, fontSize: '0.65rem', mt: 0.25, '& .MuiChip-label': { px: 0.75 } }}
                />
              )}
            </Box>
          </Stack>
        ),
      },
      { field: 'email', headerName: 'Email', flex: 1, minWidth: 180, sortable: false },
      { field: 'phone', headerName: 'Phone', width: 130, sortable: false },
      {
        field: 'role',
        headerName: 'Role',
        width: 140,
        sortable: false,
        renderCell: (params) => <Chip label={params.row.role.replace(/_/g, ' ')} size="small" variant="outlined" />,
      },
      {
        field: 'createdAt',
        headerName: 'Registered',
        width: 160,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY, hh:mm A') : '-'),
      },
      {
        field: 'active',
        headerName: 'Status',
        width: 140,
        renderCell: (params) => <StatusChip status={params.row.active ? 'ACTIVE' : 'INACTIVE'} />,
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 110,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Tooltip title={params.row.active ? 'Deactivate' : 'Activate'}>
            <IconButton size="small" onClick={() => setToggleTarget(params.row)}>
              <BlockOutlinedIcon fontSize="small" color={params.row.active ? 'warning' : 'success'} />
            </IconButton>
          </Tooltip>
        ),
      },
    ],
    [],
  );

  return (
    <Box>
      <PageHeader
        title="Users"
        subtitle="Manage every system account, including self-registered students and parents awaiting approval"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Users' }]}
      />

      {!!pendingCount && (
        <Alert
          severity="info"
          sx={{ mb: 2.5 }}
          action={
            statusFilter !== 'INACTIVE' || role || search ? (
              <Button color="inherit" size="small" onClick={viewPendingOnly}>
                View pending
              </Button>
            ) : undefined
          }
        >
          {pendingCount} account{pendingCount === 1 ? '' : 's'} pending approval.
        </Alert>
      )}

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                size="small"
                placeholder="Search by name, username or email"
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
                label="Role"
                value={role}
                onChange={(e) => setRole(e.target.value as Role | '')}
              >
                {ROLE_OPTIONS.map((opt) => (
                  <MenuItem key={opt.label} value={opt.value}>
                    {opt.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} md={6}>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
                {STATUS_OPTIONS.map((opt) => (
                  <Chip
                    key={opt.label}
                    label={opt.label}
                    size="small"
                    color={statusFilter === opt.value && opt.value !== '' ? 'primary' : 'default'}
                    variant={statusFilter === opt.value ? 'filled' : 'outlined'}
                    onClick={() => setStatusFilter(opt.value)}
                  />
                ))}
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {selectedRows.length > 0 && (
        <Box sx={{ mb: 1.5 }}>
          <Tooltip title={bulkActionAvailable ? '' : 'Select users that are all currently active, or all currently inactive, to bulk update.'}>
            <span>
              <Button
                variant="outlined"
                size="small"
                startIcon={<CheckCircleOutlinedIcon />}
                disabled={!bulkActionAvailable}
                onClick={() => setBulkConfirmOpen(true)}
              >
                {allSelectedActive ? 'Deactivate' : 'Activate'} {selectedRows.length} selected
              </Button>
            </span>
          </Tooltip>
        </Box>
      )}

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          checkboxSelection
          rowSelectionModel={selectionModel}
          onRowSelectionModelChange={setSelectionModel}
          mobileVisibleFields={['name', 'active']}
          paginationMode={statusFilter === '' ? 'server' : 'client'}
          sortingMode={statusFilter === '' ? 'server' : 'client'}
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          sortModel={sortModel}
          onSortModelChange={setSortModel}
          emptyTitle="No users found"
          emptyDescription="Try adjusting the search or filters."
        />
      </Card>

      <ConfirmDialog
        open={!!toggleTarget}
        title={toggleTarget?.active ? 'Deactivate user' : 'Activate user'}
        message={`Are you sure you want to ${toggleTarget?.active ? 'deactivate' : 'activate'} ${
          toggleTarget?.firstName ?? toggleTarget?.username ?? ''
        } ${toggleTarget?.lastName ?? ''}?${
          !toggleTarget?.active ? ' They will be able to sign in once activated.' : ''
        }`}
        confirmLabel="Confirm"
        loading={actionLoading}
        onConfirm={handleToggleActive}
        onCancel={() => setToggleTarget(null)}
      />

      <ConfirmDialog
        open={bulkConfirmOpen}
        title={allSelectedActive ? 'Deactivate selected users' : 'Activate selected users'}
        message={
          allSelectedActive
            ? `Are you sure you want to deactivate ${selectedRows.length} user(s)?`
            : `Are you sure you want to activate ${selectedRows.length} user(s)? They will be able to sign in once activated.`
        }
        confirmLabel={actionLoading ? 'Applying…' : 'Confirm'}
        loading={actionLoading}
        onConfirm={handleBulkApply}
        onCancel={() => setBulkConfirmOpen(false)}
      />
    </Box>
  );
}

export default UserListPage;
