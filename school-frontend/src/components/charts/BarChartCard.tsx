import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { useTheme } from '@mui/material/styles';
import ChartCard from './ChartCard';
import { getCategoricalColors, CHART_CHROME } from '@/theme/chartColors';
import type { ReactNode } from 'react';

export interface BarSeriesDef {
  key: string;
  label: string;
}

export interface BarChartCardProps {
  title: string;
  subtitle?: string;
  data: Array<Record<string, string | number>>;
  xKey: string;
  series: BarSeriesDef[];
  height?: number;
  action?: ReactNode;
  valueFormatter?: (value: number) => string;
}

/** Thin themed wrapper around Recharts BarChart, used for e.g. fee collection totals. */
export function BarChartCard({
  title,
  subtitle,
  data,
  xKey,
  series,
  height = 300,
  action,
  valueFormatter,
}: BarChartCardProps) {
  const theme = useTheme();
  const colors = getCategoricalColors(theme.palette.mode);
  const chrome = CHART_CHROME[theme.palette.mode];

  return (
    <ChartCard title={title} subtitle={subtitle} height={height} action={action}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: -12 }} barGap={4}>
          <CartesianGrid stroke={chrome.grid} vertical={false} />
          <XAxis
            dataKey={xKey}
            stroke={chrome.axis}
            tick={{ fill: chrome.muted, fontSize: 12 }}
            tickLine={false}
            axisLine={{ stroke: chrome.axis }}
          />
          <YAxis
            stroke={chrome.axis}
            tick={{ fill: chrome.muted, fontSize: 12 }}
            tickLine={false}
            axisLine={false}
            width={40}
          />
          <Tooltip
            cursor={{ fill: theme.palette.action.hover }}
            formatter={valueFormatter ? (v: number) => valueFormatter(v) : undefined}
            contentStyle={{
              backgroundColor: theme.palette.background.paper,
              border: `1px solid ${theme.palette.divider}`,
              borderRadius: 8,
              fontSize: 13,
            }}
          />
          {series.length > 1 && <Legend wrapperStyle={{ fontSize: 12 }} />}
          {series.map((s, idx) => (
            <Bar
              key={s.key}
              dataKey={s.key}
              name={s.label}
              fill={colors[idx % colors.length]}
              radius={[4, 4, 0, 0]}
              maxBarSize={36}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export default BarChartCard;
