import { useEffect, useState } from 'react';
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
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import dayjs, { type Dayjs } from 'dayjs';
import { useSnackbar } from 'notistack';
import classesApi from '@/api/classesApi';
import type { OnlineClass, SchoolClass, Section, Subject } from '@/types';
import type { OnlineClassPayload } from '@/api/onlineClassesApi';

export interface OnlineClassFormDialogProps {
  open: boolean;
  editing: OnlineClass | null;
  classes: SchoolClass[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: OnlineClassPayload) => void;
}

interface FormValues {
  classId: number | '';
  sectionId: number | '';
  subjectId: number | '';
  title: string;
  meetingLink: string;
  scheduledAt: Dayjs | null;
  durationMinutes: string;
}

/** Add/edit dialog for an online class: class -> section/subject cascade, title, meeting link, schedule, duration. */
export function OnlineClassFormDialog({ open, editing, classes, saving, onClose, onSubmit }: OnlineClassFormDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [sections, setSections] = useState<Section[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);

  const { control, register, handleSubmit, watch, setValue, reset } = useForm<FormValues>({
    defaultValues: {
      classId: '',
      sectionId: '',
      subjectId: '',
      title: '',
      meetingLink: '',
      scheduledAt: dayjs().add(1, 'day').hour(9).minute(0),
      durationMinutes: '40',
    },
  });

  const classId = watch('classId');

  useEffect(() => {
    if (open) {
      reset({
        classId: editing?.classId ?? '',
        sectionId: editing?.sectionId ?? '',
        subjectId: editing?.subjectId ?? '',
        title: editing?.title ?? '',
        meetingLink: editing?.meetingLink ?? '',
        scheduledAt: editing ? dayjs(editing.scheduledAt) : dayjs().add(1, 'day').hour(9).minute(0),
        durationMinutes: editing ? String(editing.durationMinutes) : '40',
      });
    }
  }, [open, editing, reset]);

  useEffect(() => {
    if (!classId) {
      setSections([]);
      setSubjects([]);
      return;
    }
    classesApi.listSections(classId as number).then((res) => setSections(res.data)).catch(() => undefined);
    classesApi.listSubjects(classId as number).then((res) => setSubjects(res.data)).catch(() => undefined);
  }, [classId]);

  const submit = (values: FormValues) => {
    if (!values.classId || !values.sectionId || !values.subjectId || !values.scheduledAt || !values.title.trim() || !values.meetingLink.trim()) {
      enqueueSnackbar('Please fill in all required fields.', { variant: 'warning' });
      return;
    }
    onSubmit({
      classId: values.classId as number,
      sectionId: values.sectionId as number,
      subjectId: values.subjectId as number,
      title: values.title.trim(),
      meetingLink: values.meetingLink.trim(),
      scheduledAt: dayjs(values.scheduledAt).toISOString(),
      durationMinutes: Number(values.durationMinutes) || 40,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Online Class' : 'Schedule Online Class'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField label="Title" fullWidth autoFocus {...register('title', { required: true })} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <Controller
                name="classId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Class"
                    fullWidth
                    value={field.value}
                    onChange={(e) => {
                      const v = e.target.value === '' ? '' : Number(e.target.value);
                      field.onChange(v);
                      setValue('sectionId', '');
                      setValue('subjectId', '');
                    }}
                  >
                    <MenuItem value="">Select a class</MenuItem>
                    {classes.map((c) => (
                      <MenuItem key={c.id} value={c.id}>{c.className}</MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <Controller
                name="sectionId"
                control={control}
                render={({ field }) => (
                  <TextField select label="Section" fullWidth disabled={!classId} value={field.value} onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}>
                    <MenuItem value="">Select a section</MenuItem>
                    {sections.map((s) => (
                      <MenuItem key={s.id} value={s.id}>{s.sectionName}</MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <Controller
                name="subjectId"
                control={control}
                render={({ field }) => (
                  <TextField select label="Subject" fullWidth disabled={!classId} value={field.value} onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}>
                    <MenuItem value="">Select a subject</MenuItem>
                    {subjects.map((s) => (
                      <MenuItem key={s.id} value={s.id}>{s.subjectName}</MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Meeting Link" fullWidth placeholder="https://meet.google.com/..." {...register('meetingLink', { required: true })} />
            </Grid>
            <Grid item xs={12} sm={7}>
              <Controller
                name="scheduledAt"
                control={control}
                render={({ field }) => (
                  <DateTimePicker
                    label="Scheduled At"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={5}>
              <TextField label="Duration (minutes)" fullWidth type="number" {...register('durationMinutes')} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default OnlineClassFormDialog;
