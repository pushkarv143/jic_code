import { useEffect, useState } from 'react';
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
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import { Link as RouterLink } from 'react-router-dom';
import ClassOutlinedIcon from '@mui/icons-material/ClassOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import VideoCameraFrontOutlinedIcon from '@mui/icons-material/VideoCameraFrontOutlined';
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import UpcomingEventsCard from './widgets/UpcomingEventsCard';
import RecentActivitiesCard from './widgets/RecentActivitiesCard';
import NotificationsCard from './widgets/NotificationsCard';
import classesApi from '@/api/classesApi';
import assignmentsApi from '@/api/assignmentsApi';
import attendanceApi from '@/api/attendanceApi';
import onlineClassesApi from '@/api/onlineClassesApi';
import leaveApi from '@/api/leaveApi';
import noticesApi from '@/api/noticesApi';
import eventsApi from '@/api/eventsApi';
import { useAppSelector } from '@/store/hooks';
import type {
  Assignment,
  AssignmentSubmission,
  CalendarEvent,
  ClassSubjectTeacher,
  LeaveApplication,
  Notice,
  OnlineClass,
} from '@/types';

/** Teaching-focused dashboard for TEACHER / CLASS_TEACHER, built from the teacher's own assignments/classes/leave/attendance data. */
export function TeacherDashboard() {
  const { enqueueSnackbar } = useSnackbar();
  const teacherId = useAppSelector((state) => state.auth.user?.teacherId);

  const [mappings, setMappings] = useState<ClassSubjectTeacher[]>([]);
  const [recentAssignments, setRecentAssignments] = useState<Assignment[]>([]);
  const [pendingGradingCount, setPendingGradingCount] = useState(0);
  const [attendancePercentage, setAttendancePercentage] = useState<number | null>(null);
  const [onlineClasses, setOnlineClasses] = useState<OnlineClass[]>([]);
  const [leaveApplications, setLeaveApplications] = useState<LeaveApplication[]>([]);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!teacherId) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    const today = dayjs().format('YYYY-MM-DD');
    const monthStart = dayjs().startOf('month').format('YYYY-MM-DD');

    (async () => {
      setLoading(true);
      const [mappingRes, assignmentRes, attendanceRes, onlineRes, leaveRes, noticesRes, eventsRes] =
        await Promise.allSettled([
          classesApi.listTeacherMappings({ teacherId }),
          assignmentsApi.list({ teacherId, size: 5, sort: 'dueDate,desc' }),
          attendanceApi.getTeacherReport({ teacherId, startDate: monthStart, endDate: today, size: 31 }),
          onlineClassesApi.list({ teacherId, upcoming: true, size: 5, sort: 'scheduledAt,asc' }),
          leaveApi.listMine({ size: 5 }),
          noticesApi.list({ size: 5, sort: 'publishedAt,desc' }),
          eventsApi.list({ startDate: today }),
        ]);
      if (cancelled) return;

      if (mappingRes.status === 'fulfilled') setMappings(mappingRes.value.data);
      else enqueueSnackbar('Could not load your assigned classes.', { variant: 'error' });

      let assignments: Assignment[] = [];
      if (assignmentRes.status === 'fulfilled') {
        assignments = assignmentRes.value.data.content;
        setRecentAssignments(assignments);
      }

      if (attendanceRes.status === 'fulfilled') {
        const rows = attendanceRes.value.data.content;
        if (rows.length > 0) {
          const present = rows.filter((r) => r.status === 'PRESENT' || r.status === 'LATE' || r.status === 'HALF_DAY').length;
          setAttendancePercentage((present / rows.length) * 100);
        }
      }

      if (onlineRes.status === 'fulfilled') setOnlineClasses(onlineRes.value.data.content);
      if (leaveRes.status === 'fulfilled') setLeaveApplications(leaveRes.value.data.content);
      if (noticesRes.status === 'fulfilled') setNotices(noticesRes.value.data.content);
      if (eventsRes.status === 'fulfilled') setEvents(eventsRes.value.data);

      // Second wave: for the handful of recent assignments, work out how many
      // submissions are still awaiting grading (status other than GRADED).
      if (assignments.length > 0) {
        const submissionResults = await Promise.allSettled(
          assignments.map((a) => assignmentsApi.listSubmissions(a.id)),
        );
        if (cancelled) return;
        const pending = submissionResults.reduce((sum, r) => {
          if (r.status !== 'fulfilled') return sum;
          const ungraded = r.value.data.filter((s: AssignmentSubmission) => s.status !== 'GRADED');
          return sum + ungraded.length;
        }, 0);
        setPendingGradingCount(pending);
      }

      setLoading(false);
    })();

    return () => {
      cancelled = true;
    };
  }, [teacherId, enqueueSnackbar]);

  if (!teacherId) {
    return (
      <EmptyState
        title="No teacher profile linked"
        description="Your account isn't linked to a teacher record yet, so class/attendance data can't be shown here."
      />
    );
  }

  if (loading) return <PageLoader label="Loading your dashboard..." />;

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

  const pendingLeaveCount = leaveApplications.filter((l) => l.status === 'PENDING').length;

  return (
    <Box>
      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<ClassOutlinedIcon />} label="Classes & Subjects Assigned" value={mappings.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<AssignmentTurnedInOutlinedIcon />}
            label="Submissions Awaiting Grading"
            value={pendingGradingCount}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<EventAvailableOutlinedIcon />}
            label="My Attendance (This Month)"
            value={attendancePercentage != null ? `${attendancePercentage.toFixed(0)}%` : '-'}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<VideoCameraFrontOutlinedIcon />}
            label="Upcoming Online Classes"
            value={onlineClasses.length}
            color="info"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardHeader
              title="My Classes & Subjects"
              subheader="Sections and subjects you're mapped to"
              action={
                <Button size="small" component={RouterLink} to="/app/attendance">
                  Mark Attendance
                </Button>
              }
            />
            <CardContent sx={{ pt: 0 }}>
              {mappings.length === 0 ? (
                <EmptyState
                  title="No classes assigned yet"
                  description="You haven't been mapped to any section or subject yet."
                />
              ) : (
                <List disablePadding>
                  {mappings.map((m, idx) => (
                    <Box key={m.id}>
                      <ListItem disableGutters sx={{ py: 1.25 }}>
                        <ListItemText
                          primary={m.subjectName ?? `Subject #${m.subjectId}`}
                          secondary={m.sectionName ?? `Section #${m.sectionId}`}
                          primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                          secondaryTypographyProps={{ fontSize: '0.75rem' }}
                        />
                      </ListItem>
                      {idx < mappings.length - 1 && <Divider />}
                    </Box>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardHeader
              title="Recent Assignments"
              subheader="Latest assignments you've set"
              action={
                <Button size="small" component={RouterLink} to="/app/assignments">
                  View All
                </Button>
              }
            />
            <CardContent sx={{ pt: 0 }}>
              {recentAssignments.length === 0 ? (
                <EmptyState
                  title="No assignments yet"
                  description="Assignments you create will show up here."
                />
              ) : (
                <List disablePadding>
                  {recentAssignments.map((item, idx) => (
                    <Box key={item.id}>
                      <ListItem disableGutters sx={{ py: 1.25 }}>
                        <ListItemText
                          primary={item.title}
                          secondary={`${item.subjectName ?? 'Subject'} · ${item.sectionName ?? 'Section'} · Due ${dayjs(item.dueDate).format('DD MMM YYYY')}`}
                          primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                          secondaryTypographyProps={{ fontSize: '0.75rem' }}
                        />
                      </ListItem>
                      {idx < recentAssignments.length - 1 && <Divider />}
                    </Box>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Upcoming Online Classes" subheader="Classes you've scheduled" />
            <CardContent sx={{ pt: 0 }}>
              {onlineClasses.length === 0 ? (
                <EmptyState
                  icon={<VideoCameraFrontOutlinedIcon fontSize="large" />}
                  title="No online classes scheduled"
                  description="Classes you schedule will appear here."
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
                          secondary={`${cls.sectionName ?? 'Section'} · ${dayjs(cls.scheduledAt).format('DD MMM, hh:mm A')}`}
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

        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardHeader
              title="My Leave Applications"
              subheader={`${pendingLeaveCount} pending`}
              action={
                <Button size="small" component={RouterLink} to="/app/leave">
                  Apply
                </Button>
              }
            />
            <CardContent sx={{ pt: 0 }}>
              {leaveApplications.length === 0 ? (
                <EmptyState
                  icon={<EventBusyOutlinedIcon fontSize="large" />}
                  title="No leave applications"
                  description="Applications you submit will show up here."
                />
              ) : (
                <List disablePadding>
                  {leaveApplications.map((leave, idx) => (
                    <Box key={leave.id}>
                      <ListItem disableGutters sx={{ py: 1.25 }}>
                        <ListItemText
                          primary={leave.leaveType}
                          secondary={`${dayjs(leave.startDate).format('DD MMM')} - ${dayjs(leave.endDate).format('DD MMM YYYY')}`}
                          primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                          secondaryTypographyProps={{ fontSize: '0.75rem' }}
                        />
                        <Chip
                          label={leave.status}
                          size="small"
                          color={leave.status === 'APPROVED' ? 'success' : leave.status === 'REJECTED' ? 'error' : 'warning'}
                          variant="outlined"
                        />
                      </ListItem>
                      {idx < leaveApplications.length - 1 && <Divider />}
                    </Box>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={6}>
          <UpcomingEventsCard events={upcomingEvents} />
        </Grid>
        <Grid item xs={12} lg={6}>
          <RecentActivitiesCard activities={recentNotices} title="Recent Notices" subheader="Latest circulars" />
        </Grid>
        <Grid item xs={12}>
          <NotificationsCard />
        </Grid>
      </Grid>
    </Box>
  );
}

export default TeacherDashboard;
