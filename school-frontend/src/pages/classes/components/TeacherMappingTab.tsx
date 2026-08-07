import { useEffect, useMemo, useState } from 'react';
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
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import { useSnackbar } from 'notistack';
import EmptyState from '@/components/common/EmptyState';
import classesApi from '@/api/classesApi';
import teachersApi from '@/api/teachersApi';
import type { ClassSubjectTeacher, Section, Subject, Teacher } from '@/types';

export interface TeacherMappingTabProps {
  classId: number;
  sections: Section[];
  subjects: Subject[];
}

/** Section x Subject grid for this class; click an empty cell to assign a teacher, click an assigned chip to unassign. */
export function TeacherMappingTab({ classId, sections, subjects }: TeacherMappingTabProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [mappings, setMappings] = useState<ClassSubjectTeacher[]>([]);
  const [loading, setLoading] = useState(false);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [assignTarget, setAssignTarget] = useState<{ section: Section; subject: Subject } | null>(null);
  const [selectedTeacherId, setSelectedTeacherId] = useState<number | ''>('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    if (sections.length === 0) {
      setMappings([]);
      return;
    }
    setLoading(true);
    try {
      const results = await Promise.all(
        sections.map((sec) => classesApi.listTeacherMappings({ sectionId: sec.id })),
      );
      setMappings(results.flatMap((r) => r.data));
    } catch {
      enqueueSnackbar('Could not load teacher mappings.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sections, subjects]);

  useEffect(() => {
    teachersApi
      .list({ size: 200, sort: 'employeeId,asc' })
      .then((res) => setTeachers(res.data.content))
      .catch(() => enqueueSnackbar('Could not load teachers.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const mappingFor = (sectionId: number, subjectId: number) =>
    mappings.find((m) => m.sectionId === sectionId && m.subjectId === subjectId);

  const teacherName = (teacherId: number) => {
    const t = teachers.find((tc) => tc.id === teacherId);
    return t ? `${t.firstName ?? t.username} ${t.lastName ?? ''}`.trim() : `Teacher #${teacherId}`;
  };

  const handleAssign = async () => {
    if (!assignTarget || !selectedTeacherId) return;
    setSaving(true);
    try {
      const res = await classesApi.assignTeacherMapping({
        classId,
        sectionId: assignTarget.section.id,
        subjectId: assignTarget.subject.id,
        teacherId: selectedTeacherId,
      });
      setMappings((prev) => [...prev, res.data]);
      enqueueSnackbar('Teacher assigned.', { variant: 'success' });
      setAssignTarget(null);
      setSelectedTeacherId('');
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not assign this teacher.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleUnassign = async (mapping: ClassSubjectTeacher) => {
    try {
      await classesApi.removeTeacherMapping(mapping.id);
      setMappings((prev) => prev.filter((m) => m.id !== mapping.id));
      enqueueSnackbar('Teacher unassigned.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not unassign this teacher.', { variant: 'error' });
    }
  };

  const emptyMessage = useMemo(() => {
    if (sections.length === 0) return 'Add at least one section first.';
    if (subjects.length === 0) return 'Add at least one subject first.';
    return null;
  }, [sections, subjects]);

  if (emptyMessage) {
    return <EmptyState title="Nothing to map yet" description={emptyMessage} />;
  }

  return (
    <Box>
      <TableContainer component={Paper} variant="outlined" sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>Subject \ Section</TableCell>
              {sections.map((sec) => (
                <TableCell key={sec.id} align="center" sx={{ fontWeight: 700 }}>
                  {sec.sectionName}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {subjects.map((subj) => (
              <TableRow key={subj.id}>
                <TableCell sx={{ fontWeight: 600 }}>{subj.subjectName}</TableCell>
                {sections.map((sec) => {
                  const mapping = mappingFor(sec.id, subj.id);
                  return (
                    <TableCell key={sec.id} align="center">
                      {mapping ? (
                        <Chip
                          label={mapping.teacherName ?? teacherName(mapping.teacherId)}
                          size="small"
                          onDelete={() => handleUnassign(mapping)}
                          deleteIcon={<CloseOutlinedIcon />}
                          color="primary"
                          variant="outlined"
                        />
                      ) : (
                        <Chip
                          label="Assign"
                          size="small"
                          variant="outlined"
                          onClick={() => {
                            setAssignTarget({ section: sec, subject: subj });
                            setSelectedTeacherId('');
                          }}
                        />
                      )}
                    </TableCell>
                  );
                })}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {loading && (
        <Stack alignItems="center" sx={{ py: 2 }}>
          <CircularProgress size={22} />
        </Stack>
      )}

      <Dialog open={!!assignTarget} onClose={() => setAssignTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Assign Teacher</DialogTitle>
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
                {(t.firstName ?? t.username) + ' ' + (t.lastName ?? '')}
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
            Assign
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default TeacherMappingTab;
