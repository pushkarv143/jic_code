import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Avatar from '@mui/material/Avatar';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import PersonAddAltOutlinedIcon from '@mui/icons-material/PersonAddAltOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import classesApi from '@/api/classesApi';
import studentsApi from '@/api/studentsApi';
import { getStudentDisplayName, getStudentInitials } from '@/utils/format';
import type { ClassOfficial, ClassOfficialRole, Student } from '@/types';

export interface ClassOfficialsTabProps {
  classId: number;
  /** Refetches the parent overview, whose warnings include vacant posts. */
  onChanged?: () => void;
}

const ROLES: Array<{ value: ClassOfficialRole; label: string; requires?: 'MALE' | 'FEMALE' }> = [
  { value: 'HEAD_BOY', label: 'Head Boy', requires: 'MALE' },
  { value: 'HEAD_GIRL', label: 'Head Girl', requires: 'FEMALE' },
  { value: 'MONITOR', label: 'Monitor' },
  { value: 'SPORTS_CAPTAIN', label: 'Sports Captain' },
  { value: 'CULTURAL_SECRETARY', label: 'Cultural Secretary' },
];

function roleLabel(role: string): string {
  return ROLES.find((r) => r.value === role)?.label ?? role;
}

/**
 * Current post-holders, an appointment dialog and the full tenure history.
 *
 * <p>Appointing over a sitting holder is a succession, not a replacement: the
 * server closes the outgoing tenure and opens a new one, so the history below
 * keeps every holder the class has had. Nothing here deletes a record.
 */
