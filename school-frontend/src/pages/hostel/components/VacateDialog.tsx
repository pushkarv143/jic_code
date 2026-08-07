import { useEffect } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import type { HostelStudent } from '@/types';

export interface VacateDialogProps {
  open: boolean;
  resident: HostelStudent | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (vacateDate: string) => void;
}

/** Confirms a room vacate with a vacate-date picker. */
export function VacateDialog({ open, resident, saving, onClose, onSubmit }: VacateDialogProps) {
  const { control, handleSubmit, reset } = useForm<{ vacateDate: dayjs.Dayjs | null }>({
    defaultValues: { vacateDate: dayjs() },
  });

  useEffect(() => {
    if (open) reset({ vacateDate: dayjs() });
  }, [open, reset]);

  const submit = (values: { vacateDate: dayjs.Dayjs | null }) => {
    onSubmit(dayjs(values.vacateDate ?? dayjs()).format('YYYY-MM-DD'));
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Vacate Room</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          {resident && (
            <DialogContentText sx={{ mb: 2 }}>
              {resident.studentName ?? `Student #${resident.studentId}`} — Room {resident.roomNumber ?? `#${resident.roomId}`}.
            </DialogContentText>
          )}
          <Controller
            name="vacateDate"
            control={control}
            render={({ field }) => (
              <DatePicker
                label="Vacate Date"
                value={field.value}
                onChange={(value) => field.onChange(value)}
                slotProps={{ textField: { fullWidth: true } }}
              />
            )}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" color="warning" disabled={saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
            Confirm Vacate
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default VacateDialog;
