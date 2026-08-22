import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import { useSnackbar } from 'notistack';
import EmptyState from '@/components/common/EmptyState';
import classesApi from '@/api/classesApi';
import teachersApi from '@/api/teachersApi';
import timetableApi from '@/api/timetableApi';
import { usePermissions } from '@/hooks/usePermissions';
import { useAccess } from '@/access/AccessProvider';
import type { TimetableSlotPayload } from '@/api/timetableApi';
import type {
  ClassSubjectTeacher,
  Section,
  Subject,
  Teacher,
  TimetableDay,
  TimetableSlot,
} from '@/types';
import { DEFAULT_PERIODS, WORKING_DAYS, dayLabel, periodLabel } from './timetableDays';

/**
 * Period 1 — the register period. It belongs to the section's class teacher, and
 * the server refuses to put anyone else in it.
 */
const FIRST_PERIOD = 1;

export interface TeacherMappingTabProps {
  classId: number;
  sections: Section[];
  subjects: Subject[];
}

/** Server slot -> editable payload. Drops id and the server-computed clashWarning. */
function toPayload(slot: TimetableSlot): TimetableSlotPayload {
  return {
    dayOfWeek: slot.dayOfWeek,
    periodNumber: slot.periodNumber,
    startTime: slot.startTime,
    endTime: slot.endTime,
    subjectId: slot.subjectId ?? null,
    teacherId: slot.teacherId ?? null,
    roomNumber: slot.roomNumber ?? null,
    label: slot.label ?? null,
  };
}

/**
 * One row per subject: who teaches it, and when.
 *
 * <p>Teacher and periods were previously split across this tab and the Timetable
 * grid, so setting up a subject meant two screens and this column could only say
 * "add periods on the Timetable tab". They are one decision, so they are now one
 * row — pick a teacher, pick a period, done.
 *
 * <p>Picking a period fills it in on every working day at once. That is the shape
 * a school timetable almost always has, and it turns six identical picks into one;
 * the day chips then remove the exceptions. Sunday is never offered.
 *
 * <p>The Timetable grid remains, for the whole-week view and for what a
 * per-subject row cannot express: assembly, games, free periods, different times
 * per day, or one subject sitting in two different periods.
 */
