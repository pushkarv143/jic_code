import Box from '@mui/material/Box';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';
import SchoolIcon from '@mui/icons-material/School';

export function PageLoader({ label = 'Loading...' }: { label?: string }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2.5,
        minHeight: '60vh',
        width: '100%',
      }}
    >
      <Box
        sx={{
          width: 52, height: 52, borderRadius: 3,
          background: 'linear-gradient(135deg, #2b3a8f, #3f51b5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 24px rgba(43,58,143,0.3)',
        }}
      >
        <SchoolIcon sx={{ fontSize: 28, color: '#fff' }} />
      </Box>
      <Box sx={{ width: 200, textAlign: 'center' }}>
        <LinearProgress
          variant="indeterminate"
          sx={{
            height: 4, borderRadius: 100, mb: 1.5,
            '& .MuiLinearProgress-bar': {
              background: 'linear-gradient(90deg, #2b3a8f, #ffb703)',
            },
          }}
        />
        <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500, fontSize: '0.8rem' }}>
          {label}
        </Typography>
      </Box>
    </Box>
  );
}

export function CardSkeleton({ height = 120 }: { height?: number }) {
  return (
    <Stack spacing={1.5}>
      <Skeleton variant="rounded" height={height} animation="wave" />
      <Skeleton variant="text" width="60%" animation="wave" />
      <Skeleton variant="text" width="40%" animation="wave" />
    </Stack>
  );
}

export function LoadingSpinner({ size = 24 }: { size?: number }) {
  return (
    <Box sx={{
      width: size, height: size, borderRadius: '50%',
      border: '2px solid',
      borderColor: 'primary.light',
      borderTopColor: 'primary.main',
      animation: 'spin 0.7s linear infinite',
      '@keyframes spin': { to: { transform: 'rotate(360deg)' } },
    }} />
  );
}

export default PageLoader;
