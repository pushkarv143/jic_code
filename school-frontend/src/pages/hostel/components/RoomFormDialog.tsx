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
import type { HostelRoom } from '@/types';
import type { HostelRoomPayload } from '@/api/hostelApi';

const schema = yup.object({
  roomNumber: yup.string().required('Room number is required').max(20),
  capacity: yup.number().typeError('Capacity is required').required('Capacity is required').min(1),
});
type FormValues = yup.InferType<typeof schema>;

export interface RoomFormDialogProps {
  open: boolean;
  editing: HostelRoom | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: HostelRoomPayload) => void;
}

/** Add/edit dialog for a hostel room (number, capacity). */
export function RoomFormDialog({ open, editing, saving, onClose, onSubmit }: RoomFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: yupResolver(schema), defaultValues: { roomNumber: '', capacity: 2 } });

  useEffect(() => {
    if (open) {
      reset({ roomNumber: editing?.roomNumber ?? '', capacity: editing?.capacity ?? 2 });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({ roomNumber: values.roomNumber, capacity: values.capacity });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Room' : 'Add Room'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                label="Room Number"
                fullWidth
                autoFocus
                {...register('roomNumber')}
                error={!!errors.roomNumber}
                helperText={errors.roomNumber?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Capacity"
                type="number"
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
          <Button type="submit" variant="contained" disabled={saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
            Save
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default RoomFormDialog;
