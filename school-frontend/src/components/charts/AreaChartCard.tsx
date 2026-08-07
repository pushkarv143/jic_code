import {
  ResponsiveContainer,
  AreaChart,
  Area,
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

export interface AreaSeriesDef {
  key: string;
  label: string;
}

export interface AreaChartCardProps {
  title: string;
  subtitle?: string;
  data: Array<Record<string, string | number>>;
  xKey: string;
  series: AreaSeriesDef[];
  height?: number;
  action?: ReactNode;
  valueFormatter?: (value: number) => string;
}

/** Thin themed wrapper around Recharts AreaChart, used for cumulative trend visuals. */
export function AreaChartCard({
  title,
  subtitle,
  data,
  xKey,
  series,
  height = 300,
  action,
  valueFormatter,
}: AreaChartCardProps) {
  const theme = useTheme();
  const colors = getCategoricalColors(theme.palette.mode);
  const chrome = CHART_CHROME[theme.palette.mode];

  return (
    <ChartCard title={title} subtitle={subtitle} height={height} action={action}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: -12 }}>
          <defs>
            {series.map((s, idx) => (
              <linearGradient key={s.key} id={`area-fill-${s.key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={colors[idx % colors.length]} stopOpacity={0.35} />
                <stop offset="95%" stopColor={colors[idx % colors.length]} stopOpacity={0.03} />
              </linearGradient>
            ))}
          </defs>
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
            <Area
              key={s.key}
              type="monotone"
              dataKey={s.key}
              name={s.label}
              stroke={colors[idx % colors.length]}
              strokeWidth={2}
              fill={`url(#area-fill-${s.key})`}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export default AreaChartCard;
