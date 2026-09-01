import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import type { ReactNode } from 'react';
import { alpha, useTheme } from '@mui/material/styles';

export interface StatCardProps {
  icon: ReactNode;
  label: string;
  value: string | number;
  trend?: {
    value: string;
    direction: 'up' | 'down';
    positiveIsGood?: boolean;
  };
  color?: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info';
}

export function StatCard({ icon, label, value, trend, color = 'primary' }: StatCardProps) {
  const theme = useTheme();
  const mainColor = theme.palette[color].main;
  const isLight = theme.palette.mode === 'light';

  const trendGood = trend
    ? (trend.direction === 'up') === (trend.positiveIsGood ?? true)
    : true;

  return (
    <Card
      sx={{
        height: '100%',
        position: 'relative',
        overflow: 'hidden',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0, left: 0, right: 0,
          height: 3,
          background: `linear-gradient(90deg, ${mainColor}, ${alpha(mainColor, 0.4)})`,
          borderRadius: '16px 16px 0 0',
        },
      }}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          {/* Icon container with gradient */}
          <Box sx={{
            width: 48, height: 48, borderRadius: 2.5,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            background: `linear-gradient(135deg, ${alpha(mainColor, isLight ? 0.15 : 0.22)}, ${alpha(mainColor, isLight ? 0.06 : 0.1)})`,
            color: mainColor,
            border: `1px solid ${alpha(mainColor, 0.15)}`,
            transition: 'transform 0.2s ease',
            '&:hover': { transform: 'scale(1.05)' },
          }}>
            {icon}
          </Box>

          {trend && (
            <Chip
              size="small"
              icon={
                trend.direction === 'up'
                  ? <TrendingUpIcon sx={{ fontSize: '14px !important' }} />
                  : <TrendingDownIcon sx={{ fontSize: '14px !important' }} />
              }
              label={trend.value}
              color={trendGood ? 'success' : 'error'}
              variant="outlined"
              sx={{ height: 24, '& .MuiChip-label': { px: 0.75, fontSize: '0.72rem', fontWeight: 700 } }}
            />
          )}
        </Box>

        <Typography
          variant="h4"
          sx={{
            mt: 2.5,
            fontWeight: 800,
            letterSpacing: '-0.03em',
            lineHeight: 1.1,
            fontSize: { xs: '1.5rem', xl: '1.75rem' },
          }}
        >
          {value}
        </Typography>
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ mt: 0.75, fontWeight: 500, fontSize: '0.8rem' }}
        >
          {label}
        </Typography>
      </CardContent>
    </Card>
  );
}

export default StatCard;
