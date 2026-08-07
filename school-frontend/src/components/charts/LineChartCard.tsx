import {
  ResponsiveContainer,
  LineChart,
  Line,
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

export interface LineSeriesDef {
  key: string;
  label: string;
}

export interface LineChartCardProps {
  title: string;
  subtitle?: string;
  data: Array<Record<string, string | number>>;
  xKey: string;
  series: LineSeriesDef[];
  height?: number;
  action?: ReactNode;
  valueFormatter?: (value: number) => string;
}

/** Thin themed wrapper around Recharts LineChart, used for trend data (e.g. attendance). */
export function LineChartCard({
  title,
  subtitle,
  data,
  xKey,
  series,
  height = 300,
  action,
  valueFormatter,
}: LineChartCardProps) {
  const theme = useTheme();
  const colors = getCategoricalColors(theme.palette.mode);
  const chrome = CHART_CHROME[theme.palette.mode];

  return (
    <ChartCard title={title} subtitle={subtitle} height={height} action={action}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: -12 }}>
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
            width={36}
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
            <Line
              key={s.key}
              type="monotone"
              dataKey={s.key}
              name={s.label}
              stroke={colors[idx % colors.length]}
              strokeWidth={2}
              dot={{ r: 3, strokeWidth: 0, fill: colors[idx % colors.length] }}
              activeDot={{ r: 5 }}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export default LineChartCard;
