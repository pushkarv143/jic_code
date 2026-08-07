import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Avatar from '@mui/material/Avatar';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemAvatar from '@mui/material/ListItemAvatar';
import ListItemText from '@mui/material/ListItemText';
import CircularProgress from '@mui/material/CircularProgress';
import ChevronLeftOutlinedIcon from '@mui/icons-material/ChevronLeftOutlined';
import ChevronRightOutlinedIcon from '@mui/icons-material/ChevronRightOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CakeOutlinedIcon from '@mui/icons-material/CakeOutlined';
import { useSnackbar } from 'notistack';
import dayjs, { type Dayjs } from 'dayjs';
import { alpha, useTheme } from '@mui/material/styles';
import PageHeader from '@/components/common/PageHeader';
import EmptyState from '@/components/common/EmptyState';
import eventsApi from '@/api/eventsApi';
import { useAppSelector } from '@/store/hooks';
import { getStudentDisplayName } from '@/utils/format';
import type { BirthdaysResponse, CalendarEvent, CalendarEventType, Role } from '@/types';
import type { EventPayload } from '@/api/eventsApi';
import EventFormDialog from './components/EventFormDialog';
import DayEventsDialog from './components/DayEventsDialog';
import ConfirmDialog from '@/components/common/ConfirmDialog';

const ADMIN_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

const TYPE_COLOR: Record<CalendarEventType, 'error' | 'success' | 'info' | 'default'> = {
  HOLIDAY: 'error',
  EXAM: 'info',
  EVENT: 'success',
  OTHER: 'default',
};

