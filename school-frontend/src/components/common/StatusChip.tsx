import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { alpha, useTheme } from '@mui/material/styles';

const COLOR_KEYS: Record<string, 'success' | 'error' | 'warning' | 'info' | 'default'> = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  RESIGNED: 'warning',
  TERMINATED: 'error',
  ALUMNI: 'info',
  TRANSFERRED: 'warning',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  PRESENT: 'success',
  ABSENT: 'error',
  LATE: 'warning',
  HALF_DAY: 'info',
  LEAVE: 'info',
  PAID: 'success',
  UNPAID: 'warning',
  PARTIAL: 'info',
  OVERDUE: 'error',
  ISSUED: 'info',
  RETURNED: 'success',
  SUBMITTED: 'warning',
  GRADED: 'success',
  FULL_TIME: 'success',
  PART_TIME: 'info',
  CONTRACT: 'warning',
};

export interface StatusChipProps {
  status: string;
  size?: 'small' | 'medium';
}

export function StatusChip({ status, size = 'small' }: StatusChipProps) {
  const theme = useTheme();
  const key = COLOR_KEYS[status] ?? 'default';

  const colorMap = {
    success: theme.palette.success.main,
    error: theme.palette.error.main,
    warning: theme.palette.warning.main,
    info: theme.palette.info.main,
    default: theme.palette.text.secondary,
  };
  const color = colorMap[key];

  const label = status
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');

  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        px: size === 'small' ? 1.25 : 1.5,
        py: size === 'small' ? 0.25 : 0.5,
        borderRadius: 50,
        bgcolor: alpha(color, 0.1),
        border: `1px solid ${alpha(color, 0.25)}`,
      }}
    >
      <Typography
        component="span"
        sx={{
          fontSize: size === 'small' ? '0.72rem' : '0.8rem',
          fontWeight: 700,
          color,
          letterSpacing: '0.01em',
          lineHeight: 1,
        }}
      >
        {label}
      </Typography>
    </Box>
  );
}

export default StatusChip;
