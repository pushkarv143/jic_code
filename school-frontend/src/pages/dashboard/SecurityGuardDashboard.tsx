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
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import dayjs from 'dayjs';
import { Link as RouterLink } from 'react-router-dom';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import EmptyState from '@/components/common/EmptyState';
import PageLoader from '@/components/common/PageLoader';
import RecentActivitiesCard from './widgets/RecentActivitiesCard';
import NotificationsCard from './widgets/NotificationsCard';
import leaveApi from '@/api/leaveApi';
import noticesApi from '@/api/noticesApi';
import type { LeaveApplication, Notice } from '@/types';

/**
 * Minimal, honest dashboard for SECURITY_GUARD. This role has almost no
 * module access (see navConfig.tsx) — just Leave, the Notice Board, the
 * Calendar and Notifications — so the dashboard sticks to real data behind
 * those, plus shortcuts to pages this role can genuinely reach. No shift
 * roster or visitor-log module exists yet, so that's called out honestly
 * instead of being faked.
 */
export function SecurityGuardDashboard() {
  const [leaveApplications, setLeaveApplications] = useState<LeaveApplication[]>([]);
  const [notices, setNotices] = useState<Notice[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([leaveApi.listMine({ size: 5 }), noticesApi.list({ size: 5, sort: 'publishedAt,desc' })])
      .then(([leaveRes, noticesRes]) => {
        if (leaveRes.status === 'fulfilled') setLeaveApplications(leaveRes.value.data.content);
        if (noticesRes.status === 'fulfilled') setNotices(noticesRes.value.data.content);
      })
      .finally(() => setLoading(false));
  }, []);

  const recentNotices = notices.map((n) => ({
    title: n.title,
    detail: n.description,
    time: dayjs(n.publishedAt).format('DD MMM YYYY, hh:mm A'),
  }));

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 52,
              height: 52,
              borderRadius: 2.5,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: 'action.hover',
              color: 'primary.main',
              flexShrink: 0,
            }}
          >
            <ShieldOutlinedIcon fontSize="large" />
          </Box>
          <Box>
            <Typography variant="subtitle1" fontWeight={700}>
              On duty · {dayjs().format('dddd, DD MMMM YYYY')}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Shift rostering isn&apos;t tracked in this system yet — check with administration for your duty schedule.
            </Typography>
          </Box>
        </CardContent>
      </Card>

      {loading ? (
        <PageLoader label="Loading dashboard..." />
      ) : (
        <Grid container spacing={2.5}>
          <Grid item xs={12} lg={6}>
            <Card sx={{ height: '100%' }}>
              <CardHeader
                title="My Leave Applications"
                subheader={`${leaveApplications.filter((l) => l.status === 'PENDING').length} pending`}
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
            <Card sx={{ height: '100%' }}>
              <CardHeader title="Quick Links" subheader="Pages you can access" />
              <CardContent sx={{ pt: 0 }}>
                <Stack spacing={1.5}>
                  <Button variant="outlined" fullWidth startIcon={<CampaignOutlinedIcon />} component={RouterLink} to="/app/notices">
                    Notice Board
                  </Button>
                  <Button variant="outlined" fullWidth startIcon={<CalendarMonthOutlinedIcon />} component={RouterLink} to="/app/calendar">
                    Calendar
                  </Button>
                  <Button variant="outlined" fullWidth startIcon={<EventBusyOutlinedIcon />} component={RouterLink} to="/app/leave">
                    Apply for Leave
                  </Button>
                  <Button variant="outlined" fullWidth startIcon={<PersonOutlineOutlinedIcon />} component={RouterLink} to="/app/profile">
                    My Profile
                  </Button>
                </Stack>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} lg={7}>
            <RecentActivitiesCard activities={recentNotices} title="Recent Notices" subheader="Latest circulars from the notice board" />
          </Grid>
          <Grid item xs={12} lg={5}>
            <NotificationsCard />
          </Grid>
        </Grid>
      )}
    </Box>
  );
}

export default SecurityGuardDashboard;
