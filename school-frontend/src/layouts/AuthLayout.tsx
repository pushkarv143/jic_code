import { Outlet, Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import SchoolIcon from '@mui/icons-material/School';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import BarChartOutlinedIcon from '@mui/icons-material/BarChartOutlined';
import IconButton from '@mui/material/IconButton';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import { useThemeMode } from '@/theme/ThemeModeProvider';

const HIGHLIGHTS = [
  { icon: <EventAvailableOutlinedIcon />, text: 'Real-time attendance tracking for every class' },
  { icon: <PaidOutlinedIcon />, text: 'Transparent fee collection and receipts' },
  { icon: <MenuBookOutlinedIcon />, text: 'Digital library and exam management' },
  { icon: <BarChartOutlinedIcon />, text: 'Actionable dashboards for every role' },
];

/** Split-screen centered layout for login / register / forgot / reset password pages. */
export function AuthLayout() {
  const { mode, toggleMode } = useThemeMode();

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex' }}>
      <Box
        sx={{
          flex: 1,
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'space-between',
          p: 6,
          color: '#fff',
          background: 'linear-gradient(150deg, #1c2763 0%, #2b3a8f 55%, #3a4bb0 100%)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <Box
          sx={{
            position: 'absolute',
            width: 420,
            height: 420,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(255,183,3,0.18) 0%, rgba(255,183,3,0) 70%)',
            top: -120,
            right: -120,
          }}
        />
        <Stack direction="row" spacing={1.5} alignItems="center">
          <SchoolIcon sx={{ fontSize: 36, color: 'secondary.main' }} />
          <Box>
            <Typography variant="h6" fontWeight={800}>
              Greenwood International School
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.75 }}>
              School Management System
            </Typography>
          </Box>
        </Stack>

        <Box sx={{ zIndex: 1 }}>
          <Typography variant="h3" fontWeight={800} sx={{ mb: 2, maxWidth: 460 }}>
            One platform for the entire school community.
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.85, maxWidth: 420, mb: 4 }}>
            Administrators, teachers, students and parents — all connected through a single,
            secure system.
          </Typography>
          <Stack spacing={2}>
            {HIGHLIGHTS.map((h, idx) => (
              <Stack key={idx} direction="row" spacing={1.5} alignItems="center">
                <Box
                  sx={{
                    width: 36,
                    height: 36,
                    borderRadius: 2,
                    bgcolor: 'rgba(255,255,255,0.12)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {h.icon}
                </Box>
                <Typography variant="body2">{h.text}</Typography>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Typography variant="caption" sx={{ opacity: 0.6 }}>
          © {new Date().getFullYear()} Greenwood International School
        </Typography>
      </Box>

      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 2, sm: 4 },
          position: 'relative',
        }}
      >
        <IconButton onClick={toggleMode} sx={{ position: 'absolute', top: 16, right: 16 }}>
          {mode === 'light' ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
        </IconButton>

        <Box
          component={RouterLink}
          to="/"
          sx={{
            display: { xs: 'flex', md: 'none' },
            alignItems: 'center',
            gap: 1,
            mb: 3,
            textDecoration: 'none',
            color: 'inherit',
          }}
        >
          <SchoolIcon color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h6" fontWeight={800}>
            Greenwood
          </Typography>
        </Box>

        <Paper
          elevation={0}
          sx={{
            width: '100%',
            maxWidth: 440,
            p: { xs: 3, sm: 5 },
            border: '1px solid',
            borderColor: 'divider',
          }}
        >
          <Outlet />
        </Paper>
      </Box>
    </Box>
  );
}

export default AuthLayout;
