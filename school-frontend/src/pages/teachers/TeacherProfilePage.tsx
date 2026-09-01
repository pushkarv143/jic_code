import { useEffect, useState, type SyntheticEvent } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Avatar from '@mui/material/Avatar';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Link from '@mui/material/Link';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import CreditCardOutlinedIcon from '@mui/icons-material/CreditCardOutlined';
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import StatusChip from '@/components/common/StatusChip';
import teachersApi from '@/api/teachersApi';
import classesApi from '@/api/classesApi';
import type { ClassSubjectTeacher, Teacher } from '@/types';
import { formatCurrencyINR } from '@/utils/format';
import { downloadBlob } from '@/utils/downloadBlob';

const TABS = ['Overview', 'Subjects & Classes', 'Attendance'] as const;

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

/** Read-focused teacher profile: header card + Overview / Subjects & Classes / Attendance tabs. */
export function TeacherProfilePage() {
  const { id } = useParams<{ id: string }>();
  const teacherId = Number(id);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [teacher, setTeacher] = useState<Teacher | null>(null);
  const [mappings, setMappings] = useState<ClassSubjectTeacher[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(0);
  const [downloadingIdCard, setDownloadingIdCard] = useState(false);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const [teacherRes, mappingRes] = await Promise.all([
          teachersApi.getById(teacherId),
          classesApi.listTeacherMappings({ teacherId }),
        ]);
        setTeacher(teacherRes.data);
        setMappings(mappingRes.data);
      } catch (err: any) {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this teacher.', { variant: 'error' });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teacherId]);

  if (loading) return <PageLoader label="Loading teacher profile..." />;
  if (!teacher) return <EmptyState title="Teacher not found" description="This teacher may have been removed." />;

  const firstName = teacher.firstName ?? teacher.user?.firstName ?? teacher.username ?? '';
  const lastName = teacher.lastName ?? teacher.user?.lastName ?? '';
  const initials = `${firstName[0] ?? ''}${lastName[0] ?? ''}`.toUpperCase();

  const handleDownloadIdCard = async () => {
    setDownloadingIdCard(true);
    try {
      const blob = await teachersApi.getIdCardPdf(teacherId);
      downloadBlob(blob, `teacher-id-card-${teacher.employeeId}.pdf`);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not download the ID card.', { variant: 'error' });
    } finally {
      setDownloadingIdCard(false);
    }
  };

  return (
    <Box>
      {/* Breadcrumbs only PageHeader */}
      <PageHeader
        title=""
        breadcrumbs={[
          { label: 'Dashboard', to: '/app/dashboard' },
          { label: 'Teachers', to: '/app/teachers' },
          { label: `${firstName} ${lastName}`.trim() },
        ]}
      />

      {/* CRED-level gradient hero card — teal/emerald to distinguish from student indigo */}
      <Card
        sx={{
          mb: 3,
          background: 'linear-gradient(135deg, #004d40 0%, #00695c 55%, #00897b 100%)',
          color: '#fff',
          borderRadius: 4,
          overflow: 'visible',
          position: 'relative',
        }}
      >
        {/* Dot-grid texture overlay */}
        <Box sx={{
          position: 'absolute', inset: 0, borderRadius: 4, overflow: 'hidden', pointerEvents: 'none',
          backgroundImage: 'radial-gradient(rgba(255,255,255,0.05) 1px, transparent 1px)',
          backgroundSize: '24px 24px',
        }} />
        <CardContent sx={{ p: { xs: 2.5, sm: 3.5 }, position: 'relative', zIndex: 1 }}>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={{ xs: 2, sm: 3 }}
            alignItems={{ xs: 'center', sm: 'flex-start' }}
          >
            {/* Avatar with status ring */}
            <Box sx={{ position: 'relative', flexShrink: 0 }}>
              <Avatar
                sx={{
                  width: 88,
                  height: 88,
                  fontSize: '2rem',
                  fontWeight: 800,
                  bgcolor: 'rgba(255,255,255,0.2)',
                  color: '#fff',
                  border: '3px solid rgba(255,255,255,0.5)',
                  boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
                }}
              >
                {initials}
              </Avatar>
              <Box sx={{
                position: 'absolute', bottom: 2, right: 2, width: 16, height: 16,
                borderRadius: '50%', border: '2px solid #fff',
                bgcolor: teacher.status === 'ACTIVE' ? '#4caf50' : '#9e9e9e',
              }} />
            </Box>

            {/* Identity */}
            <Box sx={{ flex: 1, textAlign: { xs: 'center', sm: 'left' } }}>
              <Typography variant="h5" fontWeight={800} sx={{ color: '#fff', lineHeight: 1.2, mb: 0.5 }}>
                {firstName} {lastName}
              </Typography>
              <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.75)', mb: 1.5 }}>
                Employee ID: {teacher.employeeId}
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" justifyContent={{ xs: 'center', sm: 'flex-start' }}>
                <StatusChip status={teacher.status} />
                {teacher.designationName && (
                  <Box sx={{ display: 'inline-flex', alignItems: 'center', px: 1.25, py: 0.25, borderRadius: 50, bgcolor: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.3)' }}>
                    <Typography sx={{ fontSize: '0.72rem', fontWeight: 700, color: '#fff' }}>
                      {teacher.designationName}
                    </Typography>
                  </Box>
                )}
                {teacher.departmentName && (
                  <Box sx={{ display: 'inline-flex', alignItems: 'center', px: 1.25, py: 0.25, borderRadius: 50, bgcolor: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.3)' }}>
                    <Typography sx={{ fontSize: '0.72rem', fontWeight: 700, color: 'rgba(255,255,255,0.85)' }}>
                      {teacher.departmentName}
                    </Typography>
                  </Box>
                )}
                {teacher.employmentType && (
                  <Box sx={{ display: 'inline-flex', alignItems: 'center', px: 1.25, py: 0.25, borderRadius: 50, bgcolor: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.2)' }}>
                    <Typography sx={{ fontSize: '0.72rem', fontWeight: 600, color: 'rgba(255,255,255,0.75)' }}>
                      {teacher.employmentType.replace('_', ' ')}
                    </Typography>
                  </Box>
                )}
              </Stack>
            </Box>

            {/* Action buttons */}
            <Stack direction={{ xs: 'row', sm: 'column', lg: 'row' }} spacing={1} flexShrink={0}>
              <Button
                variant="contained"
                startIcon={downloadingIdCard ? <CircularProgress size={16} color="inherit" /> : <CreditCardOutlinedIcon />}
                onClick={handleDownloadIdCard}
                disabled={downloadingIdCard}
                sx={{
                  bgcolor: 'rgba(255,255,255,0.15)', color: '#fff',
                  border: '1px solid rgba(255,255,255,0.3)',
                  backdropFilter: 'blur(8px)',
                  '&:hover': { bgcolor: 'rgba(255,255,255,0.25)' },
                  boxShadow: 'none', background: 'rgba(255,255,255,0.15)', fontSize: '0.8rem',
                }}
              >
                ID Card
              </Button>
              <Button
                variant="contained"
                startIcon={<EditOutlinedIcon />}
                onClick={() => navigate(`/app/teachers/${teacherId}/edit`)}
                sx={{
                  bgcolor: 'rgba(255,255,255,0.15)', color: '#fff',
                  border: '1px solid rgba(255,255,255,0.3)',
                  backdropFilter: 'blur(8px)',
                  '&:hover': { bgcolor: 'rgba(255,255,255,0.25)' },
                  boxShadow: 'none', background: 'rgba(255,255,255,0.15)', fontSize: '0.8rem',
                }}
              >
                Edit
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <Tabs
          value={tab}
          onChange={(_e: SyntheticEvent, v: number) => setTab(v)}
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
          variant="scrollable"
          scrollButtons="auto"
        >
          {TABS.map((t) => (
            <Tab key={t} label={t} />
          ))}
        </Tabs>
        <CardContent>
          {tab === 0 && (
            <Grid container spacing={4}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Account
                </Typography>
                <InfoRow label="Username" value={teacher.username ?? teacher.user?.username} />
                <InfoRow label="Email" value={teacher.email ?? teacher.user?.email} />
                <InfoRow label="Phone" value={teacher.phone ?? teacher.user?.phone} />
                <InfoRow label="Gender" value={teacher.gender} />
                <InfoRow label="Date of Birth" value={teacher.dateOfBirth ? dayjs(teacher.dateOfBirth).format('DD MMM YYYY') : '-'} />
                <InfoRow label="Blood Group" value={teacher.bloodGroup} />
                <InfoRow label="Emergency Contact" value={teacher.emergencyContact} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Employment
                </Typography>
                <InfoRow label="Department" value={teacher.departmentName} />
                <InfoRow label="Designation" value={teacher.designationName} />
                <InfoRow label="Qualification" value={teacher.qualification} />
                <InfoRow label="Experience (years)" value={teacher.experienceYears} />
                <InfoRow label="Joining Date" value={dayjs(teacher.joiningDate).format('DD MMM YYYY')} />
                <InfoRow label="Employment Type" value={teacher.employmentType} />
                <InfoRow label="Salary" value={teacher.salary != null ? formatCurrencyINR(teacher.salary) : '-'} />

                <Typography variant="subtitle2" fontWeight={700} gutterBottom sx={{ mt: 3 }}>
                  Address
                </Typography>
                <InfoRow label="Address" value={teacher.address} />
                <InfoRow label="City" value={teacher.city} />
                <InfoRow label="State" value={teacher.state} />
                <InfoRow label="Pincode" value={teacher.pincode} />
              </Grid>
            </Grid>
          )}

          {tab === 1 &&
            (mappings.length === 0 ? (
              <EmptyState
                title="No class/subject assignments yet"
                description="This teacher hasn't been mapped to any section or subject. Assign them from the Classes module's Teacher Mapping tab."
              />
            ) : (
              <Grid container spacing={2}>
                {mappings.map((m) => (
                  <Grid item xs={12} sm={6} md={4} key={m.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Typography variant="subtitle2" fontWeight={700}>
                          {m.subjectName ?? `Subject #${m.subjectId}`}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {m.sectionName ?? `Section #${m.sectionId}`}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            ))}

          {tab === 2 && (
            <Box sx={{ textAlign: 'center', py: 6 }}>
              <EventAvailableOutlinedIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1.5 }} />
              <Typography variant="body1" gutterBottom>
                Detailed attendance history is available in the Attendance module.
              </Typography>
              <Link component={RouterLink} to="/app/attendance">
                Go to Attendance
              </Link>
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default TeacherProfilePage;
