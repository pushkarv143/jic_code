import { useEffect } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { type Dayjs } from 'dayjs';
import type { ExamSchedule, Subject } from '@/types';
import type { ExamSchedulePayload } from '@/api/examApi';

export interface ExamScheduleFormDialogProps {
  open: boolean;
  editing: ExamSchedule | null;
  subjects: Subject[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: ExamSchedulePayload) => void;
}

interface FormValues {
  subjectId: number | '';
  examDate: Dayjs | null;
  startTime: Dayjs | null;
  endTime: Dayjs | null;
  maxMarks: string;
  roomNumber: string;
}

/** Add/edit dialog for a single exam schedule row (subject, date, time window, max marks, room). */
export function ExamScheduleFormDialog({ open, editing, subjects, saving, onClose, onSubmit }: ExamScheduleFormDialogProps) {
  const { control, register, handleSubmit, watch, reset } = useForm<FormValues>({
    defaultValues: { subjectId: '', examDate: dayjs(), startTime: null, endTime: null, maxMarks: '100', roomNumber: '' },
  });

  useEffect(() => {
    if (open) {
      reset({
        subjectId: editing?.subjectId ?? '',
        examDate: editing ? dayjs(editing.examDate) : dayjs(),
        startTime: editing?.startTime ? dayjs(editing.startTime, 'HH:mm:ss') : null,
        endTime: editing?.endTime ? dayjs(editing.endTime, 'HH:mm:ss') : null,
        maxMarks: editing ? String(editing.maxMarks) : '100',
        roomNumber: editing?.roomNumber ?? '',
      });
    }
  }, [open, editing, reset]);

  const subjectId = watch('subjectId');
  const examDate = watch('examDate');
  const startTime = watch('startTime');
  const endTime = watch('endTime');
  const maxMarks = watch('maxMarks');

  const canSubmit = !!subjectId && !!examDate && !!startTime && !!endTime && !!maxMarks && Number(maxMarks) > 0;

  const submit = (values: FormValues) => {
    if (!canSubmit) return;
    onSubmit({
      subjectId: values.subjectId as number,
      examDate: dayjs(values.examDate).format('YYYY-MM-DD'),
      startTime: dayjs(values.startTime).format('HH:mm:ss'),
      endTime: dayjs(values.endTime).format('HH:mm:ss'),
      maxMarks: Number(values.maxMarks),
      roomNumber: values.roomNumber.trim() || undefined,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Exam Schedule' : 'Add Exam Schedule'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <Controller
                name="subjectId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Subject"
                    fullWidth
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">Select a subject</MenuItem>
                    {subjects.map((s) => (
                      <MenuItem key={s.id} value={s.id}>
                        {s.subjectName}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <Controller
                name="examDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Exam Date"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={6} sm={4}>
              <Controller
                name="startTime"
                control={control}
                render={({ field }) => (
                  <TimePicker
                    label="Start Time"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={6} sm={4}>
              <Controller
                name="endTime"
                control={control}
                render={({ field }) => (
                  <TimePicker
                    label="End Time"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField label="Max Marks" fullWidth type="number" {...register('maxMarks')} />
            </Grid>
            <Grid item xs={6}>
              <TextField label="Room Number" fullWidth {...register('roomNumber')} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={saving || !canSubmit}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default ExamScheduleFormDialog;
