import { useEffect, useMemo, useState } from 'react';
import Grid from '@mui/material/Grid';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Avatar from '@mui/material/Avatar';
import Typography from '@mui/material/Typography';
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import VideoCameraFrontOutlinedIcon from '@mui/icons-material/VideoCameraFrontOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import PieChartCard from '@/components/charts/PieChartCard';
import UpcomingEventsCard from './widgets/UpcomingEventsCard';
import RecentActivitiesCard from './widgets/RecentActivitiesCard';
import NotificationsCard from './widgets/NotificationsCard';
import TodayScheduleCard from './widgets/TodayScheduleCard';
import parentApi from '@/api/parentApi';
import studentsApi from '@/api/studentsApi';
import attendanceApi from '@/api/attendanceApi';
import feesApi from '@/api/feesApi';
import assignmentsApi from '@/api/assignmentsApi';
import examApi from '@/api/examApi';
import onlineClassesApi from '@/api/onlineClassesApi';
import noticesApi from '@/api/noticesApi';
import eventsApi from '@/api/eventsApi';
import { formatCurrencyINR } from '@/utils/format';
import { useAppSelector } from '@/store/hooks';
import type {
  Assignment,
  AssignmentSubmission,
  CalendarEvent,
  Exam,
  Notice,
  OnlineClass,
  ParentChild,
  Student,
  StudentAttendanceSummary,
  StudentFee,
} from '@/types';

const FEE_STATUS_COLOR: Record<string, 'warning' | 'error' | 'success'> = {
  UNPAID: 'warning',
  OVERDUE: 'error',
  PARTIAL: 'warning',
  PAID: 'success',
};

