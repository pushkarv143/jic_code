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
import type { Bus, Driver } from '@/types';
import type { BusPayload } from '@/api/transportApi';

const schema = yup.object({
  busNumber: yup.string().required('Bus number is required').max(30),
  capacity: yup.number().typeError('Capacity is required').required('Capacity is required').min(1),
  driverId: yup.number().nullable().optional(),
  vehicleModel: yup.string().max(80).optional(),
  registrationNumber: yup.string().required('Registration number is required').max(30),
});
type FormValues = yup.InferType<typeof schema>;

export interface BusFormDialogProps {
  open: boolean;
  editing: Bus | null;
  drivers: Driver[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: BusPayload) => void;
}

/** Add/edit dialog for a bus (number, capacity, assigned driver, model, registration). */
export function BusFormDialog({ open, editing, drivers, saving, onClose, onSubmit }: BusFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { busNumber: '', capacity: 40, driverId: null, vehicleModel: '', registrationNumber: '' },
  });

  useEffect(() => {
    if (open) {
      reset({
        busNumber: editing?.busNumber ?? '',
        capacity: editing?.capacity ?? 40,
        driverId: editing?.driverId ?? null,
        vehicleModel: editing?.vehicleModel ?? '',
        registrationNumber: editing?.registrationNumber ?? '',
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({
      busNumber: values.busNumber,
      capacity: values.capacity,
      driverId: values.driverId || null,
      vehicleModel: values.vehicleModel || undefined,
      registrationNumber: values.registrationNumber,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Bus' : 'Add Bus'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Bus Number"
                fullWidth
                autoFocus
                {...register('busNumber')}
                error={!!errors.busNumber}
                helperText={errors.busNumber?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Registration Number"
                fullWidth
                {...register('registrationNumber')}
                error={!!errors.registrationNumber}
                helperText={errors.registrationNumber?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Capacity"
                type="number"
                fullWidth
                {...register('capacity')}
                error={!!errors.capacity}
                helperText={errors.capacity?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Vehicle Model" fullWidth {...register('vehicleModel')} />
            </Grid>
            <Grid item xs={12}>
              <TextField select label="Driver" fullWidth defaultValue={editing?.driverId ?? ''} {...register('driverId')}>
                <MenuItem value="">Unassigned</MenuItem>
                {drivers.map((d) => (
                  <MenuItem key={d.id} value={d.id}>
                    {d.name} ({d.licenseNumber})
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

export default BusFormDialog;
