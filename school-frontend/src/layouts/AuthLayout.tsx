import { Outlet, Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import IconButton from '@mui/material/IconButton';
import Chip from '@mui/material/Chip';
import SchoolIcon from '@mui/icons-material/School';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import BarChartOutlinedIcon from '@mui/icons-material/BarChartOutlined';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import { useThemeMode } from '@/theme/ThemeModeProvider';
import AppFooter from '@/components/common/AppFooter';

const HIGHLIGHTS = [
  { icon: <EventAvailableOutlinedIcon sx={{ fontSize: 18 }} />, text: 'Real-time attendance tracking for every class' },
  { icon: <PaidOutlinedIcon sx={{ fontSize: 18 }} />, text: 'Transparent fee collection and instant receipts' },
  { icon: <MenuBookOutlinedIcon sx={{ fontSize: 18 }} />, text: 'Digital library, exams and study materials' },
  { icon: <BarChartOutlinedIcon sx={{ fontSize: 18 }} />, text: 'Actionable dashboards for every role' },
];

export function AuthLayout() {
  const { mode, toggleMode } = useThemeMode();

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex' }}>
      {/* ---- Left brand panel ---- */}
      <Box
        sx={{
          flex: 1,
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'space-between',
          p: 6,
          color: '#fff',
          background: 'linear-gradient(145deg, #0f1d5e 0%, #1c2763 35%, #2b3a8f 70%, #3f51b5 100%)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Decorative blobs */}
        <Box className="blob-float" sx={{
          position: 'absolute', width: 500, height: 500, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(255,183,3,0.12) 0%, rgba(255,183,3,0) 65%)',
          top: -180, right: -180, pointerEvents: 'none',
        }} />
        <Box className="blob-float-delayed" sx={{
          position: 'absolute', width: 350, height: 350, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(85,99,184,0.35) 0%, rgba(85,99,184,0) 70%)',
          bottom: -100, left: -100, pointerEvents: 'none',
        }} />
        <Box sx={{
          position: 'absolute', inset: 0, pointerEvents: 'none',
          backgroundImage: 'radial-gradient(rgba(255,255,255,0.04) 1px, transparent 1px)',
          backgroundSize: '28px 28px',
        }} />

        {/* Logo */}
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ position: 'relative', zIndex: 1 }}>
          <Box sx={{
            width: 44, height: 44, borderRadius: 2.5,
            background: 'rgba(255,255,255,0.12)',
            backdropFilter: 'blur(8px)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            border: '1px solid rgba(255,255,255,0.18)',
          }}>
            <SchoolIcon sx={{ fontSize: 26, color: '#ffb703' }} />
          </Box>
          <Box>
            <Typography variant="h6" fontWeight={800} sx={{ lineHeight: 1.1 }}>
              Greenwood International
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.65, letterSpacing: '0.04em' }}>
              School Management System
            </Typography>
          </Box>
        </Stack>

        {/* Hero text */}
        <Box sx={{ position: 'relative', zIndex: 1 }}>
          <Chip
            icon={<AutoAwesomeIcon sx={{ fontSize: '14px !important' }} />}
            label="All-in-one platform"
            size="small"
            sx={{
              mb: 2.5,
              bgcolor: 'rgba(255,183,3,0.15)',
              color: '#ffcb47',
              border: '1px solid rgba(255,183,3,0.3)',
              fontWeight: 600,
              '& .MuiChip-icon': { color: '#ffcb47' },
            }}
          />
          <Typography
            variant="h3"
            fontWeight={800}
            sx={{ mb: 2, maxWidth: 440, lineHeight: 1.15, letterSpacing: '-0.03em' }}
          >
            One platform for the entire school community.
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.8, maxWidth: 400, mb: 4, lineHeight: 1.65 }}>
            Administrators, teachers, students and parents — all connected through a single, secure system.
          </Typography>
          <Stack spacing={1.5}>
            {HIGHLIGHTS.map((h, idx) => (
              <Stack key={idx} direction="row" spacing={1.5} alignItems="center">
                <Box sx={{
                  width: 32, height: 32, borderRadius: 2,
                  background: 'rgba(255,255,255,0.1)',
                  backdropFilter: 'blur(4px)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  border: '1px solid rgba(255,255,255,0.12)',
                  flexShrink: 0,
                }}>
                  {h.icon}
                </Box>
                <Typography variant="body2" sx={{ opacity: 0.88, fontWeight: 500 }}>
                  {h.text}
                </Typography>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Typography variant="caption" sx={{ opacity: 0.45, position: 'relative', zIndex: 1 }}>
          © {new Date().getFullYear()} Greenwood International School · All rights reserved
        </Typography>
      </Box>

      {/* ---- Right form panel ---- */}
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 2, sm: 4 },
          position: 'relative',
          bgcolor: 'background.default',
        }}
      >
        <IconButton
          onClick={toggleMode}
          sx={{ position: 'absolute', top: 16, right: 16 }}
          aria-label="toggle dark mode"
        >
          {mode === 'light' ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
        </IconButton>

        {/* Mobile logo */}
        <Box
          component={RouterLink}
          to="/"
          sx={{
            display: { xs: 'flex', md: 'none' },
            alignItems: 'center',
            gap: 1.5,
            mb: 4,
            textDecoration: 'none',
            color: 'inherit',
          }}
        >
          <SchoolIcon color="primary" sx={{ fontSize: 34 }} />
          <Box>
            <Typography variant="subtitle1" fontWeight={800} sx={{ lineHeight: 1.1 }}>
              Greenwood
            </Typography>
            <Typography variant="caption" color="text.secondary">
              School Management
            </Typography>
          </Box>
        </Box>

        <Paper
          elevation={0}
          sx={{
            width: '100%',
            maxWidth: 460,
            p: { xs: 3.5, sm: 5 },
            borderRadius: 4,
            border: '1px solid',
            borderColor: 'divider',
            boxShadow: (theme) =>
              theme.palette.mode === 'light'
                ? '0 4px 24px rgba(23,29,58,0.08), 0 1px 4px rgba(23,29,58,0.05)'
                : '0 4px 24px rgba(0,0,0,0.35)',
          }}
        >
          <Outlet />
        </Paper>

        <AppFooter variant="bare" />
      </Box>
    </Box>
  );
}

export default AuthLayout;
