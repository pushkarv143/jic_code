import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import feesApi from '@/api/feesApi';
import EmptyState from '@/components/common/EmptyState';
import { formatCurrencyINR } from '@/utils/format';

export interface ViewPaymentsDialogProps {
  open: boolean;
  studentFeeId: number | null;
  onClose: () => void;
}

/** Shows the payment history (with receipt links) for one student_fees row, fetched on demand. */
export function ViewPaymentsDialog({ open, studentFeeId, onClose }: ViewPaymentsDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);
  const [payments, setPayments] = useState<Array<{ id: number; amount: number; paymentDate: string; paymentMode: string; receiptNumber: string }>>([]);

  useEffect(() => {
    if (!open || !studentFeeId) return;
    setLoading(true);
    feesApi.studentFees
      .getById(studentFeeId)
      .then((res) => setPayments(res.data.feePayments ?? []))
      .catch(() => enqueueSnackbar('Could not load payment history.', { variant: 'error' }))
      .finally(() => setLoading(false));
  }, [open, studentFeeId, enqueueSnackbar]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Payment History</DialogTitle>
      <DialogContent dividers>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        ) : payments.length === 0 ? (
          <EmptyState title="No payments yet" description="No payments have been recorded against this fee." />
        ) : (
          <List dense disablePadding>
            {payments.map((p, idx) => (
              <Box key={p.id}>
                {idx > 0 && <Divider component="li" />}
                <ListItem
                  secondaryAction={
                    <Button
                      size="small"
                      component={RouterLink}
                      to={`/app/fees/receipt/${p.id}`}
                      startIcon={<ReceiptLongOutlinedIcon fontSize="small" />}
                    >
                      Receipt
                    </Button>
                  }
                >
                  <ListItemText
                    primary={`${formatCurrencyINR(p.amount)} · ${p.paymentMode}`}
                    secondary={`${dayjs(p.paymentDate).format('DD MMM YYYY')} · Receipt ${p.receiptNumber}`}
                  />
                </ListItem>
              </Box>
            ))}
          </List>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

export default ViewPaymentsDialog;
