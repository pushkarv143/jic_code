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

/** KPI tile used across dashboards: icon + label + big value + optional trend chip. */
export function StatCard({ icon, label, value, trend, color = 'primary' }: StatCardProps) {
  const theme = useTheme();
  const mainColor = theme.palette[color].main;

  const trendGood = trend
    ? (trend.direction === 'up') === (trend.positiveIsGood ?? true)
    : true;

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box
            sx={{
              width: 46,
              height: 46,
              borderRadius: 2.5,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: alpha(mainColor, theme.palette.mode === 'light' ? 0.12 : 0.2),
              color: mainColor,
            }}
          >
            {icon}
          </Box>
          {trend && (
            <Chip
              size="small"
              icon={
                trend.direction === 'up' ? (
                  <TrendingUpIcon fontSize="small" />
                ) : (
                  <TrendingDownIcon fontSize="small" />
                )
              }
              label={trend.value}
              color={trendGood ? 'success' : 'error'}
              variant="outlined"
              sx={{ '& .MuiChip-icon': { fontSize: 16 } }}
            />
          )}
        </Box>
        <Typography variant="h4" sx={{ mt: 2, fontWeight: 700 }}>
          {value}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          {label}
        </Typography>
      </CardContent>
    </Card>
  );
}

export default StatCard;
