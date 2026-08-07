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
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import type { AcademicYear, SchoolClass } from '@/types';

const schema = yup.object({
  className: yup.string().required('Class name is required').max(50),
  academicYearId: yup.string().required('Academic year is required'),
});
type FormValues = yup.InferType<typeof schema>;

export interface ClassFormDialogProps {
  open: boolean;
  editing: SchoolClass | null;
  years: AcademicYear[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { className: string; academicYearId: number }) => void;
}

/** Add/edit dialog for a class (simple lookup-style entity, matching the LookupManagerDialog pattern). */
export function ClassFormDialog({ open, editing, years, saving, onClose, onSubmit }: ClassFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: yupResolver(schema), defaultValues: { className: '', academicYearId: '' } });

  useEffect(() => {
    if (open) {
      reset({
        className: editing?.className ?? '',
        academicYearId: editing ? String(editing.academicYearId) : '',
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({ className: values.className, academicYearId: Number(values.academicYearId) });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Class' : 'Add Class'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                label="Class Name"
                fullWidth
                autoFocus
                {...register('className')}
                error={!!errors.className}
                helperText={errors.className?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                select
                label="Academic Year"
                fullWidth
                defaultValue=""
                {...register('academicYearId')}
                error={!!errors.academicYearId}
                helperText={errors.academicYearId?.message}
              >
                <MenuItem value="">Select an academic year</MenuItem>
                {years.map((y) => (
                  <MenuItem key={y.id} value={String(y.id)}>
                    {y.yearName}
                    {y.isCurrent ? ' (current)' : ''}
                  </MenuItem>
                ))}
              </TextField>
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
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default ClassFormDialog;