/** Month-view calendar (plain MUI Grid, no external calendar library) with a "Birthdays this month" card. */
export function CalendarPage() {
  const theme = useTheme();
  const { enqueueSnackbar } = useSnackbar();
  const user = useAppSelector((state) => state.auth.user);
  const canWrite = !!user && ADMIN_ROLES.includes(user.role);

  const [month, setMonth] = useState<Dayjs>(dayjs().startOf('month'));
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [birthdays, setBirthdays] = useState<BirthdaysResponse | null>(null);

  const [selectedDate, setSelectedDate] = useState<Dayjs | null>(null);
  const [dayDialogOpen, setDayDialogOpen] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<CalendarEvent | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<CalendarEvent | null>(null);

  const gridStart = month.startOf('week');
  const gridDays = useMemo(() => Array.from({ length: 42 }, (_, i) => gridStart.add(i, 'day')), [gridStart]);

  const loadEvents = useCallback(async () => {
    setLoading(true);
    try {
      const res = await eventsApi.list({
        startDate: gridDays[0].format('YYYY-MM-DD'),
        endDate: gridDays[41].format('YYYY-MM-DD'),
      });
      setEvents(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load calendar events.', { variant: 'error' });
      setEvents([]);
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [month, enqueueSnackbar]);

  useEffect(() => {
    loadEvents();
  }, [loadEvents]);

  useEffect(() => {
    eventsApi
      .birthdays(month.month() + 1)
      .then((res) => setBirthdays(res.data))
      .catch(() => setBirthdays(null));
  }, [month]);

  const eventsForDay = useCallback((day: Dayjs) => events.filter((e) => dayjs(e.eventDate).isSame(day, 'day')), [events]);

  const handleDayClick = (day: Dayjs) => {
    setSelectedDate(day);
    setDayDialogOpen(true);
  };

  const handleSave = async (values: EventPayload) => {
    setSaving(true);
    try {
      if (editing) {
        await eventsApi.update(editing.id, values);
        enqueueSnackbar('Event updated.', { variant: 'success' });
      } else {
        await eventsApi.create(values);
        enqueueSnackbar('Event added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      setDayDialogOpen(false);
      loadEvents();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this event.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await eventsApi.remove(deleteTarget.id);
      enqueueSnackbar('Event deleted.', { variant: 'success' });
      setDeleteTarget(null);
      setDayDialogOpen(false);
      loadEvents();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete this event.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const today = dayjs();

  return (
    <Box>
      <PageHeader
        title="School Calendar"
        subtitle="Holidays, exams and events for the school year"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Calendar' }]}
        action={
          canWrite && (
            <Button
              variant="contained"
              startIcon={<AddOutlinedIcon />}
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              Add Event
            </Button>
          )
        }
      />

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={8}>
          <Card>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 2, py: 1.5 }}>
              <IconButton onClick={() => setMonth((m) => m.subtract(1, 'month'))}>
                <ChevronLeftOutlinedIcon />
              </IconButton>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <Typography variant="h6" fontWeight={700}>
                  {month.format('MMMM YYYY')}
                </Typography>
                {loading && <CircularProgress size={16} />}
                <Button size="small" onClick={() => setMonth(dayjs().startOf('month'))}>
                  Today
                </Button>
              </Stack>
              <IconButton onClick={() => setMonth((m) => m.add(1, 'month'))}>
                <ChevronRightOutlinedIcon />
              </IconButton>
            </Box>
            <Divider />
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', px: { xs: 0.5, sm: 1.5 }, pt: 1 }}>
              {WEEKDAYS.map((d) => (
                <Typography key={d} variant="caption" color="text.secondary" fontWeight={700} textAlign="center" sx={{ py: 0.5 }}>
                  {d}
                </Typography>
              ))}
            </Box>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: 'repeat(7, 1fr)',
                gap: { xs: 0.5, sm: 1 },
                px: { xs: 0.5, sm: 1.5 },
                pb: 2,
              }}
            >
              {gridDays.map((day) => {
                const inMonth = day.isSame(month, 'month');
                const isToday = day.isSame(today, 'day');
                const dayEvents = eventsForDay(day);
                return (
                  <Box
                    key={day.format('YYYY-MM-DD')}
                    onClick={() => handleDayClick(day)}
                    sx={{
                      minHeight: { xs: 56, sm: 84 },
                      p: 0.75,
                      borderRadius: 1.5,
                      cursor: 'pointer',
                      border: '1px solid',
                      borderColor: isToday ? 'primary.main' : 'divider',
                      bgcolor: inMonth ? 'background.paper' : 'action.hover',
                      opacity: inMonth ? 1 : 0.5,
                      transition: 'background-color 0.15s',
                      '&:hover': { bgcolor: alpha(theme.palette.primary.main, 0.08) },
                    }}
                  >
                    <Typography variant="caption" fontWeight={isToday ? 800 : 500} color={isToday ? 'primary.main' : 'text.primary'}>
                      {day.date()}
                    </Typography>
                    <Stack spacing={0.25} sx={{ mt: 0.5 }}>
                      {dayEvents.slice(0, 2).map((ev) => (
                        <Chip
                          key={ev.id}
                          size="small"
                          label={ev.title}
                          color={TYPE_COLOR[ev.eventType]}
                          variant="outlined"
                          sx={{ height: 18, fontSize: '0.62rem', maxWidth: '100%', '& .MuiChip-label': { px: 0.5, overflow: 'hidden', textOverflow: 'ellipsis' } }}
                        />
                      ))}
                      {dayEvents.length > 2 && (
                        <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.62rem' }}>
                          +{dayEvents.length - 2} more
                        </Typography>
                      )}
                    </Stack>
                  </Box>
                );
              })}
            </Box>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
                <CakeOutlinedIcon color="secondary" />
                <Typography variant="subtitle1" fontWeight={700}>
                  Birthdays This Month
                </Typography>
              </Stack>
              {!birthdays || (birthdays.students.length === 0 && birthdays.teachers.length === 0) ? (
                <EmptyState title="No birthdays" description="No students or teachers have a birthday this month." />
              ) : (
                <List dense disablePadding>
                  {[...birthdays.students, ...birthdays.teachers].map((p) => (
                    <ListItem key={`${p.id}-${p.dateOfBirth}`} disableGutters>
                      <ListItemAvatar>
                        <Avatar sx={{ width: 32, height: 32, fontSize: '0.8rem' }}>
                          {(p.firstName?.[0] ?? '?').toUpperCase()}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={getStudentDisplayName({ firstName: p.firstName, lastName: p.lastName })}
                        secondary={`${dayjs(p.dateOfBirth).format('DD MMM')} · ${p.className ? `${p.className}${p.sectionName ? `-${p.sectionName}` : ''}` : p.designationName ?? 'Staff'}`}
                      />
                    </ListItem>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <DayEventsDialog
        open={dayDialogOpen}
        date={selectedDate}
        events={selectedDate ? eventsForDay(selectedDate) : []}
        canWrite={canWrite}
        onClose={() => setDayDialogOpen(false)}
        onAdd={() => {
          setEditing(null);
          setFormOpen(true);
        }}
        onEdit={(ev) => {
          setEditing(ev);
          setFormOpen(true);
        }}
        onDelete={(ev) => setDeleteTarget(ev)}
      />

      <EventFormDialog
        open={formOpen}
        editing={editing}
        defaultDate={selectedDate}
        saving={saving}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleSave}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete event"
        message={`Delete "${deleteTarget?.title ?? ''}"? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

export default CalendarPage;
