import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import dayjs from 'dayjs';
import StatusChip from '@/components/common/StatusChip';
import type { AdmissionEnquiry } from '@/types';

export interface EnquiryDetailDialogProps {
  open: boolean;
  enquiry: AdmissionEnquiry | null;
  onClose: () => void;
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <Grid item xs={12} sm={6}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2" fontWeight={600}>
        {value}
      </Typography>
    </Grid>
  );
}

/** Read-only detail dialog showing the full admission_enquiries record. */
export function EnquiryDetailDialog({ open, enquiry, onClose }: EnquiryDetailDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Admission Enquiry Details</DialogTitle>
      <DialogContent dividers>
        {enquiry && (
          <>
            <Grid container spacing={2} sx={{ mb: 1 }}>
              <Field label="Student Name" value={enquiry.studentName} />
              <Field label="Class Applying For" value={enquiry.classApplying} />
              <Field label="Parent / Guardian Name" value={enquiry.parentName} />
              <Field label="Date of Birth" value={dayjs(enquiry.dob).format('DD MMM YYYY')} />
              <Field label="Phone" value={enquiry.phone} />
              <Field label="Email" value={enquiry.email} />
            </Grid>
            <Divider sx={{ my: 1.5 }} />
            <Typography variant="caption" color="text.secondary">
              Address
            </Typography>
            <Typography variant="body2" sx={{ mb: 1.5 }}>
              {enquiry.address}
            </Typography>
            <Divider sx={{ my: 1.5 }} />
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Status
                </Typography>
                <StatusChip status={enquiry.status} />
              </Grid>
              <Grid item xs={6}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Applied On
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {dayjs(enquiry.appliedAt).format('DD MMM YYYY, hh:mm A')}
                </Typography>
              </Grid>
            </Grid>
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

export default EnquiryDetailDialog;
