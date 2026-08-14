import { useRouteError, isRouteErrorResponse, Link as RouterLink } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Paper from '@mui/material/Paper';
import ReportProblemOutlinedIcon from '@mui/icons-material/ReportProblemOutlined';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import AppFooter from '@/components/common/AppFooter';

/** Generic error boundary page used as the router's errorElement. */
export function ErrorPage() {
  const error = useRouteError();

  let title = 'Something went wrong';
  let detail = 'An unexpected error occurred while loading this page.';

  if (isRouteErrorResponse(error)) {
    title = `Error ${error.status}`;
    detail = error.statusText || detail;
  } else if (error instanceof Error) {
    detail = error.message;
  }

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
          gap: 2,
        }}
      >
        <ReportProblemOutlinedIcon color="warning" sx={{ fontSize: 56 }} />
        <Typography variant="h4" fontWeight={800}>
          {title}
        </Typography>
        <Paper variant="outlined" sx={{ p: 2, width: '100%' }}>
          <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-word' }}>
            {detail}
          </Typography>
        </Paper>
        <Button component={RouterLink} to="/" variant="contained" startIcon={<HomeOutlinedIcon />}>
          Back to Home
        </Button>
      </Box>
      {/* Rendered outside any layout — see ForbiddenPage. */}
      <AppFooter variant="bare" />
    </Container>
  );
}

export default ErrorPage;
