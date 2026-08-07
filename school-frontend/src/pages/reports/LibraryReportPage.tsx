import { useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Grid from '@mui/material/Grid';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import StatCard from '@/components/common/StatCard';
import PageLoader from '@/components/common/PageLoader';
import DataTable from '@/components/common/DataTable';
import PieChartCard from '@/components/charts/PieChartCard';
import reportsApi from '@/api/reportsApi';
import { exportRowsToCsv } from '@/utils/csvExport';
import { formatNumber } from '@/utils/format';
import type { CategoryCountBreakdown, LibrarySummaryReport } from '@/types';

/** Library summary — total books/issued/overdue stat cards + byCategory pie + an exportable table. */
export function LibraryReportPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [report, setReport] = useState<LibrarySummaryReport | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    reportsApi
      .getLibrarySummary()
      .then((res) => setReport(res.data))
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the library report.', { variant: 'error' });
        setReport(null);
      })
      .finally(() => setLoading(false));
  }, [enqueueSnackbar]);

  const columns: GridColDef<CategoryCountBreakdown>[] = useMemo(
    () => [
      { field: 'categoryName', headerName: 'Category', flex: 1 },
      { field: 'count', headerName: 'Book Count', flex: 1 },
    ],
    [],
  );

  const handleExport = () => {
    if (!report) return;
    exportRowsToCsv(
      report.byCategory as unknown as Record<string, unknown>[],
      [
        { field: 'categoryName', header: 'Category' },
        { field: 'count', header: 'Book Count' },
      ],
      'library-by-category-report',
    );
  };

  if (loading) return <PageLoader label="Loading library report..." />;
  if (!report) return null;

  const pieData = report.byCategory.map((row) => ({ name: row.categoryName, value: row.count }));

  return (
    <Box>
      <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<MenuBookOutlinedIcon />} label="Total Books" value={formatNumber(report.totalBooks)} color="primary" />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<AssignmentTurnedInOutlinedIcon />} label="Total Issued" value={formatNumber(report.totalIssued)} color="info" />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard icon={<WarningAmberOutlinedIcon />} label="Overdue" value={formatNumber(report.totalOverdue)} color="error" />
        </Grid>
      </Grid>

      <Box sx={{ mb: 2.5 }}>
        <PieChartCard title="Books by Category" data={pieData} />
      </Box>

      <Card>
        <DataTable
          rows={report.byCategory}
          columns={columns}
          getRowId={(row) => row.categoryName}
          onExport={handleExport}
          emptyTitle="No category breakdown available"
        />
      </Card>
    </Box>
  );
}

export default LibraryReportPage;
