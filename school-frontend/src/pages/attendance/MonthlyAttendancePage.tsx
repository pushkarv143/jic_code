import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import Tooltip from '@mui/material/Tooltip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import PageLoader from '@/components/common/PageLoader';
import classesApi from '@/api/classesApi';
import attendanceApi from '@/api/attendanceApi';
import { getAttendanceStatusColor } from '@/theme/chartColors';
import { useAppSelector } from '@/store/hooks';
import { getStudentDisplayName } from '@/utils/format';
import type { AttendanceStatus, MonthlyAttendanceRow, SchoolClass, Section } from '@/types';

const STATUS_LEGEND: Array<{ status: AttendanceStatus | 'UNMARKED'; label: string }> = [
  { status: 'PRESENT', label: 'Present' },
  { status: 'ABSENT', label: 'Absent' },
  { status: 'LATE', label: 'Late' },
  { status: 'HALF_DAY', label: 'Half Day' },
  { status: 'LEAVE', label: 'Leave' },
  { status: 'UNMARKED', label: 'Not marked' },
];

const MONTH_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ value: i + 1, label: dayjs().month(i).format('MMMM') }));

/** Class + section + month/year -> a calendar-grid attendance register (students x days 1-31). */
export function MonthlyAttendancePage() {
  const { enqueueSnackbar } = useSnackbar();
  const theme = useTheme();
  const user = useAppSelector((state) => state.auth.user);
  const isSelfView = user?.role === 'STUDENT' || user?.role === 'PARENT';

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [sections, setSections] = useState<Section[]>([]);
  const [classId, setClassId] = useState<number | ''>(isSelfView ? user?.classId ?? '' : '');
  const [sectionId, setSectionId] = useState<number | ''>(isSelfView ? user?.sectionId ?? '' : '');
  const [year, setYear] = useState(dayjs().year());
  const [month, setMonth] = useState(dayjs().month() + 1);

  const [rows, setRows] = useState<MonthlyAttendanceRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const yearOptions = useMemo(() => {
    const current = dayjs().year();
    return [current - 2, current - 1, current, current + 1];
  }, []);

  const daysInMonth = useMemo(() => dayjs(`${year}-${month}-01`).daysInMonth(), [year, month]);

  useEffect(() => {
    if (isSelfView) return;
    classesApi
      .list({ size: 200, sort: 'className,asc' })
      .then((res) => setClasses(res.data.content))
      .catch(() => enqueueSnackbar('Could not load classes.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isSelfView]);

  useEffect(() => {
    if (isSelfView || !classId) {
      if (!isSelfView) {
        setSections([]);
        setSectionId('');
      }
      return;
    }
    classesApi
      .listSections(classId as number)
      .then((res) => setSections(res.data))
      .catch(() => enqueueSnackbar('Could not load sections.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId, isSelfView]);

  const loadMonthly = useCallback(async () => {
    if (!classId || !sectionId) return;
    setLoading(true);
    setLoaded(false);
    try {
      const res = await attendanceApi.getMonthly({
        classId: classId as number,
        sectionId: sectionId as number,
        year,
        month,
      });
      setRows(isSelfView && user?.studentId ? res.data.filter((r) => r.studentId === user.studentId) : res.data);
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the monthly register.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [classId, sectionId, year, month, isSelfView, user?.studentId, enqueueSnackbar]);

  useEffect(() => {
    loadMonthly();
  }, [loadMonthly]);

  const dayNumbers = Array.from({ length: daysInMonth }, (_, i) => i + 1);

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            {!isSelfView && (
              <>
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
              </>
            )}
            <Grid item xs={6} sm={4} md={isSelfView ? 3 : 2}>
              <TextField
                select
                fullWidth
                size="small"
                label="Month"
                value={month}
                onChange={(e) => setMonth(Number(e.target.value))}
              >
                {MONTH_OPTIONS.map((m) => (
                  <MenuItem key={m.value} value={m.value}>
                    {m.label}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={6} sm={4} md={isSelfView ? 3 : 2}>
              <TextField select fullWidth size="small" label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
                {yearOptions.map((y) => (
                  <MenuItem key={y} value={y}>
                    {y}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {!classId || !sectionId ? (
        <EmptyState
          title={isSelfView ? 'No class assigned' : 'Select a class and section'}
          description={
            isSelfView
              ? 'Your class/section could not be determined for this account.'
              : 'Choose a class, section, month and year above to load the register.'
          }
        />
      ) : loading ? (
        <PageLoader label="Loading register..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No data found" description="No attendance has been marked for this period yet." />
      ) : (
        <Card>
          <Box sx={{ px: 2, pt: 2 }}>
            <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
              {STATUS_LEGEND.map((item) => (
                <Stack key={item.status} direction="row" spacing={0.75} alignItems="center">
                  <Box
                    sx={{
                      width: 12,
                      height: 12,
                      borderRadius: 0.5,
                      bgcolor: getAttendanceStatusColor(theme.palette.mode, item.status === 'UNMARKED' ? null : item.status),
                    }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {item.label}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </Box>
          <TableContainer sx={{ maxHeight: 620 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ minWidth: 60, position: 'sticky', left: 0, zIndex: 3, bgcolor: 'background.paper' }}>
                    Roll No.
                  </TableCell>
                  <TableCell sx={{ minWidth: 170, position: 'sticky', left: 60, zIndex: 3, bgcolor: 'background.paper' }}>
                    Student
                  </TableCell>
                  {dayNumbers.map((d) => (
                    <TableCell key={d} align="center" sx={{ minWidth: 34, px: 0.5 }}>
                      {d}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.studentId} hover>
                    <TableCell sx={{ position: 'sticky', left: 0, zIndex: 2, bgcolor: 'background.paper' }}>
                      {row.rollNumber}
                    </TableCell>
                    <TableCell
                      sx={{
                        position: 'sticky',
                        left: 60,
                        zIndex: 2,
                        bgcolor: 'background.paper',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      {getStudentDisplayName(row)}
                    </TableCell>
                    {dayNumbers.map((d) => {
                      const status = row.days?.[String(d)] ?? null;
                      return (
                        <TableCell key={d} align="center" sx={{ p: 0.4 }}>
                          <Tooltip title={status ?? 'Not marked'}>
                            <Box
                              sx={{
                                width: 22,
                                height: 22,
                                mx: 'auto',
                                borderRadius: 0.75,
                                bgcolor: getAttendanceStatusColor(theme.palette.mode, status),
                                opacity: status ? 1 : 0.35,
                              }}
                            />
                          </Tooltip>
                        </TableCell>
                      );
                    })}
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

export default MonthlyAttendancePage;
