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
import type { ClassSubjectTeacher, Section, Subject, Teacher, TimetableSlot } from '@/types';

export interface TeacherMappingTabProps {
  classId: number;
  sections: Section[];
  subjects: Subject[];
}

function shortDay(day: string) {
  return day.charAt(0) + day.slice(1, 3).toLowerCase();
}

/**
 * Subject -> teacher for this class, one row per subject.
 *
 * <p>The section axis is gone: with a single section per class, a "section x
 * subject" grid was a table one column wide. What replaces it is the piece that
 * was missing — each subject's scheduled periods, so it is visible at a glance
 * whether an assigned teacher has actually been timetabled.
 *
 * <p>Periods are shown here but edited on the Timetable tab, which owns the
 * week-at-a-time save and the teacher/room clash detection. Two editors for the
 * same rows would be two chances to disagree.
 */
export function TeacherMappingTab({ classId, sections, subjects }: TeacherMappingTabProps) {
  const { enqueueSnackbar } = useSnackbar();
  const { isManagement } = usePermissions();
  const [mappings, setMappings] = useState<ClassSubjectTeacher[]>([]);
  const [slots, setSlots] = useState<TimetableSlot[]>([]);
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

  const load = useCallback(async () => {
    if (!section) {
      setMappings([]);
      setSlots([]);
      return;
    }
    setLoading(true);
    try {
      const [mappingRes, slotRes, allMappingRes] = await Promise.all([
        classesApi.listTeacherMappings({ sectionId: section.id }),
        // Management-only endpoint. A teacher may read this tab but not the class's
        // week, so the periods column is skipped for them rather than failing the
        // whole tab on a 403.
        isManagement ? timetableApi.getForClass(classId) : Promise.resolve(null),
        // Unfiltered: "who is the maths teacher" is a school-wide question, not a
        // question about this class. One small request — the table holds a handful
        // of rows per class — rather than one per teacher.
        classesApi.listTeacherMappings({}),
      ]);
      setMappings(mappingRes.data);
      setSlots(slotRes ? slotRes.data : []);

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

  const mappingFor = (subjectId: number) => mappings.find((m) => m.subjectId === subjectId);

  const slotsFor = useCallback(
    (subjectId: number) =>
      slots
        .filter((s) => s.subjectId === subjectId)
        .sort((a, b) => a.periodNumber - b.periodNumber),
    [slots],
  );

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

      <TableContainer component={Paper} variant="outlined" sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Subject</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Teacher</TableCell>
              {/* Only management can read the class's week, so the column is left
                  out for anyone else rather than shown uniformly empty. */}
              {isManagement && <TableCell sx={{ fontWeight: 700 }}>Time slots</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {subjects.map((subj) => {
              const mapping = mappingFor(subj.id);
              const subjectSlots = slotsFor(subj.id);
              return (
                <TableRow key={subj.id} hover>
                  <TableCell sx={{ fontWeight: 600 }}>
                    {subj.subjectName}
                    <Typography variant="caption" color="text.secondary" display="block">
                      {subj.subjectCode}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {mapping ? (
                      <Chip
                        label={mapping.teacherName ?? teacherName(mapping.teacherId)}
                        size="small"
                        onClick={() => {
                          setAssignTarget(subj);
                          setSelectedTeacherId(mapping.teacherId);
                        }}
                        onDelete={() => handleUnassign(mapping)}
                        deleteIcon={<CloseOutlinedIcon />}
                        color="primary"
                        variant="outlined"
                      />
                    ) : (
                      <Chip
                        label="Assign teacher"
                        size="small"
                        variant="outlined"
                        onClick={() => {
                          setAssignTarget(subj);
                          setSelectedTeacherId('');
                        }}
                      />
                    )}
                  </TableCell>
                  {isManagement && (
                  <TableCell>
                    {subjectSlots.length === 0 ? (
                      <Typography variant="caption" color="warning.main">
                        Not timetabled — add periods on the Timetable tab
                      </Typography>
                    ) : (
                      <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                        {subjectSlots.map((slot) => (
                          <Chip
                            key={slot.id}
                            size="small"
                            variant="outlined"
                            label={`${shortDay(slot.dayOfWeek)} P${slot.periodNumber} ${slot.startTime.slice(0, 5)}`}
                            color={
                              // A period taught by someone other than the subject's
                              // assigned teacher is legitimate (a cover lesson) but
                              // worth showing, since it is usually a mistake.
                              mapping && slot.teacherId && slot.teacherId !== mapping.teacherId
                                ? 'warning'
                                : 'default'
                            }
                          />
                        ))}
                      </Stack>
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
