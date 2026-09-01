import { useNavigate, useLocation } from 'react-router-dom';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Tooltip from '@mui/material/Tooltip';
import Avatar from '@mui/material/Avatar';
import Stack from '@mui/material/Stack';
import SchoolIcon from '@mui/icons-material/School';
import { useTranslation } from '@/i18n/LanguageProvider';
import { useAccess } from '@/access/AccessProvider';
import { MenuIcon } from './menuIcons';
import { useAppSelector } from '@/store/hooks';

export const SIDEBAR_WIDTH = 264;
export const SIDEBAR_WIDTH_COLLAPSED = 72;

interface SidebarContentProps {
  collapsed: boolean;
  onNavigate?: () => void;
}

function SidebarContent({ collapsed, onNavigate }: SidebarContentProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { access } = useAccess();
  const groups = access?.menus ?? [];
  const t = useTranslation();
  const user = useAppSelector((s) => s.auth.user);

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : '?';
  const displayName = user ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() : '';
  const roleName = user?.role
    ? (user.role as string).replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
    : '';

  return (
    <Box
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'sidebar.background',
        color: 'sidebar.color',
        overflowX: 'hidden',
      }}
    >
      {/* ---- Brand header ---- */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          px: collapsed ? 1.5 : 2.5,
          height: 64,
          flexShrink: 0,
          position: 'relative',
        }}
      >
        <Box sx={{
          width: 36, height: 36, borderRadius: 2,
          background: 'rgba(255,183,3,0.15)',
          border: '1px solid rgba(255,183,3,0.25)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
        }}>
          <SchoolIcon sx={{ color: 'secondary.main', fontSize: 22 }} />
        </Box>
        {!collapsed && (
          <Box sx={{ overflow: 'hidden' }}>
            <Typography
              variant="subtitle2"
              sx={{ fontWeight: 800, color: 'inherit', lineHeight: 1.1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
            >
              Greenwood
            </Typography>
            <Typography
              component="span"
              sx={{ display: 'block', fontSize: '0.68rem', color: 'rgba(230,233,242,0.55)', letterSpacing: '0.02em', whiteSpace: 'nowrap' }}
            >
              International School
            </Typography>
          </Box>
        )}
      </Box>
      <Divider sx={{ borderColor: 'rgba(230,233,242,0.1)' }} />

      {/* ---- Navigation ---- */}
      <Box sx={{ flexGrow: 1, overflowY: 'auto', overflowX: 'hidden', py: 1.5 }}>
        {groups.map((group) => (
          <Box key={group.menuKey} sx={{ mb: 0.5 }}>
            {!collapsed && (
              <Typography
                variant="overline"
                sx={{
                  display: 'block',
                  px: 2.5,
                  pt: 1.5,
                  pb: 0.75,
                  color: 'rgba(230,233,242,0.35)',
                  fontSize: '0.65rem',
                  letterSpacing: '0.1em',
                  fontWeight: 700,
                }}
              >
                {group.i18nKey ? t(group.i18nKey) : group.label}
              </Typography>
            )}
            {collapsed && <Box sx={{ height: 12 }} />}
            <List disablePadding>
              {group.children.map((item) => {
                const path = item.path;
                if (!path) return null;
                const selected = location.pathname.startsWith(path);
                const label = item.i18nKey ? t(item.i18nKey) : item.label;

                const button = (
                  <ListItemButton
                    key={path}
                    selected={selected}
                    onClick={() => { navigate(path); onNavigate?.(); }}
                    sx={{
                      mx: 1.25,
                      my: 0.2,
                      borderRadius: 2,
                      color: 'inherit',
                      justifyContent: collapsed ? 'center' : 'flex-start',
                      px: collapsed ? 1.25 : 1.75,
                      py: 0.85,
                      position: 'relative',
                      transition: 'all 0.15s ease',
                      '&.Mui-selected': {
                        background: 'linear-gradient(90deg, rgba(255,183,3,0.18) 0%, rgba(255,183,3,0.04) 100%)',
                        color: '#ffcb47',
                        '&::before': {
                          content: '""',
                          position: 'absolute',
                          left: 0,
                          top: '20%',
                          height: '60%',
                          width: 3,
                          borderRadius: '0 3px 3px 0',
                          backgroundColor: '#ffb703',
                        },
                        '& .MuiListItemIcon-root': { color: '#ffcb47' },
                      },
                      '&:hover:not(.Mui-selected)': {
                        bgcolor: 'rgba(230,233,242,0.07)',
                      },
                    }}
                  >
                    <ListItemIcon
                      sx={{
                        color: selected ? '#ffcb47' : 'rgba(230,233,242,0.65)',
                        minWidth: collapsed ? 0 : 38,
                        justifyContent: 'center',
                        transition: 'color 0.15s ease',
                      }}
                    >
                      <MenuIcon name={item.icon} />
                    </ListItemIcon>
                    {!collapsed && (
                      <ListItemText
                        primary={label}
                        primaryTypographyProps={{
                          fontSize: '0.85rem',
                          fontWeight: selected ? 700 : 500,
                          letterSpacing: '0.005em',
                        }}
                      />
                    )}
                  </ListItemButton>
                );
                return collapsed ? (
                  <Tooltip key={path} title={label} placement="right">
                    {button}
                  </Tooltip>
                ) : (
                  button
                );
              })}
            </List>
          </Box>
        ))}
      </Box>

      {/* ---- User profile card at bottom ---- */}
      {user && (
        <>
          <Divider sx={{ borderColor: 'rgba(230,233,242,0.1)' }} />
          <Box sx={{
            px: collapsed ? 1.25 : 1.75,
            py: 1.5,
            display: 'flex',
            alignItems: 'center',
            gap: 1.25,
          }}>
            <Avatar
              src={user.profileImage ?? undefined}
              sx={{
                width: 34, height: 34, fontSize: '0.8rem', fontWeight: 700, flexShrink: 0,
                background: 'linear-gradient(135deg, #ffb703, #c88900)',
                color: '#1c2763',
              }}
            >
              {initials}
            </Avatar>
            {!collapsed && (
              <Stack sx={{ overflow: 'hidden', minWidth: 0 }}>
                <Typography
                  variant="body2"
                  sx={{ fontWeight: 700, color: 'rgba(230,233,242,0.9)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontSize: '0.82rem' }}
                >
                  {displayName || 'User'}
                </Typography>
                <Typography
                  sx={{ fontSize: '0.68rem', color: 'rgba(230,233,242,0.45)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                >
                  {roleName}
                </Typography>
              </Stack>
            )}
          </Box>
        </>
      )}
    </Box>
  );
}

export interface SidebarProps {
  mobileOpen: boolean;
  collapsed: boolean;
  onClose: () => void;
}

export function Sidebar({ mobileOpen, collapsed, onClose }: SidebarProps) {
  const width = collapsed ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH;

  return (
    <Box component="nav" sx={{ width: { md: width }, flexShrink: { md: 0 } }}>
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onClose}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { width: SIDEBAR_WIDTH },
        }}
      >
        <SidebarContent collapsed={false} onNavigate={onClose} />
      </Drawer>
      <Drawer
        variant="permanent"
        open
        sx={{
          display: { xs: 'none', md: 'block' },
          '& .MuiDrawer-paper': {
            width,
            boxSizing: 'border-box',
            transition: 'width 0.22s cubic-bezier(0.4,0,0.2,1)',
            overflowX: 'hidden',
          },
        }}
      >
        <SidebarContent collapsed={collapsed} />
      </Drawer>
    </Box>
  );
}

export default Sidebar;
