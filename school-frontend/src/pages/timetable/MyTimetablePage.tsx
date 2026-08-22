import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Alert from '@mui/material/Alert';
import Divider from '@mui/material/Divider';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import timetableApi from '@/api/timetableApi';
import classesApi from '@/api/classesApi';
import parentApi from '@/api/parentApi';
import { useAppSelector } from '@/store/hooks';
import type { ClassSubjectTeacher, ParentChild, TimetableDay, TimetableSlot } from '@/types';

const DAYS: TimetableDay[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

const TEACHER_ROLES = ['TEACHER'];

function titleCase(day: string) {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

/** "09:00:00" -> "09:00". Times arrive as the API's HH:mm:ss throughout. */
function hhmm(time?: string | null) {
  return time ? time.slice(0, 5) : '';
}

/** dayjs()'s 0=Sunday indexing mapped onto the API's day names. */
function todayName(): TimetableDay {
  const names: TimetableDay[] = [
    'SUNDAY',
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
  ];
  return names[dayjs().day()];
}

/**
 * The signed-in user's own timetable — one screen serving three roles.
 *
 * <p>A teacher sees the periods they teach and which class each one is for; a
 * student sees their class's week and who teaches each subject. Both come from
 * /timetable/me, so the server decides what the caller is entitled to and there
 * is no id in the URL to tamper with.
 *
 * <p>A parent of one child is answered by /me as well. A parent of several
 * cannot be — picking a child would be a guess — so the child list is fetched
 * and the chosen one asked for by id. That branch is driven by the server
 * rejecting /me rather than by inspecting the role, which keeps the two in step.
 */
export function MyTimetablePage() {
  const user = useAppSelector((state) => state.auth.user);
  const isTeacher = !!user?.role && TEACHER_ROLES.includes(user.role);

  const [slots, setSlots] = useState<TimetableSlot[]>([]);
  const [mappings, setMappings] = useState<ClassSubjectTeacher[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [view, setView] = useState<'week' | 'today'>('week');

  // Only populated when /me refuses to guess between several children.
  const [children, setChildren] = useState<ParentChild[]>([]);
  const [childId, setChildId] = useState<number | ''>('');

  const loadFor = useCallback(async (studentId?: number) => {
    setLoading(true);
    setError(null);
    try {
      const [slotRes, mappingRes] = await Promise.all([
        studentId ? timetableApi.getForStudent(studentId) : timetableApi.getMine(),
        studentId
          ? classesApi.listTeacherMappingsForStudent(studentId)
          : classesApi.listMyTeacherMappings(),
      ]);
      setSlots(slotRes.data ?? []);
      setMappings(mappingRes.data ?? []);
      return true;
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Could not load your timetable.');
      setSlots([]);
      setMappings([]);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const ok = await loadFor();
      if (cancelled || ok) return;
      // /me could not answer. A parent of several children is the one case that
      // recovers: fetch them and let the parent choose.
      try {
        const res = await parentApi.getMyChildren();
        if (cancelled || res.data.length === 0) return;
        setChildren(res.data);
        setChildId(res.data[0].studentId);
        await loadFor(res.data[0].studentId);
      } catch {
        // Not a parent, or no children on file — the original message stands.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [loadFor]);

  const handleChildChange = async (nextId: number) => {
    setChildId(nextId);
    await loadFor(nextId);
  };

  const today = todayName();
  const visibleDays = view === 'today' ? [today] : DAYS;

  // Periods present in the data rather than a fixed 1..8: a read-only grid should
  // not invent rows for periods this timetable does not use.
  const periods = useMemo(() => {
    const set = new Set<number>();
    slots.forEach((s) => set.add(s.periodNumber));
    return [...set].sort((a, b) => a - b);
  }, [slots]);

  const slotAt = useCallback(
    (day: TimetableDay, period: number) =>
      slots.find((s) => s.dayOfWeek === day && s.periodNumber === period) ?? null,
    [slots],
  );

  const todayCount = useMemo(
    () => slots.filter((s) => s.dayOfWeek === today && s.subjectId).length,
    [slots, today],
  );

  const subjectCount = useMemo(
    () => new Set(mappings.map((m) => m.subjectId)).size,
    [mappings],
  );

  if (loading) return <PageLoader label="Loading your timetable..." />;

  return (
    <Box>
      <PageHeader
        title="My Timetable"
        subtitle={
          isTeacher
            ? 'The periods you teach, and the subjects you are assigned to'
            : 'Your class week, and who teaches each subject'
        }
      />

      {error && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {children.length > 1 && (
        <TextField
          select
          size="small"
          label="Child"
          value={childId}
          onChange={(e) => handleChildChange(Number(e.target.value))}
          sx={{ minWidth: 240, mb: 2 }}
        >
          {children.map((child) => (
            <MenuItem key={child.studentId} value={child.studentId}>
              {[child.firstName, child.lastName].filter(Boolean).join(' ')}
              {child.className ? ` — ${child.className}` : ''}
            </MenuItem>
          ))}
        </TextField>
      )}

      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
        <Chip
          icon={<CalendarMonthOutlinedIcon />}
          label={`${todayCount} period${todayCount === 1 ? '' : 's'} today (${titleCase(today)})`}
          color={todayCount > 0 ? 'primary' : 'default'}
          variant={todayCount > 0 ? 'filled' : 'outlined'}
        />
        <Chip
          icon={<MenuBookOutlinedIcon />}
          label={`${subjectCount} subject${subjectCount === 1 ? '' : 's'}`}
          variant="outlined"
        />
        <Box sx={{ flex: 1 }} />
        <ToggleButtonGroup
          size="small"
          exclusive
          value={view}
          onChange={(_e, next) => next && setView(next)}
        >
          <ToggleButton value="week">Whole week</ToggleButton>
          <ToggleButton value="today">Today</ToggleButton>
        </ToggleButtonGroup>
      </Stack>

      {periods.length === 0 ? (
        <EmptyState
          title="No timetable yet"
          description={
            isTeacher
              ? 'You have no periods scheduled. Once the office timetables your subjects they will appear here.'
              : 'Your class timetable has not been published yet.'
          }
        />
      ) : (
        <Card variant="outlined" sx={{ mb: 3 }}>
          <TableContainer sx={{ maxHeight: 620 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell
                    sx={{ minWidth: 92, position: 'sticky', left: 0, zIndex: 3, bgcolor: 'background.paper' }}
                  >
                    Period
                  </TableCell>
                  {visibleDays.map((day) => (
                    <TableCell key={day} sx={{ minWidth: 170 }}>
                      {titleCase(day)}
                      {day === today && (
                        <Chip label="Today" size="small" color="primary" sx={{ ml: 0.75, height: 18 }} />
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {periods.map((period) => (
                  <TableRow key={period} hover>
                    <TableCell
                      sx={{ position: 'sticky', left: 0, zIndex: 2, bgcolor: 'background.paper' }}
                    >
                      <Typography variant="body2" fontWeight={700}>
                        {period}
                      </Typography>
                    </TableCell>
                    {visibleDays.map((day) => {
                      const slot = slotAt(day, period);
                      if (!slot) {
                        return (
                          <TableCell key={day} sx={{ color: 'text.disabled' }}>
                            —
                          </TableCell>
                        );
                      }
                      return (
                        <TableCell key={day} sx={{ verticalAlign: 'top' }}>
                          <Typography variant="body2" fontWeight={600}>
                            {slot.subjectName ?? slot.label ?? 'Free'}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" display="block">
                            {hhmm(slot.startTime)}–{hhmm(slot.endTime)}
                          </Typography>
                          {/* A teacher needs to know which class to walk into; a
                              student already knows, and needs the teacher's name. */}
                          {isTeacher
                            ? slot.className && (
                                <Typography variant="caption" color="text.secondary" display="block">
                                  {slot.className}
                                </Typography>
                              )
                            : slot.teacherName && (
                                <Typography variant="caption" color="text.secondary" display="block">
                                  {slot.teacherName}
                                </Typography>
                              )}
                          {slot.roomNumber && (
                            <Typography variant="caption" color="text.secondary" display="block">
                              Room {slot.roomNumber}
                            </Typography>
                          )}
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

      <Card variant="outlined">
        <CardHeader
          title={isTeacher ? 'My Subjects & Classes' : 'My Subjects & Teachers'}
          subheader={
            isTeacher
              ? 'Every subject you are assigned to teach'
              : 'Who teaches each subject in your class'
          }
        />
        <Divider />
        <CardContent sx={{ pt: 0 }}>
          {mappings.length === 0 ? (
            <EmptyState
              title="Nothing assigned yet"
              description={
                isTeacher
                  ? 'You have not been assigned to a subject yet.'
                  : 'Subject teachers have not been assigned for your class yet.'
              }
            />
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Subject</TableCell>
                  <TableCell>Class</TableCell>
                  <TableCell>{isTeacher ? 'Periods / week' : 'Teacher'}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {mappings.map((m) => {
                  const periodsForSubject = slots.filter((s) => s.subjectId === m.subjectId).length;
                  return (
                    <TableRow key={m.id} hover>
                      <TableCell>{m.subjectName ?? `Subject #${m.subjectId}`}</TableCell>
                      <TableCell>{m.className ?? `Class #${m.classId}`}</TableCell>
                      <TableCell>
                        {isTeacher ? (
                          periodsForSubject > 0 ? (
                            `${periodsForSubject} period${periodsForSubject === 1 ? '' : 's'}`
                          ) : (
                            <Typography variant="caption" color="warning.main">
                              Not timetabled yet
                            </Typography>
                          )
                        ) : (
                          (m.teacherName ?? 'Unassigned')
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default MyTimetablePage;
