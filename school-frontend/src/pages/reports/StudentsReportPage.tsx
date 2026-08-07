import { useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Grid from '@mui/material/Grid';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import DataTable from '@/components/common/DataTable';
import PieChartCard from '@/components/charts/PieChartCard';
import reportsApi from '@/api/reportsApi';
import { exportRowsToCsv } from '@/utils/csvExport';
import { formatNumber } from '@/utils/format';
import type { ClassCountBreakdown, StudentsSummaryReport } from '@/types';

/** Students summary — total active + byClass / byStatus breakdown pies + an exportable byClass table. */
export function StudentsReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [report, setReport] = useState<StudentsSummaryReport | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    reportsApi
      .getStudentsSummary()
      .then((res) => setReport(res.data))
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the students report.', { variant: 'error' });
        setReport(null);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  const columns: GridColDef<ClassCountBreakdown>[] = useMemo(
    () => [
      { field: 'className', headerName: 'Class', flex: 1 },
      { field: 'count', headerName: 'Student Count', flex: 1 },
    ],
    [],
  );

  const handleExport = () => {
    if (!report) return;
    exportRowsToCsv(
      report.byClass as unknown as Record<string, unknown>[],
      [
        { field: 'className', header: 'Class' },
        { field: 'count', header: 'Student Count' },
      ],
      'students-by-class-report',
    );
  };

  if (loading) return <PageLoader label="Loading students report..." />;
  if (!report) return null;

  const byClassPie = report.byClass.map((row) => ({ name: row.className, value: row.count }));
  const byStatusPie = report.byStatus.map((row) => ({ name: row.status, value: row.count }));

  return (
    <Box>
      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<SchoolOutlinedIcon />} label="Total Active Students" value={formatNumber(report.totalActive)} color="primary" />
        </Grid>
      </Grid>

      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={12} md={6}>
          <PieChartCard title="Students by Class" data={byClassPie} />
        </Grid>
        <Grid item xs={12} md={6}>
          <PieChartCard title="Students by Status" data={byStatusPie} />
        </Grid>
      </Grid>

      <Card>
        <DataTable
          rows={report.byClass}
          columns={columns}
          getRowId={(row) => row.className}
          onExport={handleExport}
          emptyTitle="No class breakdown available"
        />
      </Card>
    </Box>
  );
}

export default StudentsReportPage;
