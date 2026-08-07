import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import feesApi from '@/api/feesApi';
import academicYearsApi from '@/api/academicYearsApi';
import { useAppSelector } from '@/store/hooks';
import { formatCurrencyINR } from '@/utils/format';
import type { AcademicYear, Role, Scholarship } from '@/types';
import ScholarshipFormDialog from './components/ScholarshipFormDialog';

const WRITE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'ACCOUNTANT'];

/** Paginated scholarship directory. STUDENT/PARENT see only their own, read-only; staff get full CRUD. */
export function ScholarshipsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const isSelfView = user?.role === 'STUDENT' || user?.role === 'PARENT';
  const canWrite = !!user && WRITE_ROLES.includes(user.role);

  const [rows, setRows] = useState<Scholarship[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Scholarship | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Scholarship | null>(null);

  useEffect(() => {
    academicYearsApi.list().then((res) => setYears(res.data)).catch(() => undefined);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await feesApi.scholarships.list({
        studentId: isSelfView ? user?.studentId ?? undefined : undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load scholarships.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [isSelfView, user?.studentId, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSave = async (values: Parameters<typeof feesApi.scholarships.create>[0]) => {
    setSaving(true);
    try {
      if (editing) {
        await feesApi.scholarships.update(editing.id, values);
        enqueueSnackbar('Scholarship updated.', { variant: 'success' });
      } else {
        await feesApi.scholarships.create(values);
        enqueueSnackbar('Scholarship added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this scholarship.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await feesApi.scholarships.remove(deleteTarget.id);
      enqueueSnackbar('Scholarship deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this scholarship.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<Scholarship>[] = useMemo(
    () => [
      ...(isSelfView
        ? []
        : ([{ field: 'studentName', headerName: 'Student', flex: 1, minWidth: 160, valueGetter: (_v, row) => row.studentName ?? `#${row.studentId}` }] as GridColDef<Scholarship>[])),
      { field: 'title', headerName: 'Title', flex: 1, minWidth: 160 },
      {
        field: 'amount',
        headerName: 'Amount',
        width: 130,
        valueFormatter: (value, row) => (row.type === 'PERCENTAGE' ? `${value}%` : formatCurrencyINR(Number(value ?? 0))),
      },
      {
        field: 'type',
        headerName: 'Type',
        width: 130,
        renderCell: (params) => <Chip size="small" label={params.row.type === 'PERCENTAGE' ? 'Percentage' : 'Fixed'} variant="outlined" />,
      },
      { field: 'academicYearName', headerName: 'Academic Year', width: 140, valueGetter: (_v, row) => row.academicYearName ?? '-' },
      ...(canWrite
        ? ([
            {
              field: 'actions',
              headerName: 'Actions',
              width: 110,
              sortable: false,
              filterable: false,
              renderCell: (params: any) => (
                <Stack direction="row" spacing={0.5}>
                  <Tooltip title="Edit">
                    <IconButton
                      size="small"
                      onClick={() => {
                        setEditing(params.row);
                        setFormOpen(true);
                      }}
                    >
                      <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Delete">
                    <IconButton size="small" onClick={() => setDeleteTarget(params.row)}>
                      <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                    </IconButton>
                  </Tooltip>
                </Stack>
              ),
            } as GridColDef<Scholarship>,
          ])
        : []),
    ],
    [isSelfView, canWrite],
  );

  return (
    <Box>
      {canWrite && (
        <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
          <Button
            variant="contained"
            startIcon={<AddOutlinedIcon />}
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            Add Scholarship
          </Button>
        </Stack>
      )}

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          paginationMode="server"
          rowCount={rowCount}
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          emptyTitle="No scholarships found"
          emptyDescription={canWrite ? 'Add a scholarship to get started.' : 'No scholarships have been recorded yet.'}
        />
      </Card>

      {canWrite && (
        <>
          <ScholarshipFormDialog
            open={formOpen}
            editing={editing}
            years={years}
            saving={saving}
            onClose={() => {
              setFormOpen(false);
              setEditing(null);
            }}
            onSubmit={handleSave}
          />

          <ConfirmDialog
            open={!!deleteTarget}
            title="Delete scholarship"
            message={`Delete "${deleteTarget?.title ?? ''}"? This cannot be undone.`}
            confirmLabel="Delete"
            destructive
            onConfirm={handleDelete}
            onCancel={() => setDeleteTarget(null)}
          />
        </>
      )}
    </Box>
  );
}

export default ScholarshipsPage;
