import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import GlobalStyles from '@mui/material/GlobalStyles';
import PrintOutlinedIcon from '@mui/icons-material/PrintOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import { useSnackbar } from 'notistack';
import examApi from '@/api/examApi';
import studentsApi from '@/api/studentsApi';
import EmptyState from '@/components/common/EmptyState';
import type { ReportCard } from '@/types';
import { downloadBlob } from '@/utils/downloadBlob';
import ReportCardView from './ReportCardView';

export interface ReportCardDialogProps {
  open: boolean;
  studentId: number | null;
  examId: number | null;
  onClose: () => void;
}

const PRINT_AREA_ID = 'report-card-dialog-print-area';

/** Fetches and prints a single student's report card (GET /marks/report-card/{studentId}?examId=) inside a dialog. */
export function ReportCardDialog({ open, studentId, examId, onClose }: ReportCardDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [report, setReport] = useState<ReportCard | null>(null);
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!open || !studentId || !examId) return;
    setLoading(true);
    setReport(null);
    Promise.all([
      examApi.marks.getReportCard(studentId, examId),
      studentsApi.getById(studentId).catch(() => null),
    ])
      .then(([reportRes, studentRes]) => {
        setReport(reportRes.data);
        setPhotoUrl(studentRes?.data.photoUrl ?? null);
      })
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this report card.', { variant: 'error' });
      })
      .finally(() => setLoading(false));
  }, [open, studentId, examId, enqueueSnackbar]);

  const handleDownloadPdf = async () => {
    if (!studentId || !examId) return;
    setDownloading(true);
    try {
      const blob = await examApi.marks.getReportCardPdf(studentId, examId);
      downloadBlob(blob, `report-card-${studentId}-${examId}.pdf`);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not download the report card PDF.', { variant: 'error' });
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <GlobalStyles
        styles={{
          '@media print': {
            'body *': { visibility: 'hidden' },
            [`#${PRINT_AREA_ID}, #${PRINT_AREA_ID} *`]: { visibility: 'visible' },
            [`#${PRINT_AREA_ID}`]: { position: 'fixed', inset: 0, width: '100%' },
          },
        }}
      />
      <DialogContent sx={{ bgcolor: 'action.hover', py: 3 }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress size={32} />
          </Box>
        ) : report ? (
          <ReportCardView report={report} photoUrl={photoUrl} printAreaId={PRINT_AREA_ID} />
        ) : (
          <EmptyState title="Report card not available" description="This student's report card could not be loaded for this exam." />
        )}
      </DialogContent>
      <DialogActions sx={{ '@media print': { display: 'none' } }}>
        <Button onClick={onClose}>Close</Button>
        <Button
          variant="outlined"
          startIcon={downloading ? <CircularProgress size={16} color="inherit" /> : <PictureAsPdfOutlinedIcon />}
          disabled={!report || downloading}
          onClick={handleDownloadPdf}
        >
          Download PDF
        </Button>
        <Button variant="contained" startIcon={<PrintOutlinedIcon />} disabled={!report} onClick={() => window.print()}>
          Print
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default ReportCardDialog;
