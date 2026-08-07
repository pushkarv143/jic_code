import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
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
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import CircularProgress from '@mui/material/CircularProgress';
import { useSnackbar } from 'notistack';
import dayjs, { type Dayjs } from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import PageLoader from '@/components/common/PageLoader';
import classesApi from '@/api/classesApi';
import attendanceApi from '@/api/attendanceApi';
import type { AttendanceStatus, SchoolClass, Section } from '@/types';
import { getStudentDisplayName } from '@/utils/format';

const STATUS_OPTIONS: Array<{ value: AttendanceStatus; label: string; color: 'success' | 'error' | 'warning' | 'info' | 'secondary' }> = [
  { value: 'PRESENT', label: 'Present', color: 'success' },
  { value: 'ABSENT', label: 'Absent', color: 'error' },
  { value: 'LATE', label: 'Late', color: 'warning' },
  { value: 'HALF_DAY', label: 'Half Day', color: 'info' },
  { value: 'LEAVE', label: 'Leave', color: 'secondary' },
];

interface RowState {
  studentId: number;
  firstName: string | null;
  lastName: string | null;
  rollNumber: string;
  status: AttendanceStatus;
  remarks: string;
  /** True once the row's status originated from the API or the user explicitly touched it. */
  marked: boolean;
}

/** Class + section + date -> editable per-student status grid, posted in one batch. */
export function MarkAttendancePage() {
  const { enqueueSnackbar } = useSnackbar();

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [sectionId, setSectionId] = useState<number | ''>('');
  const [date, setDate] = useState<Dayjs>(dayjs());

  const [rows, setRows] = useState<RowState[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    classesApi
      .list({ size: 200, sort: 'className,asc' })
      .then((res) => setClasses(res.data.content))
      .catch(() => enqueueSnackbar('Could not load classes.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!classId) {
      setSections([]);
      setSectionId('');
      return;
    }
    classesApi
      .listSections(classId as number)
      .then((res) => setSections(res.data))
      .catch(() => enqueueSnackbar('Could not load sections for that class.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId]);

  const loadGrid = useCallback(async () => {
    if (!classId || !sectionId || !date) return;
    setLoading(true);
    setLoaded(false);
    try {
      const res = await attendanceApi.getStudentMarkingGrid({
        classId: classId as number,
        sectionId: sectionId as number,
        date: date.format('YYYY-MM-DD'),
      });
      setRows(
        res.data.map((r) => ({
          studentId: r.studentId,
          firstName: r.firstName,
          lastName: r.lastName,
          rollNumber: r.rollNumber,
          status: r.status ?? 'PRESENT',
          remarks: r.remarks ?? '',
          marked: r.status !== null,
        })),
      );
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load students for that class/section.', {
        variant: 'error',
      });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [classId, sectionId, date, enqueueSnackbar]);

  useEffect(() => {
    loadGrid();
  }, [loadGrid]);

  const setRowStatus = (studentId: number, status: AttendanceStatus) => {
    setRows((prev) => prev.map((r) => (r.studentId === studentId ? { ...r, status, marked: true } : r)));
  };

  const setRowRemarks = (studentId: number, remarks: string) => {
    setRows((prev) => prev.map((r) => (r.studentId === studentId ? { ...r, remarks } : r)));
  };

  const markAllPresent = () => {
    setRows((prev) => prev.map((r) => ({ ...r, status: 'PRESENT', marked: true })));
  };

  const unmarkedCount = useMemo(() => rows.filter((r) => !r.marked).length, [rows]);

  const handleSave = async () => {
    if (!classId || !sectionId || rows.length === 0) return;
    setSaving(true);
    try {
      await attendanceApi.markStudents({
        classId: classId as number,
        sectionId: sectionId as number,
        attendanceDate: date.format('YYYY-MM-DD'),
        records: rows.map((r) => ({ studentId: r.studentId, status: r.status, remarks: r.remarks || undefined })),
      });
      enqueueSnackbar(`Attendance saved for ${rows.length} student${rows.length === 1 ? '' : 's'}.`, {
        variant: 'success',
      });
      setRows((prev) => prev.map((r) => ({ ...r, marked: true })));
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save attendance. Please try again.', {
        variant: 'error',
      });
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
              <TextField
                select
                fullWidth
                size="small"
                label="Class"
                value={classId}
                onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">Select a class</MenuItem>
                {classes.map((cls) => (
                  <MenuItem key={cls.id} value={cls.id}>
                    {cls.className}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Section"
                value={sectionId}
                disabled={!classId}
                onChange={(e) => setSectionId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">Select a section</MenuItem>
                {sections.map((sec) => (
                  <MenuItem key={sec.id} value={sec.id}>
                    {sec.sectionName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <DatePicker
                label="Date"
                value={date}
                onChange={(value) => value && setDate(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' } }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <Stack direction="row" spacing={1} justifyContent={{ xs: 'flex-start', md: 'flex-end' }}>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<DoneAllOutlinedIcon />}
                  disabled={rows.length === 0}
                  onClick={markAllPresent}
                >
                  Mark all present
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {!classId || !sectionId ? (
        <EmptyState
          title="Select a class and section"
          description="Choose a class, section and date above to load the attendance grid."
        />
      ) : loading ? (
        <PageLoader label="Loading students..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No students found" description="This class/section has no enrolled students." />
      ) : (
        <Card>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2, py: 1.5 }}>
            <Stack direction="row" spacing={1} alignItems="center">
              {unmarkedCount > 0 && (
                <Chip size="small" color="warning" variant="outlined" label={`${unmarkedCount} unmarked`} />
              )}
              <Chip size="small" variant="outlined" label={`${rows.length} student${rows.length === 1 ? '' : 's'}`} />
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
          <TableContainer sx={{ maxHeight: 600 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ minWidth: 60 }}>Roll No.</TableCell>
                  <TableCell sx={{ minWidth: 180 }}>Student</TableCell>
                  <TableCell sx={{ minWidth: 380 }}>Status</TableCell>
                  <TableCell sx={{ minWidth: 200 }}>Remarks</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.studentId} hover>
                    <TableCell>{row.rollNumber}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.75} alignItems="center">
                        <span>{getStudentDisplayName(row)}</span>
                        {!row.marked && (
                          <Tooltip title="Not yet marked for this date — defaults to Present unless changed">
                            <Chip size="small" label="Unmarked" color="default" variant="outlined" sx={{ height: 20 }} />
                          </Tooltip>
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell>
                      <ToggleButtonGroup
                        size="small"
                        exclusive
                        value={row.status}
                        onChange={(_e, value: AttendanceStatus | null) => value && setRowStatus(row.studentId, value)}
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
                      <TextField
                        size="small"
                        fullWidth
                        placeholder="Optional remarks"
                        value={row.remarks}
                        onChange={(e) => setRowRemarks(row.studentId, e.target.value)}
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

export default MarkAttendancePage;
