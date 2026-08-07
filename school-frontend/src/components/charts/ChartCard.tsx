import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Box from '@mui/material/Box';
import type { ReactNode } from 'react';

export interface ChartCardProps {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  height?: number;
  children: ReactNode;
}

/** Shared card shell (title/subtitle/action) wrapping every Recharts widget. */
export function ChartCard({ title, subtitle, action, height = 300, children }: ChartCardProps) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardHeader title={title} subheader={subtitle} action={action} sx={{ pb: 0 }} />
      <CardContent>
        <Box sx={{ width: '100%', height }}>{children}</Box>
      </CardContent>
    </Card>
  );
}

export default ChartCard;
