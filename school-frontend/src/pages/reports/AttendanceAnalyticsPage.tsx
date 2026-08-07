import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs, { type Dayjs } from 'dayjs';
import PercentOutlinedIcon from '@mui/icons-material/PercentOutlined';
import StatCard from '@/components/common/StatCard';
import DataTable from '@/components/common/DataTable';
import BarChartCard from '@/components/charts/BarChartCard';
import reportsApi from '@/api/reportsApi';
import classesApi from '@/api/classesApi';
import { exportRowsToCsv } from '@/utils/csvExport';
import type { AttendanceSummaryReport, ClassPercentageBreakdown, SchoolClass } from '@/types';

/** Attendance analytics — date range + class filters -> average % stat, byClass bar chart and an exportable table. */
export function AttendanceAnalyticsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [startDate, setStartDate] = useState<Dayjs>(dayjs().startOf('month'));
  const [endDate, setEndDate] = useState<Dayjs>(dayjs());

  const [report, setReport] = useState<AttendanceSummaryReport | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    classesApi.list({ size: 200, sort: 'className,asc' }).then((res) => setClasses(res.data.content)).catch(() => undefined);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reportsApi.getAttendanceSummary({
        startDate: startDate.format('YYYY-MM-DD'),
        endDate: endDate.format('YYYY-MM-DD'),
        classId: classId || undefined,
      });
      setReport(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the attendance report.', { variant: 'error' });
      setReport(null);
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate, classId, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: GridColDef<ClassPercentageBreakdown>[] = useMemo(
    () => [
      { field: 'className', headerName: 'Class', flex: 1 },
      { field: 'percentage', headerName: 'Attendance %', flex: 1, valueFormatter: (value) => `${Number(value ?? 0).toFixed(1)}%` },
    ],
    [],
  );

  const handleExport = () => {
    if (!report) return;
    exportRowsToCsv(
      report.byClass as unknown as Record<string, unknown>[],
      [
        { field: 'className', header: 'Class' },
        { field: 'percentage', header: 'Attendance %' },
      ],
      'attendance-by-class-report',
    );
  };

  const chartData = (report?.byClass ?? []).map((row) => ({ className: row.className, Attendance: row.percentage }));

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Class"
                value={classId}
                onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All classes</MenuItem>
                {classes.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.className}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={6} sm={4} md={3}>
              <DatePicker
                label="Start Date"
                value={startDate}
                onChange={(value) => value && setStartDate(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' } }}
              />
            </Grid>
            <Grid item xs={6} sm={4} md={3}>
              <DatePicker
                label="End Date"
                value={endDate}
                onChange={(value) => value && setEndDate(value)}
                disableFuture
                slotProps={{ textField: { fullWidth: true, size: 'small' } }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {report && (
        <Grid container spacing={2.5} sx={{ mb: 2.5 }}>
          <Grid item xs={12} sm={4}>
            <StatCard icon={<PercentOutlinedIcon />} label="Average Attendance" value={`${report.averagePercentage.toFixed(1)}%`} color="success" />
          </Grid>
        </Grid>
      )}

      <Box sx={{ mb: 2.5 }}>
        <BarChartCard
          title="Attendance by Class"
          subtitle="Average attendance percentage for the selected period"
          data={chartData}
          xKey="className"
          series={[{ key: 'Attendance', label: 'Attendance %' }]}
          valueFormatter={(v) => `${v.toFixed(1)}%`}
        />
      </Box>

      <Card>
        <DataTable
          rows={report?.byClass ?? []}
          columns={columns}
          loading={loading}
          getRowId={(row) => row.className}
          onExport={handleExport}
          emptyTitle="No attendance data found"
          emptyDescription="Try adjusting the class or date range filters."
        />
      </Card>
    </Box>
  );
}

export default AttendanceAnalyticsPage;
