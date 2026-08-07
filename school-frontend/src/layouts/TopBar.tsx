import { useEffect, useRef, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Box from '@mui/material/Box';
import InputBase from '@mui/material/InputBase';
import Badge from '@mui/material/Badge';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Button from '@mui/material/Button';
import Tooltip from '@mui/material/Tooltip';
import Popper from '@mui/material/Popper';
import Paper from '@mui/material/Paper';
import ClickAwayListener from '@mui/material/ClickAwayListener';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListSubheader from '@mui/material/ListSubheader';
import CircularProgress from '@mui/material/CircularProgress';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import ToggleButton from '@mui/material/ToggleButton';
import MenuIcon from '@mui/icons-material/Menu';
import MenuOpenIcon from '@mui/icons-material/MenuOpen';
import SearchIcon from '@mui/icons-material/Search';
import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import TranslateOutlinedIcon from '@mui/icons-material/TranslateOutlined';
import CheckIcon from '@mui/icons-material/Check';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import { alpha } from '@mui/material/styles';
import { useThemeMode } from '@/theme/ThemeModeProvider';
import { ACCENT_COLOR_SWATCHES } from '@/theme/theme';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { markAllAsRead } from '@/store/notificationSlice';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import searchApi from '@/api/searchApi';
import type { GlobalSearchResponse, GlobalSearchResultItem } from '@/types';
import { useLanguage } from '@/i18n/LanguageProvider';
import { LANGUAGE_LABELS, type Language } from '@/i18n/translations';
import ProfileMenu from './ProfileMenu';
import dayjs from 'dayjs';

export interface TopBarProps {
  onMenuClick: () => void;
  onCollapseClick: () => void;
  collapsed: boolean;
}

function resultLabel(item: GlobalSearchResultItem): string {
  return item.displayName ?? item.name ?? item.title ?? `#${item.id}`;
}

function resultSubtitle(item: GlobalSearchResultItem): string | null {
  return item.subtitle ?? item.admissionNumber ?? item.employeeId ?? item.isbn ?? null;
}

/** Top application bar: menu toggle, wired global search, theme customizer, language, notifications, profile. */
export function TopBar({ onMenuClick, onCollapseClick, collapsed }: TopBarProps) {
  const { mode, toggleMode, accentColor, setAccentColor, density, setDensity } = useThemeMode();
  const { language, setLanguage } = useLanguage();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const notifications = useAppSelector((state) => state.notifications.items);
  const unreadCount = notifications.filter((n) => !n.read).length;

  const [notifAnchor, setNotifAnchor] = useState<null | HTMLElement>(null);
  const [themeAnchor, setThemeAnchor] = useState<null | HTMLElement>(null);
  const [langAnchor, setLangAnchor] = useState<null | HTMLElement>(null);

  const searchBoxRef = useRef<HTMLDivElement>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchResults, setSearchResults] = useState<GlobalSearchResponse | null>(null);
  const debouncedQuery = useDebouncedValue(searchQuery, 350);

  useEffect(() => {
    const query = debouncedQuery.trim();
    if (!query) {
      setSearchResults(null);
      setSearchLoading(false);
      return;
    }
    let cancelled = false;
    setSearchLoading(true);
    searchApi
      .global(query)
      .then((res) => {
        if (!cancelled) setSearchResults(res.data);
      })
      .catch(() => {
        if (!cancelled) setSearchResults({ students: [], teachers: [], books: [] });
      })
      .finally(() => {
        if (!cancelled) setSearchLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery]);

  const closeSearch = () => setSearchOpen(false);

  const handleSelectResult = (path: string) => {
    setSearchQuery('');
    setSearchResults(null);
    closeSearch();
    navigate(path);
  };

  const hasResults =
    !!searchResults &&
    (searchResults.students.length > 0 || searchResults.teachers.length > 0 || searchResults.books.length > 0);

  return (
    <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
      <Toolbar sx={{ gap: 1.5 }}>
        <IconButton onClick={onMenuClick} sx={{ display: { xs: 'inline-flex', md: 'none' } }} edge="start">
          <MenuIcon />
        </IconButton>
        <IconButton onClick={onCollapseClick} sx={{ display: { xs: 'none', md: 'inline-flex' } }} edge="start">
          {collapsed ? <MenuIcon /> : <MenuOpenIcon />}
        </IconButton>

        <Box
          ref={searchBoxRef}
          sx={{
            display: { xs: 'none', sm: 'flex' },
            alignItems: 'center',
            gap: 1,
            bgcolor: (theme) => alpha(theme.palette.text.primary, 0.05),
            borderRadius: 2,
            px: 1.5,
            py: 0.5,
            flexGrow: 1,
            maxWidth: 420,
            position: 'relative',
          }}
        >
          <SearchIcon fontSize="small" sx={{ color: 'text.secondary' }} />
          <InputBase
            placeholder="Search students, teachers, books..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onFocus={() => setSearchOpen(true)}
            sx={{ fontSize: '0.875rem', flexGrow: 1 }}
            inputProps={{ 'aria-label': 'search' }}
          />
        </Box>

        <Popper
          open={searchOpen && searchQuery.trim().length > 0}
          anchorEl={searchBoxRef.current}
          placement="bottom-start"
          style={{ zIndex: 1301, width: searchBoxRef.current?.clientWidth ?? 360 }}
        >
          <ClickAwayListener onClickAway={closeSearch}>
            <Paper elevation={6} sx={{ mt: 0.5, maxHeight: 420, overflowY: 'auto' }}>
              {searchLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                  <CircularProgress size={24} />
                </Box>
              ) : !hasResults ? (
                <Box sx={{ px: 2, py: 3, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">
                    No results found.
                  </Typography>
                </Box>
              ) : (
                <List dense disablePadding>
                  {searchResults!.students.length > 0 && (
                    <li>
                      <ul style={{ padding: 0 }}>
                        <ListSubheader disableSticky>Students</ListSubheader>
                        {searchResults!.students.map((item) => (
                          <ListItemButton key={`student-${item.id}`} onClick={() => handleSelectResult(`/app/students/${item.id}`)}>
                            <ListItemIcon sx={{ minWidth: 36 }}>
                              <SchoolOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText primary={resultLabel(item)} secondary={resultSubtitle(item)} />
                          </ListItemButton>
                        ))}
                      </ul>
                    </li>
                  )}
                  {searchResults!.teachers.length > 0 && (
                    <li>
                      <ul style={{ padding: 0 }}>
                        <ListSubheader disableSticky>Teachers</ListSubheader>
                        {searchResults!.teachers.map((item) => (
                          <ListItemButton key={`teacher-${item.id}`} onClick={() => handleSelectResult(`/app/teachers/${item.id}`)}>
                            <ListItemIcon sx={{ minWidth: 36 }}>
                              <BadgeOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText primary={resultLabel(item)} secondary={resultSubtitle(item)} />
                          </ListItemButton>
                        ))}
                      </ul>
                    </li>
                  )}
                  {searchResults!.books.length > 0 && (
                    <li>
                      <ul style={{ padding: 0 }}>
                        <ListSubheader disableSticky>Books</ListSubheader>
                        {searchResults!.books.map((item) => (
                          <ListItemButton key={`book-${item.id}`} onClick={() => handleSelectResult('/app/library/books')}>
                            <ListItemIcon sx={{ minWidth: 36 }}>
                              <MenuBookOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText primary={resultLabel(item)} secondary={resultSubtitle(item)} />
                          </ListItemButton>
                        ))}
                      </ul>
                    </li>
                  )}
                </List>
              )}
            </Paper>
          </ClickAwayListener>
        </Popper>

        <Box sx={{ flexGrow: 1, display: { xs: 'block', sm: 'none' } }} />

        <Tooltip title="Language">
          <IconButton onClick={(e) => setLangAnchor(e.currentTarget)}>
            <TranslateOutlinedIcon />
          </IconButton>
        </Tooltip>
        <Menu anchorEl={langAnchor} open={Boolean(langAnchor)} onClose={() => setLangAnchor(null)}>
          {(Object.keys(LANGUAGE_LABELS) as Language[]).map((code) => (
            <MenuItem
              key={code}
              selected={language === code}
              onClick={() => {
                setLanguage(code);
                setLangAnchor(null);
              }}
            >
              <ListItemIcon sx={{ minWidth: 28 }}>{language === code ? <CheckIcon fontSize="small" /> : null}</ListItemIcon>
              <ListItemText>{LANGUAGE_LABELS[code]}</ListItemText>
            </MenuItem>
          ))}
        </Menu>

        <Tooltip title="Theme customizer">
          <IconButton onClick={(e) => setThemeAnchor(e.currentTarget)}>
            <PaletteOutlinedIcon />
          </IconButton>
        </Tooltip>
        <Menu
          anchorEl={themeAnchor}
          open={Boolean(themeAnchor)}
          onClose={() => setThemeAnchor(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          transformOrigin={{ vertical: 'top', horizontal: 'right' }}
          PaperProps={{ sx: { width: 280, p: 2 } }}
        >
          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            Accent Color
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
            {ACCENT_COLOR_SWATCHES.map((swatch) => {
              const isDefault = !accentColor && swatch.label === 'Indigo';
              const selected = accentColor === swatch.value || isDefault;
              return (
                <Tooltip key={swatch.value} title={swatch.label}>
                  <Box
                    onClick={() => setAccentColor(swatch.label === 'Indigo' ? undefined : swatch.value)}
                    sx={{
                      width: 28,
                      height: 28,
                      borderRadius: '50%',
                      bgcolor: swatch.value,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: selected ? '2px solid' : '2px solid transparent',
                      borderColor: selected ? 'text.primary' : 'transparent',
                      boxShadow: selected ? 2 : 0,
                    }}
                  >
                    {selected && <CheckIcon sx={{ fontSize: 16, color: '#fff' }} />}
                  </Box>
                </Tooltip>
              );
            })}
          </Box>

          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            Density
          </Typography>
          <ToggleButtonGroup
            exclusive
            fullWidth
            size="small"
            value={density}
            onChange={(_e, value) => value && setDensity(value)}
          >
            <ToggleButton value="comfortable">Comfortable</ToggleButton>
            <ToggleButton value="compact">Compact</ToggleButton>
          </ToggleButtonGroup>
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1.5, display: 'block' }}>
            Your preference is saved to this browser and applied automatically next time.
          </Typography>
        </Menu>

        <Tooltip title={mode === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}>
          <IconButton onClick={toggleMode}>
            {mode === 'light' ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
          </IconButton>
        </Tooltip>

        <Tooltip title="Notifications">
          <IconButton onClick={(e) => setNotifAnchor(e.currentTarget)}>
            <Badge badgeContent={unreadCount} color="error">
              <NotificationsNoneOutlinedIcon />
            </Badge>
          </IconButton>
        </Tooltip>
        <Menu
          anchorEl={notifAnchor}
          open={Boolean(notifAnchor)}
          onClose={() => setNotifAnchor(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          transformOrigin={{ vertical: 'top', horizontal: 'right' }}
          PaperProps={{ sx: { width: 340, maxHeight: 420 } }}
        >
          <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="subtitle2" fontWeight={700}>
              Notifications
            </Typography>
            <Button size="small" onClick={() => dispatch(markAllAsRead())}>
              Mark all read
            </Button>
          </Box>
          <Divider />
          {notifications.length === 0 && (
            <Box sx={{ px: 2, py: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                You&apos;re all caught up.
              </Typography>
            </Box>
          )}
          {notifications.map((n) => (
            <Box
              key={n.id}
              sx={{
                px: 2,
                py: 1.25,
                borderLeft: '3px solid',
                borderLeftColor: n.read ? 'transparent' : 'primary.main',
                bgcolor: n.read ? 'transparent' : (theme) => alpha(theme.palette.primary.main, 0.06),
              }}
            >
              <Typography variant="body2" fontWeight={600}>
                {n.title}
              </Typography>
              <Typography variant="caption" color="text.secondary" display="block">
                {n.message}
              </Typography>
              <Typography variant="caption" color="text.disabled">
                {dayjs(n.createdAt).format('DD MMM, hh:mm A')}
              </Typography>
            </Box>
          ))}
          <Divider />
          <Box sx={{ px: 2, py: 1 }}>
            <Button size="small" component={RouterLink} to="/app/dashboard" fullWidth>
              View all
            </Button>
          </Box>
        </Menu>

        <ProfileMenu />
      </Toolbar>
    </AppBar>
  );
}

export default TopBar;
