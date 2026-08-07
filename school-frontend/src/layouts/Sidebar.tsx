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
import SchoolIcon from '@mui/icons-material/School';
import { useAppSelector } from '@/store/hooks';
import { useTranslation } from '@/i18n/LanguageProvider';
import { getNavForRole } from './navConfig';

export const SIDEBAR_WIDTH = 264;
export const SIDEBAR_WIDTH_COLLAPSED = 76;

interface SidebarContentProps {
  collapsed: boolean;
  onNavigate?: () => void;
}

function SidebarContent({ collapsed, onNavigate }: SidebarContentProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const role = useAppSelector((state) => state.auth.user?.role);
  const groups = getNavForRole(role);
  const t = useTranslation();

  return (
    <Box
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'sidebar.background',
        color: 'sidebar.color',
      }}
    >
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          px: collapsed ? 1.5 : 2.5,
          height: 64,
          flexShrink: 0,
        }}
      >
        <SchoolIcon sx={{ color: 'secondary.main', fontSize: 30 }} />
        {!collapsed && (
          <Typography variant="subtitle1" sx={{ fontWeight: 700, color: 'inherit', lineHeight: 1.1 }}>
            Greenwood
            <Typography component="span" display="block" variant="caption" color="rgba(230,233,242,0.6)">
              International School
            </Typography>
          </Typography>
        )}
      </Box>
      <Divider sx={{ borderColor: 'rgba(230,233,242,0.12)' }} />
      <Box sx={{ flexGrow: 1, overflowY: 'auto', py: 1 }}>
        {groups.map((group) => (
          <Box key={group.title} sx={{ mb: 1 }}>
            {!collapsed && (
              <Typography
                variant="overline"
                sx={{
                  display: 'block',
                  px: 2.5,
                  pt: 1.5,
                  pb: 0.5,
                  color: 'rgba(230,233,242,0.45)',
                  fontSize: '0.68rem',
                  letterSpacing: '0.08em',
                }}
              >
                {group.title}
              </Typography>
            )}
            <List disablePadding>
              {group.items.map((item) => {
                const selected = location.pathname.startsWith(item.path);
                const label = item.i18nKey ? t(item.i18nKey) : item.label;
                const button = (
                  <ListItemButton
                    key={item.path}
                    selected={selected}
                    onClick={() => {
                      navigate(item.path);
                      onNavigate?.();
                    }}
                    sx={{
                      mx: 1,
                      my: 0.25,
                      borderRadius: 2,
                      color: 'inherit',
                      justifyContent: collapsed ? 'center' : 'flex-start',
                      px: collapsed ? 1.5 : 2,
                      '&.Mui-selected': {
                        bgcolor: 'rgba(255,183,3,0.16)',
                        color: 'secondary.main',
                        '& .MuiListItemIcon-root': { color: 'secondary.main' },
                      },
                      '&:hover': { bgcolor: 'rgba(230,233,242,0.08)' },
                    }}
                  >
                    <ListItemIcon
                      sx={{
                        color: 'rgba(230,233,242,0.75)',
                        minWidth: collapsed ? 0 : 40,
                        justifyContent: 'center',
                      }}
                    >
                      {item.icon}
                    </ListItemIcon>
                    {!collapsed && (
                      <ListItemText
                        primary={label}
                        primaryTypographyProps={{ fontSize: '0.875rem', fontWeight: selected ? 700 : 500 }}
                      />
                    )}
                  </ListItemButton>
                );
                return collapsed ? (
                  <Tooltip key={item.path} title={label} placement="right">
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
    </Box>
  );
}

export interface SidebarProps {
  mobileOpen: boolean;
  collapsed: boolean;
  onClose: () => void;
}

/** Collapsible, role-filtered navigation sidebar for the authenticated app shell. */
export function Sidebar({ mobileOpen, collapsed, onClose }: SidebarProps) {
  const width = collapsed ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH;

  return (
    <Box component="nav" sx={{ width: { md: width }, flexShrink: { md: 0 } }}>
      {/* Mobile temporary drawer */}
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

      {/* Desktop persistent drawer */}
      <Drawer
        variant="permanent"
        open
        sx={{
          display: { xs: 'none', md: 'block' },
          '& .MuiDrawer-paper': {
            width,
            boxSizing: 'border-box',
            transition: 'width 0.2s ease',
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
