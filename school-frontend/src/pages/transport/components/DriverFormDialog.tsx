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
import type { Driver } from '@/types';
import type { DriverPayload } from '@/api/transportApi';

const schema = yup.object({
  name: yup.string().required('Name is required').max(100),
  phone: yup.string().required('Phone is required').max(20),
  licenseNumber: yup.string().required('License number is required').max(30),
  address: yup.string().max(250).optional(),
});
type FormValues = yup.InferType<typeof schema>;

export interface DriverFormDialogProps {
  open: boolean;
  editing: Driver | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: DriverPayload) => void;
}

/** Add/edit dialog for a driver. */
export function DriverFormDialog({ open, editing, saving, onClose, onSubmit }: DriverFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: yupResolver(schema), defaultValues: { name: '', phone: '', licenseNumber: '', address: '' } });

  useEffect(() => {
    if (open) {
      reset({
        name: editing?.name ?? '',
        phone: editing?.phone ?? '',
        licenseNumber: editing?.licenseNumber ?? '',
        address: editing?.address ?? '',
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({ name: values.name, phone: values.phone, licenseNumber: values.licenseNumber, address: values.address || undefined });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Driver' : 'Add Driver'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField label="Name" fullWidth autoFocus {...register('name')} error={!!errors.name} helperText={errors.name?.message} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Phone" fullWidth {...register('phone')} error={!!errors.phone} helperText={errors.phone?.message} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="License Number"
                fullWidth
                {...register('licenseNumber')}
                error={!!errors.licenseNumber}
                helperText={errors.licenseNumber?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Address" fullWidth multiline minRows={2} {...register('address')} />
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

export default DriverFormDialog;
