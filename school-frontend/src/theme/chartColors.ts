import type { PaletteMode } from '@mui/material';

/**
 * Validated categorical / sequential / status colors for Recharts, kept
 * separate from the MUI UI palette (theme.ts) because chart series identity
 * needs CVD-safe hues in a fixed order, not the brand accent colors.
 * Source: dataviz skill reference palette (adjacent-pair CVD delta >= 8,
 * normal-vision floor >= 15 in both light and dark).
 */
export const CATEGORICAL: Record<PaletteMode, string[]> = {
  light: [
    '#2a78d6', // blue
    '#eb6834', // orange
    '#1baf7a', // aqua
    '#eda100', // yellow
    '#e87ba4', // magenta
    '#008300', // green
    '#4a3aa7', // violet
    '#e34948', // red
  ],
  dark: [
    '#3987e5',
    '#d95926',
    '#199e70',
    '#c98500',
    '#d55181',
    '#008300',
    '#9085e9',
    '#e66767',
  ],
};

export const SEQUENTIAL_BLUE: Record<PaletteMode, string[]> = {
  light: ['#cde2fb', '#9ec5f4', '#6da7ec', '#3987e5', '#256abf', '#184f95'],
  dark: ['#cde2fb', '#9ec5f4', '#6da7ec', '#3987e5', '#256abf', '#184f95'],
};

export const STATUS = {
  good: '#0ca30c',
  warning: '#fab219',
  serious: '#ec835a',
  critical: '#d03b3b',
};

export const CHART_CHROME: Record<PaletteMode, { grid: string; axis: string; muted: string }> = {
  light: { grid: '#e1e0d9', axis: '#c3c2b7', muted: '#898781' },
  dark: { grid: '#2c2c2a', axis: '#383835', muted: '#898781' },
};

export function getCategoricalColors(mode: PaletteMode): string[] {
  return CATEGORICAL[mode];
}

/**
 * Fixed color-by-status map for the attendance calendar grid (MonthlyAttendancePage)
 * and any other place that needs an at-a-glance attendance-status swatch. Reuses the
 * validated STATUS good/warning/serious/critical scale plus one categorical hue for
 * HALF_DAY/LEAVE so all five statuses stay visually distinct.
 */
export const ATTENDANCE_STATUS_COLORS: Record<PaletteMode, Record<string, string>> = {
  light: {
    PRESENT: STATUS.good,
    ABSENT: STATUS.critical,
    LATE: STATUS.warning,
    HALF_DAY: CATEGORICAL.light[0],
    LEAVE: CATEGORICAL.light[4],
    UNMARKED: CHART_CHROME.light.muted,
  },
  dark: {
    PRESENT: STATUS.good,
    ABSENT: STATUS.critical,
    LATE: STATUS.warning,
    HALF_DAY: CATEGORICAL.dark[0],
    LEAVE: CATEGORICAL.dark[4],
    UNMARKED: CHART_CHROME.dark.muted,
  },
};

export function getAttendanceStatusColor(mode: PaletteMode, status: string | null | undefined): string {
  if (!status) return ATTENDANCE_STATUS_COLORS[mode].UNMARKED;
  return ATTENDANCE_STATUS_COLORS[mode][status] ?? ATTENDANCE_STATUS_COLORS[mode].UNMARKED;
}
