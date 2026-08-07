import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Button from '@mui/material/Button';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Box from '@mui/material/Box';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import dayjs from 'dayjs';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { markAllAsRead } from '@/store/notificationSlice';
import EmptyState from '@/components/common/EmptyState';

const ICONS = {
  INFO: <InfoOutlinedIcon color="info" fontSize="small" />,
  SUCCESS: <CheckCircleOutlineIcon color="success" fontSize="small" />,
  WARNING: <WarningAmberOutlinedIcon color="warning" fontSize="small" />,
  ERROR: <ErrorOutlineOutlinedIcon color="error" fontSize="small" />,
};

/** Reads the notification list straight from Redux notificationSlice. */
export function NotificationsCard() {
  const notifications = useAppSelector((state) => state.notifications.items);
  const dispatch = useAppDispatch();

  return (
    <Card sx={{ height: '100%' }}>
      <CardHeader
        title="Notifications"
        action={
          notifications.length > 0 && (
            <Button size="small" onClick={() => dispatch(markAllAsRead())}>
              Mark all read
            </Button>
          )
        }
      />
      <CardContent sx={{ pt: 0 }}>
        {notifications.length === 0 ? (
          <EmptyState title="No notifications" description="You're all caught up for now." />
        ) : (
          <List disablePadding>
            {notifications.map((n) => (
              <ListItem key={n.id} disableGutters sx={{ alignItems: 'flex-start', py: 1 }}>
                <Box sx={{ mr: 1.5, mt: 0.25 }}>{ICONS[n.type]}</Box>
                <ListItemText
                  primary={n.title}
                  secondary={
                    <>
                      {n.message}
                      <Box component="span" sx={{ display: 'block', fontSize: '0.7rem', opacity: 0.6, mt: 0.25 }}>
                        {dayjs(n.createdAt).format('DD MMM, hh:mm A')}
                      </Box>
                    </>
                  }
                  primaryTypographyProps={{ fontWeight: n.read ? 500 : 700, fontSize: '0.875rem' }}
                  secondaryTypographyProps={{ fontSize: '0.75rem', component: 'div' }}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
}

export default NotificationsCard;
