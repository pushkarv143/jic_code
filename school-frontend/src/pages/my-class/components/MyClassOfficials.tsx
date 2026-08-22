import { useState } from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Alert from '@mui/material/Alert';
import { useSnackbar } from 'notistack';
import myClassApi from '@/api/myClassApi';
import { useAccess } from '@/access/AccessProvider';
import { getStudentDisplayName } from '@/utils/format';
import type { ClassOfficial, ClassOfficialRole, Student } from '@/types';

/** The posts a class can fill, in the order a school would list them. */
const POSTS: Array<{ role: ClassOfficialRole; label: string }> = [
  { role: 'HEAD_BOY', label: 'Head Boy' },
  { role: 'HEAD_GIRL', label: 'Head Girl' },
  { role: 'MONITOR', label: 'Monitor' },
  { role: 'SPORTS_CAPTAIN', label: 'Sports Captain' },
  { role: 'CULTURAL_SECRETARY', label: 'Cultural Secretary' },
];

export interface MyClassOfficialsProps {
  officials: ClassOfficial[];
  /** The roster, used to populate the candidate dropdowns. */
  students: Student[];
  onChanged: () => void;
}

/**
 * Class posts for the caller's own section.
 *
 * <p>Gated on MY_CLASS_OFFICIALS_MANAGE, which is a separate grant from the roster
 * one — a school can let the class teacher keep student records while reserving
 * "who is head boy" for the principal, or the reverse, without a code change.
 *
 * <p>Read access needs only MY_CLASS_VIEW, so a class teacher without the manage
 * grant still sees who holds what; the controls are simply absent.
 */
export function MyClassOfficials({ officials, students, onChanged }: MyClassOfficialsProps) {
  const { enqueueSnackbar } = useSnackbar();
  const { can } = useAccess();
  const canManage = can('MY_CLASS_OFFICIALS_MANAGE');

  const [pending, setPending] = useState<ClassOfficialRole | null>(null);
  const [selection, setSelection] = useState<Record<string, number | ''>>({});

  const holderOf = (role: ClassOfficialRole) =>
    officials.find((official) => official.role === role && official.current !== false);

  async function appoint(role: ClassOfficialRole) {
    const studentId = selection[role];
    if (!studentId) {
      enqueueSnackbar('Pick a student first.', { variant: 'warning' });
      return;
    }
    setPending(role);
    try {
      await myClassApi.appointOfficial({ studentId: Number(studentId), role });
      enqueueSnackbar('Appointed.', { variant: 'success' });
      setSelection((prev) => ({ ...prev, [role]: '' }));
      onChanged();
    } catch (error) {
      // The backend enforces the real rules — head boy must be male, a student
      // cannot hold two posts, the student must be in this class — so its message
      // is more useful than anything guessable here.
      const message =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not appoint that student.';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setPending(null);
    }
  }

  async function vacate(official: ClassOfficial) {
    setPending(official.role);
    try {
      await myClassApi.endOfficial(official.id);
      enqueueSnackbar('Post is now vacant.', { variant: 'success' });
      onChanged();
    } catch {
      enqueueSnackbar('Could not end that appointment.', { variant: 'error' });
    } finally {
      setPending(null);
    }
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 0.5 }}>
          Class Posts
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {canManage
            ? 'Appointing a student ends the sitting holder’s tenure — the history is kept, not overwritten.'
            : 'You can see who holds each post. Appointing is handled by the office.'}
        </Typography>

        {students.length === 0 && canManage && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Your roster is empty, so there is nobody to appoint yet.
          </Alert>
        )}

        <Grid container spacing={2}>
          {POSTS.map(({ role, label }) => {
            const holder = holderOf(role);
            return (
              <Grid item xs={12} md={6} key={role}>
                <Card variant="outlined">
                  <CardContent>
                    <Stack spacing={1.5}>
                      <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                        <Typography variant="subtitle2">{label}</Typography>
                        {holder ? (
                          <Chip size="small" color="success" label={holder.studentName ?? 'Held'} />
                        ) : (
                          <Chip size="small" variant="outlined" label="Vacant" />
                        )}
                      </Stack>

                      {canManage &&
                        (holder ? (
                          <Button
                            size="small"
                            color="error"
                            variant="outlined"
                            disabled={pending === role}
                            onClick={() => void vacate(holder)}
                          >
                            End appointment
                          </Button>
                        ) : (
                          <Stack direction="row" spacing={1}>
                            <FormControl size="small" fullWidth>
                              <InputLabel id={`pick-${role}`}>Student</InputLabel>
                              <Select
                                labelId={`pick-${role}`}
                                label="Student"
                                value={selection[role] ?? ''}
                                onChange={(e) =>
                                  setSelection((prev) => ({ ...prev, [role]: e.target.value as number }))
                                }
                              >
                                {students.map((student) => (
                                  <MenuItem key={student.id} value={student.id}>
                                    {student.rollNumber ? `${student.rollNumber}. ` : ''}
                                    {getStudentDisplayName(student)}
                                  </MenuItem>
                                ))}
                              </Select>
                            </FormControl>
                            <Button
                              size="small"
                              variant="contained"
                              disabled={pending === role || students.length === 0}
                              onClick={() => void appoint(role)}
                            >
                              Appoint
                            </Button>
                          </Stack>
                        ))}
                    </Stack>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      </CardContent>
    </Card>
  );
}

export default MyClassOfficials;
