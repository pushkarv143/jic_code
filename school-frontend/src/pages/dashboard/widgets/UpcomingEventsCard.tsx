import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import dayjs from 'dayjs';
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined';
import EmptyState from '@/components/common/EmptyState';

export interface EventItem {
  title: string;
  date: string;
  type: 'HOLIDAY' | 'EVENT' | 'EXAM' | 'OTHER';
}

const TYPE_COLOR: Record<EventItem['type'], 'success' | 'primary' | 'warning' | 'default'> = {
  HOLIDAY: 'success',
  EVENT: 'primary',
  EXAM: 'warning',
  OTHER: 'default',
};

export function UpcomingEventsCard({ events }: { events: EventItem[] }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardHeader title="Upcoming Events" subheader="From the school calendar" />
      <CardContent sx={{ pt: 0 }}>
        {events.length === 0 ? (
          <EmptyState
            icon={<EventBusyOutlinedIcon fontSize="large" />}
            title="No upcoming events"
            description="Nothing on the school calendar right now."
          />
        ) : (
        <List disablePadding>
          {events.map((event, idx) => {
            const date = dayjs(event.date);
            return (
              <ListItem key={idx} disableGutters sx={{ alignItems: 'flex-start', py: 1.25 }}>
                <Box
                  sx={{
                    width: 46,
                    textAlign: 'center',
                    borderRadius: 2,
                    bgcolor: 'action.hover',
                    py: 0.5,
                    mr: 1.5,
                    flexShrink: 0,
                  }}
                >
                  <Typography variant="caption" color="text.secondary" display="block" fontWeight={700}>
                    {date.format('MMM')}
                  </Typography>
                  <Typography variant="subtitle1" fontWeight={700} lineHeight={1}>
                    {date.format('DD')}
                  </Typography>
                </Box>
                <ListItemText
                  primary={event.title}
                  secondary={date.format('dddd, DD MMM YYYY')}
                  primaryTypographyProps={{ fontWeight: 600, fontSize: '0.875rem' }}
                  secondaryTypographyProps={{ fontSize: '0.75rem' }}
                />
                <Chip label={event.type} size="small" color={TYPE_COLOR[event.type]} variant="outlined" />
              </ListItem>
            );
          })}
        </List>
        )}
      </CardContent>
    </Card>
  );
}

export default UpcomingEventsCard;
