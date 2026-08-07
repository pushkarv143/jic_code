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
import { yupResolver } from '@hookform/resolvers/yup';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import * as yup from 'yup';
import dayjs from 'dayjs';
import type { AcademicYear, FeeCategory, FeeStructure, SchoolClass } from '@/types';

const schema = yup.object({
  classId: yup.string().required('Class is required'),
  academicYearId: yup.string().required('Academic year is required'),
  feeCategoryId: yup.string().required('Fee category is required'),
  amount: yup
    .string()
    .required('Amount is required')
    .test('num', 'Enter a valid amount', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value)),
  dueDate: yup
    .mixed<dayjs.Dayjs>()
    .required('Due date is required')
    .test('is-valid', 'Enter a valid due date', (value) => !!value && dayjs(value).isValid()),
});
type FormValues = yup.InferType<typeof schema>;

export interface FeeStructureFormDialogProps {
  open: boolean;
  editing: FeeStructure | null;
  classes: SchoolClass[];
  years: AcademicYear[];
  categories: FeeCategory[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { classId: number; academicYearId: number; feeCategoryId: number; amount: number; dueDate: string }) => void;
}

/** Add/edit dialog for a fee structure (class + year + category + amount + due date). */
export function FeeStructureFormDialog({
  open,
  editing,
  classes,
  years,
  categories,
  saving,
  onClose,
  onSubmit,
}: FeeStructureFormDialogProps) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { classId: '', academicYearId: '', feeCategoryId: '', amount: '', dueDate: undefined },
  });

  useEffect(() => {
    if (open) {
      reset({
        classId: editing ? String(editing.classId) : '',
        academicYearId: editing ? String(editing.academicYearId) : '',
        feeCategoryId: editing ? String(editing.feeCategoryId) : '',
        amount: editing ? String(editing.amount) : '',
        dueDate: editing ? dayjs(editing.dueDate) : undefined,
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({
      classId: Number(values.classId),
      academicYearId: Number(values.academicYearId),
      feeCategoryId: Number(values.feeCategoryId),
      amount: Number(values.amount),
      dueDate: dayjs(values.dueDate).format('YYYY-MM-DD'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Fee Structure' : 'Add Fee Structure'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                label="Class"
                fullWidth
                defaultValue=""
                {...register('classId')}
                error={!!errors.classId}
                helperText={errors.classId?.message}
              >
                <MenuItem value="">Select a class</MenuItem>
                {classes.map((c) => (
                  <MenuItem key={c.id} value={String(c.id)}>
                    {c.className}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                label="Academic Year"
                fullWidth
                defaultValue=""
                {...register('academicYearId')}
                error={!!errors.academicYearId}
                helperText={errors.academicYearId?.message}
              >
                <MenuItem value="">Select a year</MenuItem>
                {years.map((y) => (
                  <MenuItem key={y.id} value={String(y.id)}>
                    {y.yearName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                label="Fee Category"
                fullWidth
                defaultValue=""
                {...register('feeCategoryId')}
                error={!!errors.feeCategoryId}
                helperText={errors.feeCategoryId?.message}
              >
                <MenuItem value="">Select a category</MenuItem>
                {categories.map((c) => (
                  <MenuItem key={c.id} value={String(c.id)}>
                    {c.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Amount (INR)"
                fullWidth
                {...register('amount')}
                error={!!errors.amount}
                helperText={errors.amount?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="dueDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Due Date"
                    value={field.value ?? null}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{
                      textField: { fullWidth: true, error: !!errors.dueDate, helperText: errors.dueDate?.message },
                    }}
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
          <Button type="submit" variant="contained" disabled={saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default FeeStructureFormDialog;
