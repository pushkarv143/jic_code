import { useEffect } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import type { Section } from '@/types';

const schema = yup.object({
  sectionName: yup.string().required('Section name is required').max(20),
  roomNumber: yup.string().max(20).optional(),
  capacity: yup
    .string()
    .optional()
    .test('num', 'Enter a valid capacity', (value) => !value || /^[0-9]{1,4}$/.test(value)),
});
type FormValues = yup.InferType<typeof schema>;

export interface SectionFormDialogProps {
  open: boolean;
  editing: Section | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { sectionName: string; roomNumber?: string; capacity?: number }) => void;
}

/**
 * Edits the room and capacity of a class's single section.
 *
 * <p>The section name is not shown: every class has exactly one section, named
 * "A", and the backend refuses anything else. It is still submitted, because the
 * update endpoint takes the whole section, so it is carried through from the
 * record being edited rather than typed in.
 */
export function SectionFormDialog({ open, editing, saving, onClose, onSubmit }: SectionFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: yupResolver(schema), defaultValues: { sectionName: '', roomNumber: '', capacity: '' } });

  useEffect(() => {
    if (open) {
      reset({
        sectionName: editing?.sectionName ?? '',
        roomNumber: editing?.roomNumber ?? '',
        capacity: editing?.capacity != null ? String(editing.capacity) : '',
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({
      sectionName: values.sectionName,
      roomNumber: values.roomNumber || undefined,
      capacity: values.capacity ? Number(values.capacity) : undefined,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Edit Room &amp; Capacity</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={6}>
              <TextField label="Room Number" fullWidth autoFocus {...register('roomNumber')} />
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Capacity"
                fullWidth
                {...register('capacity')}
                error={!!errors.capacity}
                helperText={errors.capacity?.message}
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

export default SectionFormDialog;
