import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import CircularProgress from '@mui/material/CircularProgress';
import PrintOutlinedIcon from '@mui/icons-material/PrintOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import feesApi from '@/api/feesApi';
import type { FeeReceipt } from '@/types';
import { formatCurrencyINR } from '@/utils/format';
import { downloadBlob } from '@/utils/downloadBlob';

/** Printable fee payment receipt — GET /fee-payments/{id}/receipt, with a real @media print layout. */
export function FeeReceiptPage() {
  const { paymentId } = useParams<{ paymentId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [receipt, setReceipt] = useState<FeeReceipt | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!paymentId) return;
    setLoading(true);
    feesApi.feePayments
      .getReceipt(Number(paymentId))
      .then((res) => setReceipt(res.data))
      .catch((err) => {
        setNotFound(true);
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this receipt.', { variant: 'error' });
      })
      .finally(() => setLoading(false));
  }, [paymentId, enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading receipt..." />;
  if (notFound || !receipt) {
    return <EmptyState title="Receipt not found" description="This payment receipt could not be loaded." />;
  }

  const handleDownloadPdf = async () => {
    if (!paymentId) return;
    setDownloading(true);
    try {
      const blob = await feesApi.feePayments.getReceiptPdf(Number(paymentId));
      downloadBlob(blob, `fee-receipt-${receipt.receiptNumber}.pdf`);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not download the receipt PDF.', { variant: 'error' });
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Box>
      <Box sx={{ '@media print': { display: 'none' } }}>
        <PageHeader
          title="Fee Receipt"
          subtitle={`Receipt ${receipt.receiptNumber}`}
          breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Fees', to: '/app/fees' }, { label: 'Receipt' }]}
          action={
            <Stack direction="row" spacing={1.5}>
              <Button variant="outlined" startIcon={<ArrowBackOutlinedIcon />} onClick={() => navigate(-1)}>
                Back
              </Button>
              <Button
                variant="outlined"
                startIcon={downloading ? <CircularProgress size={16} color="inherit" /> : <PictureAsPdfOutlinedIcon />}
                onClick={handleDownloadPdf}
                disabled={downloading}
              >
                Download PDF
              </Button>
              <Button variant="contained" startIcon={<PrintOutlinedIcon />} onClick={() => window.print()}>
                Print
              </Button>
            </Stack>
          }
        />
      </Box>

      <Card
        id="fee-receipt-print-area"
        sx={{
          maxWidth: 640,
          mx: 'auto',
          '@media print': {
            boxShadow: 'none',
            border: 'none',
          },
        }}
      >
        <CardContent sx={{ p: { xs: 3, sm: 5 } }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Typography variant="h5" fontWeight={700}>
              {receipt.schoolName ?? 'Greenwood International School'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {receipt.schoolAddress ?? 'School Address, City, State — PIN'}
            </Typography>
            <Typography variant="subtitle1" fontWeight={700} sx={{ mt: 2, textTransform: 'uppercase', letterSpacing: 1 }}>
              Fee Payment Receipt
            </Typography>
          </Box>

          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={1.5} sx={{ mb: 2 }}>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Receipt No.
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.receiptNumber}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Payment Date
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {dayjs(receipt.paymentDate).format('DD MMM YYYY')}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Student Name
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.studentName ?? '-'}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Admission No.
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.admissionNumber ?? '-'}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Class / Section
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.className ? `${receipt.className}${receipt.sectionName ? ` - ${receipt.sectionName}` : ''}` : '-'}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Academic Year
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.academicYearName ?? '-'}
              </Typography>
            </Grid>
          </Grid>

          <Divider sx={{ mb: 2 }} />

          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              py: 1,
              borderBottom: '1px dashed',
              borderColor: 'divider',
            }}
          >
            <Typography variant="body2">{receipt.feeCategoryName ?? 'Fee Payment'}</Typography>
            <Typography variant="body2" fontWeight={600}>
              {formatCurrencyINR(receipt.amount)}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1.5 }}>
            <Typography variant="subtitle1" fontWeight={700}>
              Total Paid
            </Typography>
            <Typography variant="h6" fontWeight={700} color="success.main">
              {formatCurrencyINR(receipt.amount)}
            </Typography>
          </Box>

          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={1.5} sx={{ mb: 3 }}>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Payment Mode
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.paymentMode}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Transaction / Ref. ID
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.transactionId ?? '-'}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Collected By
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {receipt.collectedByName ?? '-'}
              </Typography>
            </Grid>
          </Grid>

          <Typography variant="caption" color="text.secondary" display="block" textAlign="center">
            This is a system-generated receipt and does not require a signature.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
}

export default FeeReceiptPage;
