import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Alert from '@mui/material/Alert';
import { useSnackbar } from 'notistack';
import myClassApi, { type MyClassStudentUpdatePayload } from '@/api/myClassApi';
import type { Gender, Student } from '@/types';

export interface MyClassStudentDialogProps {
  open: boolean;
  /** Null to add, a student to edit. */
  student: Student | null;
  /** Shown as read-only context so it is obvious which class the student lands in. */
  className: string;
  onClose: () => void;
  onSaved: () => void;
}

interface FormState {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  gender: Gender | '';
  bloodGroup: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
}

const EMPTY: FormState = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  dateOfBirth: '',
  gender: '',
  bloodGroup: '',
  address: '',
  city: '',
  state: '',
  pincode: '',
};

/**
 * Add/edit a student on your own class roster.
 *
 * <p>There is deliberately no class or section picker. The backend takes both from
 * the caller's homeroom assignment, and the payload types have nowhere to put them
 * — so on create the student can only land in this class, and on edit they cannot
 * be moved out of it. Moving a student between classes is a transfer and belongs to
 * the office.
 */
export function MyClassStudentDialog({
  open,
  student,
  className,
  onClose,
  onSaved,
}: MyClassStudentDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [form, setForm] = useState<FormState>(EMPTY);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setForm(
      student
        ? {
            firstName: student.firstName ?? '',
            lastName: student.lastName ?? '',
            email: student.email ?? '',
            phone: student.phone ?? '',
            dateOfBirth: student.dateOfBirth ?? '',
            gender: student.gender ?? '',
            bloodGroup: student.bloodGroup ?? '',
            address: student.address ?? '',
            city: student.city ?? '',
            state: student.state ?? '',
            pincode: student.pincode ?? '',
          }
        : EMPTY,
    );
  }, [open, student]);

  const set = (key: keyof FormState) => (value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  async function handleSave() {
    if (!form.firstName.trim()) {
      enqueueSnackbar('First name is required.', { variant: 'warning' });
      return;
    }

    // Empty strings are dropped rather than sent. The backend reads null as
    // "leave unchanged" on edit, and an empty string would fail the @Email and
    // @Pattern checks on fields the user simply did not fill in.
    const payload: MyClassStudentUpdatePayload = {
      firstName: form.firstName.trim(),
      ...(form.lastName.trim() ? { lastName: form.lastName.trim() } : {}),
      ...(form.email.trim() ? { email: form.email.trim() } : {}),
      ...(form.phone.trim() ? { phone: form.phone.trim() } : {}),
      ...(form.dateOfBirth ? { dateOfBirth: form.dateOfBirth } : {}),
      ...(form.gender ? { gender: form.gender as Gender } : {}),
      ...(form.bloodGroup.trim() ? { bloodGroup: form.bloodGroup.trim() } : {}),
      ...(form.address.trim() ? { address: form.address.trim() } : {}),
      ...(form.city.trim() ? { city: form.city.trim() } : {}),
      ...(form.state.trim() ? { state: form.state.trim() } : {}),
      ...(form.pincode.trim() ? { pincode: form.pincode.trim() } : {}),
    };

    setSaving(true);
    try {
      // Edit only. There is no create path: admissions are the office's job and
      // the API has no route for a class teacher to enrol anyone.
      if (!student) return;
      await myClassApi.updateStudent(student.id, payload);
      enqueueSnackbar('Student updated.', { variant: 'success' });
      onSaved();
    } catch (error) {
      const message =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not save the student.';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Edit Student</DialogTitle>
      <DialogContent dividers>
        <Alert severity="info" sx={{ mb: 2 }}>
          Editing a student in {className}. Class and section cannot be changed here — ask the
          office for a transfer.
        </Alert>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              label="First name"
              required
              fullWidth
              size="small"
              value={form.firstName}
              onChange={(e) => set('firstName')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Last name"
              fullWidth
              size="small"
              value={form.lastName}
              onChange={(e) => set('lastName')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Email"
              type="email"
              fullWidth
              size="small"
              value={form.email}
              onChange={(e) => set('email')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Phone"
              fullWidth
              size="small"
              value={form.phone}
              onChange={(e) => set('phone')(e.target.value)}
            />
          </Grid>
          {/*
            No roll-number field. It is the student's position in the class,
            assigned by the server and unique per class, so there is nothing to
            type — on create or on edit. The roster column shows it read-only.
          */}
          {student?.rollNumber != null && (
            <Grid item xs={12} sm={4}>
              <TextField
                label="Roll number"
                value={student.rollNumber}
                fullWidth
                size="small"
                disabled
                helperText="Assigned automatically"
              />
            </Grid>
          )}
          <Grid item xs={12} sm={4}>
            <TextField
              label="Date of birth"
              type="date"
              fullWidth
              size="small"
              InputLabelProps={{ shrink: true }}
              value={form.dateOfBirth}
              onChange={(e) => set('dateOfBirth')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Gender"
              select
              fullWidth
              size="small"
              value={form.gender}
              onChange={(e) => set('gender')(e.target.value)}
            >
              <MenuItem value="">—</MenuItem>
              <MenuItem value="MALE">Male</MenuItem>
              <MenuItem value="FEMALE">Female</MenuItem>
              <MenuItem value="OTHER">Other</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Blood group"
              fullWidth
              size="small"
              value={form.bloodGroup}
              onChange={(e) => set('bloodGroup')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={8}>
            <TextField
              label="Address"
              fullWidth
              size="small"
              value={form.address}
              onChange={(e) => set('address')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="City"
              fullWidth
              size="small"
              value={form.city}
              onChange={(e) => set('city')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="State"
              fullWidth
              size="small"
              value={form.state}
              onChange={(e) => set('state')(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Pincode"
              fullWidth
              size="small"
              value={form.pincode}
              onChange={(e) => set('pincode')(e.target.value)}
            />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSave} disabled={saving}>
          {saving ? 'Saving…' : 'Save changes'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default MyClassStudentDialog;
