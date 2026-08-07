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
import type { FeeCategory } from '@/types';

const schema = yup.object({
  name: yup.string().required('Name is required').max(100),
  description: yup.string().max(300).optional(),
});
type FormValues = yup.InferType<typeof schema>;

export interface FeeCategoryFormDialogProps {
  open: boolean;
  editing: FeeCategory | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { name: string; description?: string }) => void;
}

/** Add/edit dialog for a fee category (name + description, matching the LookupManagerDialog pattern). */
export function FeeCategoryFormDialog({ open, editing, saving, onClose, onSubmit }: FeeCategoryFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: yupResolver(schema), defaultValues: { name: '', description: '' } });

  useEffect(() => {
    if (open) {
      reset({ name: editing?.name ?? '', description: editing?.description ?? '' });
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    onSubmit({ name: values.name, description: values.description || undefined });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Fee Category' : 'Add Fee Category'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                label="Name"
                fullWidth
                autoFocus
                {...register('name')}
                error={!!errors.name}
                helperText={errors.name?.message}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Description" fullWidth multiline minRows={2} {...register('description')} />
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

export default FeeCategoryFormDialog;
