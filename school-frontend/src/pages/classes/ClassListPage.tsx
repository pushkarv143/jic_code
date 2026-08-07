import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import classesApi from '@/api/classesApi';
import academicYearsApi from '@/api/academicYearsApi';
import type { AcademicYear, SchoolClass } from '@/types';
import ClassFormDialog from './components/ClassFormDialog';
import AcademicYearManagerDialog from './components/AcademicYearManagerDialog';

/** Class directory: filter by academic year, add/edit/delete classes, drill into a class for sections/subjects/mapping. */
export function ClassListPage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [rows, setRows] = useState<SchoolClass[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SchoolClass | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<SchoolClass | null>(null);
  const [yearManagerOpen, setYearManagerOpen] = useState(false);

  const loadYears = useCallback(() => {
    academicYearsApi
      .list()
      .then((res) => setYears(res.data))
      .catch(() => enqueueSnackbar('Could not load academic years.', { variant: 'error' }));
  }, [enqueueSnackbar]);

  useEffect(() => {
    loadYears();
  }, [loadYears]);

  const loadClasses = useCallback(async () => {
    setLoading(true);
    try {
      const res = await classesApi.list({
        academicYearId: academicYearId || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'className,asc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load classes.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [academicYearId, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    loadClasses();
  }, [loadClasses]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [academicYearId]);

  const handleSave = async (values: { className: string; academicYearId: number }) => {
    setSaving(true);
    try {
      if (editing) {
        await classesApi.update(editing.id, values);
        enqueueSnackbar('Class updated.', { variant: 'success' });
      } else {
        await classesApi.create(values);
        enqueueSnackbar('Class added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      loadClasses();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this class.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await classesApi.remove(deleteTarget.id);
      enqueueSnackbar('Class deleted.', { variant: 'success' });
      setDeleteTarget(null);
      loadClasses();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this class.', { variant: 'error' });
    }
  };

  const columns: GridColDef<SchoolClass>[] = useMemo(
    () => [
      { field: 'className', headerName: 'Class', flex: 1, minWidth: 160 },
      { field: 'academicYearName', headerName: 'Academic Year', flex: 1, minWidth: 140 },
      { field: 'sectionCount', headerName: 'Sections', width: 110, valueGetter: (_v, row) => row.sectionCount ?? '-' },
      { field: 'studentCount', headerName: 'Students', width: 110, valueGetter: (_v, row) => row.studentCount ?? '-' },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 160,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="Open">
              <IconButton size="small" onClick={() => navigate(`/app/classes/${params.row.id}`)}>
                <ArrowForwardOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
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
      },
    ],
    [navigate],
  );

  return (
    <Box>
      <PageHeader
        title="Classes & Sections"
        subtitle="Manage classes, sections, subjects and teacher mapping"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Classes & Sections' }]}
        action={
          <Stack direction="row" spacing={1.5}>
            <Button variant="outlined" startIcon={<CalendarMonthOutlinedIcon />} onClick={() => setYearManagerOpen(true)}>
              Academic Years
            </Button>
            <Button
              variant="contained"
              startIcon={<AddOutlinedIcon />}
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              Add Class
            </Button>
          </Stack>
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Academic Year"
                value={academicYearId}
                onChange={(e) => setAcademicYearId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All academic years</MenuItem>
                {years.map((y) => (
                  <MenuItem key={y.id} value={y.id}>
                    {y.yearName}
                    {y.isCurrent ? ' (current)' : ''}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            {years.find((y) => y.isCurrent) && (
              <Grid item>
                <Chip
                  size="small"
                  color="success"
                  variant="outlined"
                  label={`Current year: ${years.find((y) => y.isCurrent)?.yearName}`}
                />
              </Grid>
            )}
          </Grid>
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
          emptyTitle="No classes found"
          emptyDescription="Add a class to get started."
        />
      </Card>

      <ClassFormDialog
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
        title="Delete class"
        message={`Are you sure you want to delete ${deleteTarget?.className ?? ''}? This may fail if it still has sections, subjects or students.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <AcademicYearManagerDialog
        open={yearManagerOpen}
        onClose={() => setYearManagerOpen(false)}
        onChanged={loadYears}
      />
    </Box>
  );
}

export default ClassListPage;
