import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Chip from '@mui/material/Chip';
import Alert from '@mui/material/Alert';
import LinearProgress from '@mui/material/LinearProgress';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import StatCard from '@/components/common/StatCard';
import EmptyState from '@/components/common/EmptyState';
import type { ClassOverview } from '@/types';

export interface ClassOverviewTabProps {
  overview: ClassOverview | null;
}

const PLACEHOLDER = '—';

/** Renders a nullable stat without letting a missing figure read as a zero result. */
function statValue(value: number | null | undefined, suffix = ''): string {
  return value === null || value === undefined ? PLACEHOLDER : `${value}${suffix}`;
}

/**
 * Read-only summary of a class: strength against capacity, who teaches and who
 * holds a post, cross-module figures, and the setup gaps worth fixing.
 *
 * <p>Everything here comes from one `/classes/{id}/overview` call — the strength,
 * the gender split and the warnings all derive from the same roster, so fetching
 * them separately risks showing figures that disagree with each other.
 */
export function ClassOverviewTab({ overview }: ClassOverviewTabProps) {
  if (!overview) {
    return <EmptyState title="No overview available" description="This class could not be summarised." />;
  }

  const { genderSplit, stats, warnings, occupancyPercentage, totalCapacity } = overview;
  const genderEntries = Object.entries(genderSplit ?? {});
  const overCapacity = occupancyPercentage != null && occupancyPercentage > 100;

  return (
    <Box>
      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={6} md={3}>
          <StatCard
            icon={<GroupsOutlinedIcon />}
            label="Total Students"
            value={overview.totalStudents}
            color="primary"
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <StatCard
            icon={<SchoolOutlinedIcon />}
            label="Sections / Subjects"
            value={`${overview.totalSections} / ${overview.totalSubjects}`}
            color="info"
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <StatCard
            icon={<EventAvailableOutlinedIcon />}
            label="Attendance"
            value={statValue(stats?.attendancePercentage, '%')}
            color="success"
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <StatCard
            icon={<PaymentsOutlinedIcon />}
            label="Fee Defaulters"
            value={statValue(stats?.feeDefaulterCount)}
            color="warning"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Strength
              </Typography>

              {totalCapacity == null ? (
                <Typography variant="body2" color="text.secondary">
                  No section declares a capacity, so occupancy cannot be worked out.
                </Typography>
              ) : (
                <Box sx={{ mb: 2 }}>
                  <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
                    <Typography variant="body2" color="text.secondary">
                      {overview.totalStudents} of {totalCapacity} seats
                    </Typography>
                    <Typography variant="body2" fontWeight={600} color={overCapacity ? 'error.main' : 'text.primary'}>
                      {occupancyPercentage}%
                    </Typography>
                  </Stack>
                  <LinearProgress
                    variant="determinate"
                    // The bar itself is capped so an over-subscribed class does not
                    // overflow its track; the figure above it still reads past 100.
                    value={Math.min(occupancyPercentage ?? 0, 100)}
                    color={overCapacity ? 'error' : 'primary'}
                    sx={{ height: 8, borderRadius: 1 }}
                  />
                </Box>
              )}

              <Typography variant="caption" color="text.secondary">
                Gender split
              </Typography>
              <Stack direction="row" spacing={1} sx={{ mt: 0.5 }} flexWrap="wrap" useFlexGap>
                {genderEntries.length === 0 ? (
                  <Typography variant="body2" color="text.secondary">
                    No students enrolled yet.
                  </Typography>
                ) : (
                  genderEntries.map(([gender, count]) => (
                    <Chip key={gender} size="small" variant="outlined" label={`${gender}: ${count}`} />
                  ))
                )}
              </Stack>

              <Box sx={{ mt: 2 }}>
                <Typography variant="caption" color="text.secondary">
                  Averages
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 0.5 }} flexWrap="wrap" useFlexGap>
                  <Chip
                    size="small"
                    variant="outlined"
                    label={`Avg marks: ${statValue(stats?.averageMarksPercentage, '%')}`}
                  />
                  <Chip
                    size="small"
                    variant="outlined"
                    label={`Outstanding fees: ${statValue(stats?.feeOutstandingAmount)}`}
                  />
                  <Chip
                    size="small"
                    variant="outlined"
                    label={`Marked days: ${statValue(stats?.attendanceMarkedDays)}`}
                  />
                </Stack>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Setup checks
              </Typography>
              {warnings.length === 0 ? (
                <Alert severity="success" variant="outlined">
                  Nothing outstanding — every section has a class teacher and every subject a teacher.
                </Alert>
              ) : (
                <Stack spacing={1}>
                  {warnings.map((warning, index) => (
                    <Alert
                      key={`${warning.code}-${index}`}
                      severity={warning.severity === 'WARNING' ? 'warning' : 'info'}
                      variant="outlined"
                      sx={{ py: 0.25 }}
                    >
                      {warning.message}
                    </Alert>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card variant="outlined">
            <CardContent sx={{ pb: 0 }}>
              <Typography variant="subtitle1" fontWeight={700}>
                Sections
              </Typography>
            </CardContent>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Section</TableCell>
                    <TableCell>Class Teacher</TableCell>
                    <TableCell>Room</TableCell>
                    <TableCell align="right">Students</TableCell>
                    <TableCell align="right">Capacity</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {overview.sections.map((section) => {
                    const full =
                      section.capacity != null &&
                      section.capacity > 0 &&
                      (section.studentCount ?? 0) > section.capacity;
                    return (
                      <TableRow key={section.id} hover>
                        <TableCell>{section.sectionName}</TableCell>
                        <TableCell>
                          {section.classTeacherName ?? (
                            <Chip size="small" color="warning" variant="outlined" label="Unassigned" />
                          )}
                        </TableCell>
                        <TableCell>{section.roomNumber ?? PLACEHOLDER}</TableCell>
                        <TableCell align="right">
                          <Typography variant="body2" color={full ? 'error.main' : 'text.primary'} fontWeight={full ? 700 : 400}>
                            {section.studentCount ?? 0}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">{section.capacity ?? PLACEHOLDER}</TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default ClassOverviewTab;
