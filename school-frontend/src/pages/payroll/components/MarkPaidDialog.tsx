import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { type Dayjs } from 'dayjs';
import type { PayrollRun } from '@/types';

export interface MarkPaidDialogProps {
  open: boolean;
  payrollRow: PayrollRun | null;
  saving: boolean;
  onClose: () => void;
  onConfirm: (paymentDate: string) => void;
}

/** Small confirmation dialog to record the payment date when marking a payroll row PAID. */
export function MarkPaidDialog({ open, payrollRow, saving, onClose, onConfirm }: MarkPaidDialogProps) {
  const [paymentDate, setPaymentDate] = useState<Dayjs>(dayjs());

  useEffect(() => {
    if (open) setPaymentDate(dayjs());
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Mark Payroll Paid</DialogTitle>
      <DialogContent>
        <Box sx={{ mb: 1.5, mt: 0.5 }}>
          {payrollRow && (
            <Typography variant="body2">
              {payrollRow.employeeName ?? `Employee #${payrollRow.employeeId}`} —{' '}
              {dayjs(`${payrollRow.year}-${payrollRow.month}-01`).format('MMMM YYYY')}
            </Typography>
          )}
        </Box>
        <DatePicker
          label="Payment Date"
          value={paymentDate}
          onChange={(value) => value && setPaymentDate(value)}
          slotProps={{ textField: { fullWidth: true } }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={saving}>
          Cancel
        </Button>
        <Button
          variant="contained"
          disabled={saving}
          startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          onClick={() => onConfirm(paymentDate.format('YYYY-MM-DD'))}
        >
          Mark Paid
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default MarkPaidDialog;
