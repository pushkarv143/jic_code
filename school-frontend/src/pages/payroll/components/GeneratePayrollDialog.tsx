import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import CircularProgress from '@mui/material/CircularProgress';
import dayjs from 'dayjs';
import type { PayrollEmployeeType } from '@/types';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export interface GeneratePayrollDialogProps {
  open: boolean;
  generating: boolean;
  onClose: () => void;
  onGenerate: (values: { employeeType: PayrollEmployeeType; month: number; year: number }) => void;
}

/** "Generate Payroll" workflow: pick employee type + month + year, then POST /payroll/generate. */
export function GeneratePayrollDialog({ open, generating, onClose, onGenerate }: GeneratePayrollDialogProps) {
  const [employeeType, setEmployeeType] = useState<PayrollEmployeeType>('TEACHER');
  const [month, setMonth] = useState(dayjs().month() + 1);
  const [year, setYear] = useState(dayjs().year());

  useEffect(() => {
    if (open) {
      setEmployeeType('TEACHER');
      setMonth(dayjs().month() + 1);
      setYear(dayjs().year());
    }
  }, [open]);

  const years = Array.from({ length: 5 }, (_, i) => dayjs().year() - 3 + i);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Generate Payroll</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.5 }}>
          <Grid item xs={12}>
            <TextField
              select
              fullWidth
              label="Employee Type"
              value={employeeType}
              onChange={(e) => setEmployeeType(e.target.value as PayrollEmployeeType)}
            >
              <MenuItem value="TEACHER">Teacher</MenuItem>
              <MenuItem value="STAFF">Staff</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={6}>
            <TextField select fullWidth label="Month" value={month} onChange={(e) => setMonth(Number(e.target.value))}>
              {MONTHS.map((m, idx) => (
                <MenuItem key={m} value={idx + 1}>
                  {m}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={6}>
            <TextField select fullWidth label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {years.map((y) => (
                <MenuItem key={y} value={y}>
                  {y}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={generating}>
          Cancel
        </Button>
        <Button
          variant="contained"
          disabled={generating}
          startIcon={generating ? <CircularProgress size={16} color="inherit" /> : undefined}
          onClick={() => onGenerate({ employeeType, month, year })}
        >
          Generate
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default GeneratePayrollDialog;
