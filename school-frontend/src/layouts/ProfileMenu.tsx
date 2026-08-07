import { useState, type MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Avatar from '@mui/material/Avatar';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import Typography from '@mui/material/Typography';
import ButtonBase from '@mui/material/ButtonBase';
import Chip from '@mui/material/Chip';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { logout as logoutAction } from '@/store/authSlice';
import { authApi } from '@/api/authApi';
import { useSnackbar } from 'notistack';
import { formatRoleLabel } from '@/utils/format';

export function ProfileMenu() {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const user = useAppSelector((state) => state.auth.user);
  const refreshToken = useAppSelector((state) => state.auth.refreshToken);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const open = Boolean(anchorEl);

  const handleOpen = (event: MouseEvent<HTMLElement>) => setAnchorEl(event.currentTarget);
  const handleClose = () => setAnchorEl(null);

  const handleLogout = async () => {
    handleClose();
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      // Backend may be unreachable; still log the user out client-side.
    } finally {
      dispatch(logoutAction());
      enqueueSnackbar('You have been logged out.', { variant: 'info' });
      navigate('/login');
    }
  };

  if (!user) return null;

  const initials = `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();

  return (
    <>
      <ButtonBase
        onClick={handleOpen}
        sx={{ borderRadius: 6, p: 0.5, display: 'flex', alignItems: 'center', gap: 1 }}
      >
        <Avatar sx={{ width: 36, height: 36, bgcolor: 'secondary.main', color: 'secondary.contrastText', fontSize: 14, fontWeight: 700 }}>
          {initials || <PersonOutlineIcon fontSize="small" />}
        </Avatar>
        <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'left' }}>
          <Typography variant="body2" fontWeight={600} lineHeight={1.2}>
            {user.firstName} {user.lastName}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {formatRoleLabel(user.role)}
          </Typography>
        </Box>
      </ButtonBase>
      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{ sx: { width: 260, mt: 1 } }}
      >
        <Box sx={{ px: 2, py: 1.5 }}>
          <Typography variant="subtitle2" fontWeight={700}>
            {user.firstName} {user.lastName}
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block">
            {user.email}
          </Typography>
          <Chip label={formatRoleLabel(user.role)} size="small" color="primary" variant="outlined" sx={{ mt: 1 }} />
        </Box>
        <Divider />
        <MenuItem
          onClick={() => {
            handleClose();
            navigate('/app/profile');
          }}
        >
          <ListItemIcon>
            <PersonOutlineIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>My Profile</ListItemText>
        </MenuItem>
        <MenuItem
          onClick={() => {
            handleClose();
            navigate('/app/settings');
          }}
        >
          <ListItemIcon>
            <SettingsOutlinedIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Settings</ListItemText>
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <LogoutIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Logout</ListItemText>
        </MenuItem>
      </Menu>
    </>
  );
}

export default ProfileMenu;
