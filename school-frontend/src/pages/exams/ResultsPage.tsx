import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import EmptyState from '@/components/common/EmptyState';
import BarChartCard from '@/components/charts/BarChartCard';
import examApi from '@/api/examApi';
import studentsApi from '@/api/studentsApi';
import { useAppSelector } from '@/store/hooks';
import type { Exam, ExamResultRow, ExamSchedule, ReportCard } from '@/types';
import { downloadBlob } from '@/utils/downloadBlob';
import ReportCardDialog from './components/ReportCardDialog';
import ReportCardView from './components/ReportCardView';

/** Staff view: pick exam -> results table with rank/percentage + a class-average-per-subject bar chart. */
function StaffResultsView() {
  const { enqueueSnackbar } = useSnackbar();
  const [exams, setExams] = useState<Exam[]>([]);
  const [examId, setExamId] = useState<number | ''>('');

  const [rows, setRows] = useState<ExamResultRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const [chartData, setChartData] = useState<Array<{ subject: string; average: number }>>([]);
  const [chartLoading, setChartLoading] = useState(false);

  const [reportCardTarget, setReportCardTarget] = useState<number | null>(null);

  const selectedExam = exams.find((e) => e.id === examId) ?? null;

  useEffect(() => {
    examApi.exams
      .list({ size: 200, sort: 'startDate,desc' })
      .then((res) => setExams(res.data.content))
      .catch(() => enqueueSnackbar('Could not load exams.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);


  const loadResults = useCallback(async () => {
    if (!selectedExam) return;
    setLoading(true);
    setLoaded(false);
    try {
      const res = await examApi.exams.results(selectedExam.id, {
        classId: selectedExam.classId,
      });
      setRows(res.data);
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load results for this exam.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [selectedExam, enqueueSnackbar]);

  useEffect(() => {
    loadResults();
  }, [loadResults]);

  const loadChart = useCallback(async () => {
    if (!selectedExam) {
      setChartData([]);
      return;
    }
    setChartLoading(true);
    try {
      const scheduleRes = await examApi.schedules.list(selectedExam.id);
      const schedules: ExamSchedule[] = scheduleRes.data;
      const perSubject = await Promise.all(
        schedules.map(async (s) => {
          const marksRes = await examApi.marks.list({ examScheduleId: s.id, size: 500 });
          const entered = marksRes.data.content.filter((m) => m.marksObtained !== null && m.marksObtained !== undefined);
          const average =
            entered.length > 0 && s.maxMarks > 0
              ? (entered.reduce((sum, m) => sum + (m.marksObtained ?? 0), 0) / entered.length / s.maxMarks) * 100
              : 0;
          return { subject: s.subjectName ?? `Subject #${s.subjectId}`, average: Number(average.toFixed(1)) };
        }),
      );
      setChartData(perSubject);
    } catch {
      setChartData([]);
    } finally {
      setChartLoading(false);
    }
  }, [selectedExam]);

  useEffect(() => {
    loadChart();
  }, [loadChart]);

  const columns: GridColDef<ExamResultRow>[] = useMemo(
    () => [
      { field: 'rank', headerName: 'Rank', width: 80 },
      { field: 'rollNumber', headerName: 'Roll No.', width: 100 },
      { field: 'studentName', headerName: 'Student', flex: 1, minWidth: 170 },
      { field: 'totalObtained', headerName: 'Obtained', width: 100 },
      { field: 'totalMax', headerName: 'Max', width: 90 },
      {
        field: 'percentage',
        headerName: 'Percentage',
        width: 130,
        renderCell: (params) => (
          <Chip
            size="small"
            variant="outlined"
            label={`${params.row.percentage.toFixed(1)}%`}
            color={params.row.percentage >= 40 ? 'success' : 'error'}
          />
        ),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 130,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Tooltip title="View report card">
            <IconButton size="small" onClick={() => setReportCardTarget(params.row.studentId)}>
              <DescriptionOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
      },
    ],
    [],
  );

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                select
                fullWidth
                size="small"
                label="Exam"
                value={examId}
                onChange={(e) => setExamId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">Select an exam</MenuItem>
                {exams.map((ex) => (
                  <MenuItem key={ex.id} value={ex.id}>
                    {(ex.examTypeName ?? `Exam #${ex.id}`) + ' — ' + (ex.className ?? '')}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {!selectedExam ? (
        <EmptyState title="Select an exam" description="Choose an exam above to view its class results and report cards." />
      ) : (
        <Stack spacing={2.5}>
          {chartLoading ? (
            <Card sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress size={28} />
            </Card>
          ) : chartData.length > 0 ? (
            <BarChartCard
              title="Class Average by Subject"
              subtitle={`${selectedExam.examTypeName ?? 'Exam'} — ${selectedExam.className ?? ''}`}
              data={chartData}
              xKey="subject"
              series={[{ key: 'average', label: 'Average %' }]}
              valueFormatter={(v) => `${v}%`}
            />
          ) : null}

          <Card>
            <DataTable
              rows={rows}
              columns={columns}
              loading={loading}
              getRowId={(row) => row.studentId}
              mobileVisibleFields={['studentName', 'percentage']}
              emptyTitle="No results found"
              emptyDescription={loaded ? 'Marks may not have been entered for this exam yet.' : undefined}
            />
          </Card>
        </Stack>
      )}

      <ReportCardDialog
        open={!!reportCardTarget}
        studentId={reportCardTarget}
        examId={selectedExam?.id ?? null}
        onClose={() => setReportCardTarget(null)}
      />
    </Box>
  );
}

/** STUDENT/PARENT self-view: pick an exam, see the own report card directly (no class-wide results). */
function SelfResultsView() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const [exams, setExams] = useState<Exam[]>([]);
  const [examId, setExamId] = useState<number | ''>('');
  const [report, setReport] = useState<ReportCard | null>(null);
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!user?.classId) return;
    examApi.exams
      .list({ classId: user.classId, size: 200, sort: 'startDate,desc' })
      .then((res) => setExams(res.data.content))
      .catch(() => enqueueSnackbar('Could not load your exams.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.classId]);

  useEffect(() => {
    if (!examId || !user?.studentId) {
      setReport(null);
      return;
    }
    setLoading(true);
    setLoaded(false);
    Promise.all([
      examApi.marks.getReportCard(user.studentId, examId as number),
      studentsApi.getById(user.studentId).catch(() => null),
    ])
      .then(([reportRes, studentRes]) => {
        setReport(reportRes.data);
        setPhotoUrl(studentRes?.data.photoUrl ?? null);
        setLoaded(true);
      })
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load your report card.', { variant: 'error' });
        setReport(null);
        setLoaded(true);
      })
      .finally(() => setLoading(false));
  }, [examId, user?.studentId, enqueueSnackbar]);

  if (!user?.studentId) {
    return <EmptyState title="No student record linked" description="Your account is not linked to a student record." />;
  }

  const handleDownloadPdf = async () => {
    if (!user?.studentId || !examId) return;
    setDownloading(true);
    try {
      const blob = await examApi.marks.getReportCardPdf(user.studentId, examId as number);
      downloadBlob(blob, `report-card-${user.studentId}-${examId}.pdf`);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not download your report card PDF.', { variant: 'error' });
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <TextField
            select
            fullWidth
            size="small"
            label="Exam"
            value={examId}
            onChange={(e) => setExamId(e.target.value === '' ? '' : Number(e.target.value))}
            sx={{ maxWidth: 360 }}
          >
            <MenuItem value="">Select an exam</MenuItem>
            {exams.map((ex) => (
              <MenuItem key={ex.id} value={ex.id}>
                {ex.examTypeName ?? `Exam #${ex.id}`}
              </MenuItem>
            ))}
          </TextField>
        </CardContent>
      </Card>

      {!examId ? (
        <EmptyState title="Select an exam" description="Choose an exam above to view your report card." />
      ) : loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress size={32} />
        </Box>
      ) : report ? (
        <Stack spacing={1.5}>
          <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button
              variant="outlined"
              size="small"
              startIcon={downloading ? <CircularProgress size={16} color="inherit" /> : <PictureAsPdfOutlinedIcon />}
              disabled={downloading}
              onClick={handleDownloadPdf}
            >
              Download PDF
            </Button>
          </Box>
          <ReportCardView report={report} photoUrl={photoUrl} />
        </Stack>
      ) : loaded ? (
        <EmptyState title="Report card not available" description="Your report card for this exam has not been published yet." />
      ) : null}
    </Box>
  );
}

/** Branches by role: management/teachers get the full results + report-card workflow; STUDENT/PARENT get their own report card directly. */
export function ResultsPage() {
  const role = useAppSelector((state) => state.auth.user?.role);
  const isSelfView = role === 'STUDENT' || role === 'PARENT';
  return isSelfView ? <SelfResultsView /> : <StaffResultsView />;
}

export default ResultsPage;
