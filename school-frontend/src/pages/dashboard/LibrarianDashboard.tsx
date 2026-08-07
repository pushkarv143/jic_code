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
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import LibraryBooksOutlinedIcon from '@mui/icons-material/LibraryBooksOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import NotificationsCard from './widgets/NotificationsCard';
import libraryApi from '@/api/libraryApi';
import { formatNumber } from '@/utils/format';
import type { BookIssue, LibraryDashboard } from '@/types';

/** Library-operations dashboard for LIBRARIAN, built from live library/dashboard, book-issues and overdue data. */
export function LibrarianDashboard() {
  const { enqueueSnackbar } = useSnackbar();
  const [dashboard, setDashboard] = useState<LibraryDashboard | null>(null);
  const [recentIssues, setRecentIssues] = useState<BookIssue[]>([]);
  const [overdue, setOverdue] = useState<BookIssue[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([
      libraryApi.dashboard.get(),
      libraryApi.bookIssues.list({ size: 5, sort: 'issueDate,desc' }),
      libraryApi.bookIssues.overdue({ size: 5 }),
    ])
      .then(([dashRes, issuesRes, overdueRes]) => {
        if (dashRes.status === 'fulfilled') setDashboard(dashRes.value.data);
        else enqueueSnackbar('Could not load the library dashboard.', { variant: 'error' });
        if (issuesRes.status === 'fulfilled') setRecentIssues(issuesRes.value.data.content);
        if (overdueRes.status === 'fulfilled') setOverdue(overdueRes.value.data.content);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading dashboard..." />;

  return (
    <Box>
      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard icon={<MenuBookOutlinedIcon />} label="Total Books" value={formatNumber(dashboard?.totalBooks ?? 0)} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<LibraryBooksOutlinedIcon />}
            label="Available Copies"
            value={formatNumber(dashboard?.availableCopies ?? 0)}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<AssignmentTurnedInOutlinedIcon />}
            label="Currently Issued"
            value={formatNumber(dashboard?.issuedCount ?? 0)}
            color="info"
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={3}>
          <StatCard
            icon={<WarningAmberOutlinedIcon />}
            label="Overdue"
            value={formatNumber(dashboard?.overdueCount ?? 0)}
            color="warning"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardHeader
              title="Recent Issues"
              subheader="Latest books issued"
              action={
                <Button size="small" component={RouterLink} to="/app/library/books">
                  Manage Books
                </Button>
              }
            />
            <CardContent sx={{ pt: 0 }}>
              {recentIssues.length === 0 ? (
                <EmptyState title="No books issued yet" description="Books you issue will show up here." />
              ) : (
                <List disablePadding>
                  {recentIssues.map((issue, idx) => (
                    <Box key={issue.id}>
                      <ListItem disableGutters sx={{ py: 1.25 }}>
                        <ListItemText
                          primary={issue.bookTitle ?? `Book #${issue.bookId}`}
                          secondary={`${issue.studentName ?? issue.teacherName ?? 'Unknown borrower'} · Due ${dayjs(issue.dueDate).format('DD MMM YYYY')}`}
                          primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                          secondaryTypographyProps={{ fontSize: '0.75rem' }}
                        />
                        <Chip
                          label={issue.status}
                          size="small"
                          color={issue.status === 'OVERDUE' ? 'error' : issue.status === 'RETURNED' ? 'success' : 'default'}
                          variant="outlined"
                        />
                      </ListItem>
                      {idx < recentIssues.length - 1 && <Divider />}
                    </Box>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Overdue Books" subheader="Need follow-up" />
            <CardContent sx={{ pt: 0 }}>
              {overdue.length === 0 ? (
                <EmptyState
                  icon={<CheckCircleOutlineIcon fontSize="large" color="success" />}
                  title="No overdue books"
                  description="Everything is returned on time. Nice work."
                />
              ) : (
                <List disablePadding>
                  {overdue.map((issue, idx) => (
                    <Box key={issue.id}>
                      <ListItem disableGutters sx={{ py: 1.25 }}>
                        <ListItemText
                          primary={issue.bookTitle ?? `Book #${issue.bookId}`}
                          secondary={`${issue.studentName ?? issue.teacherName ?? 'Unknown borrower'} · Was due ${dayjs(issue.dueDate).format('DD MMM YYYY')}`}
                          primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                          secondaryTypographyProps={{ fontSize: '0.75rem' }}
                        />
                        <Chip label={`Fine ₹${issue.fineAmount}`} size="small" color="error" variant="outlined" />
                      </ListItem>
                      {idx < overdue.length - 1 && <Divider />}
                    </Box>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={6}>
          <Card sx={{ height: '100%' }}>
            <CardHeader title="Quick Actions" subheader="Jump to what you use most" />
            <CardContent sx={{ pt: 0 }}>
              <Stack spacing={1.5}>
                <Button variant="outlined" fullWidth component={RouterLink} to="/app/library/books">
                  Issue / Return a Book
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} lg={6}>
          <NotificationsCard />
        </Grid>
      </Grid>
    </Box>
  );
}

export default LibrarianDashboard;
