import { useEffect, useRef, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Avatar from '@mui/material/Avatar';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemAvatar from '@mui/material/ListItemAvatar';
import ListItemText from '@mui/material/ListItemText';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import Alert from '@mui/material/Alert';
import Badge from '@mui/material/Badge';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import PageHeader from '@/components/common/PageHeader';
import dayjs from 'dayjs';

interface ChatMessage {
  id: number;
  from: 'me' | 'them';
  text: string;
  at: string;
}

interface Conversation {
  id: number;
  name: string;
  subtitle: string;
  unread: number;
  messages: ChatMessage[];
}

const AUTO_REPLIES = [
  "Thanks for your message — I'll get back to you shortly.",
  'Got it, noted. (This is a placeholder conversation; no message was actually sent.)',
  "Sounds good! Let's follow up on this soon.",
];

function seedConversations(): Conversation[] {
  const now = dayjs();
  return [
    {
      id: 1,
      name: 'Priya Sharma',
      subtitle: 'Class Teacher — Class 6A',
      unread: 2,
      messages: [
        { id: 1, from: 'them', text: 'Hello! Just a reminder about tomorrow’s parent-teacher meeting.', at: now.subtract(2, 'day').toISOString() },
        { id: 2, from: 'me', text: 'Thank you for the reminder, I will be there.', at: now.subtract(2, 'day').add(10, 'minute').toISOString() },
        { id: 3, from: 'them', text: 'Great, see you then!', at: now.subtract(1, 'day').toISOString() },
      ],
    },
    {
      id: 2,
      name: 'Front Office',
      subtitle: 'Reception / Admin Desk',
      unread: 0,
      messages: [
        { id: 1, from: 'them', text: 'Your transport pass renewal is due next week.', at: now.subtract(5, 'day').toISOString() },
        { id: 2, from: 'me', text: 'Thanks, I will renew it this weekend.', at: now.subtract(5, 'day').add(20, 'minute').toISOString() },
      ],
    },
    {
      id: 3,
      name: 'Admin Support',
      subtitle: 'IT / Accounts Helpdesk',
      unread: 0,
      messages: [
        { id: 1, from: 'them', text: 'Hi! Let us know if you run into any issues with the portal.', at: now.subtract(10, 'day').toISOString() },
      ],
    },
  ];
}

/**
 * Client-side chat placeholder: two-pane conversation list + message thread.
 * Pre-seeded with sample conversations; sending a message appends it locally
 * and echoes a canned auto-reply after a short delay. Not connected to any
 * real messaging backend yet — see the banner below.
 */
export function ChatPage() {
  const [conversations, setConversations] = useState<Conversation[]>(seedConversations);
  const [activeId, setActiveId] = useState<number>(1);
  const [draft, setDraft] = useState('');
  const scrollRef = useRef<HTMLDivElement>(null);

  const active = conversations.find((c) => c.id === activeId) ?? conversations[0];

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [active?.messages.length, activeId]);

  const handleSelectConversation = (id: number) => {
    setActiveId(id);
    setConversations((prev) => prev.map((c) => (c.id === id ? { ...c, unread: 0 } : c)));
  };

  const handleSend = () => {
    const text = draft.trim();
    if (!text || !active) return;
    setDraft('');
    const now = dayjs().toISOString();
    setConversations((prev) =>
      prev.map((c) =>
        c.id === active.id
          ? { ...c, messages: [...c.messages, { id: c.messages.length + 1, from: 'me', text, at: now }] }
          : c,
      ),
    );
    window.setTimeout(() => {
      const reply = AUTO_REPLIES[Math.floor(Math.random() * AUTO_REPLIES.length)];
      setConversations((prev) =>
        prev.map((c) =>
          c.id === active.id
            ? {
                ...c,
                messages: [
                  ...c.messages,
                  { id: c.messages.length + 2, from: 'them', text: reply, at: dayjs().toISOString() },
                ],
              }
            : c,
        ),
      );
    }, 900);
  };

  return (
    <Box>
      <PageHeader
        title="Chat"
        subtitle="Message teachers, staff and the front office"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Chat' }]}
      />

      <Alert severity="info" sx={{ mb: 2.5 }}>
        Full messaging backend coming soon — conversations shown here are sample data for preview purposes and
        messages you send are not delivered to anyone.
      </Alert>

      <Card sx={{ display: 'flex', height: 560, overflow: 'hidden' }}>
        <Box sx={{ width: 300, flexShrink: 0, borderRight: 1, borderColor: 'divider', overflowY: 'auto' }}>
          <List disablePadding>
            {conversations.map((c) => (
              <ListItemButton
                key={c.id}
                selected={c.id === activeId}
                onClick={() => handleSelectConversation(c.id)}
                sx={{ py: 1.5, px: 2 }}
              >
                <ListItemAvatar>
                  <Badge badgeContent={c.unread} color="error">
                    <Avatar>{c.name[0]}</Avatar>
                  </Badge>
                </ListItemAvatar>
                <ListItemText
                  primary={c.name}
                  secondary={c.messages[c.messages.length - 1]?.text ?? c.subtitle}
                  primaryTypographyProps={{ fontWeight: 600, fontSize: '0.9rem' }}
                  secondaryTypographyProps={{
                    fontSize: '0.78rem',
                    noWrap: true,
                    sx: { maxWidth: 200 },
                  }}
                />
              </ListItemButton>
            ))}
          </List>
        </Box>

        <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          {active && (
            <>
              <Box sx={{ px: 2.5, py: 1.75, display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Avatar>{active.name[0]}</Avatar>
                <Box>
                  <Typography variant="subtitle2" fontWeight={700}>
                    {active.name}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {active.subtitle}
                  </Typography>
                </Box>
              </Box>
              <Divider />

              <Box ref={scrollRef} sx={{ flexGrow: 1, overflowY: 'auto', px: 2.5, py: 2 }}>
                <Stack spacing={1.5}>
                  {active.messages.map((m) => (
                    <Box
                      key={m.id}
                      sx={{
                        display: 'flex',
                        justifyContent: m.from === 'me' ? 'flex-end' : 'flex-start',
                      }}
                    >
                      <Box
                        sx={{
                          maxWidth: '70%',
                          px: 1.75,
                          py: 1,
                          borderRadius: 2.5,
                          bgcolor: m.from === 'me' ? 'primary.main' : 'action.hover',
                          color: m.from === 'me' ? 'primary.contrastText' : 'text.primary',
                        }}
                      >
                        <Typography variant="body2">{m.text}</Typography>
                        <Typography
                          variant="caption"
                          sx={{
                            display: 'block',
                            mt: 0.5,
                            opacity: 0.7,
                            color: m.from === 'me' ? 'primary.contrastText' : 'text.secondary',
                          }}
                        >
                          {dayjs(m.at).format('DD MMM, hh:mm A')}
                        </Typography>
                      </Box>
                    </Box>
                  ))}
                </Stack>
              </Box>

              <Divider />
              <Box sx={{ p: 1.5, display: 'flex', gap: 1 }}>
                <TextField
                  fullWidth
                  size="small"
                  placeholder="Type a message..."
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSend();
                    }
                  }}
                />
                <IconButton color="primary" onClick={handleSend} disabled={!draft.trim()}>
                  <SendOutlinedIcon />
                </IconButton>
              </Box>
            </>
          )}
        </Box>
      </Card>
    </Box>
  );
}

export default ChatPage;