/** Personal dashboard for STUDENT / PARENT roles, wired to the real student's own data. */
export function StudentParentDashboard() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isParent = user?.role === 'PARENT';

  const [children, setChildren] = useState<ParentChild[]>([]);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(
    isParent ? null : user?.studentId ?? null,
  );

  const [student, setStudent] = useState<Student | null>(null);
  const [attendanceSummary, setAttendanceSummary] = useState<StudentAttendanceSummary | null>(null);
  const [fees, setFees] = useState<StudentFee[]>([]);
  const [assignments, setAssignments] = useState<Array<Assignment & { submitted?: boolean }>>([]);
  const [exams, setExams] = useState<Exam[]>([]);
  const [onlineClasses, setOnlineClasses] = useState<OnlineClass[]>([]);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loadingChildren, setLoadingChildren] = useState(isParent);
  const [loading, setLoading] = useState(true);

  // For PARENT: load the list of children once, then default to the first one.
  useEffect(() => {
    if (!isParent) return;
    let cancelled = false;
    setLoadingChildren(true);
    parentApi
      .getMyChildren()
      .then((res) => {
        if (cancelled) return;
        setChildren(res.data);
        setSelectedStudentId((prev) => prev ?? res.data[0]?.studentId ?? null);
      })
      .catch(() => {
        if (!cancelled) enqueueSnackbar('Could not load your children.', { variant: 'error' });
      })
      .finally(() => {
        if (!cancelled) setLoadingChildren(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isParent]);

  // Load the dashboard data for whichever student is currently selected.
  useEffect(() => {
    if (!selectedStudentId) {
      setLoading(false);
      return;
    }
    let cancelled = false;

    (async () => {
      setLoading(true);
      const today = dayjs().format('YYYY-MM-DD');

      const [studentRes, summaryRes, feesRes, noticesRes, eventsRes] = await Promise.allSettled([
        studentsApi.getById(selectedStudentId),
        attendanceApi.getStudentSummary(selectedStudentId),
        feesApi.studentFees.list({ studentId: selectedStudentId, size: 20, sort: 'dueDate,asc' }),
        noticesApi.list({ size: 5, sort: 'publishedAt,desc' }),
        eventsApi.list({ startDate: today }),
      ]);
      if (cancelled) return;

      let studentData: Student | null = null;
      if (studentRes.status === 'fulfilled') {
        studentData = studentRes.value.data;
        setStudent(studentData);
      } else {
        enqueueSnackbar('Could not load student details.', { variant: 'error' });
      }
      if (summaryRes.status === 'fulfilled') setAttendanceSummary(summaryRes.value.data);
      else setAttendanceSummary(null);
      if (feesRes.status === 'fulfilled') setFees(feesRes.value.data.content);
      else setFees([]);
      if (noticesRes.status === 'fulfilled') setNotices(noticesRes.value.data.content);
      if (eventsRes.status === 'fulfilled') setEvents(eventsRes.value.data);

      if (studentData) {
        const { classId, sectionId } = studentData;
        const [assignmentRes, examRes, onlineRes] = await Promise.allSettled([
          assignmentsApi.list({ classId, sectionId, size: 10, sort: 'dueDate,asc' }),
          examApi.exams.list({ classId, size: 5, sort: 'startDate,asc' }),
          onlineClassesApi.list({ classId, sectionId, upcoming: true, size: 5, sort: 'scheduledAt,asc' }),
        ]);
        if (cancelled) return;

        let upcomingAssignments: Assignment[] = [];
        if (assignmentRes.status === 'fulfilled') {
          upcomingAssignments = assignmentRes.value.data.content.filter(
            (a) => !dayjs(a.dueDate).isBefore(dayjs(), 'day'),
          );
        }

        if (!isParent && upcomingAssignments.length > 0) {
          // Only meaningful for the logged-in student themselves — "my submission" has
          // no equivalent for a parent viewing a child's assignments.
          const submissionResults = await Promise.allSettled(
            upcomingAssignments.map((a) => assignmentsApi.getMySubmission(a.id)),
          );
          if (cancelled) return;
          setAssignments(
            upcomingAssignments.map((a, idx) => {
              const res = submissionResults[idx];
              const submission: AssignmentSubmission | null =
                res.status === 'fulfilled' ? res.value.data : null;
              return { ...a, submitted: Boolean(submission) };
            }),
          );
        } else {
          setAssignments(upcomingAssignments);
        }

        if (examRes.status === 'fulfilled') {
          setExams(examRes.value.data.content.filter((e) => !dayjs(e.endDate).isBefore(dayjs(), 'day')));
        } else {
          setExams([]);
        }
        if (onlineRes.status === 'fulfilled') setOnlineClasses(onlineRes.value.data.content);
        else setOnlineClasses([]);
      } else {
        setAssignments([]);
        setExams([]);
        setOnlineClasses([]);
      }

      setLoading(false);
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedStudentId]);

  const feesDueTotal = useMemo(
    () => fees.filter((f) => f.status !== 'PAID').reduce((sum, f) => sum + (f.amountDue - f.amountPaid), 0),
    [fees],
  );
  const feesDueList = useMemo(() => fees.filter((f) => f.status !== 'PAID').slice(0, 5), [fees]);

  if (isParent && loadingChildren) return <PageLoader label="Loading your children..." />;

  if (isParent && children.length === 0) {
    return (
      <EmptyState
        title="No children linked"
        description="Your account isn't linked to any student records yet. Contact the school office if this looks wrong."
      />
    );
  }

  if (!isParent && !user?.studentId) {
    return (
      <EmptyState
        title="No student profile linked"
        description="Your account isn't linked to a student record yet, so personal data can't be shown here."
      />
    );
  }

  const attendancePieData = attendanceSummary
    ? [
        { name: 'Present', value: attendanceSummary.presentDays },
        { name: 'Absent', value: attendanceSummary.absentDays },
        { name: 'Late', value: attendanceSummary.lateDays },
        { name: 'Half Day', value: attendanceSummary.halfDays },
        { name: 'On Leave', value: attendanceSummary.leaveDays },
      ].filter((d) => d.value > 0)
    : [];

  const upcomingEvents = events
    .filter((e) => !dayjs(e.eventDate).isBefore(dayjs(), 'day'))
    .sort((a, b) => dayjs(a.eventDate).diff(dayjs(b.eventDate)))
    .slice(0, 5)
    .map((e) => ({ title: e.title, date: e.eventDate, type: e.eventType }));

  const recentNotices = notices.map((n) => ({
    title: n.title,
    detail: n.description,
    time: dayjs(n.publishedAt).format('DD MMM YYYY, hh:mm A'),
  }));

  return (
    <Box>
      {isParent && children.length > 1 && (
        <Stack direction="row" spacing={1} sx={{ mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
          {children.map((child) => {
            const name = [child.firstName, child.lastName].filter(Boolean).join(' ') || child.admissionNumber;
            const selected = child.studentId === selectedStudentId;
            return (
              <Chip
                key={child.studentId}
                avatar={<Avatar src={child.photoUrl ?? undefined}>{name[0]}</Avatar>}
                label={`${name}${child.className ? ` · ${child.className}${child.sectionName ? `-${child.sectionName}` : ''}` : ''}`}
                color={selected ? 'primary' : 'default'}
                variant={selected ? 'filled' : 'outlined'}
                onClick={() => setSelectedStudentId(child.studentId)}
              />
            );
          })}
        </Stack>
      )}

      {loading ? (
        <PageLoader label="Loading dashboard..." />
      ) : (
        <>
          {student && (
            <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 2.5 }}>
              <Avatar src={student.photoUrl ?? undefined}>{student.firstName?.[0] ?? student.admissionNumber[0]}</Avatar>
              <Box>
                <Typography variant="subtitle1" fontWeight={700}>
                  {[student.firstName, student.lastName].filter(Boolean).join(' ') || student.admissionNumber}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {student.className ?? `Class #${student.classId}`}
                  {student.sectionName ? `-${student.sectionName}` : ''} · Roll No. {student.rollNumber} · Admission No. {student.admissionNumber}
                </Typography>
              </Box>
            </Stack>
          )}

          {/* ── TODAY'S TIMETABLE — most useful thing for a student/parent ── */}
          <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
            <Grid item xs={12}>
              <TodayScheduleCard studentId={selectedStudentId ?? undefined} />
            </Grid>
          </Grid>

          <Grid container spacing={2.5}>
            <Grid item xs={12} sm={6} lg={3}>
              <StatCard
                icon={<EventAvailableOutlinedIcon />}
                label={isParent ? "Child's Attendance" : 'My Attendance'}
                value={attendanceSummary ? `${attendanceSummary.percentage.toFixed(1)}%` : '-'}
                color="success"
              />
            </Grid>
            <Grid item xs={12} sm={6} lg={3}>
              <StatCard
                icon={<PaidOutlinedIcon />}
                label="Fees Due"
                value={formatCurrencyINR(Math.max(0, feesDueTotal))}
                color={feesDueTotal > 0 ? 'error' : 'success'}
              />
            </Grid>
            <Grid item xs={12} sm={6} lg={3}>
              <StatCard
                icon={<AssignmentOutlinedIcon />}
                label="Upcoming Assignments"
                value={assignments.length}
                color="info"
              />
            </Grid>
            <Grid item xs={12} sm={6} lg={3}>
              <StatCard
                icon={<VideoCameraFrontOutlinedIcon />}
                label="Upcoming Online Classes"
                value={onlineClasses.length}
                color="primary"
              />
            </Grid>
          </Grid>

          <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
            <Grid item xs={12} lg={5}>
              {attendancePieData.length === 0 ? (
                <Card sx={{ height: '100%' }}>
                  <CardHeader title="Attendance Breakdown" subheader="This academic year" />
                  <CardContent sx={{ pt: 0 }}>
                    <EmptyState
                      icon={<EventAvailableOutlinedIcon fontSize="large" />}
                      title="No attendance recorded yet"
                      description="Attendance hasn't been marked for this student yet."
                    />
                  </CardContent>
                </Card>
              ) : (
                <PieChartCard title="Attendance Breakdown" subtitle="This academic year" data={attendancePieData} height={300} />
              )}
            </Grid>

            <Grid item xs={12} lg={7}>
              <Card sx={{ height: '100%' }}>
                <CardHeader title="Fees Due" subheader="Outstanding payments" />
                <CardContent sx={{ pt: 0 }}>
                  {feesDueList.length === 0 ? (
                    <EmptyState title="No dues" description="All fees are paid up. Nothing outstanding right now." />
                  ) : (
                    <List disablePadding>
                      {feesDueList.map((fee, idx) => (
                        <Box key={fee.id}>
                          <ListItem disableGutters sx={{ py: 1.25 }}>
                            <ListItemText
                              primary={fee.feeCategoryName ?? `Fee #${fee.feeStructureId}`}
                              secondary={`Due ${dayjs(fee.dueDate).format('DD MMM YYYY')} · ${formatCurrencyINR(fee.amountDue - fee.amountPaid)}`}
                              primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                              secondaryTypographyProps={{ fontSize: '0.75rem' }}
                            />
                            <Chip label={fee.status} size="small" color={FEE_STATUS_COLOR[fee.status] ?? 'default'} sx={{ mr: 1 }} />
                          </ListItem>
                          {idx < feesDueList.length - 1 && <Divider />}
                        </Box>
                      ))}
                    </List>
                  )}
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} lg={6}>
              <Card sx={{ height: '100%' }}>
                <CardHeader title="Upcoming Assignments" subheader="Homework and project deadlines" />
                <CardContent sx={{ pt: 0 }}>
                  {assignments.length === 0 ? (
                    <EmptyState
                      icon={<AssignmentTurnedInOutlinedIcon fontSize="large" />}
                      title="Nothing due"
                      description="No upcoming assignments right now."
                    />
                  ) : (
                    <List disablePadding>
                      {assignments.map((item, idx) => (
                        <Box key={item.id}>
                          <ListItem disableGutters sx={{ py: 1.25 }}>
                            <ListItemText
                              primary={item.title}
                              secondary={`${item.subjectName ?? 'Subject'} · Due ${dayjs(item.dueDate).format('DD MMM YYYY')}`}
                              primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                              secondaryTypographyProps={{ fontSize: '0.75rem' }}
                            />
                            {!isParent && (
                              <Chip
                                label={item.submitted ? 'Submitted' : 'Pending'}
                                size="small"
                                color={item.submitted ? 'success' : 'warning'}
                                variant="outlined"
                              />
                            )}
                          </ListItem>
                          {idx < assignments.length - 1 && <Divider />}
                        </Box>
                      ))}
                    </List>
                  )}
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} lg={6}>
              <Card sx={{ height: '100%' }}>
                <CardHeader title="Upcoming Online Classes" subheader="Scheduled live classes" />
                <CardContent sx={{ pt: 0 }}>
                  {onlineClasses.length === 0 ? (
                    <EmptyState
                      icon={<VideoCameraFrontOutlinedIcon fontSize="large" />}
                      title="No online classes scheduled"
                      description="Nothing scheduled right now."
                    />
                  ) : (
                    <List disablePadding>
                      {onlineClasses.map((cls, idx) => (
                        <Box key={cls.id}>
                          <ListItem
                            disableGutters
                            sx={{ py: 1.25 }}
                            secondaryAction={
                              <Button size="small" variant="outlined" component="a" href={cls.meetingLink} target="_blank" rel="noopener">
                                Join
                              </Button>
                            }
                          >
                            <ListItemText
                              primary={cls.title}
                              secondary={`${cls.subjectName ?? 'Subject'} · ${dayjs(cls.scheduledAt).format('DD MMM, hh:mm A')}`}
                              primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                              secondaryTypographyProps={{ fontSize: '0.75rem' }}
                              sx={{ mr: 8 }}
                            />
                          </ListItem>
                          {idx < onlineClasses.length - 1 && <Divider />}
                        </Box>
                      ))}
                    </List>
                  )}
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} lg={5}>
              <Card sx={{ height: '100%' }}>
                <CardHeader title="Upcoming Exams" subheader="Exam periods for this class" />
                <CardContent sx={{ pt: 0 }}>
                  {exams.length === 0 ? (
                    <EmptyState title="No exams scheduled" description="Nothing scheduled right now." />
                  ) : (
                    <List disablePadding>
                      {exams.map((exam, idx) => (
                        <Box key={exam.id}>
                          <ListItem disableGutters sx={{ py: 1.25 }}>
                            <ListItemText
                              primary={exam.examTypeName ?? 'Exam'}
                              secondary={`${dayjs(exam.startDate).format('DD MMM')} - ${dayjs(exam.endDate).format('DD MMM YYYY')}`}
                              primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                              secondaryTypographyProps={{ fontSize: '0.75rem' }}
                            />
                          </ListItem>
                          {idx < exams.length - 1 && <Divider />}
                        </Box>
                      ))}
                    </List>
                  )}
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} lg={7}>
              <UpcomingEventsCard events={upcomingEvents} />
            </Grid>

            <Grid item xs={12} lg={7}>
              <RecentActivitiesCard activities={recentNotices} title="Recent Notices" subheader="Latest circulars" />
            </Grid>
            <Grid item xs={12} lg={5}>
              <NotificationsCard />
            </Grid>
          </Grid>
        </>
      )}
    </Box>
  );
}

export default StudentParentDashboard;
