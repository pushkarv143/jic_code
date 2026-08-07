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
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import PageHeader from '@/components/common/PageHeader';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import payrollApi from '@/api/payrollApi';
import type { SalaryStructurePayload } from '@/api/payrollApi';
import { formatCurrencyINR } from '@/utils/format';
import type { PayrollEmployeeType, SalaryStructure } from '@/types';
import SalaryStructureFormDialog from './components/SalaryStructureFormDialog';

/** Salary structure CRUD: basic/HRA/DA/allowances/PF%/ESI% per employee. */
export function SalaryStructuresPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [employeeType, setEmployeeType] = useState<PayrollEmployeeType | ''>('');
  const [rows, setRows] = useState<SalaryStructure[]>([]);
  const [loading, setLoading] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SalaryStructure | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<SalaryStructure | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await payrollApi.salaryStructures.list({ employeeType: employeeType || undefined });
      setRows(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load salary structures.', { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [employeeType, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSave = async (values: SalaryStructurePayload) => {
    setSaving(true);
    try {
      if (editing) {
        await payrollApi.salaryStructures.update(editing.id, values);
        enqueueSnackbar('Salary structure updated.', { variant: 'success' });
      } else {
        await payrollApi.salaryStructures.create(values);
        enqueueSnackbar('Salary structure added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this salary structure.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await payrollApi.salaryStructures.remove(deleteTarget.id);
      enqueueSnackbar('Salary structure deleted.', { variant: 'success' });
      setDeleteTarget(null);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this salary structure.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<SalaryStructure>[] = useMemo(
    () => [
      { field: 'employeeName', headerName: 'Employee', flex: 1, minWidth: 170, valueGetter: (_v, row) => row.employeeName ?? `Employee #${row.employeeId}` },
      { field: 'employeeType', headerName: 'Type', width: 100 },
      { field: 'basicSalary', headerName: 'Basic', width: 110, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'hra', headerName: 'HRA', width: 100, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'da', headerName: 'DA', width: 100, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'otherAllowances', headerName: 'Other Allow.', width: 120, valueFormatter: (value) => formatCurrencyINR(Number(value ?? 0)) },
      { field: 'pfPercentage', headerName: 'PF %', width: 80, valueFormatter: (value) => `${value}%` },
      { field: 'esiPercentage', headerName: 'ESI %', width: 80, valueFormatter: (value) => `${value}%` },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 110,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => { setEditing(params.row); setFormOpen(true); }}>
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
      <PageHeader
        title="Salary Structures"
        subtitle="Define basic pay, HRA, DA, allowances, PF and ESI percentages per employee"
        action={
          <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
            Add Salary Structure
          </Button>
        }
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                select
                fullWidth
                size="small"
                label="Employee Type"
                value={employeeType}
                onChange={(e) => setEmployeeType(e.target.value as PayrollEmployeeType | '')}
              >
                <MenuItem value="">All types</MenuItem>
                <MenuItem value="TEACHER">Teacher</MenuItem>
                <MenuItem value="STAFF">Staff</MenuItem>
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
          emptyTitle="No salary structures found"
          emptyDescription="Add a salary structure to get started with payroll."
        />
      </Card>

      <SalaryStructureFormDialog
        open={formOpen}
        editing={editing}
        saving={saving}
        onClose={() => { setFormOpen(false); setEditing(null); }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete salary structure"
        message={`Delete the salary structure for "${deleteTarget?.employeeName ?? `Employee #${deleteTarget?.employeeId}`}"? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

export default SalaryStructuresPage;
