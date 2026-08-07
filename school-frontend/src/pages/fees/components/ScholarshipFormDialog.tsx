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
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import studentsApi from '@/api/studentsApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { getStudentDisplayName } from '@/utils/format';
import type { AcademicYear, Scholarship, ScholarshipType, Student } from '@/types';

const TYPES: ScholarshipType[] = ['PERCENTAGE', 'FIXED'];

const schema = yup.object({
  studentId: yup.string().required('Student is required'),
  title: yup.string().required('Title is required').max(150),
  amount: yup
    .string()
    .required('Amount is required')
    .test('num', 'Enter a valid amount', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value) && Number(value) > 0),
  type: yup.mixed<ScholarshipType>().oneOf(TYPES).required('Type is required'),
  academicYearId: yup.string().required('Academic year is required'),
});
type FormValues = yup.InferType<typeof schema>;

export interface ScholarshipFormDialogProps {
  open: boolean;
  editing: Scholarship | null;
  years: AcademicYear[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { studentId: number; title: string; amount: number; type: ScholarshipType; academicYearId: number }) => void;
}

/** Add/edit dialog for a scholarship — student search, title, amount, type (percentage/fixed), academic year. */
export function ScholarshipFormDialog({ open, editing, years, saving, onClose, onSubmit }: ScholarshipFormDialogProps) {
  const [studentQuery, setStudentQuery] = useState('');
  const debouncedQuery = useDebouncedValue(studentQuery, 400);
  const [studentOptions, setStudentOptions] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { studentId: '', title: '', amount: '', type: 'FIXED', academicYearId: '' },
  });

  useEffect(() => {
    if (open) {
      reset({
        studentId: editing ? String(editing.studentId) : '',
        title: editing?.title ?? '',
        amount: editing ? String(editing.amount) : '',
        type: editing?.type ?? 'FIXED',
        academicYearId: editing ? String(editing.academicYearId) : '',
      });
      setSelectedStudent(
        editing
          ? ({ id: editing.studentId, firstName: editing.studentName ?? null, lastName: null, admissionNumber: editing.admissionNumber ?? '' } as Student)
          : null,
      );
      setStudentQuery('');
    }
  }, [open, editing, reset]);

  useEffect(() => {
    if (!debouncedQuery) return;
    studentsApi
      .list({ search: debouncedQuery, size: 20 })
      .then((res) => setStudentOptions(res.data.content))
      .catch(() => undefined);
  }, [debouncedQuery]);

  const submit = (values: FormValues) => {
    onSubmit({
      studentId: Number(values.studentId),
      title: values.title,
      amount: Number(values.amount),
      type: values.type,
      academicYearId: Number(values.academicYearId),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Scholarship' : 'Add Scholarship'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <Controller
                name="studentId"
                control={control}
                render={({ field }) => (
                  <Autocomplete
                    options={studentOptions}
                    value={selectedStudent}
                    getOptionLabel={(o) => `${getStudentDisplayName(o)} (${o.admissionNumber})`}
                    isOptionEqualToValue={(o, v) => o.id === v.id}
                    onChange={(_e, value) => {
                      setSelectedStudent(value);
                      field.onChange(value ? String(value.id) : '');
                    }}
                    onInputChange={(_e, value) => setStudentQuery(value)}
                    renderInput={(params) => (
                      <TextField {...params} label="Student" placeholder="Search by name or admission no." error={!!errors.studentId} helperText={errors.studentId?.message} />
                    )}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Title" fullWidth {...register('title')} error={!!errors.title} helperText={errors.title?.message} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                select
                label="Type"
                fullWidth
                defaultValue="FIXED"
                {...register('type')}
                error={!!errors.type}
                helperText={errors.type?.message}
              >
                <MenuItem value="FIXED">Fixed Amount</MenuItem>
                <MenuItem value="PERCENTAGE">Percentage</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Amount" fullWidth {...register('amount')} error={!!errors.amount} helperText={errors.amount?.message} />
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

export default ScholarshipFormDialog;
