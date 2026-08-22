import { useEffect, useState } from 'react';
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
import Chip from '@mui/material/Chip';
import { Controller, useForm } from 'react-hook-form';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import dayjs, { type Dayjs } from 'dayjs';
import type { Notice, Role } from '@/types';
import type { NoticePayload } from '@/api/noticesApi';
import { formatRoleLabel } from '@/utils/format';

const ROLES: Role[] = [
  'SUPER_ADMIN',
  'PRINCIPAL',
  'VICE_PRINCIPAL',
  'TEACHER',
  'ACCOUNTANT',
  'LIBRARIAN',
  'RECEPTIONIST',
  'STUDENT',
  'PARENT',
  'SECURITY_GUARD',
];

export interface NoticeFormDialogProps {
  open: boolean;
  editing: Notice | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: NoticePayload) => void;
}

interface FormValues {
  title: string;
  description: string;
  targetRole: Role | '';
  expiryDate: Dayjs | null;
}

/** Add/edit dialog for a notice: title/description, optional target role, optional expiry date, optional attachment (multipart). */
export function NoticeFormDialog({ open, editing, saving, onClose, onSubmit }: NoticeFormDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const { control, register, handleSubmit, reset } = useForm<FormValues>({
    defaultValues: { title: '', description: '', targetRole: '', expiryDate: null },
  });

  useEffect(() => {
    if (open) {
      reset({
        title: editing?.title ?? '',
        description: editing?.description ?? '',
        targetRole: editing?.targetRole ?? '',
        expiryDate: editing?.expiryDate ? dayjs(editing.expiryDate) : null,
      });
      setFile(null);
    }
  }, [open, editing, reset]);

  const submit = (values: FormValues) => {
    if (!values.title.trim() || !values.description.trim()) return;
    onSubmit({
      title: values.title.trim(),
      description: values.description.trim(),
      targetRole: values.targetRole || undefined,
      expiryDate: values.expiryDate ? dayjs(values.expiryDate).format('YYYY-MM-DD') : undefined,
      file,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Notice' : 'Add Notice'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField label="Title" fullWidth autoFocus {...register('title', { required: true })} />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Description" fullWidth multiline minRows={3} {...register('description', { required: true })} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="targetRole"
                control={control}
                render={({ field }) => (
                  <TextField select label="Target Role (optional)" fullWidth value={field.value} onChange={(e) => field.onChange(e.target.value as Role | '')}>
                    <MenuItem value="">Everyone</MenuItem>
                    {ROLES.map((r) => (
                      <MenuItem key={r} value={r}>{formatRoleLabel(r)}</MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="expiryDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Expiry Date (optional)"
                    value={field.value}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{ textField: { fullWidth: true } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Button component="label" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
                {file ? 'Change Attachment' : 'Attach File (optional)'}
                <input hidden type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
              </Button>
              {file && <Chip size="small" label={file.name} onDelete={() => setFile(null)} sx={{ ml: 1.5 }} />}
              {!file && editing?.attachmentUrl && (
                <Chip size="small" variant="outlined" label="Existing attachment kept unless replaced" sx={{ ml: 1.5 }} />
              )}
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

export default NoticeFormDialog;
