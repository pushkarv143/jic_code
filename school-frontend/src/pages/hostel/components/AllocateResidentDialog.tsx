import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import studentsApi from '@/api/studentsApi';
import hostelApi from '@/api/hostelApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { getStudentDisplayName } from '@/utils/format';
import type { Hostel, HostelRoom, Student } from '@/types';

export interface AllocateResidentDialogProps {
  open: boolean;
  hostels: Hostel[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { studentId: number; roomId: number; allocationDate: string }) => void;
}

interface FormValues {
  hostelId: number | '';
  roomId: number | '';
  allocationDate: dayjs.Dayjs | null;
}

/** Allocate a student to a hostel room: student autocomplete + hostel -> room cascade, with a client-side capacity check. */
export function AllocateResidentDialog({ open, hostels, saving, onClose, onSubmit }: AllocateResidentDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [studentQuery, setStudentQuery] = useState('');
  const debouncedQuery = useDebouncedValue(studentQuery, 400);
  const [studentOptions, setStudentOptions] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [rooms, setRooms] = useState<HostelRoom[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(false);

  const { control, watch, setValue, reset } = useForm<FormValues>({
    defaultValues: { hostelId: '', roomId: '', allocationDate: dayjs() },
  });
  const hostelId = watch('hostelId');
  const roomId = watch('roomId');
  const allocationDate = watch('allocationDate');

  useEffect(() => {
    if (open) {
      setSelectedStudent(null);
      setStudentQuery('');
      reset({ hostelId: '', roomId: '', allocationDate: dayjs() });
      setRooms([]);
    }
  }, [open, reset]);

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

  useEffect(() => {
    if (!hostelId) {
      setRooms([]);
      return;
    }
    setLoadingRooms(true);
    hostelApi.hostelRooms
      .list(hostelId as number)
      .then((res) => setRooms(res.data))
      .catch(() => enqueueSnackbar('Could not load rooms for this hostel.', { variant: 'error' }))
      .finally(() => setLoadingRooms(false));
  }, [hostelId, enqueueSnackbar]);

  const selectedRoom = rooms.find((r) => r.id === roomId);
  const roomFull = !!selectedRoom && selectedRoom.occupiedCount >= selectedRoom.capacity;

  const canSubmit = !!selectedStudent && !!roomId && !roomFull && !!allocationDate;

  const submit = () => {
    if (!canSubmit || !selectedStudent) return;
    onSubmit({
      studentId: selectedStudent.id,
      roomId: roomId as number,
      allocationDate: dayjs(allocationDate ?? dayjs()).format('YYYY-MM-DD'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Allocate Hostel Room</DialogTitle>
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
          <Grid item xs={12} sm={6}>
            <Controller
              name="hostelId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  label="Hostel"
                  fullWidth
                  value={field.value}
                  onChange={(e) => {
                    const v = e.target.value === '' ? '' : Number(e.target.value);
                    field.onChange(v);
                    setValue('roomId', '');
                  }}
                >
                  <MenuItem value="">Select a hostel</MenuItem>
                  {hostels.map((h) => (
                    <MenuItem key={h.id} value={h.id}>
                      {h.name} ({h.type === 'BOYS' ? 'Boys' : 'Girls'})
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Controller
              name="roomId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  label="Room"
                  fullWidth
                  disabled={!hostelId || loadingRooms}
                  value={field.value}
                  onChange={(e) => field.onChange(e.target.value === '' ? '' : Number(e.target.value))}
                >
                  <MenuItem value="">{loadingRooms ? 'Loading...' : 'Select a room'}</MenuItem>
                  {rooms.map((r) => (
                    <MenuItem key={r.id} value={r.id} disabled={r.occupiedCount >= r.capacity}>
                      Room {r.roomNumber} ({r.occupiedCount}/{r.capacity})
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
          </Grid>
          {roomFull && (
            <Grid item xs={12}>
              <DialogContentText color="error">This room is already at full capacity. Choose another room.</DialogContentText>
            </Grid>
          )}
          <Grid item xs={12} sm={6}>
            <Controller
              name="allocationDate"
              control={control}
              render={({ field }) => (
                <DatePicker
                  label="Allocation Date"
                  value={field.value}
                  onChange={(value) => field.onChange(value)}
                  slotProps={{ textField: { fullWidth: true } }}
                />
              )}
            />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={saving}>
          Cancel
        </Button>
        <Button variant="contained" onClick={submit} disabled={saving || !canSubmit} startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}>
          Allocate
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default AllocateResidentDialog;
