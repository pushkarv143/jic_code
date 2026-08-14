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
import Chip from '@mui/material/Chip';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
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
import teachersApi, { type TeacherSelfPayload } from '@/api/teachersApi';
import type { Teacher, TeacherAssignment } from '@/types';
import { formatCurrencyINR } from '@/utils/format';

const TABS = ['Profile', 'My Classes'] as const;

/** The fields the backend's TeacherSelfUpdateRequest accepts; nothing HR-owned. */
const EDITABLE_FIELDS: Array<{ key: keyof TeacherSelfPayload; label: string }> = [
  { key: 'phone', label: 'Phone' },
  { key: 'emergencyContact', label: 'Emergency Contact' },
  { key: 'qualification', label: 'Qualification' },
  { key: 'bloodGroup', label: 'Blood Group' },
  { key: 'address', label: 'Address' },
  { key: 'city', label: 'City' },
  { key: 'state', label: 'State' },
  { key: 'pincode', label: 'Pincode' },
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

function toFormValues(teacher: Teacher): TeacherSelfPayload {
  return {
    phone: teacher.phone ?? '',
    emergencyContact: teacher.emergencyContact ?? '',
    qualification: teacher.qualification ?? '',
    bloodGroup: teacher.bloodGroup ?? '',
    address: teacher.address ?? '',
    city: teacher.city ?? '',
    state: teacher.state ?? '',
    pincode: teacher.pincode ?? '',
  };
}

function displayName(teacher: Teacher): string {
  return [teacher.firstName, teacher.lastName].filter(Boolean).join(' ') || teacher.username || 'My Profile';
}

function initials(teacher: Teacher): string {
  return displayName(teacher)
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');
}

/**
 * A teacher's view of their own record, fetched from /teachers/me — unredacted,
 * because the guard permits a teacher their own salary and personal details even
 * though those are blanked when they look at a colleague. HR fields (department,
 * designation, salary, employment type, status) are read-only here.
 */
export function MyTeacherProfilePage() {
  const { enqueueSnackbar } = useSnackbar();

  const [teacher, setTeacher] = useState<Teacher | null>(null);
  const [assignments, setAssignments] = useState<TeacherAssignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [tab, setTab] = useState(0);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<TeacherSelfPayload>({});

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      // Assignments come from their own endpoint rather than the profile's nested
      // list so the "My Classes" tab still renders if one call fails.
      const [profileRes, assignmentsRes] = await Promise.all([
        teachersApi.getOwnProfile(),
        teachersApi.getOwnAssignments(),
      ]);
      setTeacher(profileRes.data);
      setForm(toFormValues(profileRes.data));
      setAssignments(assignmentsRes.data ?? []);
    } catch {
      setLoadError('We could not load your profile. Your login may not be linked to a teacher record yet.');
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
      const res = await teachersApi.updateOwnProfile(form);
      setTeacher(res.data);
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
    if (teacher) setForm(toFormValues(teacher));
    setEditing(false);
  };

  if (loading) return <PageLoader />;
  if (loadError || !teacher) {
    return (
      <Box>
        <PageHeader
          title="My Profile"
          breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Profile' }]}
        />
        <EmptyState
          title="Profile unavailable"
          description={loadError ?? 'No teacher record is linked to your account.'}
        />
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title="My Profile"
        subtitle="Your staff record and the classes you are assigned to teach"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Profile' }]}
        action={
          editing || tab !== 0 ? undefined : (
            <Button variant="contained" startIcon={<EditOutlinedIcon />} onClick={() => setEditing(true)}>
              Edit my details
            </Button>
          )
        }
      />

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Stack alignItems="center" spacing={1.5}>
                <Avatar src={teacher.photoUrl ?? undefined} sx={{ width: 96, height: 96, fontSize: 32 }}>
                  {initials(teacher)}
                </Avatar>
                <Typography variant="h6" textAlign="center">
                  {displayName(teacher)}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {teacher.employeeId}
                </Typography>
                <StatusChip status={teacher.status} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={8}>
          <Card>
            <Tabs value={tab} onChange={(_, next) => setTab(next)} sx={{ px: 2, borderBottom: 1, borderColor: 'divider' }}>
              {TABS.map((label) => (
                <Tab key={label} label={label} />
              ))}
            </Tabs>

            <CardContent>
              {tab === 0 && (
                <>
                  <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                    Employment
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Managed by the school office — contact them if anything here is wrong.
                  </Typography>
                  <Divider sx={{ my: 1.5 }} />
                  <InfoRow label="Department" value={teacher.departmentName} />
                  <InfoRow label="Designation" value={teacher.designationName} />
                  <InfoRow label="Experience (years)" value={teacher.experienceYears} />
                  <InfoRow
                    label="Joining Date"
                    value={teacher.joiningDate ? dayjs(teacher.joiningDate).format('DD MMM YYYY') : null}
                  />
                  <InfoRow label="Employment Type" value={teacher.employmentType} />
                  <InfoRow
                    label="Salary"
                    value={teacher.salary != null ? formatCurrencyINR(teacher.salary) : null}
                  />

                  <Typography variant="subtitle1" fontWeight={700} gutterBottom sx={{ mt: 3 }}>
                    My Details
                  </Typography>
                  <Divider sx={{ my: 1.5 }} />

                  {editing ? (
                    <Stack spacing={2}>
                      <Alert severity="info">
                        You can update the fields below yourself. Department, designation, salary and
                        employment status are maintained by the school office.
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
                      <InfoRow label="Email" value={teacher.email} />
                      <InfoRow label="Phone" value={teacher.phone} />
                      <InfoRow label="Emergency Contact" value={teacher.emergencyContact} />
                      <InfoRow label="Qualification" value={teacher.qualification} />
                      <InfoRow label="Blood Group" value={teacher.bloodGroup} />
                      <InfoRow label="Address" value={teacher.address} />
                      <InfoRow label="City" value={teacher.city} />
                      <InfoRow label="State" value={teacher.state} />
                      <InfoRow label="Pincode" value={teacher.pincode} />
                    </>
                  )}
                </>
              )}

              {tab === 1 &&
                (assignments.length === 0 ? (
                  <EmptyState
                    title="No class assignments yet"
                    description="You haven't been mapped to any section or subject. The school office assigns these from the Classes module."
                  />
                ) : (
                  <Grid container spacing={2}>
                    {assignments.map((assignment) => (
                      <Grid item xs={12} sm={6} key={assignment.id}>
                        <Card variant="outlined">
                          <CardContent>
                            <Typography variant="subtitle2" fontWeight={700}>
                              {assignment.subjectName ?? `Subject #${assignment.subjectId}`}
                            </Typography>
                            <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap" useFlexGap>
                              <Chip size="small" label={assignment.className ?? `Class #${assignment.classId}`} />
                              <Chip
                                size="small"
                                variant="outlined"
                                label={assignment.sectionName ?? `Section #${assignment.sectionId}`}
                              />
                            </Stack>
                          </CardContent>
                        </Card>
                      </Grid>
                    ))}
                  </Grid>
                ))}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default MyTeacherProfilePage;
