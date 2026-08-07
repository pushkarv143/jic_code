import { Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';

/** Styled 403 page shown when a role is not permitted to view a route. */
export function ForbiddenPage() {
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
        <Box
          sx={{
            width: 84,
            height: 84,
            borderRadius: '50%',
            bgcolor: 'error.main',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            mb: 2,
          }}
        >
          <BlockOutlinedIcon sx={{ fontSize: 42 }} />
        </Box>
        <Typography variant="h4" fontWeight={800} gutterBottom>
          Access Denied
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3, maxWidth: 380 }}>
          You do not have permission to view this page. If you believe this is a mistake,
          contact your school administrator.
        </Typography>
        <Button component={RouterLink} to="/app/dashboard" variant="contained" startIcon={<HomeOutlinedIcon />} size="large">
          Back to Dashboard
        </Button>
      </Box>
    </Container>
  );
}

export default ForbiddenPage;
