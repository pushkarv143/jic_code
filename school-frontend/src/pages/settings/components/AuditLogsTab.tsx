import { useCallback, useEffect, useMemo, useState } from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import { useSnackbar } from 'notistack';
import dayjs, { type Dayjs } from 'dayjs';
import DataTable from '@/components/common/DataTable';
import auditLogsApi from '@/api/auditLogsApi';
import type { AuditLog } from '@/types';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import AuditLogDetailsDialog from './AuditLogDetailsDialog';

/** SUPER_ADMIN/PRINCIPAL-only tab: filters + paginated audit trail, GET /audit-logs. */
export function AuditLogsTab() {
  const { enqueueSnackbar } = useSnackbar();

  const [userId, setUserId] = useState('');
  const [entityName, setEntityName] = useState('');
  const [action, setAction] = useState('');
  const [startDate, setStartDate] = useState<Dayjs | null>(null);
  const [endDate, setEndDate] = useState<Dayjs | null>(null);

  const debouncedEntityName = useDebouncedValue(entityName, 400);
  const debouncedAction = useDebouncedValue(action, 400);
  const debouncedUserId = useDebouncedValue(userId, 400);

  const [rows, setRows] = useState<AuditLog[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [detailsTarget, setDetailsTarget] = useState<AuditLog | null>(null);

  const loadLogs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await auditLogsApi.list({
        userId: debouncedUserId ? Number(debouncedUserId) : undefined,
        entityName: debouncedEntityName || undefined,
        action: debouncedAction || undefined,
        startDate: startDate ? startDate.format('YYYY-MM-DD') : undefined,
        endDate: endDate ? endDate.format('YYYY-MM-DD') : undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'createdAt,desc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load audit logs.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [debouncedUserId, debouncedEntityName, debouncedAction, startDate, endDate, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [debouncedUserId, debouncedEntityName, debouncedAction, startDate, endDate]);

  const columns: GridColDef<AuditLog>[] = useMemo(
    () => [
      {
        field: 'createdAt',
        headerName: 'Timestamp',
        width: 170,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY, hh:mm A') : '-'),
      },
      {
        field: 'userName',
        headerName: 'User',
        flex: 0.9,
        minWidth: 140,
        valueGetter: (_v, row) => row.userName ?? (row.userId ? `#${row.userId}` : 'System'),
      },
      { field: 'action', headerName: 'Action', width: 130 },
      {
        field: 'entityName',
        headerName: 'Entity',
        flex: 0.9,
        minWidth: 140,
        valueGetter: (_v, row) => (row.entityId != null ? `${row.entityName} #${row.entityId}` : row.entityName),
      },
      { field: 'ipAddress', headerName: 'IP Address', width: 130, valueGetter: (_v, row) => row.ipAddress ?? '-' },
      {
        field: 'actions',
        headerName: 'Details',
        width: 100,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Tooltip title="View details">
            <IconButton size="small" onClick={() => setDetailsTarget(params.row)}>
              <VisibilityOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
      },
    ],
    [],
  );

  return (
    <>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={2}>
              <TextField
                fullWidth
                size="small"
                label="User ID"
                value={userId}
                onChange={(e) => setUserId(e.target.value.replace(/\D/g, ''))}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={2.5}>
              <TextField
                fullWidth
                size="small"
                label="Entity Name"
                placeholder="e.g. Student"
                value={entityName}
                onChange={(e) => setEntityName(e.target.value)}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={2.5}>
              <TextField
                fullWidth
                size="small"
                label="Action"
                placeholder="e.g. CREATE"
                value={action}
                onChange={(e) => setAction(e.target.value)}
              />
            </Grid>
            <Grid item xs={6} sm={3} md={2.5}>
              <DatePicker
                label="Start Date"
                value={startDate}
                onChange={setStartDate}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' }, field: { clearable: true } }}
              />
            </Grid>
            <Grid item xs={6} sm={3} md={2.5}>
              <DatePicker
                label="End Date"
                value={endDate}
                onChange={setEndDate}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' }, field: { clearable: true } }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          mobileVisibleFields={['userName', 'action']}
          loading={loading}
          getRowId={(row) => row.id}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No audit log entries found"
          emptyDescription="Try adjusting the filters or date range."
        />
      </Card>

      <AuditLogDetailsDialog open={!!detailsTarget} log={detailsTarget} onClose={() => setDetailsTarget(null)} />
    </>
  );
}

export default AuditLogsTab;
