import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';
import dayjs from 'dayjs';
import studentsApi from '@/api/studentsApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { getStudentDisplayName } from '@/utils/format';
import type { Student } from '@/types';
import type { HostelFeePayload } from '@/api/hostelApi';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export interface HostelFeeFormDialogProps {
  open: boolean;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: HostelFeePayload) => void;
}

/** "Add Charge" form: student autocomplete, month/year, amount. */
export function HostelFeeFormDialog({ open, saving, onClose, onSubmit }: HostelFeeFormDialogProps) {
  const [studentQuery, setStudentQuery] = useState('');
  const debouncedQuery = useDebouncedValue(studentQuery, 400);
  const [studentOptions, setStudentOptions] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [month, setMonth] = useState(dayjs().month() + 1);
  const [year, setYear] = useState(dayjs().year());
  const [amount, setAmount] = useState('');

  useEffect(() => {
    if (open) {
      setSelectedStudent(null);
      setStudentQuery('');
      setMonth(dayjs().month() + 1);
      setYear(dayjs().year());
      setAmount('');
    }
  }, [open]);

  useEffect(() => {
    if (!debouncedQuery) {
      setStudentOptions([]);
      return;
    }
    studentsApi
      .list({ search: debouncedQuery, size: 20 })
      .then((res) => setStudentOptions(res.data.content))
      .catch(() => undefined);
  }, [debouncedQuery]);

  const canSubmit = !!selectedStudent && !!amount && Number(amount) > 0;

  const submit = () => {
    if (!canSubmit || !selectedStudent) return;
    onSubmit({ studentId: selectedStudent.id, month, year, amount: Number(amount) });
  };

  const years = Array.from({ length: 5 }, (_, i) => dayjs().year() - 1 + i);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add Hostel Fee Charge</DialogTitle>
      <Box component="form" onSubmit={(e) => { e.preventDefault(); submit(); }} noValidate>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <Autocomplete
                options={studentOptions}
                value={selectedStudent}
                getOptionLabel={(o) => `${getStudentDisplayName(o)} (${o.admissionNumber})`}
                isOptionEqualToValue={(o, v) => o.id === v.id}
                onChange={(_e, value) => setSelectedStudent(value)}
                onInputChange={(_e, value) => setStudentQuery(value)}
                renderInput={(params) => <TextField {...params} label="Student" placeholder="Name or admission no." />}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField select label="Month" fullWidth value={month} onChange={(e) => setMonth(Number(e.target.value))}>
                {MONTHS.map((m, idx) => (
                  <MenuItem key={m} value={idx + 1}>
                    {m}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField select label="Year" fullWidth value={year} onChange={(e) => setYear(Number(e.target.value))}>
                {years.map((y) => (
                  <MenuItem key={y} value={y}>
                    {y}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField label="Amount (INR)" type="number" fullWidth value={amount} onChange={(e) => setAmount(e.target.value)} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} color="inherit" disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={saving || !canSubmit} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
            Add Charge
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

export default HostelFeeFormDialog;
