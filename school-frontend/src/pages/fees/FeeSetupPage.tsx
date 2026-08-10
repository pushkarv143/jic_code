import { useCallback, useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import CircularProgress from '@mui/material/CircularProgress';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import PlaylistAddOutlinedIcon from '@mui/icons-material/PlaylistAddOutlined';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import DataTable from '@/components/common/DataTable';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import feesApi from '@/api/feesApi';
import classesApi from '@/api/classesApi';
import academicYearsApi from '@/api/academicYearsApi';
import { formatCurrencyINR } from '@/utils/format';
import type { AcademicYear, FeeCategory, FeeStructure, SchoolClass } from '@/types';
import FeeCategoryFormDialog from './components/FeeCategoryFormDialog';
import FeeStructureFormDialog from './components/FeeStructureFormDialog';
import GenerateDuesDialog from './components/GenerateDuesDialog';

function FeeCategoriesTab() {
  const { enqueueSnackbar } = useSnackbar();
  const [items, setItems] = useState<FeeCategory[]>([]);
  const [loading, setLoading] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<FeeCategory | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<FeeCategory | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await feesApi.feeCategories.list();
      setItems(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load fee categories.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSave = async (values: { name: string; description?: string }) => {
    setSaving(true);
    try {
      if (editing) {
        await feesApi.feeCategories.update(editing.id, values);
        enqueueSnackbar('Fee category updated.', { variant: 'success' });
      } else {
        await feesApi.feeCategories.create(values);
        enqueueSnackbar('Fee category added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this fee category.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await feesApi.feeCategories.remove(deleteTarget.id);
      enqueueSnackbar('Fee category deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — it may be used by a fee structure.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button
          size="small"
          variant="contained"
          startIcon={<AddOutlinedIcon />}
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          Add Fee Category
        </Button>
      </Stack>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress size={28} />
        </Box>
      ) : items.length === 0 ? (
        <EmptyState title="No fee categories yet" description="Add categories like Tuition Fee, Transport Fee, Lab Fee etc." />
      ) : (
        <Card variant="outlined">
          <List dense disablePadding>
            {items.map((item, idx) => (
              <Box key={item.id}>
                {idx > 0 && <Divider component="li" />}
                <ListItem
                  secondaryAction={
                    <Stack direction="row" spacing={0.5}>
                      <IconButton
                        size="small"
                        onClick={() => {
                          setEditing(item);
                          setFormOpen(true);
                        }}
                      >
                        <EditOutlinedIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" onClick={() => setDeleteTarget(item)}>
                        <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                      </IconButton>
                    </Stack>
                  }
                >
                  <ListItemText primary={item.name} secondary={item.description || '—'} />
                </ListItem>
              </Box>
            ))}
          </List>
        </Card>
      )}

      <FeeCategoryFormDialog
        open={formOpen}
        editing={editing}
        saving={saving}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title={`Delete ${deleteTarget?.name ?? ''}`}
        message="This may fail if the category is still referenced by a fee structure."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

function FeeStructuresTab() {
  const { enqueueSnackbar } = useSnackbar();
  const [rows, setRows] = useState<FeeStructure[]>([]);
  const [loading, setLoading] = useState(false);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [categories, setCategories] = useState<FeeCategory[]>([]);
  const [classId, setClassId] = useState<number | ''>('');
  const [academicYearId, setAcademicYearId] = useState<number | ''>('');

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<FeeStructure | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<FeeStructure | null>(null);
  const [generateOpen, setGenerateOpen] = useState(false);

  useEffect(() => {
    classesApi.list({ size: 200, sort: 'className,asc' }).then((res) => setClasses(res.data.content)).catch(() => undefined);
    academicYearsApi.list().then((res) => setYears(res.data)).catch(() => undefined);
    feesApi.feeCategories.list().then((res) => setCategories(res.data)).catch(() => undefined);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await feesApi.feeStructures.list({
        classId: classId || undefined,
        academicYearId: academicYearId || undefined,
      });
      setRows(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load fee structures.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [classId, academicYearId, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSave = async (values: { classId: number; academicYearId: number; feeCategoryId: number; amount: number; dueDate: string }) => {
    setSaving(true);
    try {
      if (editing) {
        await feesApi.feeStructures.update(editing.id, values);
        enqueueSnackbar('Fee structure updated.', { variant: 'success' });
      } else {
        await feesApi.feeStructures.create(values);
        enqueueSnackbar('Fee structure added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this fee structure.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await feesApi.feeStructures.remove(deleteTarget.id);
      enqueueSnackbar('Fee structure deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — dues may already be generated from it.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<FeeStructure>[] = useMemo(
    () => [
      { field: 'className', headerName: 'Class', flex: 0.8, minWidth: 110, valueGetter: (_v, row) => row.className ?? `#${row.classId}` },
      { field: 'feeCategoryName', headerName: 'Fee Category', flex: 1, minWidth: 140, valueGetter: (_v, row) => row.feeCategoryName ?? `#${row.feeCategoryId}` },
      { field: 'academicYearName', headerName: 'Academic Year', flex: 0.9, minWidth: 130, valueGetter: (_v, row) => row.academicYearName ?? `#${row.academicYearId}` },
      { field: 'amount', headerName: 'Amount', width: 130, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      {
        field: 'dueDate',
        headerName: 'Due Date',
        width: 130,
        valueFormatter: (value) => (value ? dayjs(value as string).format('DD MMM YYYY') : '-'),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 110,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
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
      },
    ],
    [],
  );

  return (
    <Box>
      <Card sx={{ mb: 2 }} variant="outlined">
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
            <Grid item xs={12} md={6}>
              <Stack direction="row" spacing={1.5} justifyContent={{ xs: 'flex-start', md: 'flex-end' }}>
                <Button variant="outlined" startIcon={<PlaylistAddOutlinedIcon />} onClick={() => setGenerateOpen(true)}>
                  Generate Dues
                </Button>
                <Button
                  variant="contained"
                  startIcon={<AddOutlinedIcon />}
                  onClick={() => {
                    setEditing(null);
                    setFormOpen(true);
                  }}
                >
                  Add Fee Structure
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <DataTable
          rows={rows}
          columns={columns}
          loading={loading}
          mobileVisibleFields={['className', 'feeCategoryName', 'amount']}
          emptyTitle="No fee structures found"
          emptyDescription="Add a fee structure to define what each class owes per category."
        />
      </Card>

      <FeeStructureFormDialog
        open={formOpen}
        editing={editing}
        classes={classes}
        years={years}
        categories={categories}
        saving={saving}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete fee structure"
        message="This may fail if dues have already been generated from this structure."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <GenerateDuesDialog
        open={generateOpen}
        classes={classes}
        years={years}
        onClose={() => setGenerateOpen(false)}
        onGenerated={() => setGenerateOpen(false)}
      />
    </Box>
  );
}

/** Fee Setup: Fee Categories (simple CRUD list) and Fee Structures (per class/year table + Generate Dues). */
export function FeeSetupPage() {
  const [tab, setTab] = useState(0);

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <Tabs value={tab} onChange={(_e: SyntheticEvent, v: number) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
          <Tab label="Fee Categories" />
          <Tab label="Fee Structures" />
        </Tabs>
      </Card>
      {tab === 0 ? <FeeCategoriesTab /> : <FeeStructuresTab />}
    </Box>
  );
}

export default FeeSetupPage;
