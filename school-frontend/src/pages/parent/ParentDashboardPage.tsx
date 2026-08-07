import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Grid from '@mui/material/Grid';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Avatar from '@mui/material/Avatar';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import Chip from '@mui/material/Chip';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import StatCard from '@/components/common/StatCard';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import ReportCardDialog from '@/pages/exams/components/ReportCardDialog';
import parentApi from '@/api/parentApi';
import studentsApi from '@/api/studentsApi';
import attendanceApi from '@/api/attendanceApi';
import feesApi from '@/api/feesApi';
import assignmentsApi from '@/api/assignmentsApi';
import examApi from '@/api/examApi';
import { useAppSelector } from '@/store/hooks';
import { formatCurrencyINR, getStudentDisplayName, getStudentInitials } from '@/utils/format';
import type { Assignment, Exam, ParentChild, StudentAttendanceSummary, StudentFee } from '@/types';

/**
 * Composition page for the PARENT role: fetches /parents/me/children, lets the parent switch
 * between children, then renders a per-child dashboard built entirely from EXISTING self-service
 * endpoints (attendance summary, fee dues, upcoming assignments, report card) — parameterized by
 * the selected child's studentId rather than reading it off the logged-in user's own auth fields
 * (which only STUDENT accounts have populated).
 */
export function ParentDashboardPage() {
  const { enqueueSnackbar } = useSnackbar();
  const role = useAppSelector((state) => state.auth.user?.role);

  const [children, setChildren] = useState<ParentChild[]>([]);
  const [childrenLoading, setChildrenLoading] = useState(true);
  const [childrenLoaded, setChildrenLoaded] = useState(false);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(null);

  const [attendanceSummary, setAttendanceSummary] = useState<StudentAttendanceSummary | null>(null);
  const [fees, setFees] = useState<StudentFee[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [exams, setExams] = useState<Exam[]>([]);
  const [reportCardExamId, setReportCardExamId] = useState<number | ''>('');
  const [reportCardOpen, setReportCardOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    if (role !== 'PARENT') {
      setChildrenLoading(false);
      return;
    }
    setChildrenLoading(true);
    parentApi
      .getMyChildren()
      .then((res) => {
        setChildren(res.data);
        setChildrenLoaded(true);
        if (res.data.length > 0) setSelectedStudentId(res.data[0].studentId);
      })
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load your children.', { variant: 'error' });
        setChildren([]);
        setChildrenLoaded(true);
      })
      .finally(() => setChildrenLoading(false));
  }, [role, enqueueSnackbar]);

  const loadChildDetail = useCallback(async () => {
    if (!selectedStudentId) return;
    setDetailLoading(true);
    try {
      const [studentRes, attendanceRes, feesRes] = await Promise.all([
        studentsApi.getById(selectedStudentId),
        attendanceApi.getStudentSummary(selectedStudentId).catch(() => null),
        feesApi.studentFees.list({ studentId: selectedStudentId, size: 100, sort: 'dueDate,desc' }).catch(() => null),
      ]);
      setAttendanceSummary(attendanceRes?.data ?? null);
      setFees(feesRes?.data.content ?? []);

      const [assignmentsRes, examsRes] = await Promise.all([
        assignmentsApi
          .list({ classId: studentRes.data.classId, sectionId: studentRes.data.sectionId, size: 50, sort: 'dueDate,asc' })
          .catch(() => null),
        examApi.exams.list({ classId: studentRes.data.classId, size: 100, sort: 'startDate,desc' }).catch(() => null),
      ]);
      setAssignments(assignmentsRes?.data.content ?? []);
      setExams(examsRes?.data.content ?? []);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this child’s details.', { variant: 'error' });
    } finally {
      setDetailLoading(false);
    }
  }, [selectedStudentId, enqueueSnackbar]);

  useEffect(() => {
    loadChildDetail();
    setReportCardExamId('');
  }, [loadChildDetail]);

  const upcomingAssignments = useMemo(
    () => assignments.filter((a) => !dayjs(a.dueDate).isBefore(dayjs(), 'day')).slice(0, 6),
    [assignments],
  );

  const outstandingFees = useMemo(() => fees.filter((f) => f.status !== 'PAID'), [fees]);
  const totalDue = useMemo(
    () => outstandingFees.reduce((sum, f) => sum + Math.max(f.amountDue - f.amountPaid, 0), 0),
    [outstandingFees],
  );

  if (role !== 'PARENT') {
    return (
      <EmptyState
        title="Parent account required"
        description="This page shows a per-child dashboard for parent accounts. Sign in with a parent account to use it."
      />
    );
  }

  if (childrenLoading) return <PageLoader label="Loading your children..." />;

  if (childrenLoaded && children.length === 0) {
    return (
      <Box>
        <PageHeader title="My Children" breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Children' }]} />
        <EmptyState title="No children linked" description="No students are linked to your parent account yet. Contact the school office." />
      </Box>
    );
  }

  const selectedChild = children.find((c) => c.studentId === selectedStudentId) ?? children[0];

  return (
    <Box>
      <PageHeader
        title="My Children"
        subtitle="Attendance, fees, assignments and report cards for your children"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'My Children' }]}
      />

      {children.length > 1 && (
        <Card sx={{ mb: 2.5 }}>
          <Tabs
            value={selectedStudentId ?? false}
            onChange={(_e, value) => setSelectedStudentId(value)}
            variant="scrollable"
            scrollButtons="auto"
            sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
          >
            {children.map((c) => (
              <Tab key={c.studentId} value={c.studentId} label={getStudentDisplayName(c)} sx={{ minHeight: 48 }} />
            ))}
          </Tabs>
        </Card>
      )}

      {selectedChild && (
        <Card sx={{ mb: 2.5 }}>
          <CardContent>
            <Stack direction="row" spacing={2} alignItems="center">
              <Avatar src={selectedChild.photoUrl ?? undefined} sx={{ width: 56, height: 56, bgcolor: 'primary.main' }}>
                {getStudentInitials(selectedChild)}
              </Avatar>
              <Box>
                <Typography variant="h6" fontWeight={700}>
                  {getStudentDisplayName(selectedChild)}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Admission No. {selectedChild.admissionNumber}
                  {selectedChild.className ? ` · ${selectedChild.className}${selectedChild.sectionName ? ` - ${selectedChild.sectionName}` : ''}` : ''}
                </Typography>
              </Box>
            </Stack>
          </CardContent>
        </Card>
      )}

      {detailLoading ? (
        <PageLoader label="Loading child details..." />
      ) : (
        <>
          <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
            <Grid item xs={12} sm={4}>
              <StatCard
                icon={<EventAvailableOutlinedIcon />}
                label="Attendance"
                value={attendanceSummary ? `${attendanceSummary.percentage.toFixed(1)}%` : '-'}
                color="success"
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <StatCard icon={<PaidOutlinedIcon />} label="Fees Due" value={formatCurrencyINR(totalDue)} color={totalDue > 0 ? 'error' : 'success'} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <StatCard icon={<AssignmentOutlinedIcon />} label="Upcoming Assignments" value={upcomingAssignments.length} color="info" />
            </Grid>
          </Grid>

          <Grid container spacing={2.5}>
            <Grid item xs={12} md={6}>
              <Card sx={{ height: '100%' }}>
                <CardHeader title="Fee Dues" subheader="Outstanding payments" />
                <CardContent sx={{ pt: 0 }}>
                  {outstandingFees.length === 0 ? (
                    <EmptyState title="No dues" description="All fees for this child have been paid." />
                  ) : (
                    <List disablePadding>
                      {outstandingFees.map((fee, idx) => (
                        <Box key={fee.id}>
                          <ListItem disableGutters sx={{ py: 1.25 }}>
                            <ListItemText
                              primary={fee.feeCategoryName ?? 'Fee Payment'}
                              secondary={`Due ${dayjs(fee.dueDate).format('DD MMM YYYY')} · ${formatCurrencyINR(Math.max(fee.amountDue - fee.amountPaid, 0))}`}
                              primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                              secondaryTypographyProps={{ fontSize: '0.75rem' }}
                            />
                            <Chip label={fee.status} size="small" color={fee.status === 'OVERDUE' ? 'error' : 'warning'} />
                          </ListItem>
                          {idx < outstandingFees.length - 1 && <Divider />}
                        </Box>
                      ))}
                    </List>
                  )}
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card sx={{ height: '100%' }}>
                <CardHeader title="Upcoming Assignments" subheader="Homework and project deadlines" />
                <CardContent sx={{ pt: 0 }}>
                  {upcomingAssignments.length === 0 ? (
                    <EmptyState title="No upcoming assignments" description="Nothing due right now." />
                  ) : (
                    <List disablePadding>
                      {upcomingAssignments.map((a, idx) => (
                        <Box key={a.id}>
                          <ListItem disableGutters sx={{ py: 1.25 }}>
                            <ListItemText
                              primary={a.title}
                              secondary={`${a.subjectName ?? `Subject #${a.subjectId}`} · Due ${dayjs(a.dueDate).format('DD MMM YYYY')}`}
                              primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                              secondaryTypographyProps={{ fontSize: '0.75rem' }}
                            />
                          </ListItem>
                          {idx < upcomingAssignments.length - 1 && <Divider />}
                        </Box>
                      ))}
                    </List>
                  )}
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12}>
              <Card>
                <CardHeader
                  title="Report Card"
                  subheader="Select an exam to view or print this child's report card"
                  avatar={<DescriptionOutlinedIcon color="primary" />}
                />
                <CardContent>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
                    <TextField
                      select
                      size="small"
                      label="Exam"
                      sx={{ minWidth: 260 }}
                      value={reportCardExamId}
                      onChange={(e) => setReportCardExamId(e.target.value === '' ? '' : Number(e.target.value))}
                    >
                      <MenuItem value="">Select an exam</MenuItem>
                      {exams.map((ex) => (
                        <MenuItem key={ex.id} value={ex.id}>
                          {ex.examTypeName ?? `Exam #${ex.id}`}
                        </MenuItem>
                      ))}
                    </TextField>
                    <Button
                      variant="contained"
                      disabled={!reportCardExamId}
                      startIcon={<DescriptionOutlinedIcon />}
                      onClick={() => setReportCardOpen(true)}
                    >
                      View Report Card
                    </Button>
                  </Stack>
                  {exams.length === 0 && (
                    <Typography variant="caption" color="text.secondary" sx={{ mt: 1.5, display: 'block' }}>
                      No exams have been scheduled for this child's class yet.
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </>
      )}

      <ReportCardDialog
        open={reportCardOpen}
        studentId={selectedStudentId}
        examId={(reportCardExamId as number) || null}
        onClose={() => setReportCardOpen(false)}
      />
    </Box>
  );
}

export default ParentDashboardPage;
