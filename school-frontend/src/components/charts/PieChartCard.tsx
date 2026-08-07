import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip, Legend } from 'recharts';
import { useTheme } from '@mui/material/styles';
import ChartCard from './ChartCard';
import { getCategoricalColors } from '@/theme/chartColors';
import type { ReactNode } from 'react';

export interface PieDatum {
  name: string;
  value: number;
}

export interface PieChartCardProps {
  title: string;
  subtitle?: string;
  data: PieDatum[];
  height?: number;
  action?: ReactNode;
  innerRadius?: number;
  valueFormatter?: (value: number) => string;
}

/** Thin themed wrapper around Recharts PieChart (donut by default) for composition breakdowns. */
export function PieChartCard({
  title,
  subtitle,
  data,
  height = 300,
  action,
  innerRadius = 55,
  valueFormatter,
}: PieChartCardProps) {
  const theme = useTheme();
  const colors = getCategoricalColors(theme.palette.mode);

  return (
    <ChartCard title={title} subtitle={subtitle} height={height} action={action}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            innerRadius={innerRadius}
            outerRadius={90}
            paddingAngle={2}
            strokeWidth={2}
            stroke={theme.palette.background.paper}
          >
            {data.map((_, idx) => (
              <Cell key={idx} fill={colors[idx % colors.length]} />
            ))}
          </Pie>
          <Tooltip
            formatter={valueFormatter ? (v: number) => valueFormatter(v) : undefined}
            contentStyle={{
              backgroundColor: theme.palette.background.paper,
              border: `1px solid ${theme.palette.divider}`,
              borderRadius: 8,
              fontSize: 13,
            }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
        </PieChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export default PieChartCard;
