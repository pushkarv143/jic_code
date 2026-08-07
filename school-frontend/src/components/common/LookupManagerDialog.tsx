import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import CircularProgress from '@mui/material/CircularProgress';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useSnackbar } from 'notistack';
import ConfirmDialog from './ConfirmDialog';
import EmptyState from './EmptyState';

export interface LookupItem {
  id: number;
  name: string;
  description: string | null;
}

export interface LookupManagerDialogProps {
  open: boolean;
  title: string;
  onClose: () => void;
  fetchAll: () => Promise<LookupItem[]>;
  create: (payload: { name: string; description?: string }) => Promise<LookupItem>;
  update: (id: number, payload: { name: string; description?: string }) => Promise<LookupItem>;
  remove: (id: number) => Promise<void>;
  /** Called after any successful mutation, so the parent can refresh dropdown options elsewhere. */
  onChanged?: () => void;
}

/**
 * Generic add/edit/delete manager for simple name+description lookup entities
 * (Departments, Designations). Reused instead of writing near-identical CRUD
 * dialogs twice.
 */
export function LookupManagerDialog({
  open,
  title,
  onClose,
  fetchAll,
  create,
  update,
  remove,
  onChanged,
}: LookupManagerDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [items, setItems] = useState<LookupItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<LookupItem | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      setItems(await fetchAll());
    } catch {
      enqueueSnackbar(`Could not load ${title.toLowerCase()}.`, { variant: 'error' });
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
    setName('');
    setDescription('');
  };

  const handleEdit = (item: LookupItem) => {
    setEditingId(item.id);
    setName(item.name);
    setDescription(item.description ?? '');
  };

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      if (editingId) {
        await update(editingId, { name: name.trim(), description: description.trim() || undefined });
        enqueueSnackbar(`${title.slice(0, -1)} updated.`, { variant: 'success' });
      } else {
        await create({ name: name.trim(), description: description.trim() || undefined });
        enqueueSnackbar(`${title.slice(0, -1)} added.`, { variant: 'success' });
      }
      resetForm();
      await load();
      onChanged?.();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save. Please try again.', {
        variant: 'error',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await remove(deleteTarget.id);
      enqueueSnackbar(`${title.slice(0, -1)} deleted.`, { variant: 'success' });
      setDeleteTarget(null);
      await load();
      onChanged?.();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — it may be in use.', {
        variant: 'error',
      });
      setDeleteTarget(null);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>Manage {title}</DialogTitle>
        <DialogContent dividers>
          <Box sx={{ display: 'flex', gap: 1.5, mb: 2, flexWrap: 'wrap' }}>
            <TextField
              label="Name"
              size="small"
              value={name}
              onChange={(e) => setName(e.target.value)}
              sx={{ flex: 1, minWidth: 160 }}
            />
            <TextField
              label="Description"
              size="small"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              sx={{ flex: 2, minWidth: 200 }}
            />
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!name.trim() || saving}
              startIcon={editingId ? <EditOutlinedIcon /> : <AddOutlinedIcon />}
            >
              {editingId ? 'Update' : 'Add'}
            </Button>
            {editingId && (
              <Button onClick={resetForm} color="inherit">
                Cancel
              </Button>
            )}
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={28} />
            </Box>
          ) : items.length === 0 ? (
            <EmptyState
              title={`No ${title.toLowerCase()} yet`}
              description={`Add one using the form above.`}
            />
          ) : (
            <List dense disablePadding>
              {items.map((item, idx) => (
                <Box key={item.id}>
                  {idx > 0 && <Divider component="li" />}
                  <ListItem
                    secondaryAction={
                      <Box sx={{ display: 'flex', gap: 0.5 }}>
                        <IconButton size="small" onClick={() => handleEdit(item)}>
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => setDeleteTarget(item)}>
                          <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                        </IconButton>
                      </Box>
                    }
                  >
                    <ListItemText primary={item.name} secondary={item.description || '—'} />
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
        title={`Delete ${deleteTarget?.name ?? ''}`}
        message={`This may fail if the ${title.toLowerCase().slice(0, -1)} is still in use by a teacher or staff record.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}

export default LookupManagerDialog;
