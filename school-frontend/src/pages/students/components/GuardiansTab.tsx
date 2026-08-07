import { useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { useSnackbar } from 'notistack';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import studentsApi, { type GuardianPayload } from '@/api/studentsApi';
import type { Guardian } from '@/types';
import { guardianSchema, type GuardianFormValues } from '../StudentFormPage.schema';

const RELATION_OPTIONS = ['Father', 'Mother', 'Guardian', 'Grandfather', 'Grandmother', 'Uncle', 'Aunt', 'Other'];

export interface GuardiansTabProps {
  studentId: number;
  guardians: Guardian[];
  onChanged: (guardians: Guardian[]) => void;
}

/** Guardians tab of the student profile: list + add/edit/delete, each mutation calling the guardian endpoints directly. */
export function GuardiansTab({ studentId, guardians, onChanged }: GuardiansTabProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Guardian | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Guardian | null>(null);
  const [saving, setSaving] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<GuardianFormValues>({
    resolver: yupResolver(guardianSchema) as any,
    defaultValues: { name: '', relation: '', occupation: '', phone: '', email: '', address: '', isPrimary: false },
  });

  const openAdd = () => {
    setEditing(null);
    reset({ name: '', relation: '', occupation: '', phone: '', email: '', address: '', isPrimary: guardians.length === 0 });
    setDialogOpen(true);
  };

  const openEdit = (g: Guardian) => {
    setEditing(g);
    reset({
      name: g.name,
      relation: g.relation,
      occupation: g.occupation ?? '',
      phone: g.phone,
      email: g.email ?? '',
      address: g.address ?? '',
      isPrimary: g.isPrimary,
    });
    setDialogOpen(true);
  };

  const onSubmit = async (values: GuardianFormValues) => {
    setSaving(true);
    const payload: GuardianPayload = {
      name: values.name,
      relation: values.relation,
      occupation: values.occupation || undefined,
      phone: values.phone,
      email: values.email || undefined,
      address: values.address || undefined,
      isPrimary: !!values.isPrimary,
    };
    try {
      let next: Guardian[];
      if (editing) {
        const res = await studentsApi.updateGuardian(studentId, editing.id, payload);
        next = guardians.map((g) => (g.id === editing.id ? res.data : g));
      } else {
        const res = await studentsApi.addGuardian(studentId, payload);
        next = [...guardians, res.data];
      }
      // Backend should enforce single-primary, but keep the client list consistent either way.
      if (payload.isPrimary) {
        next = next.map((g) => ({ ...g, isPrimary: g.id === (editing?.id ?? next[next.length - 1].id) }));
      }
      onChanged(next);
      enqueueSnackbar(`Guardian ${editing ? 'updated' : 'added'}.`, { variant: 'success' });
      setDialogOpen(false);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this guardian.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await studentsApi.removeGuardian(studentId, deleteTarget.id);
      onChanged(guardians.filter((g) => g.id !== deleteTarget.id));
      enqueueSnackbar('Guardian removed.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not remove this guardian.', { variant: 'error' });
    } finally {
      setDeleteTarget(null);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button size="small" variant="outlined" startIcon={<AddOutlinedIcon />} onClick={openAdd}>
          Add Guardian
        </Button>
      </Stack>

      {guardians.length === 0 ? (
        <EmptyState title="No guardians on file" description="Add a parent or guardian for this student." />
      ) : (
        <Grid container spacing={2}>
          {guardians.map((g) => (
            <Grid item xs={12} sm={6} md={4} key={g.id}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                    <Box>
                      <Typography variant="subtitle2" fontWeight={700}>
                        {g.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {g.relation}
                      </Typography>
                    </Box>
                    {g.isPrimary && <Chip label="Primary" size="small" color="primary" />}
                  </Stack>
                  <Stack spacing={0.5} sx={{ mt: 1.5 }}>
                    <Typography variant="body2">{g.phone}</Typography>
                    {g.email && <Typography variant="body2">{g.email}</Typography>}
                    {g.occupation && (
                      <Typography variant="body2" color="text.secondary">
                        {g.occupation}
                      </Typography>
                    )}
                    {g.address && (
                      <Typography variant="body2" color="text.secondary">
                        {g.address}
                      </Typography>
                    )}
                  </Stack>
                  <Stack direction="row" spacing={0.5} sx={{ mt: 1.5 }} justifyContent="flex-end">
                    <IconButton size="small" onClick={() => openEdit(g)}>
                      <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={() => setDeleteTarget(g)}>
                      <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                    </IconButton>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Edit Guardian' : 'Add Guardian'}</DialogTitle>
        <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <DialogContent dividers>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Name"
                  fullWidth
                  {...register('name')}
                  error={!!errors.name}
                  helperText={errors.name?.message}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  select
                  label="Relation"
                  fullWidth
                  defaultValue={editing?.relation ?? ''}
                  {...register('relation')}
                  error={!!errors.relation}
                  helperText={errors.relation?.message}
                >
                  <MenuItem value="">Select</MenuItem>
                  {RELATION_OPTIONS.map((r) => (
                    <MenuItem key={r} value={r}>
                      {r}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Phone"
                  fullWidth
                  {...register('phone')}
                  error={!!errors.phone}
                  helperText={errors.phone?.message}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Email"
                  fullWidth
                  {...register('email')}
                  error={!!errors.email}
                  helperText={errors.email?.message}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField label="Occupation" fullWidth {...register('occupation')} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControlLabel control={<Checkbox {...register('isPrimary')} defaultChecked={editing?.isPrimary} />} label="Mark as primary guardian" />
              </Grid>
              <Grid item xs={12}>
                <TextField label="Address" fullWidth multiline minRows={2} {...register('address')} />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={() => setDialogOpen(false)} color="inherit" disabled={saving}>
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

      <ConfirmDialog
        open={!!deleteTarget}
        title="Remove guardian"
        message={`Remove ${deleteTarget?.name ?? ''} as a guardian for this student?`}
        confirmLabel="Remove"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

export default GuardiansTab;
