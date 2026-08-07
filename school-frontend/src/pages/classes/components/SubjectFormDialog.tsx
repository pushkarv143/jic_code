import { useEffect } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import type { Subject } from '@/types';

const schema = yup.object({
  subjectName: yup.string().required('Subject name is required').max(100),
  subjectCode: yup.string().required('Subject code is required').max(20),
  isElective: yup.boolean().default(false),
});
type FormValues = yup.InferType<typeof schema>;

export interface SubjectFormDialogProps {
  open: boolean;
  editing: Subject | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { subjectName: string; subjectCode: string; isElective: boolean }) => void;
}

/** Add/edit dialog for a subject within a class. */
export function SubjectFormDialog({ open, editing, saving, onClose, onSubmit }: SubjectFormDialogProps) {
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { subjectName: '', subjectCode: '', isElective: false },
  });

  useEffect(() => {
    if (open) {
      reset({
        subjectName: editing?.subjectName ?? '',
        subjectCode: editing?.subjectCode ?? '',
        isElective: editing?.isElective ?? false,
      });
    }
  }, [open, editing, reset]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Subject' : 'Add Subject'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                label="Subject Name"
                fullWidth
                autoFocus
                {...register('subjectName')}
                error={!!errors.subjectName}
                helperText={errors.subjectName?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Subject Code"
                fullWidth
                {...register('subjectCode')}
                error={!!errors.subjectCode}
                helperText={errors.subjectCode?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="isElective"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={<Checkbox checked={!!field.value} onChange={(e) => field.onChange(e.target.checked)} />}
                    label="This is an elective subject"
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

export default SubjectFormDialog;
