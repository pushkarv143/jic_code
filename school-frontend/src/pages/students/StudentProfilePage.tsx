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
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Divider from '@mui/material/Divider';
import Link from '@mui/material/Link';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import CreditCardOutlinedIcon from '@mui/icons-material/CreditCardOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import StatusChip from '@/components/common/StatusChip';
import studentsApi from '@/api/studentsApi';
import type { Student } from '@/types';
import { getStudentDisplayName, getStudentInitials } from '@/utils/format';
import { downloadBlob } from '@/utils/downloadBlob';
import GuardiansTab from './components/GuardiansTab';
import MedicalDetailsTab from './components/MedicalDetailsTab';
import DocumentsTab from './components/DocumentsTab';

const TABS = ['Overview', 'Guardians', 'Medical Details', 'Documents', 'Attendance'] as const;

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

/** Read-focused student profile: header card + tabbed Overview/Guardians/Medical/Documents/Attendance. */
export function StudentProfilePage() {
  const { id } = useParams<{ id: string }>();
  const studentId = Number(id);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [student, setStudent] = useState<Student | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(0);
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [alumniOpen, setAlumniOpen] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);
  const [transferRemarks, setTransferRemarks] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [downloadingIdCard, setDownloadingIdCard] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = await studentsApi.getById(studentId);
      setStudent(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this student.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studentId]);

  const handleDelete = async () => {
    setActionLoading(true);
    try {
      await studentsApi.remove(studentId);
      enqueueSnackbar('Student deleted.', { variant: 'success' });
      navigate('/app/students');
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this student.', { variant: 'error' });
    } finally {
      setActionLoading(false);
      setDeleteOpen(false);
    }
  };

  const handleMarkAlumni = async () => {
    setActionLoading(true);
    try {
      const res = await studentsApi.markAlumni(studentId);
      setStudent(res.data);
      enqueueSnackbar('Student marked as alumni.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not mark this student as alumni.', {
        variant: 'error',
      });
    } finally {
      setActionLoading(false);
      setAlumniOpen(false);
    }
  };

  const handleTransfer = async () => {
    setActionLoading(true);
    try {
      const res = await studentsApi.transfer(studentId, { remarks: transferRemarks });
      setStudent(res.data);
      enqueueSnackbar('Student transferred.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not transfer this student.', { variant: 'error' });
    } finally {
      setActionLoading(false);
      setTransferOpen(false);
      setTransferRemarks('');
    }
  };

  if (loading) return <PageLoader label="Loading student profile..." />;
  if (!student) return <EmptyState title="Student not found" description="This student may have been removed." />;

  const initials = getStudentInitials(student);
  const displayName = getStudentDisplayName(student);

  const handleDownloadIdCard = async () => {
    setDownloadingIdCard(true);
    try {
      const blob = await studentsApi.getIdCardPdf(studentId);
      downloadBlob(blob, `student-id-card-${student.admissionNumber}.pdf`);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not download the ID card.', { variant: 'error' });
    } finally {
      setDownloadingIdCard(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title={displayName}
        subtitle={`Admission No. ${student.admissionNumber}`}
        breadcrumbs={[
          { label: 'Dashboard', to: '/app/dashboard' },
          { label: 'Students', to: '/app/students' },
          { label: displayName },
        ]}
        action={
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={downloadingIdCard ? <CircularProgress size={16} color="inherit" /> : <CreditCardOutlinedIcon />}
              onClick={handleDownloadIdCard}
              disabled={downloadingIdCard}
            >
              Download ID Card
            </Button>
            <Button
              variant="outlined"
              startIcon={<EditOutlinedIcon />}
              onClick={() => navigate(`/app/students/${studentId}/edit`)}
            >
              Edit
            </Button>
            <IconButton onClick={(e) => setMenuAnchor(e.currentTarget)}>
              <MoreVertOutlinedIcon />
            </IconButton>
            <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={() => setMenuAnchor(null)}>
              <MenuItem
                onClick={() => {
                  setMenuAnchor(null);
                  setTransferOpen(true);
                }}
              >
                Transfer Student
              </MenuItem>
              <MenuItem
                onClick={() => {
                  setMenuAnchor(null);
                  setAlumniOpen(true);
                }}
              >
                Mark as Alumni
              </MenuItem>
              <Divider />
              <MenuItem
                onClick={() => {
                  setMenuAnchor(null);
                  setDeleteOpen(true);
                }}
                sx={{ color: 'error.main' }}
              >
                <DeleteOutlineOutlinedIcon fontSize="small" sx={{ mr: 1 }} />
                Delete
              </MenuItem>
            </Menu>
          </Stack>
        }
      />

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={3} alignItems="center">
            <Grid item xs="auto">
              <Avatar src={student.photoUrl ?? undefined} sx={{ width: 84, height: 84, fontSize: 28 }}>
                {initials}
              </Avatar>
            </Grid>
            <Grid item xs>
              <Typography variant="h6" fontWeight={700}>
                {displayName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {student.className ?? `Class #${student.classId}`}
                {student.sectionName ? ` - ${student.sectionName}` : ''} · Roll No. {student.rollNumber}
              </Typography>
              <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                <StatusChip status={student.status} />
              </Stack>
            </Grid>
          </Grid>
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
                  Personal Information
                </Typography>
                <InfoRow label="Gender" value={student.gender} />
                <InfoRow label="Date of Birth" value={dayjs(student.dateOfBirth).format('DD MMM YYYY')} />
                <InfoRow label="Blood Group" value={student.bloodGroup} />
                <InfoRow label="Religion" value={student.religion} />
                <InfoRow label="Category" value={student.category} />
                <InfoRow label="Email" value={student.email} />
                <InfoRow label="Phone" value={student.phone} />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Academic
                </Typography>
                <InfoRow label="Admission Number" value={student.admissionNumber} />
                <InfoRow label="Admission Date" value={dayjs(student.admissionDate).format('DD MMM YYYY')} />
                <InfoRow label="Class" value={student.className} />
                <InfoRow label="Section" value={student.sectionName} />
                <InfoRow label="Roll Number" value={student.rollNumber} />
                <InfoRow label="Status" value={student.status} />

                <Typography variant="subtitle2" fontWeight={700} gutterBottom sx={{ mt: 3 }}>
                  Address
                </Typography>
                <InfoRow label="Address" value={student.address} />
                <InfoRow label="City" value={student.city} />
                <InfoRow label="State" value={student.state} />
                <InfoRow label="Pincode" value={student.pincode} />
              </Grid>
            </Grid>
          )}

          {tab === 1 && (
            <GuardiansTab
              studentId={studentId}
              guardians={student.guardians ?? []}
              onChanged={(guardians) => setStudent({ ...student, guardians })}
            />
          )}

          {tab === 2 && (
            <MedicalDetailsTab
              studentId={studentId}
              medicalDetails={student.medicalDetails ?? null}
              onChanged={(medicalDetails) => setStudent({ ...student, medicalDetails })}
            />
          )}

          {tab === 3 && (
            <DocumentsTab
              studentId={studentId}
              documents={student.documents ?? []}
              onChanged={(documents) => setStudent({ ...student, documents })}
            />
          )}

          {tab === 4 && (
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

      <ConfirmDialog
        open={deleteOpen}
        title="Delete student"
        message={`Are you sure you want to delete ${displayName}? This action cannot be undone.`}
        confirmLabel="Delete"
        destructive
        loading={actionLoading}
        onConfirm={handleDelete}
        onCancel={() => setDeleteOpen(false)}
      />

      <ConfirmDialog
        open={alumniOpen}
        title="Mark as alumni"
        message={`Mark ${displayName} as an alumni? They will no longer appear as an active student.`}
        confirmLabel="Mark as Alumni"
        loading={actionLoading}
        onConfirm={handleMarkAlumni}
        onCancel={() => setAlumniOpen(false)}
      />

      <Dialog open={transferOpen} onClose={() => setTransferOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Transfer Student</DialogTitle>
        <DialogContent>
          <TextField
            label="Remarks"
            fullWidth
            multiline
            minRows={3}
            autoFocus
            sx={{ mt: 1 }}
            value={transferRemarks}
            onChange={(e) => setTransferRemarks(e.target.value)}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setTransferOpen(false)} color="inherit" disabled={actionLoading}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleTransfer}
            disabled={!transferRemarks.trim() || actionLoading}
            startIcon={actionLoading ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Confirm Transfer
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default StudentProfilePage;