export function ClassOfficialsTab({ classId, onChanged }: ClassOfficialsTabProps) {
  const { enqueueSnackbar } = useSnackbar();

  const [officials, setOfficials] = useState<ClassOfficial[]>([]);
  const [history, setHistory] = useState<ClassOfficial[]>([]);
  const [loading, setLoading] = useState(true);
  const [showHistory, setShowHistory] = useState(false);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [role, setRole] = useState<ClassOfficialRole>('HEAD_BOY');
  const [students, setStudents] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [fromDate, setFromDate] = useState(dayjs().format('YYYY-MM-DD'));
  const [saving, setSaving] = useState(false);
  const [endTarget, setEndTarget] = useState<ClassOfficial | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [current, past] = await Promise.all([
        classesApi.listOfficials(classId),
        classesApi.listOfficialHistory(classId),
      ]);
      setOfficials(current.data);
      setHistory(past.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load class officials.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [classId, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  // Only this class's students can hold its posts, so the picker is scoped the
  // same way the server validates — an out-of-class pick would just 400.
  useEffect(() => {
    if (!dialogOpen) return;
    studentsApi
      .list({ classId, size: 200, status: 'ACTIVE' })
      .then((res) => setStudents(res.data.content))
      .catch(() => enqueueSnackbar('Could not load students for this class.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dialogOpen, classId]);

  const required = ROLES.find((r) => r.value === role)?.requires;

  // Mirrors the server's gender rule so a mismatch is unpickable rather than a
  // 400 after the fact. The server still enforces it — this is a convenience.
  const eligibleStudents = useMemo(
    () => (required ? students.filter((s) => s.gender === required) : students),
    [students, required],
  );

  const openDialog = () => {
    setRole('HEAD_BOY');
    setSelectedStudent(null);
    setFromDate(dayjs().format('YYYY-MM-DD'));
    setDialogOpen(true);
  };

  const handleAppoint = async () => {
    if (!selectedStudent) return;
    setSaving(true);
    try {
      await classesApi.appointOfficial(classId, {
        studentId: selectedStudent.id,
        role,
        fromDate,
      });
      enqueueSnackbar(`${getStudentDisplayName(selectedStudent)} appointed as ${roleLabel(role)}.`, {
        variant: 'success',
      });
      setDialogOpen(false);
      await load();
      onChanged?.();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not appoint that student.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleEnd = async () => {
    if (!endTarget) return;
    try {
      await classesApi.endOfficial(classId, endTarget.id);
      enqueueSnackbar(`${roleLabel(endTarget.role)} post is now vacant.`, { variant: 'success' });
      setEndTarget(null);
      await load();
      onChanged?.();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not end that appointment.', { variant: 'error' });
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress size={28} />
      </Box>
    );
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="subtitle1" fontWeight={700}>
          Current post-holders
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button
            size="small"
            variant="outlined"
            startIcon={<HistoryOutlinedIcon />}
            onClick={() => setShowHistory((v) => !v)}
          >
            {showHistory ? 'Hide history' : 'History'}
          </Button>
          <Button size="small" variant="contained" startIcon={<PersonAddAltOutlinedIcon />} onClick={openDialog}>
            Appoint
          </Button>
        </Stack>
      </Stack>

      {officials.length === 0 ? (
        <EmptyState
          title="No posts filled"
          description="Appoint a head boy, head girl or other post-holder for this class."
        />
      ) : (
        <Grid container spacing={2}>
          {officials.map((official) => (
            <Grid item xs={12} sm={6} md={4} key={official.id}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" spacing={1.5} alignItems="center">
                    <Avatar sx={{ bgcolor: 'primary.main' }}>
                      {getStudentInitials({
                        firstName: official.studentName?.split(' ')[0] ?? null,
                        lastName: official.studentName?.split(' ')[1] ?? null,
                      })}
                    </Avatar>
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                      <Typography variant="subtitle2" fontWeight={700} noWrap>
                        {official.studentName ?? 'Unnamed Student'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" display="block">
                        {roleLabel(official.role)}
                        {official.rollNumber != null ? ` · Roll ${official.rollNumber}` : ''}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Since {dayjs(official.fromDate).format('DD MMM YYYY')}
                      </Typography>
                    </Box>
                  </Stack>
                  <Stack direction="row" justifyContent="flex-end" sx={{ mt: 1 }}>
                    <Button size="small" color="error" onClick={() => setEndTarget(official)}>
                      End term
                    </Button>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {showHistory && (
        <Card variant="outlined" sx={{ mt: 2.5 }}>
          <CardContent sx={{ pb: 0 }}>
            <Typography variant="subtitle1" fontWeight={700}>
              Tenure history
            </Typography>
          </CardContent>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Student</TableCell>
                  <TableCell>Post</TableCell>
                  <TableCell>From</TableCell>
                  <TableCell>To</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {history.map((row) => (
                  <TableRow key={row.id} hover>
                    <TableCell>{row.studentName ?? '—'}</TableCell>
                    <TableCell>{roleLabel(row.role)}</TableCell>
                    <TableCell>{dayjs(row.fromDate).format('DD MMM YYYY')}</TableCell>
                    <TableCell>{row.toDate ? dayjs(row.toDate).format('DD MMM YYYY') : '—'}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={row.current ? 'Current' : 'Ended'}
                        color={row.current ? 'success' : 'default'}
                        variant="outlined"
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Appoint a post-holder</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              select
              fullWidth
              size="small"
              label="Post"
              value={role}
              onChange={(e) => {
                setRole(e.target.value as ClassOfficialRole);
                // The eligible list changes with the post, so a student picked
                // under the previous one may no longer qualify.
                setSelectedStudent(null);
              }}
            >
              {ROLES.map((r) => (
                <MenuItem key={r.value} value={r.value}>
                  {r.label}
                </MenuItem>
              ))}
            </TextField>

            <Autocomplete
              size="small"
              options={eligibleStudents}
              value={selectedStudent}
              getOptionLabel={(o) => `${getStudentDisplayName(o)}${o.rollNumber ? ` (Roll ${o.rollNumber})` : ''}`}
              isOptionEqualToValue={(o, v) => o.id === v.id}
              onChange={(_e, value) => setSelectedStudent(value)}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Student"
                  helperText={
                    required
                      ? `Only ${required.toLowerCase()} students in this class can hold this post`
                      : 'Any active student in this class'
                  }
                />
              )}
            />

            <TextField
              fullWidth
              size="small"
              type="date"
              label="From"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
              helperText="A sitting holder's term is ended the day before this date"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!selectedStudent || saving}
            onClick={handleAppoint}
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Appoint
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!endTarget}
        title="End this term?"
        message={
          endTarget
            ? `${endTarget.studentName ?? 'This student'} will stop being ${roleLabel(endTarget.role)}. The record is kept in the history.`
            : ''
        }
        confirmLabel="End term"
        onConfirm={handleEnd}
        onCancel={() => setEndTarget(null)}
      />
    </Box>
  );
}

export default ClassOfficialsTab;
