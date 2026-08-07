import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import dayjs from 'dayjs';
import studentsApi from '@/api/studentsApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { getStudentDisplayName } from '@/utils/format';
import type { Student } from '@/types';
import type { HostelVisitorPayload } from '@/api/hostelApi';

const schema = yup.object({
  visitorName: yup.string().required('Visitor name is required').max(100),
  relation: yup.string().required('Relation is required').max(50),
  phone: yup.string().max(20).optional(),
  visitDate: yup.mixed<dayjs.Dayjs>().required('Visit date is required'),
  purpose: yup.string().max(250).optional(),
});
type FormValues = yup.InferType<typeof schema>;

export interface VisitorFormDialogProps {
  open: boolean;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: HostelVisitorPayload) => void;
}

/** "Log Visitor" form: student autocomplete + visitor details + purpose. */
export function VisitorFormDialog({ open, saving, onClose, onSubmit }: VisitorFormDialogProps) {
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
    defaultValues: { visitorName: '', relation: '', phone: '', visitDate: dayjs(), purpose: '' },
  });

  useEffect(() => {
    if (open) {
      setSelectedStudent(null);
      setStudentQuery('');
      reset({ visitorName: '', relation: '', phone: '', visitDate: dayjs(), purpose: '' });
    }
  }, [open, reset]);

  useEffect(() => {
    if (!debouncedQuery) {
      setStudentOptions([]);
      return;
    }
    studentsApi
      .list({ search: debouncedQuery, size: 20 })
      .then((res) => setStudentOptions(res.data.content))
      .catch(() => undefined);
  }, [debouncedQuery]);

  const submit = (values: FormValues) => {
    if (!selectedStudent) return;
    onSubmit({
      studentId: selectedStudent.id,
      visitorName: values.visitorName,
      relation: values.relation,
      phone: values.phone || undefined,
      visitDate: dayjs(values.visitDate).format('YYYY-MM-DD'),
      purpose: values.purpose || undefined,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Log Visitor</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <Autocomplete
                options={studentOptions}
                value={selectedStudent}
                getOptionLabel={(o) => `${getStudentDisplayName(o)} (${o.admissionNumber})`}
                isOptionEqualToValue={(o, v) => o.id === v.id}
                onChange={(_e, value) => setSelectedStudent(value)}
                onInputChange={(_e, value) => setStudentQuery(value)}
                renderInput={(params) => <TextField {...params} label="Resident (Student)" placeholder="Name or admission no." />}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Visitor Name"
                fullWidth
                {...register('visitorName')}
                error={!!errors.visitorName}
                helperText={errors.visitorName?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Relation"
                fullWidth
                {...register('relation')}
                error={!!errors.relation}
                helperText={errors.relation?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Phone" fullWidth {...register('phone')} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="visitDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Visit Date"
                    value={field.value ?? null}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true, error: !!errors.visitDate, helperText: errors.visitDate?.message } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Purpose" fullWidth multiline minRows={2} {...register('purpose')} />
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
            disabled={saving || !selectedStudent}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Log Visitor
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default VisitorFormDialog;
