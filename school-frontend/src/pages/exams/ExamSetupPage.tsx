import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import LookupManagerDialog from '@/components/common/LookupManagerDialog';
import examApi from '@/api/examApi';
import classesApi from '@/api/classesApi';
import academicYearsApi from '@/api/academicYearsApi';
import type { AcademicYear, Exam, ExamType, SchoolClass } from '@/types';
import type { ExamPayload } from '@/api/examApi';
import ExamFormDialog from './components/ExamFormDialog';
import ExamSchedulesDialog from './components/ExamSchedulesDialog';

/** Exam-types manager + exams directory (filterable by class/academic year), with a nested schedules view per exam. */
export function ExamSetupPage() {
  const { enqueueSnackbar } = useSnackbar();

  const [examTypes, setExamTypes] = useState<ExamType[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');
  const [examTypeId, setExamTypeId] = useState<number | ''>('');

  const [rows, setRows] = useState<Exam[]>([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 10 });

  const [typesManagerOpen, setTypesManagerOpen] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Exam | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Exam | null>(null);
  const [schedulesExam, setSchedulesExam] = useState<Exam | null>(null);

  const loadLookups = useCallback(() => {
    examApi.examTypes.list().then((res) => setExamTypes(res.data)).catch(() => undefined);
    classesApi.list({ size: 200, sort: 'className,asc' }).then((res) => setClasses(res.data.content)).catch(() => undefined);
    academicYearsApi.list().then((res) => setYears(res.data)).catch(() => undefined);
  }, []);

  useEffect(() => {
    loadLookups();
  }, [loadLookups]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await examApi.exams.list({
        classId: classId || undefined,
        academicYearId: academicYearId || undefined,
        examTypeId: examTypeId || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'startDate,desc',
      });
      setRows(res.data.content);
      setRowCount(res.data.totalElements);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load exams.', { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [classId, academicYearId, examTypeId, paginationModel, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    setPaginationModel((m) => ({ ...m, page: 0 }));
  }, [classId, academicYearId, examTypeId]);

  const handleSave = async (values: ExamPayload) => {
    setSaving(true);
    try {
      if (editing) {
        await examApi.exams.update(editing.id, values);
        enqueueSnackbar('Exam updated.', { variant: 'success' });
      } else {
        await examApi.exams.create(values);
        enqueueSnackbar('Exam added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this exam.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await examApi.exams.remove(deleteTarget.id);
      enqueueSnackbar('Exam deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this exam.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<Exam>[] = useMemo(
    () => [
      { field: 'examTypeName', headerName: 'Exam Type', flex: 1, minWidth: 140, valueGetter: (_v, row) => row.examTypeName ?? `#${row.examTypeId}` },
      { field: 'className', headerName: 'Class', flex: 0.8, minWidth: 120, valueGetter: (_v, row) => row.className ?? `#${row.classId}` },
      { field: 'academicYearName', headerName: 'Academic Year', width: 130, valueGetter: (_v, row) => row.academicYearName ?? `#${row.academicYearId}` },
      { field: 'startDate', headerName: 'Start', width: 120, valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-') },
      { field: 'endDate', headerName: 'End', width: 120, valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-') },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 170,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="Schedules">
              <IconButton size="small" onClick={() => setSchedulesExam(params.row)}>
                <EventNoteOutlinedIcon fontSize="small" />
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
    [],
  );

  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" spacing={1.5} sx={{ mb: 2 }}>
        <Button variant="outlined" startIcon={<CategoryOutlinedIcon />} onClick={() => setTypesManagerOpen(true)}>
          Exam Types
        </Button>
        <Button
          variant="contained"
          startIcon={<AddOutlinedIcon />}
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          Add Exam
        </Button>
      </Stack>

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Class"
                value={classId}
                onChange={(e) => setClassId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All classes</MenuItem>
                {classes.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.className}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Academic Year"
                value={academicYearId}
                onChange={(e) => setAcademicYearId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All years</MenuItem>
                {years.map((y) => (
                  <MenuItem key={y.id} value={y.id}>
                    {y.yearName}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Exam Type"
                value={examTypeId}
                onChange={(e) => setExamTypeId(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <MenuItem value="">All exam types</MenuItem>
                {examTypes.map((t) => (
                  <MenuItem key={t.id} value={t.id}>
                    {t.name}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
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
          emptyTitle="No exams found"
          emptyDescription="Add an exam to get started."
        />
      </Card>

      <ExamFormDialog
        open={formOpen}
        editing={editing}
        examTypes={examTypes}
        classes={classes}
        years={years}
        saving={saving}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleSave}
      />

      <ExamSchedulesDialog open={!!schedulesExam} exam={schedulesExam} onClose={() => setSchedulesExam(null)} />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete exam"
        message="This may fail if the exam still has schedules or marks recorded against it."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <LookupManagerDialog
        open={typesManagerOpen}
        title="Exam Types"
        onClose={() => setTypesManagerOpen(false)}
        fetchAll={async () => (await examApi.examTypes.list()).data.map((t) => ({ id: t.id, name: t.name, description: null }))}
        create={async ({ name }) => {
          const res = await examApi.examTypes.create({ name });
          return { id: res.data.id, name: res.data.name, description: null };
        }}
        update={async (id, { name }) => {
          const res = await examApi.examTypes.update(id, { name });
          return { id: res.data.id, name: res.data.name, description: null };
        }}
        remove={async (id) => {
          await examApi.examTypes.remove(id);
        }}
        onChanged={loadLookups}
      />
    </Box>
  );
}

export default ExamSetupPage;
