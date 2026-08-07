import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import CircularProgress from '@mui/material/CircularProgress';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import { useSnackbar } from 'notistack';
import EmptyState from '@/components/common/EmptyState';
import PageLoader from '@/components/common/PageLoader';
import examApi from '@/api/examApi';
import type { Exam, ExamSchedule } from '@/types';
import { getStudentDisplayName } from '@/utils/format';

interface RowState {
  studentId: number;
  studentName: string;
  rollNumber: string;
  marksObtained: string;
  remarks: string;
}

/** Select exam -> schedule (subject) -> editable marks grid with live percentage, bulk-saved via /marks/entry. */
export function MarksEntryPage() {
  const { enqueueSnackbar } = useSnackbar();

  const [exams, setExams] = useState<Exam[]>([]);
  const [examId, setExamId] = useState<number | ''>('');
  const [schedules, setSchedules] = useState<ExamSchedule[]>([]);
  const [scheduleId, setScheduleId] = useState<number | ''>('');

  const [rows, setRows] = useState<RowState[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    examApi.exams
      .list({ size: 200, sort: 'startDate,desc' })
      .then((res) => setExams(res.data.content))
      .catch(() => enqueueSnackbar('Could not load exams.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!examId) {
      setSchedules([]);
      setScheduleId('');
      return;
    }
    examApi.schedules
      .list(examId as number)
      .then((res) => setSchedules(res.data))
      .catch(() => enqueueSnackbar('Could not load schedules for this exam.', { variant: 'error' }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [examId]);

  const selectedSchedule = schedules.find((s) => s.id === scheduleId) ?? null;

  const loadGrid = useCallback(async () => {
    if (!scheduleId) return;
    setLoading(true);
    setLoaded(false);
    try {
      const res = await examApi.marks.roster(scheduleId as number);
      setRows(
        res.data.map((r) => ({
          studentId: r.studentId,
          studentName: getStudentDisplayName({ firstName: r.firstName, lastName: r.lastName }),
          rollNumber: r.rollNumber !== null ? String(r.rollNumber) : '-',
          marksObtained: r.marksObtained !== null && r.marksObtained !== undefined ? String(r.marksObtained) : '',
          remarks: r.remarks ?? '',
        })),
      );
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load the class roster for this schedule.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [scheduleId, enqueueSnackbar]);

  useEffect(() => {
    loadGrid();
  }, [loadGrid]);

  const setMarks = (studentId: number, value: string) => {
    setRows((prev) => prev.map((r) => (r.studentId === studentId ? { ...r, marksObtained: value } : r)));
  };

  const setRemarks = (studentId: number, value: string) => {
    setRows((prev) => prev.map((r) => (r.studentId === studentId ? { ...r, remarks: value } : r)));
  };

  const maxMarks = selectedSchedule?.maxMarks ?? 0;

  const invalidCount = useMemo(
    () =>
      rows.filter((r) => {
        if (r.marksObtained === '') return false;
        const num = Number(r.marksObtained);
        return Number.isNaN(num) || num < 0 || num > maxMarks;
      }).length,
    [rows, maxMarks],
  );

  const handleSave = async () => {
    if (!scheduleId) return;
    const records = rows
      .filter((r) => r.marksObtained !== '')
      .map((r) => ({ studentId: r.studentId, marksObtained: Number(r.marksObtained), remarks: r.remarks || undefined }));

    if (records.length === 0) {
      enqueueSnackbar('Enter marks for at least one student before saving.', { variant: 'warning' });
      return;
    }
    if (invalidCount > 0) {
      enqueueSnackbar(`${invalidCount} row(s) have marks outside 0–${maxMarks}. Fix them before saving.`, { variant: 'warning' });
      return;
    }
    setSaving(true);
    try {
      await examApi.marks.saveEntry({ examScheduleId: scheduleId as number, records });
      enqueueSnackbar(`Marks saved for ${records.length} student${records.length === 1 ? '' : 's'}.`, { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save marks. Please try again.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

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
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                select
                fullWidth
                size="small"
                label="Subject / Schedule"
                disabled={!examId}
                value={scheduleId}
                onChange={(e) => setScheduleId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">Select a subject</MenuItem>
                {schedules.map((s) => (
                  <MenuItem key={s.id} value={s.id}>
                    {s.subjectName ?? `Subject #${s.subjectId}`} (Max {s.maxMarks})
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            {selectedSchedule && (
              <Grid item>
                <Chip size="small" variant="outlined" label={`Max Marks: ${selectedSchedule.maxMarks}`} />
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>

      {!scheduleId ? (
        <EmptyState title="Select an exam and subject" description="Choose an exam and subject schedule above to load the marks entry grid." />
      ) : loading ? (
        <PageLoader label="Loading students..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No students found" description="This exam's class has no enrolled students." />
      ) : (
        <Card>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2, py: 1.5 }}>
            <Stack direction="row" spacing={1} alignItems="center">
              <Chip size="small" variant="outlined" label={`${rows.length} student${rows.length === 1 ? '' : 's'}`} />
              {invalidCount > 0 && (
                <Chip size="small" color="error" variant="outlined" label={`${invalidCount} invalid`} />
              )}
            </Stack>
            <Button
              variant="contained"
              startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveOutlinedIcon />}
              disabled={saving}
              onClick={handleSave}
            >
              Save Marks
            </Button>
          </Box>
          <TableContainer sx={{ maxHeight: 600 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ minWidth: 60 }}>Roll No.</TableCell>
                  <TableCell sx={{ minWidth: 180 }}>Student</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>Marks Obtained</TableCell>
                  <TableCell sx={{ minWidth: 100 }}>Percentage</TableCell>
                  <TableCell sx={{ minWidth: 200 }}>Remarks</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => {
                  const num = row.marksObtained === '' ? null : Number(row.marksObtained);
                  const invalid = num !== null && (Number.isNaN(num) || num < 0 || num > maxMarks);
                  const pct = num !== null && !invalid && maxMarks > 0 ? ((num / maxMarks) * 100).toFixed(1) : null;
                  return (
                    <TableRow key={row.studentId} hover>
                      <TableCell>{row.rollNumber}</TableCell>
                      <TableCell>{row.studentName}</TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          type="number"
                          value={row.marksObtained}
                          onChange={(e) => setMarks(row.studentId, e.target.value)}
                          error={invalid}
                          inputProps={{ min: 0, max: maxMarks, step: '0.5' }}
                          sx={{ width: 100 }}
                        />
                      </TableCell>
                      <TableCell>
                        {pct !== null ? (
                          <Chip
                            size="small"
                            label={`${pct}%`}
                            color={Number(pct) >= 40 ? 'success' : 'error'}
                            variant="outlined"
                          />
                        ) : (
                          '-'
                        )}
                      </TableCell>
                      <TableCell>
                        <TextField
                          size="small"
                          fullWidth
                          placeholder="Optional remarks"
                          value={row.remarks}
                          onChange={(e) => setRemarks(row.studentId, e.target.value)}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}
    </Box>
  );
}

export default MarksEntryPage;