export function TeacherMappingTab({ classId, sections, subjects }: TeacherMappingTabProps) {
  const { enqueueSnackbar } = useSnackbar();
  const { isManagement } = usePermissions();

  /*
   * Two independent grants, because they are two different jobs:
   *
   *   SUBJECT_MANAGE   - who teaches a subject (ClassSubjectTeacherController)
   *   TIMETABLE_MANAGE - when it is taught (TimetableController), held by
   *                      SUPER_ADMIN alone by default per
   *                      database/16_timetable_permission.sql
   *
   * Reading both stays open to anyone who can reach this tab; only the writes are
   * gated, and separately, so a school can let the office map teachers while the
   * administrator keeps the bell schedule.
   */
  const { can } = useAccess();
  const canManageMapping = can('SUBJECT_MANAGE');
  const canManageTimetable = can('TIMETABLE_MANAGE');

  const [mappings, setMappings] = useState<ClassSubjectTeacher[]>([]);
  const [loading, setLoading] = useState(false);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  // teacherId -> the distinct subjects they teach anywhere in the school, used to
  // label the dropdown. Built from the mapping list rather than from the teacher
  // records: TeacherDto only carries its assignments on the by-id endpoint, and
  // filling them in on the list would be one query per teacher.
  const [subjectsByTeacher, setSubjectsByTeacher] = useState<Record<number, string[]>>({});
  const [assignTarget, setAssignTarget] = useState<Subject | null>(null);
  const [selectedTeacherId, setSelectedTeacherId] = useState<number | ''>('');
  const [saving, setSaving] = useState(false);

  const section = sections[0] ?? null;

  // Memoised because buildSlot's useCallback depends on it; an inline arrow would
  // change identity every render and rebuild the scheduling callbacks with it.
  const mappingFor = useCallback(
    (subjectId: number) => mappings.find((m) => m.subjectId === subjectId),
    [mappings],
  );

  // ---------------------------------------------------------------------
  // Scheduling
  //
  // The periods live on this tab rather than only on the Timetable grid,
  // because "who teaches it" and "when" are one decision and were split across
  // two screens — the Time slots column used to just say "add periods on the
  // Timetable tab". The grid is still there for the whole-week view and for the
  // slots this tab cannot express (assembly, games, a free period, or two
  // different periods for one subject).
  //
  // Both editors write the same section week through the same endpoint, which
  // replaces it wholesale. That is safe because this tab holds the entire week
  // in `schedule` and only ever adds or removes rows for the one subject being
  // edited — anything it does not manage is written back untouched. It reloads
  // whenever the section changes, so the copy it saves is the copy it read.
  // ---------------------------------------------------------------------
  const [schedule, setSchedule] = useState<TimetableSlotPayload[]>([]);
  const [dirty, setDirty] = useState(false);
  const [savingSchedule, setSavingSchedule] = useState(false);

  const PERIOD_NUMBERS = useMemo(
    () => DEFAULT_PERIODS.map((_p, index) => index + 1),
    [],
  );

  const slotsForSubject = useCallback(
    (subjectId: number) => schedule.filter((s) => s.subjectId === subjectId),
    [schedule],
  );

  /** Whether the class teacher holds any subject here — the precondition for period 1. */
  const classTeacherTeachesAnything = useMemo(
    () =>
      section?.classTeacherId != null
      && mappings.some((m) => m.teacherId === section.classTeacherId),
    [mappings, section],
  );

  /**
   * The period this subject sits in, or null when it is not timetabled.
   *
   * <p>Takes the first slot's period. A subject spread across two different
   * periods is legitimate but not something this column can represent — the
   * Timetable grid is where that is built, and picking a period here would
   * collapse it onto one. `mixedPeriods` below is what warns about that case
   * instead of silently flattening it.
   */
  const periodOf = useCallback(
    (subjectId: number): number | null => slotsForSubject(subjectId)[0]?.periodNumber ?? null,
    [slotsForSubject],
  );

  const hasMixedPeriods = useCallback(
    (subjectId: number) => new Set(slotsForSubject(subjectId).map((s) => s.periodNumber)).size > 1,
    [slotsForSubject],
  );

  /**
   * Why this period cannot be given to this subject, or null when it can.
   *
   * <p>Mirrors the rules the server enforces on save, so they show up as a greyed
   * option with a reason instead of a rejected save:
   *
   * <ul>
   *   <li><b>period 1</b> belongs to the class teacher — the register is taken in it
   *       and only they take it — so it is offered only for a subject that teacher
   *       is assigned to;</li>
   *   <li><b>the subject's teacher is already busy</b> in that period elsewhere in
   *       this class. A teacher cannot take two classes at once. Only conflicts
   *       within this class are visible here; one against another class is caught on
   *       save, since this screen does not hold the rest of the school's week.</li>
   * </ul>
   */
  const periodUnavailableReason = useCallback(
    (periodNumber: number, subjectId: number): string | null => {
      const teacherId = mappingFor(subjectId)?.teacherId ?? null;

      if (periodNumber === FIRST_PERIOD) {
        if (section?.classTeacherId == null) {
          return 'no class teacher assigned yet';
        }
        if (teacherId !== section.classTeacherId) {
          // Named, so it is obvious who period 1 is waiting for rather than just
          // that this subject cannot have it.
          const who = section.classTeacherName ?? 'the class teacher';
          return `period 1 is ${who}’s`;
        }
      }

      if (teacherId != null) {
        const busy = schedule.find(
          (s) => s.periodNumber === periodNumber && s.subjectId !== subjectId && s.teacherId === teacherId,
        );
        if (busy) {
          return 'this teacher is already teaching then';
        }
      }
      return null;
    },
    [mappingFor, schedule, section],
  );

  /**
   * Set when this subject already sits in period 1 but should not.
   *
   * <p>The period picker refuses to *create* such a slot, but rows saved before the
   * rule existed can still be in that state — the seeded Pre-Nursery week has
   * Art &amp; Craft at P1 under a teacher who is not the class teacher. Left alone
   * those rows look editable, the day chips invite a click, and the save is then
   * rejected wholesale. Naming the problem and freezing the row is the honest
   * alternative: the fix is to move the subject off period 1.
   */
  const firstPeriodViolation = useCallback(
    (subjectId: number): string | null => {
      if (periodOf(subjectId) !== FIRST_PERIOD) return null;
      const teacherId = mappingFor(subjectId)?.teacherId ?? null;
      if (section?.classTeacherId != null && teacherId === section.classTeacherId) return null;
      const who = section?.classTeacherName ?? 'the class teacher';
      return `Period 1 belongs to ${who}. Move this subject to another period.`;
    },
    [periodOf, mappingFor, section],
  );

  /**
   * What else already occupies this period, if anything — used to grey out the
   * option rather than let a pick silently displace another subject.
   *
   * <p>`uq_timetable_slot (section_id, day_of_week, period_number)` allows one
   * row per period per day, so two subjects cannot share P3. Rejecting the pick
   * up front is kinder than accepting it and quietly overwriting a colleague's
   * work; to move a subject into an occupied period, free that period first.
   */
  const periodHeldByOther = useCallback(
    (periodNumber: number, subjectId: number): string | null => {
      const occupant = schedule.find(
        (s) => s.periodNumber === periodNumber && s.subjectId !== subjectId,
      );
      if (!occupant) return null;
      if (occupant.subjectId == null) return occupant.label ?? 'another activity';
      return subjects.find((s) => s.id === occupant.subjectId)?.subjectName ?? 'another subject';
    },
    [schedule, subjects],
  );

  const buildSlot = useCallback(
    (subjectId: number, day: TimetableDay, periodNumber: number): TimetableSlotPayload => {
      const bell = DEFAULT_PERIODS[periodNumber - 1];
      return {
        dayOfWeek: day,
        periodNumber,
        startTime: bell?.startTime ?? '09:00:00',
        endTime: bell?.endTime ?? '09:40:00',
        subjectId,
        // The subject's assigned teacher, so scheduling does not need a second
        // pick. Null when the subject has no teacher yet — the slot is still a
        // real period, it just has nobody in front of it.
        teacherId: mappingFor(subjectId)?.teacherId ?? null,
        roomNumber: section?.roomNumber ?? null,
        label: null,
      };
    },
    [mappingFor, section],
  );

  /**
   * Puts this subject at `periodNumber` on every working day, or clears it.
   *
   * <p>The auto-fill is the point: a class that has English at P2 has it at P2
   * all week, and asking for six identical picks was busywork. Sunday is never
   * included — see WORKING_DAYS.
   */
  const applyPeriod = (subjectId: number, periodNumber: number | null) => {
    setSchedule((prev) => {
      const others = prev.filter((s) => s.subjectId !== subjectId);
      if (periodNumber == null) return others;
      return [...others, ...WORKING_DAYS.map((day) => buildSlot(subjectId, day, periodNumber))];
    });
    setDirty(true);
  };

  /** Adds or removes one day, for a subject that does not run the full week. */
  const toggleDay = (subjectId: number, day: TimetableDay) => {
    const periodNumber = periodOf(subjectId);
    if (periodNumber == null) return;
    setSchedule((prev) => {
      const existing = prev.find((s) => s.subjectId === subjectId && s.dayOfWeek === day);
      if (existing) {
        return prev.filter((s) => !(s.subjectId === subjectId && s.dayOfWeek === day));
      }
      return [...prev, buildSlot(subjectId, day, periodNumber)];
    });
    setDirty(true);
  };

  const handleSaveSchedule = async () => {
    if (!section) return;
    setSavingSchedule(true);
    try {
      const res = await timetableApi.saveForSection(classId, section.id, schedule);
      setSchedule(res.data.map(toPayload));
      setDirty(false);
      enqueueSnackbar('Timetable saved.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save the timetable.', {
        variant: 'error',
      });
    } finally {
      setSavingSchedule(false);
    }
  };

  const load = useCallback(async () => {
    if (!section) {
      setMappings([]);
      setSchedule([]);
      setDirty(false);
      return;
    }
    setLoading(true);
    try {
      const [mappingRes, slotRes, allMappingRes] = await Promise.all([
        classesApi.listTeacherMappings({ sectionId: section.id }),
        // Management-only endpoint. A teacher may read this tab but not the class's
        // week, so the period columns are skipped for them rather than failing the
        // whole tab on a 403.
        //
        // Per section, not per class: this tab now edits the week as well as showing
        // it, and saveForSection replaces exactly one section's rows. Loading the
        // class-wide set would mean saving back slots that belong elsewhere.
        isManagement ? timetableApi.getForSection(classId, section.id) : Promise.resolve(null),
        // Unfiltered: "who is the maths teacher" is a school-wide question, not a
        // question about this class. One small request — the table holds a handful
        // of rows per class — rather than one per teacher.
        classesApi.listTeacherMappings({}),
      ]);
      setMappings(mappingRes.data);
      setSchedule(slotRes ? slotRes.data.map(toPayload) : []);
      setDirty(false);

      const index: Record<number, string[]> = {};
      allMappingRes.data.forEach((m) => {
        const name = m.subjectName;
        if (!name) return;
        const existing = index[m.teacherId] ?? [];
        // A teacher taking the same subject in several classes should read once.
        if (!existing.includes(name)) index[m.teacherId] = [...existing, name];
      });
      Object.values(index).forEach((names) => names.sort((a, b) => a.localeCompare(b)));
      setSubjectsByTeacher(index);
    } catch {
      enqueueSnackbar('Could not load teacher mappings.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [classId, section, isManagement, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    teachersApi
      .list({ size: 200, sort: 'employeeId,asc' })
      .then((res) => setTeachers(res.data.content))
      .catch(() => enqueueSnackbar('Could not load teachers.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const teacherName = (teacherId: number) => {
    const t = teachers.find((tc) => tc.id === teacherId);
    return t ? `${t.firstName ?? t.username} ${t.lastName ?? ''}`.trim() : `Teacher #${teacherId}`;
  };

  /**
   * "Aarav Singh (Mathematics)" — the subjects the teacher already takes, so the
   * maths teacher is recognisable without cross-checking another screen.
   *
   * <p>Capped at two names plus a count: one teacher here takes five subjects, and
   * spelling all of them out makes the list unreadable at exactly the moment it is
   * being scanned. "No subjects yet" is stated rather than left blank, because a
   * free teacher is a useful thing to be able to see in this list.
   */
  const teacherSubjectLabel = (teacherId: number) => {
    const subjectNames = subjectsByTeacher[teacherId] ?? [];
    if (subjectNames.length === 0) return 'no subjects yet';
    if (subjectNames.length <= 2) return subjectNames.join(', ');
    return `${subjectNames.slice(0, 2).join(', ')} +${subjectNames.length - 2}`;
  };

  const handleAssign = async () => {
    if (!assignTarget || !selectedTeacherId || !section) return;
    setSaving(true);
    try {
      // Re-assigning means replacing: uq_cst allows one teacher per subject, so an
      // existing mapping is removed before the new one is created rather than
      // letting the API reject the second insert as a duplicate.
      const existing = mappingFor(assignTarget.id);
      if (existing) {
        await classesApi.removeTeacherMapping(existing.id);
      }
      const res = await classesApi.assignTeacherMapping({
        classId,
        sectionId: section.id,
        subjectId: assignTarget.id,
        teacherId: selectedTeacherId,
      });
      setMappings((prev) => [...prev.filter((m) => m.subjectId !== assignTarget.id), res.data]);
      enqueueSnackbar('Teacher assigned.', { variant: 'success' });
      setAssignTarget(null);
      setSelectedTeacherId('');
      // Re-read so the dropdown's "(subjects)" labels reflect the change that was
      // just made — this assignment is one of the rows they are built from.
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not assign this teacher.', { variant: 'error' });
      // The delete may have gone through before the create failed, so re-read
      // rather than leaving the screen showing a mapping that no longer exists.
      load();
    } finally {
      setSaving(false);
    }
  };

  const handleUnassign = async (mapping: ClassSubjectTeacher) => {
    try {
      await classesApi.removeTeacherMapping(mapping.id);
      setMappings((prev) => prev.filter((m) => m.id !== mapping.id));
      enqueueSnackbar('Teacher unassigned.', { variant: 'success' });
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not unassign this teacher.', { variant: 'error' });
    }
  };

  const unassignedCount = useMemo(
    () => subjects.filter((s) => !mappings.some((m) => m.subjectId === s.id)).length,
    [subjects, mappings],
  );

  if (!section) {
    return (
      <EmptyState
        title="Nothing to map yet"
        description="This class has no section record, so subjects cannot be assigned to teachers."
      />
    );
  }
  if (subjects.length === 0) {
    return <EmptyState title="Nothing to map yet" description="Add at least one subject first." />;
  }

  return (
    <Box>
      {unassignedCount > 0 && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {unassignedCount} of {subjects.length} subject{subjects.length === 1 ? '' : 's'} has no teacher yet.
        </Alert>
      )}

      {/*
        Period edits are collected and saved together rather than written on every
        click. The API replaces a section's whole week in one call, so a PUT per
        chip would be a stream of full-week writes — and moving a subject from P2
        to P3 would briefly double-book P3 against the (day, period) unique key.
      */}
      {isManagement && dirty && (
        <Alert
          severity="warning"
          sx={{ mb: 2 }}
          action={
            <Button
              color="inherit"
              size="small"
              variant="outlined"
              onClick={handleSaveSchedule}
              disabled={savingSchedule}
            >
              {savingSchedule ? 'Saving…' : 'Save timetable'}
            </Button>
          }
        >
          Unsaved timetable changes.
        </Alert>
      )}

      {isManagement && !canManageTimetable && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Periods are read-only for your role — assembling the timetable is the
          administrator&apos;s job.
        </Alert>
      )}

      {/*
        Period 1 is the class teacher's and must hold a subject they teach, so if
        they are mapped to none of this class's subjects it cannot be filled at all.
        Stated up front: otherwise every row's P1 option is greyed out with a reason
        that explains the rule but not the way out of it.
      */}
      {isManagement && canManageTimetable && section?.classTeacherId == null && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          This class has no class teacher, so period 1 cannot be timetabled. Assign one
          on the Class Setup tab first.
        </Alert>
      )}
      {isManagement && canManageTimetable && section?.classTeacherId != null && !classTeacherTeachesAnything && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {section.classTeacherName ?? 'The class teacher'} is not assigned to teach any
          subject in this class, so period 1 cannot be filled. Give them one of the
          subjects above and period 1 becomes available.
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined" sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Subject</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Teacher</TableCell>
              {/* Only management can read the class's week, so these columns are
                  left out for anyone else rather than shown uniformly empty. */}
              {isManagement && <TableCell sx={{ fontWeight: 700 }}>Period</TableCell>}
              {isManagement && <TableCell sx={{ fontWeight: 700 }}>Days</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {subjects.map((subj) => {
              const mapping = mappingFor(subj.id);
              const subjectSlots = slotsForSubject(subj.id);
              return (
                <TableRow key={subj.id} hover>
                  <TableCell sx={{ fontWeight: 600 }}>
                    {subj.subjectName}
                    <Typography variant="caption" color="text.secondary" display="block">
                      {subj.subjectCode}
                    </Typography>
                  </TableCell>
                  {/*
                    Who teaches what is readable by any teacher who reaches this
                    tab. Changing it writes class_subject_teacher and needs
                    SUBJECT_MANAGE, so without the grant the chip keeps showing the
                    name but loses its click and delete handlers — the assigned
                    teacher stays visible, the controls do not pretend to work.
                  */}
                  <TableCell>
                    {mapping ? (
                      <Chip
                        label={mapping.teacherName ?? teacherName(mapping.teacherId)}
                        size="small"
                        onClick={
                          canManageMapping
                            ? () => {
                                setAssignTarget(subj);
                                setSelectedTeacherId(mapping.teacherId);
                              }
                            : undefined
                        }
                        onDelete={canManageMapping ? () => handleUnassign(mapping) : undefined}
                        deleteIcon={<CloseOutlinedIcon />}
                        color="primary"
                        variant="outlined"
                      />
                    ) : canManageMapping ? (
                      <Chip
                        label="Assign teacher"
                        size="small"
                        variant="outlined"
                        onClick={() => {
                          setAssignTarget(subj);
                          setSelectedTeacherId('');
                        }}
                      />
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        Not assigned
                      </Typography>
                    )}
                  </TableCell>
                  {/*
                    Period picker. Choosing one schedules this subject at that
                    period on every working day at once — Monday to Saturday, never
                    Sunday — which is the shape almost every primary timetable
                    actually has. The Days column then trims the exceptions.
                  */}
                  {isManagement && (
                    <TableCell sx={{ minWidth: 190 }}>
                      <TextField
                        select
                        size="small"
                        fullWidth
                        // Locked without a teacher (nothing to timetable yet), and
                        // while the subject sits in two periods (the value shown
                        // would be only one of them).
                        disabled={
                          !canManageTimetable || hasMixedPeriods(subj.id) || !mapping
                        }
                        value={periodOf(subj.id) ?? ''}
                        onChange={(e) =>
                          applyPeriod(subj.id, e.target.value === '' ? null : Number(e.target.value))
                        }
                        helperText={
                          !canManageTimetable
                            ? 'Only an administrator can change the timetable'
                            : !mapping
                              // The rule the server enforces, stated where it is hit
                              // rather than after a rejected save.
                              ? 'Assign a teacher first'
                              : undefined
                        }
                      >
                        <MenuItem value="">
                          <em>Not timetabled</em>
                        </MenuItem>
                        {PERIOD_NUMBERS.map((p) => {
                          const holder = periodHeldByOther(p, subj.id);
                          const blocked = periodUnavailableReason(p, subj.id);
                          const reason = holder ? `taken by ${holder}` : blocked;
                          return (
                            <MenuItem key={p} value={p} disabled={Boolean(reason)}>
                              {periodLabel(p)}
                              {reason ? ` — ${reason}` : ''}
                            </MenuItem>
                          );
                        })}
                      </TextField>
                    </TableCell>
                  )}

                  {/*
                    One toggle per working day, so a subject that runs four days a
                    week is two clicks from the auto-filled six. Disabled when the
                    subject has no period yet — there is nothing to place.
                  */}
                  {isManagement && (
                    <TableCell sx={{ minWidth: 230 }}>
                      {hasMixedPeriods(subj.id) ? (
                        // This row cannot represent two periods, and picking one
                        // here would silently flatten the other. Say so and send
                        // the user to the editor that can.
                        <Typography variant="caption" color="warning.main">
                          Runs in more than one period — edit on the Timetable tab
                        </Typography>
                      ) : periodOf(subj.id) == null ? (
                        <Typography variant="caption" color="text.secondary">
                          Pick a period to schedule this subject
                        </Typography>
                      ) : (
                        <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                          {WORKING_DAYS.map((day) => {
                            const on = subjectSlots.some((s) => s.dayOfWeek === day);
                            // Frozen while the row breaks the period-1 rule: adding
                            // days would only build more rows the save refuses.
                            const frozen = Boolean(firstPeriodViolation(subj.id));
                            return (
                              <Chip
                                key={day}
                                size="small"
                                label={dayLabel(day)}
                                color={on ? 'primary' : 'default'}
                                variant={on ? 'filled' : 'outlined'}
                                disabled={frozen}
                                onClick={
                                  canManageTimetable && !frozen
                                    ? () => toggleDay(subj.id, day)
                                    : undefined
                                }
                              />
                            );
                          })}
                        </Stack>
                      )}
                      {firstPeriodViolation(subj.id) && (
                        <Typography variant="caption" color="error.main" display="block" sx={{ mt: 0.5 }}>
                          {firstPeriodViolation(subj.id)}
                        </Typography>
                      )}
                      {/* A cover lesson is legitimate, but it is usually a mistake,
                          so it is stated rather than left to be noticed. */}
                      {subjectSlots.some(
                        (s) => mapping && s.teacherId && s.teacherId !== mapping.teacherId,
                      ) && (
                        <Typography variant="caption" color="warning.main" display="block" sx={{ mt: 0.5 }}>
                          Some periods are taught by another teacher
                        </Typography>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
      {loading && (
        <Stack alignItems="center" sx={{ py: 2 }}>
          <CircularProgress size={22} />
        </Stack>
      )}

      <Dialog open={!!assignTarget} onClose={() => setAssignTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>{assignTarget?.subjectName ?? 'Assign Teacher'}</DialogTitle>
        <DialogContent>
          <TextField
            select
            label="Teacher"
            fullWidth
            sx={{ mt: 1 }}
            value={selectedTeacherId}
            onChange={(e) => setSelectedTeacherId(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <MenuItem value="">Select a teacher</MenuItem>
            {teachers.map((t) => (
              <MenuItem key={t.id} value={t.id}>
                {`${(t.firstName ?? t.username) ?? ''} ${t.lastName ?? ''}`.trim()}
                <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 0.75 }}>
                  ({teacherSubjectLabel(t.id)})
                </Typography>
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setAssignTarget(null)} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleAssign}
            disabled={!selectedTeacherId || saving}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default TeacherMappingTab;
