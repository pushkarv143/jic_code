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
import { yupResolver } from '@hookform/resolvers/yup';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import { leaveApplicationSchema, LEAVE_TYPE_OPTIONS, type LeaveApplicationFormValues } from '../leaveSchema';
import type { LeaveApplicationPayload } from '@/api/leaveApi';

export interface LeaveFormDialogProps {
  open: boolean;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: LeaveApplicationPayload) => void;
}

/** "Apply for Leave" dialog used by every role — leave type, date range, reason. */
export function LeaveFormDialog({ open, saving, onClose, onSubmit }: LeaveFormDialogProps) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<LeaveApplicationFormValues>({
    resolver: yupResolver(leaveApplicationSchema),
    defaultValues: { leaveType: '', startDate: undefined, endDate: undefined, reason: '' },
  });

  useEffect(() => {
    if (open) {
      reset({ leaveType: '', startDate: undefined, endDate: undefined, reason: '' });
    }
  }, [open, reset]);

  const submit = (values: LeaveApplicationFormValues) => {
    onSubmit({
      leaveType: values.leaveType,
      startDate: dayjs(values.startDate).format('YYYY-MM-DD'),
      endDate: dayjs(values.endDate).format('YYYY-MM-DD'),
      reason: values.reason,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Apply for Leave</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField
                select
                label="Leave Type"
                fullWidth
                defaultValue=""
                {...register('leaveType')}
                error={!!errors.leaveType}
                helperText={errors.leaveType?.message}
              >
                <MenuItem value="">Select a leave type</MenuItem>
                {LEAVE_TYPE_OPTIONS.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t.charAt(0) + t.slice(1).toLowerCase()}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="startDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Start Date"
                    value={field.value ?? null}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{
                      textField: {
                        fullWidth: true,
                        error: !!errors.startDate,
                        helperText: errors.startDate?.message,
                      },
                    }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="endDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="End Date"
                    value={field.value ?? null}
                    onChange={(value) => field.onChange(value)}
                    slotProps={{
                      textField: {
                        fullWidth: true,
                        error: !!errors.endDate,
                        helperText: errors.endDate?.message,
                      },
                    }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Reason"
                fullWidth
                multiline
                minRows={3}
                {...register('reason')}
                error={!!errors.reason}
                helperText={errors.reason?.message}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
            Submit
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default LeaveFormDialog;
