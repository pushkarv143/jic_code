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
import { Controller, useForm } from 'react-hook-form';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { type Dayjs } from 'dayjs';
import type { CalendarEvent, CalendarEventType } from '@/types';
import type { EventPayload } from '@/api/eventsApi';

const EVENT_TYPES: CalendarEventType[] = ['HOLIDAY', 'EVENT', 'EXAM', 'OTHER'];

export interface EventFormDialogProps {
  open: boolean;
  editing: CalendarEvent | null;
  defaultDate?: Dayjs | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: EventPayload) => void;
}

interface FormValues {
  title: string;
  description: string;
  eventDate: Dayjs | null;
  eventType: CalendarEventType;
}

/** Add/edit dialog for a calendar event/holiday: title, description, date, type. */
export function EventFormDialog({ open, editing, defaultDate, saving, onClose, onSubmit }: EventFormDialogProps) {
  const { control, register, handleSubmit, reset } = useForm<FormValues>({
    defaultValues: { title: '', description: '', eventDate: dayjs(), eventType: 'EVENT' },
  });

  useEffect(() => {
    if (open) {
      reset({
        title: editing?.title ?? '',
        description: editing?.description ?? '',
        eventDate: editing ? dayjs(editing.eventDate) : defaultDate ?? dayjs(),
        eventType: editing?.eventType ?? 'EVENT',
      });
    }
  }, [open, editing, defaultDate, reset]);

  const submit = (values: FormValues) => {
    if (!values.title.trim() || !values.eventDate) return;
    onSubmit({
      title: values.title.trim(),
      description: values.description.trim() || undefined,
      eventDate: dayjs(values.eventDate).format('YYYY-MM-DD'),
      eventType: values.eventType,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{editing ? 'Edit Event' : 'Add Event'}</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField label="Title" fullWidth autoFocus {...register('title', { required: true })} />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Description (optional)" fullWidth multiline minRows={2} {...register('description')} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="eventDate"
                control={control}
                render={({ field }) => (
                  <DatePicker label="Date" value={field.value} onChange={(value) => field.onChange(value)} slotProps={{ textField: { fullWidth: true } }} />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="eventType"
                control={control}
                render={({ field }) => (
                  <TextField select label="Type" fullWidth value={field.value} onChange={(e) => field.onChange(e.target.value as CalendarEventType)}>
                    {EVENT_TYPES.map((t) => (
                      <MenuItem key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</MenuItem>
                    ))}
                  </TextField>
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

export default EventFormDialog;
