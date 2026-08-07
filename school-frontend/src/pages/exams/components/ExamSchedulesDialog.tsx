import { useCallback, useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import examApi from '@/api/examApi';
import classesApi from '@/api/classesApi';
import type { Exam, ExamSchedule, Subject } from '@/types';
import type { ExamSchedulePayload } from '@/api/examApi';
import ExamScheduleFormDialog from './ExamScheduleFormDialog';

export interface ExamSchedulesDialogProps {
  open: boolean;
  exam: Exam | null;
  onClose: () => void;
}

/** Detail dialog: list + add/edit/delete the exam_schedules rows (subject, date, time, max marks, room) for one exam. */
export function ExamSchedulesDialog({ open, exam, onClose }: ExamSchedulesDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [schedules, setSchedules] = useState<ExamSchedule[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ExamSchedule | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ExamSchedule | null>(null);

  const load = useCallback(async () => {
    if (!exam) return;
    setLoading(true);
    try {
      const [scheduleRes, subjectRes] = await Promise.all([
        examApi.schedules.list(exam.id),
        classesApi.listSubjects(exam.classId),
      ]);
      setSchedules(scheduleRes.data);
      setSubjects(subjectRes.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load exam schedules.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [exam, enqueueSnackbar]);

  useEffect(() => {
    if (open) load();
  }, [open, load]);

  const handleSave = async (values: ExamSchedulePayload) => {
    if (!exam) return;
    setSaving(true);
    try {
      if (editing) {
        await examApi.schedules.update(editing.id, values);
        enqueueSnackbar('Exam schedule updated.', { variant: 'success' });
      } else {
        await examApi.schedules.create(exam.id, values);
        enqueueSnackbar('Exam schedule added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this schedule.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await examApi.schedules.remove(deleteTarget.id);
      enqueueSnackbar('Exam schedule deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this schedule.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogTitle>
          Schedules — {exam?.examTypeName ?? 'Exam'} ({exam?.className ?? ''})
        </DialogTitle>
        <DialogContent dividers>
          <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
            <Button
              size="small"
              variant="outlined"
              startIcon={<AddOutlinedIcon />}
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              Add Schedule
            </Button>
          </Stack>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress size={28} />
            </Box>
          ) : schedules.length === 0 ? (
            <EmptyState title="No schedules yet" description="Add a subject-wise schedule for this exam using the button above." />
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Subject</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Time</TableCell>
                  <TableCell>Max Marks</TableCell>
                  <TableCell>Room</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {schedules.map((s) => (
                  <TableRow key={s.id} hover>
                    <TableCell>{s.subjectName ?? `#${s.subjectId}`}</TableCell>
                    <TableCell>{dayjs(s.examDate).format('DD MMM YYYY')}</TableCell>
                    <TableCell>
                      {dayjs(s.startTime, 'HH:mm:ss').format('hh:mm A')} – {dayjs(s.endTime, 'HH:mm:ss').format('hh:mm A')}
                    </TableCell>
                    <TableCell>
                      <Chip size="small" variant="outlined" label={s.maxMarks} />
                    </TableCell>
                    <TableCell>{s.roomNumber ?? '-'}</TableCell>
                    <TableCell align="right">
                      <Tooltip title="Edit">
                        <IconButton
                          size="small"
                          onClick={() => {
                            setEditing(s);
                            setFormOpen(true);
                          }}
                        >
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton size="small" onClick={() => setDeleteTarget(s)}>
                          <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Close</Button>
        </DialogActions>
      </Dialog>

      <ExamScheduleFormDialog
        open={formOpen}
        editing={editing}
        subjects={subjects}
        saving={saving}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete exam schedule"
        message={`Delete the ${deleteTarget?.subjectName ?? ''} schedule? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}

export default ExamSchedulesDialog;
