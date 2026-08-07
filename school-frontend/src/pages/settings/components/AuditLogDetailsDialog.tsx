import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import dayjs from 'dayjs';
import type { AuditLog } from '@/types';

export interface AuditLogDetailsDialogProps {
  open: boolean;
  log: AuditLog | null;
  onClose: () => void;
}

function prettyPrint(value: string | null): string {
  if (!value) return '—';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

/** Shows the before/after JSON payload for a single audit_logs row. */
export function AuditLogDetailsDialog({ open, log, onClose }: AuditLogDetailsDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Audit Log Details</DialogTitle>
      <DialogContent dividers>
        {log && (
          <>
            <Grid container spacing={1.5} sx={{ mb: 2 }}>
              <Grid item xs={6} sm={3}>
                <Typography variant="caption" color="text.secondary">
                  Timestamp
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {dayjs(log.createdAt).format('DD MMM YYYY, hh:mm:ss A')}
                </Typography>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Typography variant="caption" color="text.secondary">
                  User
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {log.userName ?? (log.userId ? `#${log.userId}` : 'System')}
                </Typography>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Typography variant="caption" color="text.secondary">
                  Action
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {log.action}
                </Typography>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Typography variant="caption" color="text.secondary">
                  Entity
                </Typography>
                <Typography variant="body2" fontWeight={600}>
                  {log.entityName}
                  {log.entityId != null ? ` #${log.entityId}` : ''}
                </Typography>
              </Grid>
            </Grid>

            <Divider sx={{ mb: 2 }} />

            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Old Value
                </Typography>
                <Box
                  component="pre"
                  sx={{
                    m: 0,
                    p: 1.5,
                    borderRadius: 1.5,
                    bgcolor: 'action.hover',
                    fontSize: '0.75rem',
                    maxHeight: 320,
                    overflow: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                  }}
                >
                  {prettyPrint(log.oldValue)}
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  New Value
                </Typography>
                <Box
                  component="pre"
                  sx={{
                    m: 0,
                    p: 1.5,
                    borderRadius: 1.5,
                    bgcolor: 'action.hover',
                    fontSize: '0.75rem',
                    maxHeight: 320,
                    overflow: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                  }}
                >
                  {prettyPrint(log.newValue)}
                </Box>
              </Grid>
            </Grid>
          </>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button variant="contained" onClick={onClose}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default AuditLogDetailsDialog;
