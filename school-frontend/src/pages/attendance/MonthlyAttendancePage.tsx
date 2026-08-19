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
import studentsApi from '@/api/studentsApi';
import { getAttendanceStatusColor } from '@/theme/chartColors';
import { useAppSelector } from '@/store/hooks';
import { getStudentDisplayName } from '@/utils/format';
import type { AttendanceStatus, MonthlyAttendanceRow, SchoolClass, Student } from '@/types';

const STATUS_LEGEND: Array<{ status: AttendanceStatus | 'UNMARKED'; label: string }> = [
  { status: 'PRESENT', label: 'Present' },
  { status: 'ABSENT', label: 'Absent' },
  { status: 'LATE', label: 'Late' },
  { status: 'HALF_DAY', label: 'Half Day' },
  { status: 'LEAVE', label: 'Leave' },
  { status: 'UNMARKED', label: 'Not marked' },
];

const MONTH_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ value: i + 1, label: dayjs().month(i).format('MMMM') }));

/** Class + month/year -> a calendar-grid attendance register (students x days 1-31). */
export function MonthlyAttendancePage() {
  const { enqueueSnackbar } = useSnackbar();
  const theme = useTheme();
  const user = useAppSelector((state) => state.auth.user);
  // A student and a parent both get a self-view, but they reach it differently:
  // login stamps classId/sectionId onto a student's own account, while a parent
  // has no class of their own and has to pick one of their children first.
  const isStudentView = user?.role === 'STUDENT';
  const isParentView = user?.role === 'PARENT';
  const isSelfView = isStudentView || isParentView;

  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [sectionId, setSectionId] = useState<number | ''>('');
  const [year, setYear] = useState(dayjs().year());
  const [month, setMonth] = useState(dayjs().month() + 1);

  const [children, setChildren] = useState<Student[]>([]);
  const [childId, setChildId] = useState<number | ''>('');
  const [childrenLoaded, setChildrenLoaded] = useState(false);

  const [rows, setRows] = useState<MonthlyAttendanceRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const selectedChild = useMemo(
    () => children.find((child) => child.id === childId) ?? null,
    [children, childId],
  );

  // The register is always addressed by class/section; only where those come from
  // varies by role, so resolve them once here rather than in every consumer.
  const effectiveClassId: number | '' = isStudentView
    ? user?.classId ?? ''
    : isParentView
      ? selectedChild?.classId ?? ''
      : classId;
  const effectiveSectionId: number | '' = isStudentView
    ? user?.sectionId ?? ''
    : isParentView
      ? selectedChild?.sectionId ?? ''
      : sectionId;

  // Whose row to keep. The API already narrows a self-view caller to their own
  // students, so this is presentation rather than access control: it picks the one
  // child a parent asked about out of however many the API returned.
  const focusStudentId: number | undefined = isStudentView
    ? user?.studentId ?? undefined
    : isParentView
      ? (childId as number) || undefined
      : undefined;

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

  // /students is row-scoped to the caller's own children for a PARENT, so an
  // unfiltered list is exactly the set they are allowed to choose between.
  useEffect(() => {
    if (!isParentView) return;
    studentsApi
      .list({ size: 100, sort: 'id,asc' })
      .then((res) => {
        setChildren(res.data.content);
        setChildId((current) => current || res.data.content[0]?.id || '');
      })
      .catch(() => enqueueSnackbar('Could not load your children.', { variant: 'error' }))
      .finally(() => setChildrenLoaded(true));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isParentView]);

  // The register is fetched by section id, so the class's one section is looked
  // up and selected rather than offered as a choice with a single answer.
  useEffect(() => {
    if (isSelfView || !classId) {
      if (!isSelfView) setSectionId('');
      return;
    }
    classesApi
      .listSections(classId as number)
      .then((res) => setSectionId(res.data[0]?.id ?? ''))
      .catch(() => enqueueSnackbar('Could not load the section for that class.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [classId, isSelfView]);

  const loadMonthly = useCallback(async () => {
    if (!effectiveClassId || !effectiveSectionId) return;
    setLoading(true);
    setLoaded(false);
    try {
      const res = await attendanceApi.getMonthly({
        classId: effectiveClassId as number,
        sectionId: effectiveSectionId as number,
        year,
        month,
      });
      setRows(focusStudentId ? res.data.filter((r) => r.studentId === focusStudentId) : res.data);
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the monthly register.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [effectiveClassId, effectiveSectionId, year, month, focusStudentId, enqueueSnackbar]);

  useEffect(() => {
    loadMonthly();
  }, [loadMonthly]);

  const dayNumbers = Array.from({ length: daysInMonth }, (_, i) => i + 1);
  const parentHasNoChildren = isParentView && childrenLoaded && children.length === 0;

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            {isParentView && children.length > 0 && (
              <Grid item xs={12} sm={4} md={3}>
                <TextField
                  select
                  fullWidth
                  size="small"
                  label="Child"
                  value={childId}
                  onChange={(e) => setChildId(e.target.value === '' ? '' : Number(e.target.value))}
                >
                  {children.map((child) => (
                    <MenuItem key={child.id} value={child.id}>
                      {getStudentDisplayName(child)}
                      {child.className ? ` — ${child.className}${child.sectionName ? ` ${child.sectionName}` : ''}` : ''}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            )}
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

      {isParentView && !childrenLoaded ? (
        <PageLoader label="Loading your children..." />
      ) : !effectiveClassId || !effectiveSectionId ? (
        <EmptyState
          title={parentHasNoChildren ? 'No children linked' : isSelfView ? 'No class assigned' : 'Select a class and section'}
          description={
            parentHasNoChildren
              ? 'No student records are linked to your account, so there is no register to show.'
              : isSelfView
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
