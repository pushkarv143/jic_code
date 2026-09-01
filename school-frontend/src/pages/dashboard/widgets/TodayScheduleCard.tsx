import { useEffect, useState } from 'react';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Chip from '@mui/material/Chip';
import Skeleton from '@mui/material/Skeleton';
import CalendarTodayOutlinedIcon from '@mui/icons-material/CalendarTodayOutlined';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import PlayCircleOutlineIcon from '@mui/icons-material/PlayCircleOutline';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import MeetingRoomOutlinedIcon from '@mui/icons-material/MeetingRoomOutlined';
import { alpha, useTheme } from '@mui/material/styles';
import dayjs from 'dayjs';
import timetableApi from '@/api/timetableApi';
import type { TimetableSlot } from '@/types';
import { useSnackbar } from 'notistack';

/** "HH:mm:ss" → minutes since midnight, for time comparison */
function toMinutes(timeStr: string): number {
  const [h, m] = timeStr.split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
}

function formatTime(timeStr: string): string {
  const [h, m] = timeStr.split(':').map(Number);
  const period = h >= 12 ? 'PM' : 'AM';
  const hour = h % 12 || 12;
  return `${hour}:${String(m).padStart(2, '0')} ${period}`;
}

type SlotStatus = 'done' | 'now' | 'upcoming';

function getSlotStatus(slot: TimetableSlot, nowMinutes: number): SlotStatus {
  const start = toMinutes(slot.startTime);
  const end = toMinutes(slot.endTime);
  if (nowMinutes > end) return 'done';
  if (nowMinutes >= start && nowMinutes <= end) return 'now';
  return 'upcoming';
}

interface TodayScheduleCardProps {
  /** If provided, fetches timetable for this specific student (parent use-case). */
  studentId?: number;
}

/**
 * Premium "Today's Schedule" card for the Teacher and Student/Parent dashboards.
 *
 * Fetches the caller's own week from GET /api/v1/timetable/me (or /students/:id),
 * filters for today's day-of-week, and presents each period with a live
 * NOW / done / upcoming indicator — no new API needed.
 */
