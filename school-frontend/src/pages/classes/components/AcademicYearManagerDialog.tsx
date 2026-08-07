import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import CircularProgress from '@mui/material/CircularProgress';
import Tooltip from '@mui/material/Tooltip';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import academicYearsApi from '@/api/academicYearsApi';
import type { AcademicYear } from '@/types';

export interface AcademicYearManagerDialogProps {
  open: boolean;
  onClose: () => void;
  onChanged: () => void;
}

/** Add/edit/delete academic years and mark one as current. */
export function AcademicYearManagerDialog({ open, onClose, onChanged }: AcademicYearManagerDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [years, setYears] = useState<AcademicYear[]>([]);
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [yearName, setYearName] = useState('');
  const [startDate, setStartDate] = useState<dayjs.Dayjs | null>(null);
  const [endDate, setEndDate] = useState<dayjs.Dayjs | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<AcademicYear | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const res = await academicYearsApi.list();
      setYears(res.data);
    } catch {
      enqueueSnackbar('Could not load academic years.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      load();
      resetForm();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const resetForm = () => {
    setEditingId(null);
    setYearName('');
    setStartDate(null);
    setEndDate(null);
  };

  const handleEdit = (year: AcademicYear) => {
    setEditingId(year.id);
    setYearName(year.yearName);
    setStartDate(dayjs(year.startDate));
    setEndDate(dayjs(year.endDate));
  };

  const handleSave = async () => {
    if (!yearName.trim() || !startDate || !endDate) return;
    setSaving(true);
    const payload = {
      yearName: yearName.trim(),
      startDate: startDate.format('YYYY-MM-DD'),
      endDate: endDate.format('YYYY-MM-DD'),
    };
    try {
      if (editingId) {
        await academicYearsApi.update(editingId, payload);
        enqueueSnackbar('Academic year updated.', { variant: 'success' });
      } else {
        await academicYearsApi.create(payload);
        enqueueSnackbar('Academic year added.', { variant: 'success' });
      }
      resetForm();
      await load();
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save. Please try again.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleSetCurrent = async (year: AcademicYear) => {
    try {
      await academicYearsApi.setCurrent(year.id);
      enqueueSnackbar(`${year.yearName} marked as the current academic year.`, { variant: 'success' });
      await load();
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not update the current academic year.', {
        variant: 'error',
      });
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await academicYearsApi.remove(deleteTarget.id);
      enqueueSnackbar('Academic year deleted.', { variant: 'success' });
      setDeleteTarget(null);
      await load();
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — it may be in use.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>Manage Academic Years</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={1.5} sx={{ mb: 2 }}>
            <Grid item xs={12} sm={4}>
              <TextField
                label="Year Name"
                size="small"
                fullWidth
                placeholder="2026-2027"
                value={yearName}
                onChange={(e) => setYearName(e.target.value)}
              />
            </Grid>
            <Grid item xs={6} sm={3}>
              <DatePicker
                label="Start Date"
                value={startDate}
                onChange={setStartDate}
                slotProps={{ textField: { size: 'small', fullWidth: true } }}
              />
            </Grid>
            <Grid item xs={6} sm={3}>
              <DatePicker
                label="End Date"
                value={endDate}
                onChange={setEndDate}
                slotProps={{ textField: { size: 'small', fullWidth: true } }}
              />
            </Grid>
            <Grid item xs={12} sm={2}>
              <Button
                fullWidth
                variant="contained"
                onClick={handleSave}
                disabled={!yearName.trim() || !startDate || !endDate || saving}
                startIcon={editingId ? <EditOutlinedIcon /> : <AddOutlinedIcon />}
              >
                {editingId ? 'Update' : 'Add'}
              </Button>
            </Grid>
          </Grid>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={28} />
            </Box>
          ) : years.length === 0 ? (
            <EmptyState title="No academic years yet" description="Add one using the form above." />
          ) : (
            <List dense disablePadding>
              {years.map((year, idx) => (
                <Box key={year.id}>
                  {idx > 0 && <Divider component="li" />}
                  <ListItem
                    secondaryAction={
                      <Box sx={{ display: 'flex', gap: 0.5 }}>
                        {!year.isCurrent && (
                          <Tooltip title="Mark as current">
                            <IconButton size="small" onClick={() => handleSetCurrent(year)}>
                              <CheckCircleOutlineOutlinedIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        <IconButton size="small" onClick={() => handleEdit(year)}>
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => setDeleteTarget(year)}>
                          <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                        </IconButton>
                      </Box>
                    }
                  >
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          {year.yearName}
                          {year.isCurrent && <Chip label="Current" size="small" color="success" />}
                        </Box>
                      }
                      secondary={`${dayjs(year.startDate).format('DD MMM YYYY')} - ${dayjs(year.endDate).format('DD MMM YYYY')}`}
                    />
                  </ListItem>
                </Box>
              ))}
            </List>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Close</Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title={`Delete ${deleteTarget?.yearName ?? ''}`}
        message="This may fail if classes are still linked to this academic year."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}

export default AcademicYearManagerDialog;
