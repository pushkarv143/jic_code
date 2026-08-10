import { useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Grid from '@mui/material/Grid';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import DataTable from '@/components/common/DataTable';
import PieChartCard from '@/components/charts/PieChartCard';
import reportsApi from '@/api/reportsApi';
import { exportRowsToCsv } from '@/utils/csvExport';
import { formatNumber } from '@/utils/format';
import type { DepartmentCountBreakdown, TeachersSummaryReport } from '@/types';

/** Teachers summary — total active + byDepartment breakdown pie + an exportable byDepartment table. */
export function TeachersReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [report, setReport] = useState<TeachersSummaryReport | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    reportsApi
      .getTeachersSummary()
      .then((res) => setReport(res.data))
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the teachers report.', { variant: 'error' });
        setReport(null);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  const columns: GridColDef<DepartmentCountBreakdown>[] = useMemo(
    () => [
      { field: 'departmentName', headerName: 'Department', flex: 1 },
      { field: 'count', headerName: 'Teacher Count', flex: 1 },
    ],
    [],
  );

  const handleExport = () => {
    if (!report) return;
    exportRowsToCsv(
      report.byDepartment as unknown as Record<string, unknown>[],
      [
        { field: 'departmentName', header: 'Department' },
        { field: 'count', header: 'Teacher Count' },
      ],
      'teachers-by-department-report',
    );
  };

  if (loading) return <PageLoader label="Loading teachers report..." />;
  if (!report) return null;

  const byDepartmentPie = report.byDepartment.map((row) => ({ name: row.departmentName, value: row.count }));

  return (
    <Box>
      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<BadgeOutlinedIcon />} label="Total Active Teachers" value={formatNumber(report.totalActive)} color="info" />
        </Grid>
      </Grid>

      <Box sx={{ mb: 2.5 }}>
        <PieChartCard title="Teachers by Department" data={byDepartmentPie} />
      </Box>

      <Card>
        <DataTable
          rows={report.byDepartment}
          columns={columns}
          mobileVisibleFields={['departmentName', 'count']}
          getRowId={(row) => row.departmentName}
          onExport={handleExport}
          emptyTitle="No department breakdown available"
        />
      </Card>
    </Box>
  );
}

export default TeachersReportPage;
