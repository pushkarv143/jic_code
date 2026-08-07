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
import { Controller, useForm } from 'react-hook-form';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import dayjs, { type Dayjs } from 'dayjs';
import type { PickupPoint } from '@/types';
import type { PickupPointPayload } from '@/api/transportApi';

interface FormValues {
  pointName: string;
  pickupTime: Dayjs | null;
  dropTime: Dayjs | null;
}

export interface PickupPointFormDialogProps {
  open: boolean;
  editing: PickupPoint | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: PickupPointPayload) => void;
}

/** Add/edit dialog for a route's pickup point (name, pickup time, drop time). */
export function PickupPointFormDialog({ open, editing, saving, onClose, onSubmit }: PickupPointFormDialogProps) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ defaultValues: { pointName: '', pickupTime: null, dropTime: null } });

  useEffect(() => {
    if (open) {
      reset({
        pointName: editing?.pointName ?? '',
        pickupTime: editing?.pickupTime ? dayjs(editing.pickupTime, 'HH:mm:ss') : null,
        dropTime: editing?.dropTime ? dayjs(editing.dropTime, 'HH:mm:ss') : null,
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    if (!values.pointName.trim() || !values.pickupTime || !values.dropTime) return;
    onSubmit({
      pointName: values.pointName.trim(),
      pickupTime: values.pickupTime.format('HH:mm:ss'),
      dropTime: values.dropTime.format('HH:mm:ss'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Pickup Point' : 'Add Pickup Point'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                label="Point Name"
                fullWidth
                autoFocus
                {...register('pointName', { required: true })}
                error={!!errors.pointName}
                helperText={errors.pointName ? 'Point name is required' : undefined}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="pickupTime"
                control={control}
                rules={{ required: true }}
                render={({ field }) => (
                  <TimePicker
                    label="Pickup Time"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true, error: !!errors.pickupTime } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="dropTime"
                control={control}
                rules={{ required: true }}
                render={({ field }) => (
                  <TimePicker
                    label="Drop Time"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true, error: !!errors.dropTime } }}
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

export default PickupPointFormDialog;
