import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import Tooltip from '@mui/material/Tooltip';
import Alert from '@mui/material/Alert';
import IconButton from '@mui/material/IconButton';
import CircularProgress from '@mui/material/CircularProgress';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { useSnackbar } from 'notistack';
import EmptyState from '@/components/common/EmptyState';
import timetableApi, { type TimetableSlotPayload } from '@/api/timetableApi';
import classesApi from '@/api/classesApi';
import type { ClassSubjectTeacher, Section, Subject, Teacher, TimetableDay, TimetableSlot } from '@/types';
import { DEFAULT_PERIODS, WORKING_DAYS } from './timetableDays';

/** Period 1 — the register period, which belongs to the section's class teacher. */
const FIRST_PERIOD = 1;

export interface ClassTimetableTabProps {
  classId: number;
  sections: Section[];
  subjects: Subject[];
  teachers: Teacher[];
}

// Shared with TeacherMappingTab so the two editors cannot disagree about what a
// week is or when the bells are.
const DAYS = WORKING_DAYS;

type EditableSlot = TimetableSlotPayload & { clashWarning?: string | null };

function toEditable(slot: TimetableSlot): EditableSlot {
  return {
    dayOfWeek: slot.dayOfWeek,
    periodNumber: slot.periodNumber,
    startTime: slot.startTime,
    endTime: slot.endTime,
    subjectId: slot.subjectId ?? null,
    teacherId: slot.teacherId ?? null,
    roomNumber: slot.roomNumber ?? null,
    label: slot.label ?? null,
    clashWarning: slot.clashWarning,
  };
}

/**
 * Weekly grid for one section: rows are periods, columns are days.
 *
 * <p>Edited and saved a whole week at a time, matching the API — a grid edit
 * usually moves several periods at once, and saving them one by one would trip
 * the (section, day, period) unique key mid-swap.
 */
