import { useCallback, useEffect, useState, type ReactElement } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import Pagination from '@mui/material/Pagination';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import SmsOutlinedIcon from '@mui/icons-material/SmsOutlined';
import NotificationsActiveOutlinedIcon from '@mui/icons-material/NotificationsActiveOutlined';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import { useSnackbar } from 'notistack';
import dayjs from 'dayjs';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import EmptyState from '@/components/common/EmptyState';
import notificationsApi from '@/api/notificationsApi';
import { useAppSelector } from '@/store/hooks';
import type { NotificationChannel, NotificationRecord, Role, SendNotificationPayload } from '@/types';
import ComposeNotificationDialog from './components/ComposeNotificationDialog';

const COMPOSE_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
const PAGE_SIZE = 10;

const TYPE_ICON: Record<NotificationChannel, ReactElement> = {
  EMAIL: <EmailOutlinedIcon color="info" fontSize="small" />,
  SMS: <SmsOutlinedIcon color="warning" fontSize="small" />,
  PUSH: <NotificationsActiveOutlinedIcon color="secondary" fontSize="small" />,
  IN_APP: <CampaignOutlinedIcon color="primary" fontSize="small" />,
};

const STATUS_COLOR: Record<string, 'success' | 'warning' | 'error'> = {
  SENT: 'success',
  PENDING: 'warning',
  FAILED: 'error',
};

/** Personal notification feed for any role (GET /notifications/my), with a Compose action for admin roles. */
export function NotificationsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const role = useAppSelector((state) => state.auth.user?.role);
  const canCompose = !!role && COMPOSE_ROLES.includes(role);

  const [rows, setRows] = useState<NotificationRecord[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const [composeOpen, setComposeOpen] = useState(false);
  const [sending, setSending] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await notificationsApi.listMy({ page, size: PAGE_SIZE });
      setRows(res.data.content);
      setTotalPages(res.data.totalPages);
      setLoaded(true);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load your notifications.', { variant: 'error' });
      setRows([]);
      setLoaded(true);
    } finally {
      setLoading(false);
    }
  }, [page, enqueueSnackbar]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSend = async (values: SendNotificationPayload) => {
    setSending(true);
    try {
      await notificationsApi.send(values);
      enqueueSnackbar('Notification sent.', { variant: 'success' });
      setComposeOpen(false);
      setPage(0);
      load();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not send this notification.', { variant: 'error' });
    } finally {
      setSending(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Notifications"
        subtitle="Your notification history across email, SMS, push and in-app channels"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Notifications' }]}
        action={
          canCompose && (
            <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => setComposeOpen(true)}>
              Compose
            </Button>
          )
        }
      />

      {loading ? (
        <PageLoader label="Loading notifications..." />
      ) : loaded && rows.length === 0 ? (
        <EmptyState title="No notifications yet" description="You have no notifications right now." />
      ) : (
        <>
          <Card>
            <List disablePadding>
              {rows.map((n, idx) => (
                <Box key={n.id}>
                  {idx > 0 && <Divider component="li" />}
                  <ListItem sx={{ py: 1.75, alignItems: 'flex-start' }}>
                    <Box sx={{ mr: 1.5, mt: 0.5 }}>{TYPE_ICON[n.type] ?? TYPE_ICON.IN_APP}</Box>
                    <ListItemText
                      primary={
                        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                          <span>{n.subject}</span>
                          <Chip size="small" variant="outlined" label={n.type} />
                          <Chip size="small" color={STATUS_COLOR[n.status] ?? 'default'} label={n.status} />
                        </Stack>
                      }
                      secondary={
                        <>
                          {n.message}
                          <Box component="span" sx={{ display: 'block', fontSize: '0.75rem', opacity: 0.7, mt: 0.5 }}>
                            {n.sentAt ? dayjs(n.sentAt).format('DD MMM YYYY, hh:mm A') : 'Not sent yet'}
                          </Box>
                        </>
                      }
                      primaryTypographyProps={{ fontWeight: 700, fontSize: '0.9rem', component: 'div' }}
                      secondaryTypographyProps={{ fontSize: '0.825rem', component: 'div' }}
                    />
                  </ListItem>
                </Box>
              ))}
            </List>
          </Card>

          {totalPages > 1 && (
            <Stack direction="row" justifyContent="center" sx={{ mt: 3 }}>
              <Pagination count={totalPages} page={page + 1} onChange={(_e, value) => setPage(value - 1)} color="primary" />
            </Stack>
          )}
        </>
      )}

      {canCompose && (
        <ComposeNotificationDialog
          open={composeOpen}
          sending={sending}
          onClose={() => setComposeOpen(false)}
          onSubmit={handleSend}
        />
      )}
    </Box>
  );
}

export default NotificationsPage;
