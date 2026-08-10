import { useCallback, useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Stack from '@mui/material/Stack';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import type { GridColDef, GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs, { type Dayjs } from 'dayjs';
import DataTable from '@/components/common/DataTable';
import StatusChip from '@/components/common/StatusChip';
import EmptyState from '@/components/common/EmptyState';
import PageLoader from '@/components/common/PageLoader';
import attendanceApi from '@/api/attendanceApi';
import teachersApi from '@/api/teachersApi';
import { useAppSelector } from '@/store/hooks';
import { exportRowsToCsv } from '@/utils/csvExport';
import type { AttendanceStatus, Teacher, TeacherAttendanceReportRow } from '@/types';

const STATUS_OPTIONS: Array<{ value: AttendanceStatus; label: string; color: 'success' | 'error' | 'warning' | 'info' | 'secondary' }> = [
  { value: 'PRESENT', label: 'Present', color: 'success' },
  { value: 'ABSENT', label: 'Absent', color: 'error' },
  { value: 'LATE', label: 'Late', color: 'warning' },
  { value: 'HALF_DAY', label: 'Half Day', color: 'info' },
  { value: 'LEAVE', label: 'Leave', color: 'secondary' },
];

interface RowState {
  teacherId: number;
  firstName: string;
  lastName: string;
  employeeId?: string;
  status: AttendanceStatus;
  checkIn: Dayjs | null;
  checkOut: Dayjs | null;
  remarks: string;
  marked: boolean;
}

const MANAGEMENT_ROLES = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];

function MarkTab() {
  const { enqueueSnackbar } = useSnackbar();
  const [date, setDate] = useState<Dayjs>(dayjs());
  const [rows, setRows] = useState<RowState[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setLoaded(false);
    try {
      const res = await attendanceApi.getTeacherMarkingGrid(date.format('YYYY-MM-DD'));
      setRows(
        res.data.map((r) => ({
          teacherId: r.teacherId,
          firstName: r.firstName,
          lastName: r.lastName,
          employeeId: r.employeeId,
          status: r.status ?? 'PRESENT',
          checkIn: r.checkIn ? dayjs(r.checkIn, 'HH:mm:ss') : null,
          checkOut: r.checkOut ? dayjs(r.checkOut, 'HH:mm:ss') : null,
          remarks: r.remarks ?? '',
          marked: r.status !== null,
        })),
      );
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load teacher attendance.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [date, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const update = (teacherId: number, patch: Partial<RowState>) => {
    setRows((prev) => prev.map((r) => (r.teacherId === teacherId ? { ...r, ...patch, marked: true } : r)));
  };

  const markAllPresent = () => setRows((prev) => prev.map((r) => ({ ...r, status: 'PRESENT', marked: true })));

  const unmarkedCount = useMemo(() => rows.filter((r) => !r.marked).length, [rows]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await attendanceApi.markTeachers({
        attendanceDate: date.format('YYYY-MM-DD'),
        records: rows.map((r) => ({
          teacherId: r.teacherId,
          status: r.status,
          checkIn: r.checkIn ? r.checkIn.format('HH:mm:ss') : undefined,
          checkOut: r.checkOut ? r.checkOut.format('HH:mm:ss') : undefined,
          remarks: r.remarks || undefined,
        })),
      });
      enqueueSnackbar(`Attendance saved for ${rows.length} teacher${rows.length === 1 ? '' : 's'}.`, { variant: 'success' });
      setRows((prev) => prev.map((r) => ({ ...r, marked: true })));
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save teacher attendance.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <DatePicker
                label="Date"
                value={date}
                onChange={(value) => value && setDate(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' } }}
              />
            </Grid>
            <Grid item xs={12} sm={8} md={9}>
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button size="small" variant="outlined" startIcon={<DoneAllOutlinedIcon />} disabled={rows.length === 0} onClick={markAllPresent}>
                  Mark all present
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {loading ? (
        <PageLoader label="Loading teachers..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No teachers found" description="There are no active teachers to mark attendance for." />
      ) : (
        <Card>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2, py: 1.5 }}>
            <Stack direction="row" spacing={1} alignItems="center">
              {unmarkedCount > 0 && <Chip size="small" color="warning" variant="outlined" label={`${unmarkedCount} unmarked`} />}
              <Chip size="small" variant="outlined" label={`${rows.length} teacher${rows.length === 1 ? '' : 's'}`} />
            </Stack>
            <Button
              variant="contained"
              startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveOutlinedIcon />}
              disabled={saving}
              onClick={handleSave}
            >
              Save Attendance
            </Button>
          </Box>
          <TableContainer sx={{ maxHeight: 560 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ minWidth: 180 }}>Teacher</TableCell>
                  <TableCell sx={{ minWidth: 340 }}>Status</TableCell>
                  <TableCell sx={{ minWidth: 130 }}>Check In</TableCell>
                  <TableCell sx={{ minWidth: 130 }}>Check Out</TableCell>
                  <TableCell sx={{ minWidth: 180 }}>Remarks</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.teacherId} hover>
                    <TableCell>
                      <Stack direction="row" spacing={0.75} alignItems="center">
                        <span>
                          {row.firstName} {row.lastName}
                        </span>
                        {!row.marked && <Chip size="small" label="Unmarked" variant="outlined" sx={{ height: 20 }} />}
                      </Stack>
                    </TableCell>
                    <TableCell>
                      <ToggleButtonGroup
                        size="small"
                        exclusive
                        value={row.status}
                        onChange={(_e, value: AttendanceStatus | null) => value && update(row.teacherId, { status: value })}
                      >
                        {STATUS_OPTIONS.map((opt) => (
                          <ToggleButton
                            key={opt.value}
                            value={opt.value}
                            sx={{
                              px: 1.25,
                              py: 0.25,
                              fontSize: '0.7rem',
                              '&.Mui-selected': {
                                bgcolor: `${opt.color}.main`,
                                color: `${opt.color}.contrastText`,
                                '&:hover': { bgcolor: `${opt.color}.dark` },
                              },
                            }}
                          >
                            {opt.label}
                          </ToggleButton>
                        ))}
                      </ToggleButtonGroup>
                    </TableCell>
                    <TableCell>
                      <TimePicker
                        value={row.checkIn}
                        onChange={(value) => update(row.teacherId, { checkIn: value })}
                        slotProps={{ textField: { size: 'small', fullWidth: true } }}
                      />
                    </TableCell>
                    <TableCell>
                      <TimePicker
                        value={row.checkOut}
                        onChange={(value) => update(row.teacherId, { checkOut: value })}
                        slotProps={{ textField: { size: 'small', fullWidth: true } }}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        fullWidth
                        placeholder="Optional remarks"
                        value={row.remarks}
                        onChange={(e) => update(row.teacherId, { remarks: e.target.value })}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}
    </Box>
  );
}

function ReportTab() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isManagement = !!user && MANAGEMENT_ROLES.includes(user.role);

  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [teacherId, setTeacherId] = useState<number | ''>(isManagement ? '' : user?.teacherId ?? '');
  const [startDate, setStartDate] = useState<Dayjs>(dayjs().startOf('month'));
  const [endDate, setEndDate] = useState<Dayjs>(dayjs());

  const [rows, setRows] = useState<TeacherAttendanceReportRow[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [sortModel, setSortModel] = useState<GridSortModel>([{ field: 'attendanceDate', sort: 'desc' }]);

  useEffect(() => {
    if (!isManagement) return;
    teachersApi
      .list({ size: 200, sort: 'employeeId,asc' })
      .then((res) => setTeachers(res.data.content))
      .catch(() => enqueueSnackbar('Could not load teachers for filtering.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isManagement]);

  const loadReport = useCallback(async () => {
    setLoading(true);
    try {
      const sort = sortModel[0] ? `${sortModel[0].field},${sortModel[0].sort}` : undefined;
      const res = await attendanceApi.getTeacherReport({
        teacherId: teacherId || undefined,
        startDate: startDate.format('YYYY-MM-DD'),
        endDate: endDate.format('YYYY-MM-DD'),
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort,
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the teacher attendance report.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [teacherId, startDate, endDate, paginationModel, sortModel, enqueueSnackbar]);

  useEffect(() => {
    loadReport();
  }, [loadReport]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [teacherId, startDate, endDate]);

  const handleExport = () => {
    exportRowsToCsv(
      rows as unknown as Record<string, unknown>[],
      [
        { field: 'attendanceDate', header: 'Date' },
        { field: 'employeeId', header: 'Employee ID' },
        { field: 'firstName', header: 'First Name' },
        { field: 'lastName', header: 'Last Name' },
        { field: 'status', header: 'Status' },
        { field: 'checkIn', header: 'Check In' },
        { field: 'checkOut', header: 'Check Out' },
        { field: 'remarks', header: 'Remarks' },
      ],
      `teacher-attendance-page-${paginationModel.page + 1}`,
    );
  };

  const columns: GridColDef<TeacherAttendanceReportRow>[] = useMemo(
    () => [
      {
        field: 'attendanceDate',
        headerName: 'Date',
        width: 130,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      ...(isManagement
        ? [
            {
              field: 'name',
              headerName: 'Teacher',
              flex: 1,
              minWidth: 160,
              sortable: false,
              valueGetter: (_v, row) => `${row.firstName ?? ''} ${row.lastName ?? ''}`.trim() || '-',
            } as GridColDef<TeacherAttendanceReportRow>,
            { field: 'employeeId', headerName: 'Employee ID', width: 130 } as GridColDef<TeacherAttendanceReportRow>,
          ]
        : []),
      { field: 'status', headerName: 'Status', width: 130, renderCell: (params) => <StatusChip status={params.row.status} /> },
      { field: 'checkIn', headerName: 'Check In', width: 110 },
      { field: 'checkOut', headerName: 'Check Out', width: 110 },
      { field: 'remarks', headerName: 'Remarks', flex: 1, minWidth: 160, sortable: false },
    ],
    [isManagement],
  );

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            {isManagement && (
              <Grid item xs={12} sm={4} md={3}>
                <TextField
                  select
                  fullWidth
                  size="small"
                  label="Teacher"
                  value={teacherId}
                  onChange={(e) => setTeacherId(e.target.value === '' ? '' : Number(e.target.value))}
                >
                  <MenuItem value="">All teachers</MenuItem>
                  {teachers.map((t) => (
                    <MenuItem key={t.id} value={t.id}>
                      {t.firstName ?? t.username} {t.lastName ?? ''}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            )}
            <Grid item xs={6} sm={4} md={2.5}>
              <DatePicker
                label="Start Date"
                value={startDate}
                onChange={(value) => value && setStartDate(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' } }}
              />
            </Grid>
            <Grid item xs={6} sm={4} md={2.5}>
              <DatePicker
                label="End Date"
                value={endDate}
                onChange={(value) => value && setEndDate(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' } }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          getRowId={(row) => row.id}
          paginationMode="server"
          sortingMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          sortModel={sortModel}
          onSortModelChange={setSortModel}
          onExport={handleExport}
          mobileVisibleFields={['attendanceDate', 'name', 'status']}
          emptyTitle="No attendance records found"
          emptyDescription="Try adjusting the filters or date range."
        />
      </Card>
    </Box>
  );
}

/** Combines the teacher "mark daily roll call" grid and the teacher attendance report/history in one tabbed page. */
export function TeacherAttendancePage() {
  const [tab, setTab] = useState(0);

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <Tabs value={tab} onChange={(_e: SyntheticEvent, v: number) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
          <Tab label="Mark Attendance" />
          <Tab label="Report" />
        </Tabs>
      </Card>
      {tab === 0 ? <MarkTab /> : <ReportTab />}
    </Box>
  );
}

export default TeacherAttendancePage;
