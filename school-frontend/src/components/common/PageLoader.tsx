import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';

/** Full-page loading state used while route chunks / initial data resolve. */
export function PageLoader({ label = 'Loading...' }: { label?: string }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
        minHeight: '60vh',
        width: '100%',
      }}
    >
      <CircularProgress size={44} thickness={4} />
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Box>
  );
}

/** Skeleton placeholder for card-shaped content while data loads. */
export function CardSkeleton({ height = 120 }: { height?: number }) {
  return (
    <Stack spacing={1}>
      <Skeleton variant="rounded" height={height} />
      <Skeleton variant="text" width="60%" />
      <Skeleton variant="text" width="40%" />
    </Stack>
  );
}

/** Small inline spinner for buttons / lightweight areas. */
export function LoadingSpinner({ size = 24 }: { size?: number }) {
  return <CircularProgress size={size} thickness={4} />;
}

export default PageLoader;