export function ClassTimetableTab({ classId, sections, subjects, teachers }: ClassTimetableTabProps) {
  const { enqueueSnackbar } = useSnackbar();

  const [sectionId, setSectionId] = useState<number | ''>(sections[0]?.id ?? '');
  const [slots, setSlots] = useState<EditableSlot[]>([]);
  // Subject -> teacher for this class, used only to pre-fill a period's teacher
  // when its subject is picked.
  const [mappings, setMappings] = useState<ClassSubjectTeacher[]>([]);
  const [periodCount, setPeriodCount] = useState(DEFAULT_PERIODS.length);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    if (sectionId === '' && sections.length > 0) setSectionId(sections[0].id);
  }, [sections, sectionId]);

  const load = useCallback(async () => {
    if (!sectionId) return;
    setLoading(true);
    try {
      const res = await timetableApi.getForSection(classId, sectionId as number);
      const editable = res.data.map(toEditable);
      setSlots(editable);
      // Grow the grid to fit a week that already uses more periods than the default.
      const maxPeriod = editable.reduce((max, s) => Math.max(max, s.periodNumber), 0);
      setPeriodCount(Math.max(DEFAULT_PERIODS.length, maxPeriod));
      setDirty(false);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the timetable.', { variant: 'error' });
      setSlots([]);
    } finally {
      setLoading(false);
    }
  }, [classId, sectionId, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!sectionId) return;
    classesApi
      .listTeacherMappings({ sectionId: sectionId as number })
      .then((res) => setMappings(res.data))
      // Non-fatal: without the mappings the teacher simply is not pre-filled.
      .catch(() => setMappings([]));
  }, [sectionId]);

  const slotAt = useCallback(
    (day: TimetableDay, period: number) =>
      slots.find((s) => s.dayOfWeek === day && s.periodNumber === period) ?? null,
    [slots],
  );

  /*
   * Period 1 belongs to the class teacher.
   *
   * The register is taken in the first period and only the class teacher takes it,
   * so both halves of a P1 slot are constrained: the teacher is them, and the
   * subject has to be one they are actually mapped to teach. The server enforces
   * both; these helpers are what stop this grid offering a choice it will reject.
   */
  const classTeacherId = useMemo(
    () => sections.find((s) => s.id === sectionId)?.classTeacherId ?? null,
    [sections, sectionId],
  );

  const classTeacherSubjectIds = useMemo(
    () =>
      new Set(
        mappings
          .filter((m) => classTeacherId != null && m.teacherId === classTeacherId)
          .map((m) => m.subjectId),
      ),
    [mappings, classTeacherId],
  );

  const isFirstPeriod = (period: number) => period === FIRST_PERIOD;

  const subjectsSelectableAt = (period: number) =>
    isFirstPeriod(period) ? subjects.filter((s) => classTeacherSubjectIds.has(s.id)) : subjects;

  const teachersSelectableAt = (period: number) =>
    isFirstPeriod(period) ? teachers.filter((t) => t.id === classTeacherId) : teachers;

  const updateSlot = (day: TimetableDay, period: number, patch: Partial<EditableSlot>) => {
    setDirty(true);
    setSlots((prev) => {
      const existing = prev.find((s) => s.dayOfWeek === day && s.periodNumber === period);
      if (existing) {
        return prev.map((s) =>
          s.dayOfWeek === day && s.periodNumber === period ? { ...s, ...patch } : s,
        );
      }
      const defaults = DEFAULT_PERIODS[period - 1] ?? DEFAULT_PERIODS[DEFAULT_PERIODS.length - 1];
      return [
        ...prev,
        {
          dayOfWeek: day,
          periodNumber: period,
          startTime: defaults.startTime,
          endTime: defaults.endTime,
          subjectId: null,
          teacherId: null,
          roomNumber: null,
          label: null,
          ...patch,
        },
      ];
    });
  };

  const clearSlot = (day: TimetableDay, period: number) => {
    setDirty(true);
    setSlots((prev) => prev.filter((s) => !(s.dayOfWeek === day && s.periodNumber === period)));
  };

  const handleSave = async () => {
    if (!sectionId) return;
    setSaving(true);
    try {
      // Strip the server-supplied warning before sending it back.
      const payload: TimetableSlotPayload[] = slots.map(({ clashWarning: _ignored, ...slot }) => slot);
      const res = await timetableApi.saveForSection(classId, sectionId as number, payload);
      setSlots(res.data.map(toEditable));
      setDirty(false);
      const clashes = res.data.filter((s) => s.clashWarning).length;
      enqueueSnackbar(
        clashes > 0
          ? `Timetable saved with ${clashes} clash${clashes === 1 ? '' : 'es'} to review.`
          : 'Timetable saved.',
        { variant: clashes > 0 ? 'warning' : 'success' },
      );
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save the timetable.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const clashCount = useMemo(() => slots.filter((s) => s.clashWarning).length, [slots]);

  if (sections.length === 0) {
    return (
      <EmptyState
        title="This class has no section record"
        description="A timetable is stored against the class's section, and this class is missing it."
      />
    );
  }

  return (
    <Box>
      {/* No section picker: the class has one section, selected on mount. */}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }} sx={{ mb: 2 }}>
        <Button size="small" variant="outlined" startIcon={<AddOutlinedIcon />} onClick={() => setPeriodCount((c) => c + 1)}>
          Add period
        </Button>
        <Box sx={{ flex: 1 }} />
        <Button
          variant="contained"
          disabled={saving || !dirty}
          startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveOutlinedIcon />}
          onClick={handleSave}
        >
          Save Timetable
        </Button>
      </Stack>

      {clashCount > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {clashCount} period{clashCount === 1 ? '' : 's'} double-book a teacher or a room. Clashes are flagged
          rather than blocked, so a part-built timetable can still be saved.
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress size={28} />
        </Box>
      ) : (
        <Card variant="outlined">
          <TableContainer sx={{ maxHeight: 620 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ minWidth: 70, position: 'sticky', left: 0, zIndex: 3, bgcolor: 'background.paper' }}>
                    Period
                  </TableCell>
                  {DAYS.map((day) => (
                    <TableCell key={day} sx={{ minWidth: 210 }}>
                      {day.charAt(0) + day.slice(1).toLowerCase()}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {Array.from({ length: periodCount }, (_, i) => i + 1).map((period) => (
                  <TableRow key={period} hover>
                    <TableCell sx={{ position: 'sticky', left: 0, zIndex: 2, bgcolor: 'background.paper' }}>
                      <Typography variant="body2" fontWeight={700}>
                        {period}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {(DEFAULT_PERIODS[period - 1] ?? DEFAULT_PERIODS[DEFAULT_PERIODS.length - 1]).startTime.slice(0, 5)}
                      </Typography>
                    </TableCell>
                    {DAYS.map((day) => {
                      const slot = slotAt(day, period);
                      return (
                        <TableCell key={day} sx={{ verticalAlign: 'top', py: 0.75 }}>
                          <Stack spacing={0.75}>
                            <Stack direction="row" spacing={0.5} alignItems="center">
                              <TextField
                                select
                                size="small"
                                fullWidth
                                value={slot?.subjectId ?? ''}
                                onChange={(e) => {
                                  const subjectId = e.target.value === '' ? null : Number(e.target.value);
                                  // Default the teacher to whoever is mapped to teach
                                  // this subject, so the common case is one click
                                  // instead of two. Still editable below: a cover
                                  // lesson is a legitimate exception.
                                  const mappedTeacherId =
                                    subjectId === null
                                      ? null
                                      : (mappings.find((m) => m.subjectId === subjectId)?.teacherId ?? null);
                                  updateSlot(day, period, {
                                    subjectId,
                                    // Period 1 is the class teacher's, so it is set to
                                    // them rather than defaulted-then-editable. The
                                    // server refuses anyone else there, and offering a
                                    // choice it will reject is how this screen let a
                                    // P1 be pointed at the wrong teacher.
                                    teacherId: isFirstPeriod(period)
                                      ? classTeacherId
                                      : (slot?.teacherId ?? mappedTeacherId),
                                  });
                                }}
                                SelectProps={{ displayEmpty: true }}
                                inputProps={{ 'aria-label': `Subject for ${day} period ${period}` }}
                              >
                                <MenuItem value="">
                                  <em>Free</em>
                                </MenuItem>
                                {/*
                                  At period 1 only the class teacher's own subjects are
                                  offered: the register is taken then, they take it, and
                                  the server rejects a P1 subject they are not assigned
                                  to teach.
                                */}
                                {subjectsSelectableAt(period).map((subject) => (
                                  <MenuItem key={subject.id} value={subject.id}>
                                    {subject.subjectName}
                                  </MenuItem>
                                ))}
                              </TextField>
                              {slot?.clashWarning && (
                                <Tooltip title={slot.clashWarning}>
                                  <WarningAmberOutlinedIcon color="warning" fontSize="small" />
                                </Tooltip>
                              )}
                              {slot && (
                                <IconButton size="small" onClick={() => clearSlot(day, period)} aria-label="Clear period">
                                  <DeleteOutlineOutlinedIcon fontSize="small" />
                                </IconButton>
                              )}
                            </Stack>
                            {slot && (
                              <TextField
                                select
                                size="small"
                                fullWidth
                                value={slot.teacherId ?? ''}
                                // Locked at period 1: it belongs to the class teacher
                                // and is set from them above. Left editable elsewhere,
                                // where a cover lesson is legitimate.
                                disabled={isFirstPeriod(period)}
                                onChange={(e) =>
                                  updateSlot(day, period, {
                                    teacherId: e.target.value === '' ? null : Number(e.target.value),
                                  })
                                }
                                SelectProps={{ displayEmpty: true }}
                                inputProps={{ 'aria-label': `Teacher for ${day} period ${period}` }}
                              >
                                <MenuItem value="">
                                  <em>No teacher</em>
                                </MenuItem>
                                {teachersSelectableAt(period).map((teacher) => (
                                  <MenuItem key={teacher.id} value={teacher.id}>
                                    {[teacher.firstName, teacher.lastName].filter(Boolean).join(' ')}
                                  </MenuItem>
                                ))}
                              </TextField>
                            )}
                          </Stack>
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

export default ClassTimetableTab;
