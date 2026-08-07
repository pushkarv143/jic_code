import { Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import SchoolIcon from '@mui/icons-material/School';

/** Styled 404 page shown for any unmatched route. */
export function NotFoundPage() {
  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          gap: 1,
        }}
      >
        <SchoolIcon color="primary" sx={{ fontSize: 48, mb: 1 }} />
        <Typography variant="h1" sx={{ fontSize: '5rem', fontWeight: 800, color: 'primary.main' }}>
          404
        </Typography>
        <Typography variant="h5" fontWeight={700} gutterBottom>
          Page not found
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3, maxWidth: 380 }}>
          The page you are looking for might have been moved, renamed, or doesn&apos;t exist.
        </Typography>
        <Button component={RouterLink} to="/" variant="contained" startIcon={<HomeOutlinedIcon />} size="large">
          Back to Home
        </Button>
      </Box>
    </Container>
  );
}

export default NotFoundPage;
