import { useCallback, useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Avatar from '@mui/material/Avatar';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import TextField from '@mui/material/TextField';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import StatusChip from '@/components/common/StatusChip';
import studentsApi, { type StudentSelfPayload } from '@/api/studentsApi';
import type { Student } from '@/types';
import { getStudentDisplayName, getStudentInitials } from '@/utils/format';

/** The fields the backend's StudentSelfUpdateRequest accepts; nothing academic. */
const EDITABLE_FIELDS: Array<{ key: keyof StudentSelfPayload; label: string }> = [
  { key: 'phone', label: 'Phone' },
  { key: 'address', label: 'Address' },
  { key: 'city', label: 'City' },
  { key: 'state', label: 'State' },
  { key: 'pincode', label: 'Pincode' },
  { key: 'bloodGroup', label: 'Blood Group' },
];

function InfoRow({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <Grid container spacing={1} sx={{ py: 0.75 }}>
      <Grid item xs={5} sm={4}>
        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>
      </Grid>
      <Grid item xs={7} sm={8}>
        <Typography variant="body2" fontWeight={600}>
          {value ?? '-'}
        </Typography>
      </Grid>
    </Grid>
  );
}

function toFormValues(student: Student): StudentSelfPayload {
  return {
    phone: student.phone ?? '',
    address: student.address ?? '',
    city: student.city ?? '',
    state: student.state ?? '',
    pincode: student.pincode ?? '',
    bloodGroup: student.bloodGroup ?? '',
  };
}

/**
 * A student's view of their own record, fetched from /students/me — there is no id
 * in the request, so there is nothing for the page to get wrong about whose data
 * it is showing. Academic fields (class, section, roll number, status) are
 * read-only here and stay editable only through the office's student form.
 */
export function MyStudentProfilePage() {
  const { enqueueSnackbar } = useSnackbar();

  const [student, setStudent] = useState<Student | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<StudentSelfPayload>({});

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const res = await studentsApi.getOwnProfile();
      setStudent(res.data);
      setForm(toFormValues(res.data));
    } catch {
      setLoadError('We could not load your profile. Your login may not be linked to a student record yet.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await studentsApi.updateOwnProfile(form);
      setStudent(res.data);
      setForm(toFormValues(res.data));
      setEditing(false);
      enqueueSnackbar('Your details have been updated', { variant: 'success' });
    } catch {
      enqueueSnackbar('Could not save your details. Please check the values and try again.', {
        variant: 'error',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (student) setForm(toFormValues(student));
    setEditing(false);
  };

  if (loading) return <PageLoader />;
  if (loadError || !student) {
    return (
      <Box>
        <PageHeader title="My Profile" breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Profile' }]} />
        <EmptyState
          title="Profile unavailable"
          description={loadError ?? 'No student record is linked to your account.'}
        />
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title="My Profile"
        subtitle="Your enrolment details and the contact information you can keep up to date"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Profile' }]}
        action={
          editing ? undefined : (
            <Button variant="contained" startIcon={<EditOutlinedIcon />} onClick={() => setEditing(true)}>
              Edit contact details
            </Button>
          )
        }
      />

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Stack alignItems="center" spacing={1.5}>
                <Avatar src={student.photoUrl ?? undefined} sx={{ width: 96, height: 96, fontSize: 32 }}>
                  {getStudentInitials(student)}
                </Avatar>
                <Typography variant="h6" textAlign="center">
                  {getStudentDisplayName(student)}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {student.admissionNumber}
                </Typography>
                <StatusChip status={student.status} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={8}>
          <Card sx={{ mb: 2.5 }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Enrolment
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Managed by the school office — contact them if anything here is wrong.
              </Typography>
              <Divider sx={{ my: 1.5 }} />
              <InfoRow label="Class" value={student.className} />
              <InfoRow label="Roll Number" value={student.rollNumber} />
              <InfoRow
                label="Admission Date"
                value={student.admissionDate ? dayjs(student.admissionDate).format('DD MMM YYYY') : null}
              />
              <InfoRow
                label="Date of Birth"
                value={student.dateOfBirth ? dayjs(student.dateOfBirth).format('DD MMM YYYY') : null}
              />
              <InfoRow label="Gender" value={student.gender} />
              <InfoRow label="Email" value={student.email} />
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Contact Details
              </Typography>
              <Divider sx={{ my: 1.5 }} />

              {editing ? (
                <Stack spacing={2}>
                  <Alert severity="info">
                    You can update the fields below yourself. Everything else is maintained by the school office.
                  </Alert>
                  <Grid container spacing={2}>
                    {EDITABLE_FIELDS.map((field) => (
                      <Grid item xs={12} sm={6} key={field.key}>
                        <TextField
                          fullWidth
                          size="small"
                          label={field.label}
                          value={form[field.key] ?? ''}
                          onChange={(e) => setForm((prev) => ({ ...prev, [field.key]: e.target.value }))}
                        />
                      </Grid>
                    ))}
                  </Grid>
                  <Stack direction="row" spacing={1} justifyContent="flex-end">
                    <Button startIcon={<CloseOutlinedIcon />} onClick={handleCancel} disabled={saving}>
                      Cancel
                    </Button>
                    <Button
                      variant="contained"
                      startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveOutlinedIcon />}
                      onClick={handleSave}
                      disabled={saving}
                    >
                      Save changes
                    </Button>
                  </Stack>
                </Stack>
              ) : (
                <>
                  <InfoRow label="Phone" value={student.phone} />
                  <InfoRow label="Address" value={student.address} />
                  <InfoRow label="City" value={student.city} />
                  <InfoRow label="State" value={student.state} />
                  <InfoRow label="Pincode" value={student.pincode} />
                  <InfoRow label="Blood Group" value={student.bloodGroup} />
                </>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default MyStudentProfilePage;
