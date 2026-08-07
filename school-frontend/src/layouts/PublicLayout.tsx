import { useState } from 'react';
import { Outlet, Link as RouterLink, useLocation } from 'react-router-dom';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import MenuIcon from '@mui/icons-material/Menu';
import SchoolIcon from '@mui/icons-material/School';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import FacebookIcon from '@mui/icons-material/Facebook';
import TwitterIcon from '@mui/icons-material/Twitter';
import InstagramIcon from '@mui/icons-material/Instagram';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import PhoneOutlinedIcon from '@mui/icons-material/PhoneOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import { useThemeMode } from '@/theme/ThemeModeProvider';

const NAV_LINKS = [
  { label: 'Home', path: '/' },
  { label: 'About', path: '/about' },
  { label: 'Academics', path: '/academics' },
  { label: 'Faculty', path: '/faculty' },
  { label: 'Facilities', path: '/facilities' },
  { label: 'Gallery', path: '/gallery' },
  { label: 'Admission', path: '/admission' },
  { label: 'Contact', path: '/contact' },
];

/** Marketing site chrome: top nav with logo, nav links, theme toggle, login CTA, and footer. */
export function PublicLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const { mode, toggleMode } = useThemeMode();
  const location = useLocation();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <AppBar position="sticky" color="inherit" elevation={0} sx={{ borderBottom: '1px solid', borderColor: 'divider' }}>
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ py: 1 }}>
            <Box
              component={RouterLink}
              to="/"
              sx={{ display: 'flex', alignItems: 'center', gap: 1.25, textDecoration: 'none', color: 'inherit', flexGrow: 1 }}
            >
              <SchoolIcon color="primary" sx={{ fontSize: 34 }} />
              <Box>
                <Typography variant="subtitle1" fontWeight={800} lineHeight={1.1}>
                  Greenwood
                </Typography>
                <Typography variant="caption" color="text.secondary" lineHeight={1}>
                  International School
                </Typography>
              </Box>
            </Box>

            <Stack direction="row" spacing={0.5} sx={{ display: { xs: 'none', md: 'flex' } }}>
              {NAV_LINKS.map((link) => (
                <Button
                  key={link.path}
                  component={RouterLink}
                  to={link.path}
                  color={location.pathname === link.path ? 'primary' : 'inherit'}
                  sx={{ fontWeight: location.pathname === link.path ? 700 : 500 }}
                >
                  {link.label}
                </Button>
              ))}
            </Stack>

            <IconButton onClick={toggleMode} sx={{ ml: 1 }}>
              {mode === 'light' ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
            </IconButton>

            <Button
              component={RouterLink}
              to="/login"
              variant="contained"
              sx={{ ml: 1, display: { xs: 'none', sm: 'inline-flex' } }}
            >
              Login
            </Button>

            <IconButton sx={{ display: { xs: 'inline-flex', md: 'none' }, ml: 1 }} onClick={() => setDrawerOpen(true)}>
              <MenuIcon />
            </IconButton>
          </Toolbar>
        </Container>
      </AppBar>

      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}>
        <Box sx={{ width: 260 }} role="presentation" onClick={() => setDrawerOpen(false)}>
          <List>
            {NAV_LINKS.map((link) => (
              <ListItemButton key={link.path} component={RouterLink} to={link.path}>
                <ListItemText primary={link.label} />
              </ListItemButton>
            ))}
          </List>
          <Divider />
          <Box sx={{ p: 2 }}>
            <Button component={RouterLink} to="/login" variant="contained" fullWidth>
              Login
            </Button>
          </Box>
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1 }}>
        <Outlet />
      </Box>

      <Box component="footer" sx={{ bgcolor: 'sidebar.background', color: 'sidebar.color', mt: 6 }}>
        <Container maxWidth="lg" sx={{ py: 6 }}>
          <Grid container spacing={4}>
            <Grid item xs={12} sm={4}>
              <Stack direction="row" spacing={1.25} alignItems="center" sx={{ mb: 1.5 }}>
                <SchoolIcon sx={{ color: 'secondary.main' }} />
                <Typography variant="h6" fontWeight={700}>
                  Greenwood International
                </Typography>
              </Stack>
              <Typography variant="body2" color="rgba(230,233,242,0.75)">
                Nurturing curious minds and confident character since 1998. Committed to
                academic excellence and holistic growth.
              </Typography>
              <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
                {[FacebookIcon, TwitterIcon, InstagramIcon, LinkedInIcon].map((Icon, idx) => (
                  <IconButton key={idx} size="small" sx={{ color: 'rgba(230,233,242,0.8)', border: '1px solid rgba(230,233,242,0.2)' }}>
                    <Icon fontSize="small" />
                  </IconButton>
                ))}
              </Stack>
            </Grid>
            <Grid item xs={6} sm={4} md={3}>
              <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                Quick Links
              </Typography>
              <Stack spacing={1}>
                {NAV_LINKS.slice(1).map((link) => (
                  <Link
                    key={link.path}
                    component={RouterLink}
                    to={link.path}
                    color="rgba(230,233,242,0.75)"
                    underline="hover"
                    variant="body2"
                  >
                    {link.label}
                  </Link>
                ))}
              </Stack>
            </Grid>
            <Grid item xs={6} sm={4} md={5}>
              <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                Contact Us
              </Typography>
              <Stack spacing={1.25}>
                <Stack direction="row" spacing={1} alignItems="flex-start">
                  <LocationOnOutlinedIcon fontSize="small" sx={{ color: 'secondary.main', mt: 0.25 }} />
                  <Typography variant="body2" color="rgba(230,233,242,0.75)">
                    142 Lakeview Avenue, Sector 21, Greenwood City, 500081
                  </Typography>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                  <PhoneOutlinedIcon fontSize="small" sx={{ color: 'secondary.main' }} />
                  <Typography variant="body2" color="rgba(230,233,242,0.75)">
                    +91 98765 43210
                  </Typography>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                  <EmailOutlinedIcon fontSize="small" sx={{ color: 'secondary.main' }} />
                  <Typography variant="body2" color="rgba(230,233,242,0.75)">
                    info@greenwoodschool.edu
                  </Typography>
                </Stack>
              </Stack>
            </Grid>
          </Grid>
          <Divider sx={{ my: 3, borderColor: 'rgba(230,233,242,0.15)' }} />
          <Typography variant="caption" color="rgba(230,233,242,0.55)">
            © {new Date().getFullYear()} Greenwood International School. All rights reserved.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}

export default PublicLayout;
