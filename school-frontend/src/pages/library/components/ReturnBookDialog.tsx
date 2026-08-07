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
import type { BookIssue } from '@/types';

export interface ReturnBookDialogProps {
  open: boolean;
  issue: BookIssue | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (returnDate: string) => void;
}

/** Confirms a book return with a return-date picker. Fine (if any) is shown by the caller after the API call resolves. */
export function ReturnBookDialog({ open, issue, saving, onClose, onSubmit }: ReturnBookDialogProps) {
  const { control, handleSubmit, reset } = useForm<{ returnDate: dayjs.Dayjs | null }>({
    defaultValues: { returnDate: dayjs() },
  });

  useEffect(() => {
    if (open) reset({ returnDate: dayjs() });
  }, [open, reset]);

  const submit = (values: { returnDate: dayjs.Dayjs | null }) => {
    onSubmit(dayjs(values.returnDate ?? dayjs()).format('YYYY-MM-DD'));
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Return Book</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          {issue && (
            <DialogContentText sx={{ mb: 2 }}>
              {issue.bookTitle ?? `Book #${issue.bookId}`} — issued to{' '}
              {issue.studentName ?? issue.teacherName ?? 'borrower'} on {dayjs(issue.issueDate).format('DD MMM YYYY')}, due{' '}
              {dayjs(issue.dueDate).format('DD MMM YYYY')}.
            </DialogContentText>
          )}
          <Controller
            name="returnDate"
            control={control}
            render={({ field }) => (
              <DatePicker
                label="Return Date"
                value={field.value}
                onChange={(value) => field.onChange(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true } }}
              />
            )}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Confirm Return
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default ReturnBookDialog;
