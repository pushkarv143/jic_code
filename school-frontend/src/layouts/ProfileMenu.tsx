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
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import LogoutIcon from '@mui/icons-material/Logout';
import { alpha } from '@mui/material/styles';
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
      {/* Clickable avatar button in TopBar */}
      <ButtonBase
        onClick={handleOpen}
        sx={{
          borderRadius: 3,
          p: 0.5,
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          transition: 'background-color 0.15s ease',
          '&:hover': { bgcolor: (theme) => alpha(theme.palette.primary.main, 0.06) },
        }}
      >
        <Avatar
          sx={{
            width: 34,
            height: 34,
            background: 'linear-gradient(135deg, #ffb703, #c88900)',
            color: '#1c2763',
            fontSize: '0.78rem',
            fontWeight: 800,
          }}
        >
          {initials || <PersonOutlineIcon fontSize="small" />}
        </Avatar>
        <Box sx={{ display: { xs: 'none', sm: 'block' }, textAlign: 'left' }}>
          <Typography variant="body2" fontWeight={700} lineHeight={1.2} sx={{ fontSize: '0.82rem' }}>
            {user.firstName} {user.lastName}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.68rem' }}>
            {formatRoleLabel(user.role)}
          </Typography>
        </Box>
      </ButtonBase>

      {/* Dropdown menu */}
      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{ sx: { width: 270, mt: 1, overflow: 'hidden' } }}
      >
        {/* Gradient identity header */}
        <Box
          sx={{
            background: 'linear-gradient(135deg, #1c2763 0%, #2b3a8f 100%)',
            px: 2,
            py: 2,
            position: 'relative',
            '&::after': {
              content: '""',
              position: 'absolute',
              inset: 0,
              backgroundImage: 'radial-gradient(rgba(255,255,255,0.04) 1px, transparent 1px)',
              backgroundSize: '20px 20px',
              pointerEvents: 'none',
            },
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, position: 'relative', zIndex: 1 }}>
            <Avatar
              sx={{
                width: 44,
                height: 44,
                background: 'linear-gradient(135deg, #ffb703, #c88900)',
                color: '#1c2763',
                fontWeight: 800,
                fontSize: '1rem',
              }}
            >
              {initials}
            </Avatar>
            <Box>
              <Typography variant="subtitle2" fontWeight={700} sx={{ color: '#fff', lineHeight: 1.2 }}>
                {user.firstName} {user.lastName}
              </Typography>
              <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.6)', display: 'block', fontSize: '0.7rem' }}>
                {user.email}
              </Typography>
              <Box
                sx={{
                  mt: 0.5,
                  display: 'inline-flex',
                  alignItems: 'center',
                  px: 1,
                  py: 0.25,
                  borderRadius: 50,
                  bgcolor: 'rgba(255,183,3,0.18)',
                  border: '1px solid rgba(255,183,3,0.3)',
                }}
              >
                <Typography sx={{ fontSize: '0.65rem', fontWeight: 700, color: '#ffcb47', lineHeight: 1 }}>
                  {formatRoleLabel(user.role)}
                </Typography>
              </Box>
            </Box>
          </Box>
        </Box>

        {/* Menu items */}
        <Box sx={{ py: 0.5 }}>
          <MenuItem
            onClick={() => { handleClose(); navigate('/app/profile'); }}
            sx={{ mx: 0.75, borderRadius: 2, my: 0.25 }}
          >
            <ListItemIcon><PersonOutlineIcon fontSize="small" /></ListItemIcon>
            <ListItemText primaryTypographyProps={{ fontSize: '0.875rem', fontWeight: 500 }}>My Profile</ListItemText>
          </MenuItem>
          <MenuItem
            onClick={() => { handleClose(); navigate('/app/settings'); }}
            sx={{ mx: 0.75, borderRadius: 2, my: 0.25 }}
          >
            <ListItemIcon><SettingsOutlinedIcon fontSize="small" /></ListItemIcon>
            <ListItemText primaryTypographyProps={{ fontSize: '0.875rem', fontWeight: 500 }}>Settings</ListItemText>
          </MenuItem>
        </Box>

        <Divider sx={{ mx: 1 }} />

        <Box sx={{ py: 0.5 }}>
          <MenuItem
            onClick={handleLogout}
            sx={{ color: 'error.main', mx: 0.75, borderRadius: 2, my: 0.25 }}
          >
            <ListItemIcon><LogoutIcon fontSize="small" color="error" /></ListItemIcon>
            <ListItemText primaryTypographyProps={{ fontSize: '0.875rem', fontWeight: 600, color: 'inherit' }}>
              Sign out
            </ListItemText>
          </MenuItem>
        </Box>
      </Menu>
    </>
  );
}

export default ProfileMenu;
