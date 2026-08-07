import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Autocomplete from '@mui/material/Autocomplete';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import CircularProgress from '@mui/material/CircularProgress';
import { Controller, useForm } from 'react-hook-form';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import AssignmentReturnOutlinedIcon from '@mui/icons-material/AssignmentReturnOutlined';
import DataTable from '@/components/common/DataTable';
import StatusChip from '@/components/common/StatusChip';
import libraryApi from '@/api/libraryApi';
import studentsApi from '@/api/studentsApi';
import teachersApi from '@/api/teachersApi';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { getStudentDisplayName } from '@/utils/format';
import type { Book, BookIssue, BookIssueStatus, Student, Teacher } from '@/types';
import ReturnBookDialog from './components/ReturnBookDialog';

interface IssueFormValues {
  book: Book | null;
  borrowerType: 'STUDENT' | 'TEACHER';
  student: Student | null;
  teacher: Teacher | null;
  dueDate: dayjs.Dayjs | null;
}

const STATUS_OPTIONS: Array<{ label: string; value: BookIssueStatus | '' }> = [
  { label: 'All', value: '' },
  { label: 'Issued', value: 'ISSUED' },
  { label: 'Overdue', value: 'OVERDUE' },
  { label: 'Returned', value: 'RETURNED' },
];

