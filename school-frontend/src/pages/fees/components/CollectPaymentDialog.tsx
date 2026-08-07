import { useEffect } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import DialogContentText from '@mui/material/DialogContentText';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import * as yup from 'yup';
import dayjs from 'dayjs';
import type { PaymentMode, StudentFee } from '@/types';
import { formatCurrencyINR } from '@/utils/format';

const PAYMENT_MODES: PaymentMode[] = ['CASH', 'ONLINE', 'CHEQUE', 'CARD'];
const TRANSACTION_ID_MODES: PaymentMode[] = ['ONLINE', 'CARD', 'CHEQUE'];

const schema = yup.object({
  amount: yup
    .string()
    .required('Amount is required')
    .test('num', 'Enter a valid amount', (value) => !!value && /^\d+(\.\d{1,2})?$/.test(value) && Number(value) > 0),
  paymentDate: yup
    .mixed<dayjs.Dayjs>()
    .required('Payment date is required')
    .test('is-valid', 'Enter a valid date', (value) => !!value && dayjs(value).isValid()),
  paymentMode: yup.mixed<PaymentMode>().oneOf(PAYMENT_MODES).required('Payment mode is required'),
  transactionId: yup.string().when('paymentMode', {
    is: (mode: PaymentMode) => TRANSACTION_ID_MODES.includes(mode),
    then: (s) => s.required('Transaction / reference ID is required for this payment mode'),
    otherwise: (s) => s.optional(),
  }),
});
type FormValues = yup.InferType<typeof schema>;

export interface CollectPaymentDialogProps {
  open: boolean;
  studentFee: StudentFee | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { amount: number; paymentDate: string; paymentMode: PaymentMode; transactionId?: string }) => void;
}

/** Collect-payment dialog: amount pre-filled with the remaining balance, mode select, date, conditional transaction id. */
export function CollectPaymentDialog({ open, studentFee, saving, onClose, onSubmit }: CollectPaymentDialogProps) {
  const balance = studentFee ? Math.max(studentFee.amountDue - studentFee.amountPaid, 0) : 0;

  const {
    control,
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: yupResolver(schema),
    defaultValues: { amount: '', paymentDate: dayjs(), paymentMode: 'CASH', transactionId: '' },
  });

  const paymentMode = watch('paymentMode');

  useEffect(() => {
    if (open) {
      reset({ amount: balance ? String(balance) : '', paymentDate: dayjs(), paymentMode: 'CASH', transactionId: '' });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, studentFee]);

  const submit = (values: FormValues) => {
    onSubmit({
      amount: Number(values.amount),
      paymentDate: dayjs(values.paymentDate).format('YYYY-MM-DD'),
      paymentMode: values.paymentMode,
      transactionId: values.transactionId || undefined,
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Collect Payment</DialogTitle>
      <Box component="form" onSubmit={handleSubmit(submit)} noValidate>
        <DialogContent>
          {studentFee && (
            <DialogContentText sx={{ mb: 2 }}>
              {studentFee.feeCategoryName ?? 'Fee'} for {studentFee.studentName ?? `Student #${studentFee.studentId}`} — balance{' '}
              <strong>{formatCurrencyINR(balance)}</strong>
            </DialogContentText>
          )}
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Amount (INR)"
                fullWidth
                {...register('amount')}
                error={!!errors.amount}
                helperText={errors.amount?.message}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="paymentDate"
                control={control}
                render={({ field }) => (
                  <DatePicker
                    label="Payment Date"
                    value={field.value ?? null}
                    onChange={(value) => field.onChange(value)}
                    disableFuture
                    slotProps={{ textField: { fullWidth: true, error: !!errors.paymentDate, helperText: errors.paymentDate?.message } }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                select
                label="Payment Mode"
                fullWidth
                defaultValue="CASH"
                {...register('paymentMode')}
                error={!!errors.paymentMode}
                helperText={errors.paymentMode?.message}
              >
                {PAYMENT_MODES.map((m) => (
                  <MenuItem key={m} value={m}>
                    {m.charAt(0) + m.slice(1).toLowerCase()}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            {TRANSACTION_ID_MODES.includes(paymentMode) && (
              <Grid item xs={12}>
                <TextField
                  label="Transaction / Reference ID"
                  fullWidth
                  {...register('transactionId')}
                  error={!!errors.transactionId}
                  helperText={errors.transactionId?.message}
                />
              </Grid>
            )}
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
            Collect Payment
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default CollectPaymentDialog;
