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
import type { Bus, Route } from '@/types';
import type { RoutePayload } from '@/api/transportApi';

const schema = yup.object({
  routeName: yup.string().required('Route name is required').max(100),
  busId: yup.number().typeError('Bus is required').required('Bus is required'),
  startPoint: yup.string().required('Start point is required').max(150),
  endPoint: yup.string().required('End point is required').max(150),
});
type FormValues = yup.InferType<typeof schema>;

export interface RouteFormDialogProps {
  open: boolean;
  editing: Route | null;
  buses: Bus[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: RoutePayload) => void;
}

/** Add/edit dialog for a route (name, assigned bus, start/end points). */
export function RouteFormDialog({ open, editing, buses, saving, onClose, onSubmit }: RouteFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { routeName: '', busId: undefined, startPoint: '', endPoint: '' },
  });

  useEffect(() => {
    if (open) {
      reset({
        routeName: editing?.routeName ?? '',
        busId: editing?.busId,
        startPoint: editing?.startPoint ?? '',
        endPoint: editing?.endPoint ?? '',
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({ routeName: values.routeName, busId: values.busId, startPoint: values.startPoint, endPoint: values.endPoint });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Route' : 'Add Route'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                label="Route Name"
                fullWidth
                autoFocus
                {...register('routeName')}
                error={!!errors.routeName}
                helperText={errors.routeName?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                select
                label="Bus"
                fullWidth
                defaultValue={editing?.busId ?? ''}
                {...register('busId')}
                error={!!errors.busId}
                helperText={errors.busId?.message}
              >
                <MenuItem value="">Select a bus</MenuItem>
                {buses.map((b) => (
                  <MenuItem key={b.id} value={b.id}>
                    {b.busNumber} ({b.registrationNumber})
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Start Point"
                fullWidth
                {...register('startPoint')}
                error={!!errors.startPoint}
                helperText={errors.startPoint?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="End Point"
                fullWidth
                {...register('endPoint')}
                error={!!errors.endPoint}
                helperText={errors.endPoint?.message}
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

export default RouteFormDialog;
