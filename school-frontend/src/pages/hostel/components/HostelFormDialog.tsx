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
import type { Hostel, HostelType } from '@/types';
import type { HostelPayload } from '@/api/hostelApi';

const HOSTEL_TYPES: HostelType[] = ['BOYS', 'GIRLS'];

const schema = yup.object({
  name: yup.string().required('Name is required').max(100),
  wardenName: yup.string().max(100).optional(),
  wardenContact: yup.string().max(20).optional(),
  type: yup.mixed<HostelType>().oneOf(HOSTEL_TYPES).required('Type is required'),
});
type FormValues = yup.InferType<typeof schema>;

export interface HostelFormDialogProps {
  open: boolean;
  editing: Hostel | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: HostelPayload) => void;
}

/** Add/edit dialog for a hostel block (name, warden details, boys/girls type). */
export function HostelFormDialog({ open, editing, saving, onClose, onSubmit }: HostelFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { name: '', wardenName: '', wardenContact: '', type: 'BOYS' },
  });

  useEffect(() => {
    if (open) {
      reset({
        name: editing?.name ?? '',
        wardenName: editing?.wardenName ?? '',
        wardenContact: editing?.wardenContact ?? '',
        type: editing?.type ?? 'BOYS',
      });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({
      name: values.name,
      wardenName: values.wardenName || undefined,
      wardenContact: values.wardenContact || undefined,
      type: values.type,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Hostel' : 'Add Hostel'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField label="Name" fullWidth autoFocus {...register('name')} error={!!errors.name} helperText={errors.name?.message} />
            </Grid>
            <Grid item xs={12}>
              <TextField
                select
                label="Type"
                fullWidth
                defaultValue={editing?.type ?? 'BOYS'}
                {...register('type')}
                error={!!errors.type}
                helperText={errors.type?.message}
              >
                <MenuItem value="BOYS">Boys</MenuItem>
                <MenuItem value="GIRLS">Girls</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Warden Name" fullWidth {...register('wardenName')} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField label="Warden Contact" fullWidth {...register('wardenContact')} />
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

export default HostelFormDialog;
