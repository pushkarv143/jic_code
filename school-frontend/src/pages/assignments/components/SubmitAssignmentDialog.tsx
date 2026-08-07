import { useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import type { Assignment } from '@/types';

export interface SubmitAssignmentDialogProps {
  open: boolean;
  assignment: Assignment | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (file: File) => void;
}

/** Student's assignment submission dialog: pick a single file, submit via multipart POST. */
export function SubmitAssignmentDialog({ open, assignment, saving, onClose, onSubmit }: SubmitAssignmentDialogProps) {
  const [file, setFile] = useState<File | null>(null);

  const handleClose = () => {
    setFile(null);
    onClose();
  };

  const submit = () => {
    if (!file) return;
    onSubmit(file);
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
      <DialogTitle>Submit Assignment</DialogTitle>
      <DialogContent>
        {assignment && (
          <DialogContentText sx={{ mb: 2 }}>
            {assignment.title} — due {assignment.dueDate}
          </DialogContentText>
        )}
        <Box>
          <Button component="label" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
            {file ? 'Change File' : 'Choose File'}
            <input hidden type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          </Button>
          {file && <Chip size="small" label={file.name} onDelete={() => setFile(null)} sx={{ ml: 1.5 }} />}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleClose} color="inherit" disabled={saving}>
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={saving || !file}
          startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          Submit
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default SubmitAssignmentDialog;
