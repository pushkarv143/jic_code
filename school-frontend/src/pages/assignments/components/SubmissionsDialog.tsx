import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Link from '@mui/material/Link';
import CircularProgress from '@mui/material/CircularProgress';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import StatusChip from '@/components/common/StatusChip';
import assignmentsApi from '@/api/assignmentsApi';
import type { Assignment, AssignmentSubmission } from '@/types';

export interface SubmissionsDialogProps {
  open: boolean;
  assignment: Assignment | null;
  onClose: () => void;
}

interface RowState {
  submission: AssignmentSubmission;
  marksObtained: string;
  feedback: string;
  saving: boolean;
}

/** Teacher grading view: list of students who submitted, with an inline marks/feedback form per row. */
export function SubmissionsDialog({ open, assignment, onClose }: SubmissionsDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [rows, setRows] = useState<RowState[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open || !assignment) return;
    setLoading(true);
    assignmentsApi
      .listSubmissions(assignment.id)
      .then((res) => {
        setRows(
          res.data.map((s) => ({
            submission: s,
            marksObtained: s.marksObtained !== null && s.marksObtained !== undefined ? String(s.marksObtained) : '',
            feedback: s.feedback ?? '',
            saving: false,
          })),
        );
      })
      .catch((err) => enqueueSnackbar(err?.response?.data?.message ?? 'Could not load submissions.', { variant: 'error' }))
      .finally(() => setLoading(false));
  }, [open, assignment, enqueueSnackbar]);

  const updateRow = (id: number, patch: Partial<RowState>) => {
    setRows((prev) => prev.map((r) => (r.submission.id === id ? { ...r, ...patch } : r)));
  };

  const handleGrade = async (row: RowState) => {
    if (row.marksObtained === '' || Number.isNaN(Number(row.marksObtained))) {
      enqueueSnackbar('Enter valid marks before saving.', { variant: 'warning' });
      return;
    }
    updateRow(row.submission.id, { saving: true });
    try {
      const res = await assignmentsApi.gradeSubmission(row.submission.id, {
        marksObtained: Number(row.marksObtained),
        feedback: row.feedback || undefined,
      });
      updateRow(row.submission.id, { submission: res.data, saving: false });
      enqueueSnackbar('Submission graded.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this grade.', { variant: 'error' });
      updateRow(row.submission.id, { saving: false });
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Submissions — {assignment?.title}</DialogTitle>
      <DialogContent dividers>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress size={28} />
          </Box>
        ) : rows.length === 0 ? (
          <EmptyState title="No submissions yet" description="No students have submitted this assignment yet." />
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Student</TableCell>
                <TableCell>Submitted</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>File</TableCell>
                <TableCell sx={{ minWidth: 90 }}>Marks</TableCell>
                <TableCell sx={{ minWidth: 180 }}>Feedback</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.submission.id} hover>
                  <TableCell>
                    {row.submission.studentName ?? `#${row.submission.studentId}`}
                    {row.submission.rollNumber ? ` (${row.submission.rollNumber})` : ''}
                  </TableCell>
                  <TableCell>{dayjs(row.submission.submittedAt).format('DD MMM YYYY, hh:mm A')}</TableCell>
                  <TableCell>
                    <StatusChip status={row.submission.status} />
                  </TableCell>
                  <TableCell>
                    <Link href={row.submission.fileUrl} target="_blank" rel="noopener noreferrer">
                      View
                    </Link>
                  </TableCell>
                  <TableCell>
                    <TextField
                      size="small"
                      type="number"
                      value={row.marksObtained}
                      onChange={(e) => updateRow(row.submission.id, { marksObtained: e.target.value })}
                      sx={{ width: 80 }}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      size="small"
                      fullWidth
                      placeholder="Feedback"
                      value={row.feedback}
                      onChange={(e) => updateRow(row.submission.id, { feedback: e.target.value })}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Save grade">
                      <span>
                        <IconButton size="small" color="primary" disabled={row.saving} onClick={() => handleGrade(row)}>
                          {row.saving ? <CircularProgress size={16} /> : <SaveOutlinedIcon fontSize="small" />}
                        </IconButton>
                      </span>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

export default SubmissionsDialog;