export function TodayScheduleCard({ studentId }: TodayScheduleCardProps) {
  const theme = useTheme();
  const { enqueueSnackbar } = useSnackbar();
  const [slots, setSlots] = useState<TimetableSlot[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [, setTick] = useState(0); // forces re-render every minute for live "now" status

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = studentId
          ? await timetableApi.getForStudent(studentId)
          : await timetableApi.getMine();

        if (!cancelled) {
          const today = dayjs().format('dddd').toUpperCase();
          const todaySlots = (res.data ?? [])
            .filter((s) => s.dayOfWeek === today)
            .sort((a, b) => a.periodNumber - b.periodNumber);
          setSlots(todaySlots);
        }
      } catch {
        if (!cancelled) {
          enqueueSnackbar('Could not load today\'s schedule.', { variant: 'error' });
          setSlots([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [studentId, enqueueSnackbar]);

  // Re-render every 60s so the NOW indicator stays accurate
  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 60_000);
    return () => clearInterval(id);
  }, []);

  const now = dayjs();
  const nowMinutes = now.hour() * 60 + now.minute();
  const todayLabel = now.format('dddd, D MMM');

  const currentSlot = slots?.find((s) => getSlotStatus(s, nowMinutes) === 'now');
  const remainingCount = slots?.filter((s) => getSlotStatus(s, nowMinutes) !== 'done').length ?? 0;

  return (
    <Card sx={{ height: '100%' }}>
      <CardHeader
        avatar={
          <Box sx={{
            width: 36, height: 36, borderRadius: 2,
            background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.primary.light})`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <CalendarTodayOutlinedIcon sx={{ fontSize: 18, color: '#fff' }} />
          </Box>
        }
        title={
          <Typography variant="subtitle1" fontWeight={700}>
            Today's Schedule
          </Typography>
        }
        subheader={
          <Typography variant="caption" color="text.secondary">
            {todayLabel}
            {currentSlot && (
              <Chip
                label="Class in progress"
                size="small"
                color="success"
                variant="outlined"
                sx={{ ml: 1, height: 18, fontSize: '0.65rem', fontWeight: 700 }}
              />
            )}
          </Typography>
        }
      />
      <CardContent sx={{ pt: 0 }}>
        {loading ? (
          <Stack spacing={1.5}>
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} variant="rounded" height={56} animation="wave" />
            ))}
          </Stack>
        ) : !slots || slots.length === 0 ? (
          <Box sx={{ py: 3, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              No classes scheduled for today.
            </Typography>
          </Box>
        ) : (
          <Stack spacing={1}>
            {slots.map((slot) => {
              const status = getSlotStatus(slot, nowMinutes);
              const subjectOrLabel = slot.subjectName || slot.label || `Period ${slot.periodNumber}`;

              const statusConfig = {
                now: {
                  icon: <PlayCircleOutlineIcon sx={{ fontSize: 18 }} />,
                  color: theme.palette.success.main,
                  bg: alpha(theme.palette.success.main, 0.08),
                  border: alpha(theme.palette.success.main, 0.25),
                  label: 'NOW',
                },
                done: {
                  icon: <CheckCircleOutlineIcon sx={{ fontSize: 18 }} />,
                  color: theme.palette.text.disabled,
                  bg: 'transparent',
                  border: 'transparent',
                  label: null,
                },
                upcoming: {
                  icon: <RadioButtonUncheckedIcon sx={{ fontSize: 18 }} />,
                  color: theme.palette.primary.main,
                  bg: alpha(theme.palette.primary.main, 0.04),
                  border: alpha(theme.palette.primary.main, 0.12),
                  label: null,
                },
              }[status];

              return (
                <Box
                  key={slot.id ?? `${slot.dayOfWeek}-${slot.periodNumber}`}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    p: 1.25,
                    borderRadius: 2,
                    bgcolor: statusConfig.bg,
                    border: `1px solid ${statusConfig.border}`,
                    transition: 'all 0.2s ease',
                    opacity: status === 'done' ? 0.5 : 1,
                  }}
                >
                  {/* Status icon */}
                  <Box sx={{ color: statusConfig.color, flexShrink: 0, display: 'flex' }}>
                    {statusConfig.icon}
                  </Box>

                  {/* Period info */}
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" alignItems="center" spacing={0.75}>
                      <Typography
                        variant="body2"
                        fontWeight={status === 'now' ? 700 : 600}
                        color={status === 'done' ? 'text.disabled' : 'text.primary'}
                        noWrap
                      >
                        {subjectOrLabel}
                      </Typography>
                      {statusConfig.label && (
                        <Chip
                          label={statusConfig.label}
                          size="small"
                          color="success"
                          sx={{ height: 18, fontSize: '0.62rem', fontWeight: 800 }}
                        />
                      )}
                      {slot.className && (
                        <Typography variant="caption" color="text.secondary" noWrap sx={{ fontSize: '0.7rem' }}>
                          · {slot.className}
                        </Typography>
                      )}
                    </Stack>

                    <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mt: 0.25 }}>
                      <Stack direction="row" spacing={0.4} alignItems="center">
                        <AccessTimeOutlinedIcon sx={{ fontSize: 12, color: 'text.disabled' }} />
                        <Typography variant="caption" color="text.disabled" sx={{ fontSize: '0.7rem' }}>
                          {formatTime(slot.startTime)} – {formatTime(slot.endTime)}
                        </Typography>
                      </Stack>
                      {slot.roomNumber && (
                        <Stack direction="row" spacing={0.4} alignItems="center">
                          <MeetingRoomOutlinedIcon sx={{ fontSize: 12, color: 'text.disabled' }} />
                          <Typography variant="caption" color="text.disabled" sx={{ fontSize: '0.7rem' }}>
                            {slot.roomNumber}
                          </Typography>
                        </Stack>
                      )}
                    </Stack>
                  </Box>

                  {/* Period number badge */}
                  <Box
                    sx={{
                      width: 28, height: 28, borderRadius: 1.5,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      bgcolor: alpha(statusConfig.color, 0.12),
                      flexShrink: 0,
                    }}
                  >
                    <Typography sx={{ fontSize: '0.72rem', fontWeight: 800, color: statusConfig.color }}>
                      P{slot.periodNumber}
                    </Typography>
                  </Box>
                </Box>
              );
            })}

            {/* Footer summary */}
            {remainingCount > 0 && (
              <Typography variant="caption" color="text.secondary" sx={{ pt: 0.5, textAlign: 'center', display: 'block' }}>
                {remainingCount === 1
                  ? '1 period remaining today'
                  : `${remainingCount} periods remaining today`}
              </Typography>
            )}
            {remainingCount === 0 && slots.length > 0 && (
              <Typography variant="caption" color="success.main" sx={{ pt: 0.5, textAlign: 'center', display: 'block', fontWeight: 600 }}>
                ✓ All classes done for today
              </Typography>
            )}
          </Stack>
        )}
      </CardContent>
    </Card>
  );
}

export default TodayScheduleCard;
