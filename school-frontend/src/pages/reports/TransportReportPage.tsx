import { useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import { useSnackbar } from 'notistack';
import DirectionsBusOutlinedIcon from '@mui/icons-material/DirectionsBusOutlined';
import AltRouteOutlinedIcon from '@mui/icons-material/AltRouteOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import reportsApi from '@/api/reportsApi';
import { formatNumber } from '@/utils/format';
import type { TransportSummaryReport } from '@/types';

/** Transport summary — total buses / routes / students using transport. No breakdown data in the contract, so this is stat cards only. */
export function TransportReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [report, setReport] = useState<TransportSummaryReport | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    reportsApi
      .getTransportSummary()
      .then((res) => setReport(res.data))
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the transport report.', { variant: 'error' });
        setReport(null);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading transport report..." />;
  if (!report) return null;

  return (
    <Box>
      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<DirectionsBusOutlinedIcon />} label="Total Buses" value={formatNumber(report.totalBuses)} color="primary" />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<AltRouteOutlinedIcon />} label="Total Routes" value={formatNumber(report.totalRoutes)} color="info" />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<GroupsOutlinedIcon />} label="Students Using Transport" value={formatNumber(report.studentsUsingTransport)} color="success" />
        </Grid>
      </Grid>
    </Box>
  );
}

export default TransportReportPage;
