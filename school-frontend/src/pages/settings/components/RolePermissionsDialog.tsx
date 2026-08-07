import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import { useSnackbar } from 'notistack';
import settingsApi from '@/api/settingsApi';
import EmptyState from '@/components/common/EmptyState';
import type { PermissionInfo, RoleInfo } from '@/types';

export interface RolePermissionsDialogProps {
  open: boolean;
  role: RoleInfo | null;
  onClose: () => void;
}

/** Read-only side panel listing a role's permissions — GET /roles/{id}/permissions. */
export function RolePermissionsDialog({ open, role, onClose }: RolePermissionsDialogProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [permissions, setPermissions] = useState<PermissionInfo[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open || !role) return;
    setLoading(true);
    settingsApi
      .getRolePermissions(role.id)
      .then((res) => setPermissions(res.data))
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load permissions for this role.', { variant: 'error' });
        setPermissions([]);
      })
      .finally(() => setLoading(false));
  }, [open, role, enqueueSnackbar]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{role ? `${role.name} — Permissions` : 'Permissions'}</DialogTitle>
      <DialogContent dividers>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        ) : permissions.length === 0 ? (
          <EmptyState title="No permissions found" description="This role has no permissions assigned yet." />
        ) : (
          <List dense disablePadding>
            {permissions.map((p, idx) => (
              <Box key={p.id}>
                {idx > 0 && <Divider component="li" />}
                <ListItem
                  secondaryAction={<Chip size="small" variant="outlined" label={p.module} />}
                >
                  <ListItemText primary={p.name} secondary={p.description || '—'} />
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
  );
}

export default RolePermissionsDialog;
