import type { TimetableDay } from '@/types';

/**
 * The days a class actually runs.
 *
 * <p>Sunday is excluded — the school does not teach on it. It still exists in the
 * `timetable_slots.day_of_week` enum, because the column is a plain day-of-week
 * and narrowing the enum would be a schema change to encode a policy, but nothing
 * in the UI offers it and the "fill the whole week" action below skips it.
 *
 * <p>Shared by the two editors so they cannot disagree about what a week is.
 */
export const WORKING_DAYS: TimetableDay[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

/** Three-letter label for a day column or chip. */
export function dayLabel(day: TimetableDay): string {
  return day.charAt(0) + day.slice(1, 3).toLowerCase();
}

/**
 * Default bell times, one entry per period.
 *
 * <p>The gaps are real: 11:00–11:20 is the short break and 12:40–13:20 is lunch,
 * so period 4 does not begin when period 3 ends. Times remain editable per slot on
 * the Timetable grid — these only seed a period that has not been given its own.
 */
export const DEFAULT_PERIODS = [
  { startTime: '09:00:00', endTime: '09:40:00' },
  { startTime: '09:40:00', endTime: '10:20:00' },
  { startTime: '10:20:00', endTime: '11:00:00' },
  { startTime: '11:20:00', endTime: '12:00:00' },
  { startTime: '12:00:00', endTime: '12:40:00' },
  { startTime: '13:20:00', endTime: '14:00:00' },
  { startTime: '14:00:00', endTime: '14:40:00' },
  { startTime: '14:40:00', endTime: '15:20:00' },
];

/** "09:00" from "09:00:00" — the seconds are never meaningful here. */
export function shortTime(time: string): string {
  return time.slice(0, 5);
}

/** "P3 · 10:20–11:00", the label a period is picked by. */
export function periodLabel(periodNumber: number): string {
  const period = DEFAULT_PERIODS[periodNumber - 1];
  if (!period) return `P${periodNumber}`;
  return `P${periodNumber} · ${shortTime(period.startTime)}–${shortTime(period.endTime)}`;
}
