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
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import type { AcademicYear, Exam, ExamType, SchoolClass } from '@/types';
import type { ExamPayload } from '@/api/examApi';

export interface ExamFormDialogProps {
  open: boolean;
  editing: Exam | null;
  examTypes: ExamType[];
  classes: SchoolClass[];
  years: AcademicYear[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: ExamPayload) => void;
}

interface FormValues {
  examTypeId: number | '';
  classId: number | '';
  academicYearId: number | '';
  startDate: dayjs.Dayjs | null;
  endDate: dayjs.Dayjs | null;
}

/** Add/edit dialog for an exam: exam type, class, academic year, start/end dates. */
export function ExamFormDialog({ open, editing, examTypes, classes, years, saving, onClose, onSubmit }: ExamFormDialogProps) {
  const { control, handleSubmit, watch, reset } = useForm<FormValues>({
    defaultValues: { examTypeId: '', classId: '', academicYearId: '', startDate: dayjs(), endDate: dayjs() },
  });

  useEffect(() => {
    if (open) {
      reset({
        examTypeId: editing?.examTypeId ?? '',
        classId: editing?.classId ?? '',
        academicYearId: editing?.academicYearId ?? '',
        startDate: editing ? dayjs(editing.startDate) : dayjs(),
        endDate: editing ? dayjs(editing.endDate) : dayjs(),
      });
    }
  }, [open, editing, reset]);

  const startDate = watch('startDate');
  const examTypeId = watch('examTypeId');
  const classId = watch('classId');
  const academicYearId = watch('academicYearId');
  const endDate = watch('endDate');

  const canSubmit = !!examTypeId && !!classId && !!academicYearId && !!startDate && !!endDate;

  const submit = (values: FormValues) => {
    if (!canSubmit) return;
    onSubmit({
      examTypeId: values.examTypeId as number,
      classId: values.classId as number,
      academicYearId: values.academicYearId as number,
      startDate: dayjs(values.startDate).format('YYYY-MM-DD'),
      endDate: dayjs(values.endDate).format('YYYY-MM-DD'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Exam' : 'Add Exam'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={6}>
              <Controller
                name="examTypeId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Exam Type"
                    fullWidth
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">Select an exam type</MenuItem>
                    {examTypes.map((t) => (
                      <MenuItem key={t.id} value={t.id}>
                        {t.name}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="classId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Class"
                    fullWidth
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">Select a class</MenuItem>
                    {classes.map((c) => (
                      <MenuItem key={c.id} value={c.id}>
                        {c.className}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="academicYearId"
                control={control}
                render={({ field }) => (
                  <TextField
                    select
                    label="Academic Year"
                    fullWidth
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}
                  >
                    <MenuItem value="">Select a year</MenuItem>
                    {years.map((y) => (
                      <MenuItem key={y.id} value={y.id}>
                        {y.yearName}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6} />
            <Grid item xs={12} sm={6}>
              <Controller
                name="startDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Start Date"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="endDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="End Date"
                    value={field.value}
                    minDate={startDate ?? undefined}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
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

export default ExamFormDialog;
