import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Autocomplete from '@mui/material/Autocomplete';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import type { GridColDef, GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs, { type Dayjs } from 'dayjs';
import DataTable from '@/components/common/DataTable';
import StatusChip from '@/components/common/StatusChip';
import StatCard from '@/components/common/StatCard';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined';
import WatchLaterOutlinedIcon from '@mui/icons-material/WatchLaterOutlined';
import PercentOutlinedIcon from '@mui/icons-material/PercentOutlined';
import classesApi from '@/api/classesApi';
import studentsApi from '@/api/studentsApi';
import attendanceApi from '@/api/attendanceApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { exportRowsToCsv } from '@/utils/csvExport';
import { useAppSelector } from '@/store/hooks';
import { getStudentDisplayName } from '@/utils/format';
import type { SchoolClass, Section, Student, StudentAttendanceReportRow, StudentAttendanceSummary } from '@/types';

/** Filters (class/section/student/date range), a server-paginated register and a per-student summary panel. */
export function AttendanceReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isSelfView = user?.role === 'STUDENT' || user?.role === 'PARENT';

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [sectionId, setSectionId] = useState<number | ''>('');

  const [studentQuery, setStudentQuery] = useState('');
  const debouncedStudentQuery = useDebouncedValue(studentQuery, 400);
  const [studentOptions, setStudentOptions] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);

  const [startDate, setStartDate] = useState<Dayjs>(dayjs().startOf('month'));
  const [endDate, setEndDate] = useState<Dayjs>(dayjs());

  const [rows, setRows] = useState<StudentAttendanceReportRow[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [sortModel, setSortModel] = useState<GridSortModel>([{ field: 'attendanceDate', sort: 'desc' }]);

  const [summary, setSummary] = useState<StudentAttendanceSummary | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);

  const effectiveStudentId = isSelfView ? user?.studentId ?? undefined : selectedStudent?.id;

  useEffect(() => {
    if (isSelfView) return;
    classesApi
      .list({ size: 200, sort: 'className,asc' })
      .then((res) => setClasses(res.data.content))
      .catch(() => enqueueSnackbar('Could not load classes for filtering.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSelfView]);

  useEffect(() => {
    if (isSelfView || !classId) {
      setSections([]);
      setSectionId('');
      return;
    }
    classesApi
      .listSections(classId as number)
      .then((res) => setSections(res.data))
      .catch(() => enqueueSnackbar('Could not load sections.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId, isSelfView]);

  useEffect(() => {
    if (isSelfView || !debouncedStudentQuery) {
      if (!isSelfView) setStudentOptions([]);
      return;
    }
    studentsApi
      .list({ search: debouncedStudentQuery, size: 20 })
      .then((res) => setStudentOptions(res.data.content))
      .catch(() => undefined);
  }, [debouncedStudentQuery, isSelfView]);

  const loadReport = useCallback(async () => {
    setLoading(true);
    try {
      const sort = sortModel[0] ? `${sortModel[0].field},${sortModel[0].sort}` : undefined;
      const res = await attendanceApi.getStudentReport({
        studentId: effectiveStudentId,
        classId: isSelfView ? undefined : classId || undefined,
        sectionId: isSelfView ? undefined : sectionId || undefined,
        startDate: startDate.format('YYYY-MM-DD'),
        endDate: endDate.format('YYYY-MM-DD'),
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort,
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the attendance report.', {
        variant: 'error',
      });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [effectiveStudentId, classId, sectionId, startDate, endDate, paginationModel, sortModel, isSelfView, enqueueSnackbar]);

  useEffect(() => {
    loadReport();
  }, [loadReport]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [effectiveStudentId, classId, sectionId, startDate, endDate]);

  useEffect(() => {
    if (!effectiveStudentId) {
      setSummary(null);
      return;
    }
    setSummaryLoading(true);
    attendanceApi
      .getStudentSummary(effectiveStudentId, {
        startDate: startDate.format('YYYY-MM-DD'),
        endDate: endDate.format('YYYY-MM-DD'),
      })
      .then((res) => setSummary(res.data))
      .catch(() => setSummary(null))
      .finally(() => setSummaryLoading(false));
  }, [effectiveStudentId, startDate, endDate]);

  const handleExport = () => {
    exportRowsToCsv(
      rows as unknown as Record<string, unknown>[],
      [
        { field: 'attendanceDate', header: 'Date' },
        { field: 'rollNumber', header: 'Roll No.' },
        { field: 'firstName', header: 'First Name' },
        { field: 'lastName', header: 'Last Name' },
        { field: 'className', header: 'Class' },
        { field: 'sectionName', header: 'Section' },
        { field: 'status', header: 'Status' },
        { field: 'remarks', header: 'Remarks' },
      ],
      `attendance-report-page-${paginationModel.page + 1}`,
    );
  };

  const columns: GridColDef<StudentAttendanceReportRow>[] = useMemo(
    () => [
      {
        field: 'attendanceDate',
        headerName: 'Date',
        width: 130,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      ...(isSelfView
        ? []
        : [
            { field: 'rollNumber', headerName: 'Roll No.', width: 100 } as GridColDef<StudentAttendanceReportRow>,
            {
              field: 'name',
              headerName: 'Student',
              flex: 1,
              minWidth: 160,
              sortable: false,
              valueGetter: (_v, row) => [row.firstName, row.lastName].filter(Boolean).join(' ') || '-',
            } as GridColDef<StudentAttendanceReportRow>,
            {
              field: 'className',
              headerName: 'Class / Section',
              flex: 0.8,
              minWidth: 130,
              valueGetter: (_v, row) => (row.className ? `${row.className}${row.sectionName ? ` - ${row.sectionName}` : ''}` : '-'),
            } as GridColDef<StudentAttendanceReportRow>,
          ]),
      {
        field: 'status',
        headerName: 'Status',
        width: 130,
        renderCell: (params) => <StatusChip status={params.row.status} />,
      },
      { field: 'remarks', headerName: 'Remarks', flex: 1, minWidth: 160, sortable: false },
    ],
    [isSelfView],
  );

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            {!isSelfView && (
              <>
                <Grid item xs={12} sm={6} md={2.5}>
                  <TextField
                    select
                    fullWidth
                    size="small"
                    label="Class"
                    value={classId}
                    onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">All classes</MenuItem>
                    {classes.map((cls) => (
                      <MenuItem key={cls.id} value={cls.id}>
                        {cls.className}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid item xs={12} sm={6} md={2.5}>
                  <TextField
                    select
                    fullWidth
                    size="small"
                    label="Section"
                    value={sectionId}
                    disabled={!classId}
                    onChange={(e) => setSectionId(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">All sections</MenuItem>
                    {sections.map((sec) => (
                      <MenuItem key={sec.id} value={sec.id}>
                        {sec.sectionName}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Autocomplete
                    size="small"
                    options={studentOptions}
                    value={selectedStudent}
                    getOptionLabel={(o) => getStudentDisplayName(o)}
                    isOptionEqualToValue={(o, v) => o.id === v.id}
                    onChange={(_e, value) => setSelectedStudent(value)}
                    onInputChange={(_e, value) => setStudentQuery(value)}
                    renderInput={(params) => <TextField {...params} label="Student (optional)" placeholder="Search by name..." />}
                  />
                </Grid>
              </>
            )}
            <Grid item xs={6} sm={3} md={isSelfView ? 3 : 2}>
              <DatePicker
                label="Start Date"
                value={startDate}
                onChange={(value) => value && setStartDate(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' } }}
              />
            </Grid>
            <Grid item xs={6} sm={3} md={isSelfView ? 3 : 2}>
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

      {effectiveStudentId && summary && !summaryLoading && (
        <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<PercentOutlinedIcon />} label="Attendance %" value={`${summary.percentage.toFixed(1)}%`} color="primary" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<EventAvailableOutlinedIcon />} label="Present Days" value={summary.presentDays} color="success" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<EventBusyOutlinedIcon />} label="Absent Days" value={summary.absentDays} color="error" />
          </Grid>
          <Grid item xs={6} sm={3}>
            <StatCard icon={<WatchLaterOutlinedIcon />} label="Late / Half Day" value={summary.lateDays + summary.halfDays} color="warning" />
          </Grid>
        </Grid>
      )}

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
          emptyTitle="No attendance records found"
          emptyDescription="Try adjusting the filters or date range."
        />
      </Card>
    </Box>
  );
}

export default AttendanceReportPage;
