import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import DataTable from '@/components/common/DataTable';
import EmptyState from '@/components/common/EmptyState';
import StatusChip from '@/components/common/StatusChip';
import feesApi from '@/api/feesApi';
import studentsApi from '@/api/studentsApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useAppSelector } from '@/store/hooks';
import { formatCurrencyINR, getStudentDisplayName } from '@/utils/format';
import type { PaymentMode, Student, StudentFee } from '@/types';
import CollectPaymentDialog from './components/CollectPaymentDialog';
import ViewPaymentsDialog from './components/ViewPaymentsDialog';

export interface FeeCollectionPageProps {
  /** 'staff': search any student and collect payments. 'self': the logged-in STUDENT/PARENT viewing their own dues, read-only. */
  mode?: 'staff' | 'self';
}

/** Student fee dues + "Collect Payment" workflow. Reused read-only (mode="self") for the STUDENT/PARENT "My Fees" view. */
export function FeeCollectionPage({ mode = 'staff' }: FeeCollectionPageProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [searchParams] = useSearchParams();
  const user = useAppSelector((state) => state.auth.user);

  const [studentQuery, setStudentQuery] = useState('');
  const debouncedQuery = useDebouncedValue(studentQuery, 400);
  const [studentOptions, setStudentOptions] = useState<Student[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);

  const studentId = mode === 'self' ? user?.studentId ?? undefined : selectedStudent?.id;

  const [rows, setRows] = useState<StudentFee[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const [collectTarget, setCollectTarget] = useState<StudentFee | null>(null);
  const [saving, setSaving] = useState(false);
  const [viewPaymentsId, setViewPaymentsId] = useState<number | null>(null);

  // Pre-select a student via ?studentId= (e.g. linked from the student's profile Fees tab).
  useEffect(() => {
    if (mode !== 'staff') return;
    const preselect = searchParams.get('studentId');
    if (preselect) {
      studentsApi
        .getById(Number(preselect))
        .then((res) => setSelectedStudent(res.data))
        .catch(() => undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode]);

  useEffect(() => {
    if (mode !== 'staff' || !debouncedQuery) {
      if (mode === 'staff') setStudentOptions([]);
      return;
    }
    studentsApi
      .list({ search: debouncedQuery, size: 20 })
      .then((res) => setStudentOptions(res.data.content))
      .catch(() => undefined);
  }, [debouncedQuery, mode]);

  const loadFees = useCallback(async () => {
    if (!studentId) return;
    setLoading(true);
    setLoaded(false);
    try {
      const res = await feesApi.studentFees.list({ studentId, size: 100, sort: 'dueDate,desc' });
      setRows(res.data.content);
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load fee dues for this student.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [studentId, enqueueSnackbar]);

  useEffect(() => {
    loadFees();
  }, [loadFees]);

  const handleCollect = async (values: { amount: number; paymentDate: string; paymentMode: PaymentMode; transactionId?: string }) => {
    if (!collectTarget) return;
    setSaving(true);
    try {
      const res = await feesApi.feePayments.create({ studentFeeId: collectTarget.id, ...values });
      enqueueSnackbar(
        `Payment of ${formatCurrencyINR(values.amount)} collected. New status: ${res.data.updatedStudentFee.status}.`,
        { variant: 'success' },
      );
      setCollectTarget(null);
      setRows((prev) => prev.map((r) => (r.id === res.data.updatedStudentFee.id ? res.data.updatedStudentFee : r)));
      setViewPaymentsId(res.data.updatedStudentFee.id);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not record this payment.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const columns: GridColDef<StudentFee>[] = useMemo(
    () => [
      ...(mode === 'staff'
        ? ([
            { field: 'studentName', headerName: 'Student', flex: 1, minWidth: 160, valueGetter: (_v, row) => row.studentName ?? `#${row.studentId}` },
          ] as GridColDef<StudentFee>[])
        : []),
      { field: 'feeCategoryName', headerName: 'Fee Category', flex: 1, minWidth: 140, valueGetter: (_v, row) => row.feeCategoryName ?? '-' },
      { field: 'academicYearName', headerName: 'Academic Year', width: 130, valueGetter: (_v, row) => row.academicYearName ?? '-' },
      {
        field: 'dueDate',
        headerName: 'Due Date',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      { field: 'amountDue', headerName: 'Amount Due', width: 120, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'amountPaid', headerName: 'Amount Paid', width: 120, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      {
        field: 'balance',
        headerName: 'Balance',
        width: 120,
        valueGetter: (_v, row) => Math.max(row.amountDue - row.amountPaid, 0),
        valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)),
      },
      { field: 'status', headerName: 'Status', width: 110, renderCell: (params) => <StatusChip status={params.row.status} /> },
      {
        field: 'actions',
        headerName: 'Actions',
        width: mode === 'staff' ? 150 : 90,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            {mode === 'staff' && params.row.status !== 'PAID' && (
              <Tooltip title="Collect payment">
                <IconButton size="small" color="primary" onClick={() => setCollectTarget(params.row)}>
                  <PaidOutlinedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
            <Tooltip title="View payments">
              <IconButton size="small" onClick={() => setViewPaymentsId(params.row.id)}>
                <VisibilityOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    [mode],
  );

  return (
    <Box>
      {mode === 'staff' && (
        <Card sx={{ mb: 2.5 }}>
          <CardContent>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={8} md={5}>
                <Autocomplete
                  size="small"
                  options={studentOptions}
                  value={selectedStudent}
                  getOptionLabel={(o) => `${getStudentDisplayName(o)} (${o.admissionNumber})`}
                  isOptionEqualToValue={(o, v) => o.id === v.id}
                  onChange={(_e, value) => setSelectedStudent(value)}
                  onInputChange={(_e, value) => setStudentQuery(value)}
                  renderInput={(params) => <TextField {...params} label="Search student" placeholder="Name or admission no." />}
                />
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}

      {!studentId ? (
        <EmptyState
          title={mode === 'staff' ? 'Search for a student' : 'No fee records'}
          description={
            mode === 'staff'
              ? 'Search and select a student above to view and collect their fee dues.'
              : 'Your fee records could not be loaded for this account.'
          }
        />
      ) : loaded && rows.length === 0 && !loading ? (
        <EmptyState title="No fee dues found" description="This student has no fee records for the current setup." />
      ) : (
        <Card>
          <DataTable
            rows={rows}
            columns={columns}
            loading={loading}
            emptyTitle="No fee dues found"
            emptyDescription="This student has no fee records yet."
          />
        </Card>
      )}

      <CollectPaymentDialog
        open={!!collectTarget}
        studentFee={collectTarget}
        saving={saving}
        onClose={() => setCollectTarget(null)}
        onSubmit={handleCollect}
      />

      <ViewPaymentsDialog open={!!viewPaymentsId} studentFeeId={viewPaymentsId} onClose={() => setViewPaymentsId(null)} />
    </Box>
  );
}

export default FeeCollectionPage;
