import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import transportApi from '@/api/transportApi';
import type { PickupPoint, Route } from '@/types';
import PickupPointFormDialog from './PickupPointFormDialog';

export interface RouteDetailDialogProps {
  open: boolean;
  route: Route | null;
  onClose: () => void;
  onChanged?: () => void;
}

/** Nested management dialog: pickup points for a single route (add/edit/delete), matching the ClassDetailPage pattern. */
export function RouteDetailDialog({ open, route, onClose, onChanged }: RouteDetailDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [points, setPoints] = useState<PickupPoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<PickupPoint | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<PickupPoint | null>(null);

  const load = async () => {
    if (!route) return;
    setLoading(true);
    try {
      const res = await transportApi.pickupPoints.list(route.id);
      setPoints(res.data);
    } catch {
      enqueueSnackbar('Could not load pickup points.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, route?.id]);

  const handleSave = async (values: Parameters<typeof transportApi.pickupPoints.create>[1]) => {
    if (!route) return;
    setSaving(true);
    try {
      if (editing) {
        await transportApi.pickupPoints.update(editing.id, values);
        enqueueSnackbar('Pickup point updated.', { variant: 'success' });
      } else {
        await transportApi.pickupPoints.create(route.id, values);
        enqueueSnackbar('Pickup point added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      await load();
      onChanged?.();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this pickup point.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await transportApi.pickupPoints.remove(deleteTarget.id);
      enqueueSnackbar('Pickup point deleted.', { variant: 'success' });
      setDeleteTarget(null);
      await load();
      onChanged?.();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — students may be assigned to it.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {route?.routeName ?? 'Route'} — Pickup Points
          <Typography variant="body2" color="text.secondary">
            {route?.startPoint} → {route?.endPoint}
          </Typography>
        </DialogTitle>
        <DialogContent dividers>
          <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
            <Button
              size="small"
              variant="outlined"
              startIcon={<AddOutlinedIcon />}
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              Add Pickup Point
            </Button>
          </Stack>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={28} />
            </Box>
          ) : points.length === 0 ? (
            <EmptyState title="No pickup points yet" description="Add a pickup point to this route." />
          ) : (
            <List dense disablePadding>
              {points.map((p, idx) => (
                <Box key={p.id}>
                  {idx > 0 && <Divider component="li" />}
                  <ListItem
                    secondaryAction={
                      <Stack direction="row" spacing={0.5}>
                        <IconButton
                          size="small"
                          onClick={() => {
                            setEditing(p);
                            setFormOpen(true);
                          }}
                        >
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => setDeleteTarget(p)}>
                          <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                        </IconButton>
                      </Stack>
                    }
                  >
                    <ListItemText
                      primary={p.pointName}
                      secondary={`Pickup ${dayjs(p.pickupTime, 'HH:mm:ss').format('hh:mm A')} · Drop ${dayjs(p.dropTime, 'HH:mm:ss').format('hh:mm A')}`}
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

      <PickupPointFormDialog
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
        title={`Delete ${deleteTarget?.pointName ?? ''}`}
        message="This may fail if students are currently assigned to this pickup point."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </>
  );
}

export default RouteDetailDialog;