/** "Issue a Book" form + a DataTable of current issues with a Return row action. */
export function IssueReturnPage() {
  const { enqueueSnackbar } = useSnackbar();

  const [bookQuery, setBookQuery] = useState('');
  const debouncedBookQuery = useDebouncedValue(bookQuery, 400);
  const [bookOptions, setBookOptions] = useState<Book[]>([]);

  const [borrowerQuery, setBorrowerQuery] = useState('');
  const debouncedBorrowerQuery = useDebouncedValue(borrowerQuery, 400);
  const [studentOptions, setStudentOptions] = useState<Student[]>([]);
  const [teacherOptions, setTeacherOptions] = useState<Teacher[]>([]);

  const { control, handleSubmit, watch, reset } = useForm<IssueFormValues>({
    defaultValues: { book: null, borrowerType: 'STUDENT', student: null, teacher: null, dueDate: dayjs().add(14, 'day') },
  });
  const borrowerType = watch('borrowerType');
  const [issuing, setIssuing] = useState(false);

  const [rows, setRows] = useState<BookIssue[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<BookIssueStatus | ''>('');
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [returnTarget, setReturnTarget] = useState<BookIssue | null>(null);
  const [returning, setReturning] = useState(false);

  useEffect(() => {
    if (!debouncedBookQuery) {
      setBookOptions([]);
      return;
    }
    libraryApi.books
      .list({ search: debouncedBookQuery, size: 20 })
      .then((res) => setBookOptions(res.data.content))
      .catch(() => undefined);
  }, [debouncedBookQuery]);

  useEffect(() => {
    if (!debouncedBorrowerQuery) {
      setStudentOptions([]);
      setTeacherOptions([]);
      return;
    }
    if (borrowerType === 'STUDENT') {
      studentsApi
        .list({ search: debouncedBorrowerQuery, size: 20 })
        .then((res) => setStudentOptions(res.data.content))
        .catch(() => undefined);
    } else {
      teachersApi
        .list({ search: debouncedBorrowerQuery, size: 20 })
        .then((res) => setTeacherOptions(res.data.content))
        .catch(() => undefined);
    }
  }, [debouncedBorrowerQuery, borrowerType]);

  const loadIssues = useCallback(async () => {
    setLoading(true);
    try {
      const res = await libraryApi.bookIssues.list({
        status: status || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'issueDate,desc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load book issues.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [status, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    loadIssues();
  }, [loadIssues]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [status]);

  const handleIssue = async (values: IssueFormValues) => {
    if (!values.book || !values.dueDate) return;
    if (values.borrowerType === 'STUDENT' && !values.student) return;
    if (values.borrowerType === 'TEACHER' && !values.teacher) return;
    setIssuing(true);
    try {
      await libraryApi.bookIssues.issue({
        bookId: values.book.id,
        studentId: values.borrowerType === 'STUDENT' ? values.student!.id : undefined,
        teacherId: values.borrowerType === 'TEACHER' ? values.teacher!.id : undefined,
        dueDate: dayjs(values.dueDate).format('YYYY-MM-DD'),
      });
      enqueueSnackbar('Book issued successfully.', { variant: 'success' });
      reset({ book: null, borrowerType: values.borrowerType, student: null, teacher: null, dueDate: dayjs().add(14, 'day') });
      setBookQuery('');
      setBorrowerQuery('');
      loadIssues();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not issue this book.', { variant: 'error' });
    } finally {
      setIssuing(false);
    }
  };

  const handleReturn = async (returnDate: string) => {
    if (!returnTarget) return;
    setReturning(true);
    try {
      const res = await libraryApi.bookIssues.returnBook(returnTarget.id, { returnDate });
      enqueueSnackbar(
        res.data.fineAmount > 0
          ? `Book returned. Fine of ₹${res.data.fineAmount} applies.`
          : 'Book returned. No fine due.',
        { variant: res.data.fineAmount > 0 ? 'warning' : 'success' },
      );
      setReturnTarget(null);
      loadIssues();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not process this return.', { variant: 'error' });
    } finally {
      setReturning(false);
    }
  };

  const columns: GridColDef<BookIssue>[] = useMemo(
    () => [
      { field: 'bookTitle', headerName: 'Book', flex: 1.2, minWidth: 180, valueGetter: (_v, row) => row.bookTitle ?? `#${row.bookId}` },
      {
        field: 'borrower',
        headerName: 'Borrower',
        flex: 1,
        minWidth: 160,
        valueGetter: (_v, row) => row.studentName ?? row.teacherName ?? '-',
      },
      {
        field: 'issueDate',
        headerName: 'Issued',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      {
        field: 'dueDate',
        headerName: 'Due',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      {
        field: 'returnDate',
        headerName: 'Returned',
        width: 120,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      { field: 'fineAmount', headerName: 'Fine', width: 90, valueFormatter: (value) => (Number(value) > 0 ? `₹${value}` : '-') },
      { field: 'status', headerName: 'Status', width: 110, renderCell: (params) => <StatusChip status={params.row.status} /> },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 90,
        sortable: false,
        filterable: false,
        renderCell: (params) =>
          params.row.status !== 'RETURNED' ? (
            <Tooltip title="Return book">
              <IconButton size="small" color="primary" onClick={() => setReturnTarget(params.row)}>
                <AssignmentReturnOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : null,
      },
    ],
    [],
  );

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <CardHeader title="Issue a Book" subheader="Search for a book and a borrower, then set a due date." />
        <CardContent>
          <Box component="form" onSubmit={handleSubmit(handleIssue)} noValidate>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={6} md={4}>
                <Controller
                  name="book"
                  control={control}
                  render={({ field }) => (
                    <Autocomplete
                      size="small"
                      options={bookOptions}
                      value={field.value}
                      getOptionLabel={(o) => `${o.title} (${o.availableCopies}/${o.totalCopies} available)`}
                      isOptionEqualToValue={(o, v) => o.id === v.id}
                      getOptionDisabled={(o) => o.availableCopies <= 0}
                      onChange={(_e, value) => field.onChange(value)}
                      onInputChange={(_e, value) => setBookQuery(value)}
                      renderInput={(params) => <TextField {...params} label="Book" placeholder="Search title, author or ISBN" />}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={2.5}>
                <Controller
                  name="borrowerType"
                  control={control}
                  render={({ field }) => (
                    <ToggleButtonGroup
                      exclusive
                      size="small"
                      color="primary"
                      value={field.value}
                      onChange={(_e, value) => {
                        if (value) {
                          field.onChange(value);
                          setBorrowerQuery('');
                        }
                      }}
                      fullWidth
                    >
                      <ToggleButton value="STUDENT">Student</ToggleButton>
                      <ToggleButton value="TEACHER">Teacher</ToggleButton>
                    </ToggleButtonGroup>
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={2.5}>
                {borrowerType === 'STUDENT' ? (
                  <Controller
                    name="student"
                    control={control}
                    render={({ field }) => (
                      <Autocomplete
                        size="small"
                        options={studentOptions}
                        value={field.value}
                        getOptionLabel={(o) => `${getStudentDisplayName(o)} (${o.admissionNumber})`}
                        isOptionEqualToValue={(o, v) => o.id === v.id}
                        onChange={(_e, value) => field.onChange(value)}
                        onInputChange={(_e, value) => setBorrowerQuery(value)}
                        renderInput={(params) => <TextField {...params} label="Student" placeholder="Name or admission no." />}
                      />
                    )}
                  />
                ) : (
                  <Controller
                    name="teacher"
                    control={control}
                    render={({ field }) => (
                      <Autocomplete
                        size="small"
                        options={teacherOptions}
                        value={field.value}
                        getOptionLabel={(o) => `${o.firstName ?? o.username} ${o.lastName ?? ''}`.trim()}
                        isOptionEqualToValue={(o, v) => o.id === v.id}
                        onChange={(_e, value) => field.onChange(value)}
                        onInputChange={(_e, value) => setBorrowerQuery(value)}
                        renderInput={(params) => <TextField {...params} label="Teacher" placeholder="Search by name" />}
                      />
                    )}
                  />
                )}
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <Controller
                  name="dueDate"
                  control={control}
                  render={({ field }) => (
                    <DatePicker
                      label="Due Date"
                      value={field.value}
                      onChange={(value) => field.onChange(value)}
                      disablePast
                      slotProps={{ textField: { fullWidth: true, size: 'small' } }}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={1}>
                <Button
                  type="submit"
                  variant="contained"
                  fullWidth
                  disabled={issuing}
                  startIcon={issuing ? <CircularProgress size={16} color="inherit" /> : undefined}
                >
                  Issue
                </Button>
              </Grid>
            </Grid>
          </Box>
        </CardContent>
      </Card>

      <Card sx={{ mb: 2 }} variant="outlined">
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
          <TextField
            select
            size="small"
            label="Status"
            value={status}
            sx={{ minWidth: 160 }}
            onChange={(e) => setStatus(e.target.value as BookIssueStatus | '')}
          >
            {STATUS_OPTIONS.map((opt) => (
              <MenuItem key={opt.label} value={opt.value}>
                {opt.label}
              </MenuItem>
            ))}
          </TextField>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No book issues found"
          emptyDescription="Issue a book using the form above to get started."
        />
      </Card>

      <ReturnBookDialog
        open={!!returnTarget}
        issue={returnTarget}
        saving={returning}
        onClose={() => setReturnTarget(null)}
        onSubmit={handleReturn}
      />
    </Box>
  );
}

export default IssueReturnPage;
