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
import ConfirmDialog from '@/components/common/ConfirmDialog';
import EmptyState from '@/components/common/EmptyState';
import libraryApi from '@/api/libraryApi';
import type { BookCategory } from '@/types';

export interface CategoryManagerDialogProps {
  open: boolean;
  onClose: () => void;
  onChanged: () => void;
}

/** Simple add/edit/delete manager for book categories (name only, per schema). */
export function CategoryManagerDialog({ open, onClose, onChanged }: CategoryManagerDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [items, setItems] = useState<BookCategory[]>([]);
  const [loading, setLoading] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<BookCategory | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const res = await libraryApi.bookCategories.list();
      setItems(res.data);
    } catch {
      enqueueSnackbar('Could not load book categories.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      load();
      setEditingId(null);
      setName('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      if (editingId) {
        await libraryApi.bookCategories.update(editingId, { name: name.trim() });
        enqueueSnackbar('Category updated.', { variant: 'success' });
      } else {
        await libraryApi.bookCategories.create({ name: name.trim() });
        enqueueSnackbar('Category added.', { variant: 'success' });
      }
      setEditingId(null);
      setName('');
      await load();
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this category.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await libraryApi.bookCategories.remove(deleteTarget.id);
      enqueueSnackbar('Category deleted.', { variant: 'success' });
      setDeleteTarget(null);
      await load();
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — it may be in use by a book.', {
        variant: 'error',
      });
      setDeleteTarget(null);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
        <DialogTitle>Manage Book Categories</DialogTitle>
        <DialogContent dividers>
          <Box sx={{ display: 'flex', gap: 1.5, mb: 2 }}>
            <TextField
              label="Category name"
              size="small"
              fullWidth
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!name.trim() || saving}
              startIcon={editingId ? <EditOutlinedIcon /> : <AddOutlinedIcon />}
            >
              {editingId ? 'Update' : 'Add'}
            </Button>
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={28} />
            </Box>
          ) : items.length === 0 ? (
            <EmptyState title="No categories yet" description="Add a category using the form above." />
          ) : (
            <List dense disablePadding>
              {items.map((item, idx) => (
                <Box key={item.id}>
                  {idx > 0 && <Divider component="li" />}
                  <ListItem
                    secondaryAction={
                      <Box sx={{ display: 'flex', gap: 0.5 }}>
                        <IconButton
                          size="small"
                          onClick={() => {
                            setEditingId(item.id);
                            setName(item.name);
                          }}
                        >
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => setDeleteTarget(item)}>
                          <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                        </IconButton>
                      </Box>
                    }
                  >
                    <ListItemText primary={item.name} />
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
        message="This may fail if the category is still in use by a book."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}

export default CategoryManagerDialog;
