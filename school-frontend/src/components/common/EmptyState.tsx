import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined';
import { alpha, useTheme } from '@mui/material/styles';
import type { ReactNode } from 'react';

export interface EmptyStateProps {
  icon?: ReactNode;
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({
  icon,
  title = 'Nothing here yet',
  description = 'There is no data to show right now.',
  actionLabel,
  onAction,
}: EmptyStateProps) {
  const theme = useTheme();
  const primaryColor = theme.palette.primary.main;

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        py: 7,
        px: 3,
      }}
    >
      <Box
        sx={{
          width: 72,
          height: 72,
          borderRadius: 4,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: `linear-gradient(135deg, ${alpha(primaryColor, 0.15)}, ${alpha(primaryColor, 0.05)})`,
          border: `1px solid ${alpha(primaryColor, 0.12)}`,
          mb: 2.5,
          color: 'primary.main',
          '& .MuiSvgIcon-root': { fontSize: 32 },
        }}
      >
        {icon ?? <InboxOutlinedIcon />}
      </Box>
      <Typography variant="h6" fontWeight={700} color="text.primary" gutterBottom>
        {title}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 380, lineHeight: 1.65, mb: actionLabel ? 3 : 0 }}>
        {description}
      </Typography>
      {actionLabel && onAction && (
        <Button variant="contained" onClick={onAction} sx={{ mt: 1 }}>
          {actionLabel}
        </Button>
      )}
    </Box>
  );
}

export default EmptyState;
