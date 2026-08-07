import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import dayjs from 'dayjs';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import hostelApi from '@/api/hostelApi';
import { useAppSelector } from '@/store/hooks';
import type { HostelVisitor, Role } from '@/types';
import VisitorFormDialog from './components/VisitorFormDialog';

const WRITE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];

/** Server-paginated hostel visitor log. Staff can log/check-out visitors; STUDENT/PARENT see their own read-only history. */
export function VisitorsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isSelfView = user?.role === 'STUDENT' || user?.role === 'PARENT';
  const canWrite = !!user && WRITE_ROLES.includes(user.role);

  const [rows, setRows] = useState<HostelVisitor[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [checkingOutId, setCheckingOutId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await hostelApi.hostelVisitors.list({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'visitDate,desc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the visitor log.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleLog = async (values: Parameters<typeof hostelApi.hostelVisitors.create>[0]) => {
    setSaving(true);
    try {
      await hostelApi.hostelVisitors.create(values);
      enqueueSnackbar('Visitor logged.', { variant: 'success' });
      setFormOpen(false);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not log this visitor.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleCheckout = async (visitor: HostelVisitor) => {
    setCheckingOutId(visitor.id);
    try {
      await hostelApi.hostelVisitors.checkout(visitor.id);
      enqueueSnackbar('Visitor checked out.', { variant: 'success' });
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not check out this visitor.', { variant: 'error' });
    } finally {
      setCheckingOutId(null);
    }
  };

  const columns: GridColDef<HostelVisitor>[] = useMemo(
    () => [
      ...(isSelfView
        ? []
        : ([{ field: 'studentName', headerName: 'Student', flex: 1, minWidth: 150, valueGetter: (_v, row) => row.studentName ?? `#${row.studentId}` }] as GridColDef<HostelVisitor>[])),
      { field: 'visitorName', headerName: 'Visitor', flex: 1, minWidth: 140 },
      { field: 'relation', headerName: 'Relation', width: 120 },
      {
        field: 'visitDate',
        headerName: 'Visit Date',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      {
        field: 'checkIn',
        headerName: 'Check In',
        width: 150,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM, hh:mm A') : '-'),
      },
      {
        field: 'checkOut',
        headerName: 'Check Out',
        width: 160,
        renderCell: (params) =>
          params.row.checkOut ? (
            dayjs(params.row.checkOut).format('DD MMM, hh:mm A')
          ) : (
            <Chip size="small" label="Still inside" color="warning" variant="outlined" />
          ),
      },
      ...(canWrite
        ? ([
            {
              field: 'actions',
              headerName: 'Actions',
              width: 90,
              sortable: false,
              filterable: false,
              renderCell: (params: any) =>
                !params.row.checkOut ? (
                  <Tooltip title="Check out">
                    <IconButton
                      size="small"
                      color="primary"
                      disabled={checkingOutId === params.row.id}
                      onClick={() => handleCheckout(params.row)}
                    >
                      <LogoutOutlinedIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                ) : null,
            } as GridColDef<HostelVisitor>,
          ])
        : []),
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [isSelfView, canWrite, checkingOutId],
  );

  return (
    <Box>
      {canWrite && (
        <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => setFormOpen(true)}>
            Log Visitor
          </Button>
        </Stack>
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
          emptyTitle="No visitors logged"
          emptyDescription={canWrite ? 'Log a visitor to get started.' : 'No visitors have been logged for you yet.'}
        />
      </Card>

      {canWrite && (
        <VisitorFormDialog open={formOpen} saving={saving} onClose={() => setFormOpen(false)} onSubmit={handleLog} />
      )}
    </Box>
  );
}

export default VisitorsPage;
