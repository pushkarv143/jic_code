import { useEffect, useState } from 'react';
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
import { useSnackbar } from 'notistack';
import studentsApi from '@/api/studentsApi';
import transportApi from '@/api/transportApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { getStudentDisplayName } from '@/utils/format';
import type { PickupPoint, Route, Student, StudentTransport } from '@/types';

export interface StudentTransportFormDialogProps {
  open: boolean;
  editing: StudentTransport | null;
  routes: Route[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: { studentId: number; routeId: number; pickupPointId: number; monthlyFee: number }) => void;
}

/** Add/edit dialog for a student transport allocation: student autocomplete, route -> pickup point cascade, monthly fee. */
export function StudentTransportFormDialog({ open, editing, routes, saving, onClose, onSubmit }: StudentTransportFormDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [studentQuery, setStudentQuery] = useState('');
  const debouncedQuery = useDebouncedValue(studentQuery, 400);
  const [studentOptions, setStudentOptions] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);

  const [routeId, setRouteId] = useState<number | ''>('');
  const [pickupPointId, setPickupPointId] = useState<number | ''>('');
  const [pickupPoints, setPickupPoints] = useState<PickupPoint[]>([]);
  const [loadingPoints, setLoadingPoints] = useState(false);
  const [monthlyFee, setMonthlyFee] = useState('');

  useEffect(() => {
    if (open) {
      setSelectedStudent(null);
      setStudentQuery('');
      setRouteId(editing?.routeId ?? '');
      setPickupPointId(editing?.pickupPointId ?? '');
      setMonthlyFee(editing ? String(editing.monthlyFee) : '');
    }
  }, [open, editing]);

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
    if (!routeId) {
      setPickupPoints([]);
      return;
    }
    setLoadingPoints(true);
    transportApi.pickupPoints
      .list(routeId as number)
      .then((res) => {
        setPickupPoints(res.data);
        if (!res.data.some((p) => p.id === pickupPointId)) {
          setPickupPointId(editing && editing.routeId === routeId ? editing.pickupPointId : '');
        }
      })
      .catch(() => enqueueSnackbar('Could not load pickup points for this route.', { variant: 'error' }))
      .finally(() => setLoadingPoints(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeId]);

  const studentId = editing?.studentId ?? selectedStudent?.id;
  const canSubmit = !!studentId && !!routeId && !!pickupPointId && !!monthlyFee && Number(monthlyFee) >= 0;

  const submit = () => {
    if (!canSubmit) return;
    onSubmit({
      studentId: studentId!,
      routeId: routeId as number,
      pickupPointId: pickupPointId as number,
      monthlyFee: Number(monthlyFee),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{editing ? 'Edit Transport Assignment' : 'Assign Transport'}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.5 }}>
          <Grid item xs={12}>
            {editing ? (
              <TextField
                label="Student"
                fullWidth
                disabled
                value={editing.studentName ? `${editing.studentName} (${editing.admissionNumber ?? ''})` : `Student #${editing.studentId}`}
              />
            ) : (
              <Autocomplete
                options={studentOptions}
                value={selectedStudent}
                getOptionLabel={(o) => `${getStudentDisplayName(o)} (${o.admissionNumber})`}
                isOptionEqualToValue={(o, v) => o.id === v.id}
                onChange={(_e, value) => setSelectedStudent(value)}
                onInputChange={(_e, value) => setStudentQuery(value)}
                renderInput={(params) => <TextField {...params} label="Student" placeholder="Name or admission no." />}
              />
            )}
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              select
              label="Route"
              fullWidth
              value={routeId}
              onChange={(e) => setRouteId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">Select a route</MenuItem>
              {routes.map((r) => (
                <MenuItem key={r.id} value={r.id}>
                  {r.routeName} ({r.startPoint} → {r.endPoint})
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              select
              label="Pickup Point"
              fullWidth
              disabled={!routeId || loadingPoints}
              value={pickupPointId}
              onChange={(e) => setPickupPointId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <MenuItem value="">{loadingPoints ? 'Loading...' : 'Select a pickup point'}</MenuItem>
              {pickupPoints.map((p) => (
                <MenuItem key={p.id} value={p.id}>
                  {p.pointName}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Monthly Fee (INR)"
              fullWidth
              type="number"
              value={monthlyFee}
              onChange={(e) => setMonthlyFee(e.target.value)}
            />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit" disabled={saving}>
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={saving || !canSubmit}
          startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default StudentTransportFormDialog;
