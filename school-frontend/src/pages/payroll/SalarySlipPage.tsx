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
import payrollApi from '@/api/payrollApi';
import type { SalarySlip } from '@/types';
import { formatCurrencyINR } from '@/utils/format';
import { downloadBlob } from '@/utils/downloadBlob';

/** Printable salary slip — GET /payroll/{id}/salary-slip, with a real @media print layout (same pattern as FeeReceiptPage). */
export function SalarySlipPage() {
  const { payrollId } = useParams<{ payrollId: string }>();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const [slip, setSlip] = useState<SalarySlip | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!payrollId) return;
    setLoading(true);
    payrollApi.payroll
      .getSalarySlip(Number(payrollId))
      .then((res) => setSlip(res.data))
      .catch((err) => {
        setNotFound(true);
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load this salary slip.', { variant: 'error' });
      })
      .finally(() => setLoading(false));
  }, [payrollId, enqueueSnackbar]);

  if (loading) return <PageLoader label="Loading salary slip..." />;
  if (notFound || !slip) {
    return <EmptyState title="Salary slip not found" description="This salary slip could not be loaded." />;
  }

  const handleDownloadPdf = async () => {
    if (!payrollId) return;
    setDownloading(true);
    try {
      const blob = await payrollApi.payroll.getSalarySlipPdf(Number(payrollId));
      downloadBlob(blob, `salary-slip-${slip.employeeId ?? payrollId}-${slip.month}-${slip.year}.pdf`);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not download the salary slip PDF.', { variant: 'error' });
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Box>
      <Box sx={{ '@media print': { display: 'none' } }}>
        <PageHeader
          title="Salary Slip"
          subtitle={`${slip.employeeName ?? 'Employee'} — ${dayjs(`${slip.year}-${slip.month}-01`).format('MMMM YYYY')}`}
          breadcrumbs={[
            { label: 'Dashboard', to: '/app/dashboard' },
            { label: 'Payroll', to: '/app/payroll/runs' },
            { label: 'Salary Slip' },
          ]}
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
        id="salary-slip-print-area"
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
              {slip.schoolName ?? 'Greenwood International School'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              School Address, City, State — PIN
            </Typography>
            <Typography variant="subtitle1" fontWeight={700} sx={{ mt: 2, textTransform: 'uppercase', letterSpacing: 1 }}>
              Salary Slip — {dayjs(`${slip.year}-${slip.month}-01`).format('MMMM YYYY')}
            </Typography>
          </Box>

          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={1.5} sx={{ mb: 2 }}>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Employee Name
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {slip.employeeName ?? '-'}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Employee ID
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {slip.employeeId ?? '-'}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Department
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {slip.departmentName ?? '-'}
              </Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography variant="caption" color="text.secondary">
                Designation
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {slip.designationName ?? '-'}
              </Typography>
            </Grid>
          </Grid>

          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={1} sx={{ mb: 1 }}>
            <Grid item xs={12}>
              <Typography variant="subtitle2" fontWeight={700}>
                Earnings
              </Typography>
            </Grid>
          </Grid>
          {[
            ['Basic Salary', slip.basicSalary],
            ['HRA', slip.hra ?? null],
            ['DA', slip.da ?? null],
            ['Other Allowances', slip.otherAllowances ?? null],
          ]
            .filter(([, value]) => value !== null)
            .map(([label, value]) => (
              <Box key={label as string} sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}>
                <Typography variant="body2">{label}</Typography>
                <Typography variant="body2" fontWeight={600}>
                  {formatCurrencyINR(Number(value))}
                </Typography>
              </Box>
            ))}

          <Divider sx={{ my: 1.5 }} />

          <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 0.5 }}>
            Deductions
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}>
            <Typography variant="body2">PF</Typography>
            <Typography variant="body2" fontWeight={600}>
              {formatCurrencyINR(slip.pf)}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}>
            <Typography variant="body2">ESI</Typography>
            <Typography variant="body2" fontWeight={600}>
              {formatCurrencyINR(slip.esi)}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}>
            <Typography variant="body2">Total Deductions</Typography>
            <Typography variant="body2" fontWeight={600} color="error.main">
              {formatCurrencyINR(slip.pf + slip.esi)}
            </Typography>
          </Box>

          <Divider sx={{ my: 1.5 }} />

          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1 }}>
            <Typography variant="subtitle1" fontWeight={700}>
              Net Salary
            </Typography>
            <Typography variant="h6" fontWeight={700} color="success.main">
              {formatCurrencyINR(slip.netSalary)}
            </Typography>
          </Box>

          <Divider sx={{ my: 2 }} />

          <Typography variant="caption" color="text.secondary" display="block" textAlign="center">
            This is a system-generated salary slip and does not require a signature.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
}

export default SalarySlipPage;
