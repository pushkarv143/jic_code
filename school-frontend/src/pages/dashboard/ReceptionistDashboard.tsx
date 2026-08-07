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
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import { Link as RouterLink } from 'react-router-dom';
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined';
import PendingActionsOutlinedIcon from '@mui/icons-material/PendingActionsOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import PersonSearchOutlinedIcon from '@mui/icons-material/PersonSearchOutlined';
import DirectionsBusOutlinedIcon from '@mui/icons-material/DirectionsBusOutlined';
import ApartmentOutlinedIcon from '@mui/icons-material/ApartmentOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import NotificationsCard from './widgets/NotificationsCard';
import admissionApi from '@/api/admissionApi';
import type { AdmissionEnquiry, AdmissionEnquiryStatus } from '@/types';

const STATUS_COLOR: Record<AdmissionEnquiryStatus, 'warning' | 'success' | 'error'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
};

/** Front-desk dashboard for RECEPTIONIST — admission enquiries plus shortcuts to the pages this role can actually reach. */
export function ReceptionistDashboard() {
  const { enqueueSnackbar } = useSnackbar();
  const [enquiries, setEnquiries] = useState<AdmissionEnquiry[]>([]);
  const [totalEnquiries, setTotalEnquiries] = useState(0);
  const [pendingCount, setPendingCount] = useState(0);
  const [approvedCount, setApprovedCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([
      admissionApi.list({ sort: 'appliedAt,desc', size: 50 }),
      admissionApi.list({ status: 'PENDING', size: 1 }),
      admissionApi.list({ status: 'APPROVED', size: 1 }),
    ])
      .then(([recentRes, pendingRes, approvedRes]) => {
        if (recentRes.status === 'fulfilled') {
          setEnquiries(recentRes.value.data.content);
          setTotalEnquiries(recentRes.value.data.totalElements);
        } else {
          enqueueSnackbar('Could not load admission enquiries.', { variant: 'error' });
        }
        if (pendingRes.status === 'fulfilled') setPendingCount(pendingRes.value.data.totalElements);
        if (approvedRes.status === 'fulfilled') setApprovedCount(approvedRes.value.data.totalElements);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading dashboard..." />;

  const todaysEnquiries = enquiries.filter((e) => dayjs(e.appliedAt).isSame(dayjs(), 'day'));
  const recentEnquiries = enquiries.slice(0, 5);

  return (
    <Box>
      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<HowToRegOutlinedIcon />} label="Today's Enquiries" value={todaysEnquiries.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<PendingActionsOutlinedIcon />} label="Pending Review" value={pendingCount} color="warning" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<DoneAllOutlinedIcon />} label="Approved" value={approvedCount} color="success" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<SchoolOutlinedIcon />} label="Total Enquiries" value={totalEnquiries} color="info" />
        </Grid>
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
        <Grid item xs={12} lg={7}>
          <Card sx={{ height: '100%' }}>
            <CardHeader
              title="Recent Admission Enquiries"
              subheader="Latest enquiries submitted"
              action={
                <Button size="small" component={RouterLink} to="/app/admission">
                  View All
                </Button>
              }
            />
            <CardContent sx={{ pt: 0 }}>
              {recentEnquiries.length === 0 ? (
                <EmptyState title="No enquiries yet" description="New admission enquiries will show up here." />
              ) : (
                <List disablePadding>
                  {recentEnquiries.map((enquiry, idx) => (
                    <Box key={enquiry.id}>
                      <ListItem disableGutters sx={{ py: 1.25 }}>
                        <ListItemText
                          primary={`${enquiry.studentName} · Class ${enquiry.classApplying}`}
                          secondary={`${enquiry.parentName} · ${dayjs(enquiry.appliedAt).format('DD MMM YYYY')}`}
                          primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                          secondaryTypographyProps={{ fontSize: '0.75rem' }}
                        />
                        <Chip label={enquiry.status} size="small" color={STATUS_COLOR[enquiry.status]} variant="outlined" />
                      </ListItem>
                      {idx < recentEnquiries.length - 1 && <Divider />}
                    </Box>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Quick Actions" subheader="Jump to what you use most" />
            <CardContent sx={{ pt: 0 }}>
              <Stack spacing={1.5}>
                <Button variant="outlined" fullWidth startIcon={<PersonSearchOutlinedIcon />} component={RouterLink} to="/app/students">
                  Search Students
                </Button>
                <Button variant="outlined" fullWidth startIcon={<HowToRegOutlinedIcon />} component={RouterLink} to="/app/admission">
                  Admission Enquiries
                </Button>
                <Button variant="outlined" fullWidth startIcon={<DirectionsBusOutlinedIcon />} component={RouterLink} to="/app/transport/assignments">
                  Transport
                </Button>
                <Button variant="outlined" fullWidth startIcon={<ApartmentOutlinedIcon />} component={RouterLink} to="/app/hostel/residents">
                  Hostel
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <NotificationsCard />
        </Grid>
      </Grid>
    </Box>
  );
}

export default ReceptionistDashboard;
