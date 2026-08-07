import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import AssignmentReturnOutlinedIcon from '@mui/icons-material/AssignmentReturnOutlined';
import DataTable from '@/components/common/DataTable';
import libraryApi from '@/api/libraryApi';
import type { BookIssue } from '@/types';
import ReturnBookDialog from './components/ReturnBookDialog';

/** Overdue book issues (GET /book-issues/overdue) with fine amounts highlighted and a quick Return action. */
export function OverdueBooksPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [rows, setRows] = useState<BookIssue[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });
  const [returnTarget, setReturnTarget] = useState<BookIssue | null>(null);
  const [returning, setReturning] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await libraryApi.bookIssues.overdue({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'dueDate,asc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load overdue books.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleReturn = async (returnDate: string) => {
    if (!returnTarget) return;
    setReturning(true);
    try {
      const res = await libraryApi.bookIssues.returnBook(returnTarget.id, { returnDate });
      enqueueSnackbar(
        res.data.fineAmount > 0 ? `Book returned. Fine of ₹${res.data.fineAmount} applies.` : 'Book returned. No fine due.',
        { variant: res.data.fineAmount > 0 ? 'warning' : 'success' },
      );
      setReturnTarget(null);
      load();
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
        field: 'daysOverdue',
        headerName: 'Days Overdue',
        width: 130,
        valueGetter: (_v, row) => Math.max(dayjs().diff(dayjs(row.dueDate), 'day'), 0),
      },
      {
        field: 'fineAmount',
        headerName: 'Fine',
        width: 110,
        renderCell: (params) => (
          <Chip
            size="small"
            label={params.row.fineAmount > 0 ? `₹${params.row.fineAmount}` : 'None yet'}
            color={params.row.fineAmount > 0 ? 'error' : 'default'}
            variant={params.row.fineAmount > 0 ? 'filled' : 'outlined'}
          />
        ),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 90,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Tooltip title="Return book">
            <IconButton size="small" color="primary" onClick={() => setReturnTarget(params.row)}>
              <AssignmentReturnOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
      },
    ],
    [],
  );

  return (
    <Box>
      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No overdue books"
          emptyDescription="Every issued book is currently within its due date. Nice work!"
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

export default OverdueBooksPage;
