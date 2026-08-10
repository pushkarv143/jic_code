import { useEffect, useRef, useState } from 'react';
import Fab from '@mui/material/Fab';
import Drawer from '@mui/material/Drawer';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import TextField from '@mui/material/TextField';
import Divider from '@mui/material/Divider';
import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Avatar from '@mui/material/Avatar';
import SmartToyOutlinedIcon from '@mui/icons-material/SmartToyOutlined';
import CloseIcon from '@mui/icons-material/Close';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import dayjs from 'dayjs';

interface AssistantMessage {
  id: number;
  from: 'assistant' | 'me';
  text: string;
  at: string;
}

const WELCOME_MESSAGE =
  "Hi! I'm your AI Assistant (placeholder). Ask me anything about the school system and I'll do my best to help once I'm fully connected.";

const CANNED_REPLY =
  "This is a placeholder response — the full AI Assistant backend is coming soon, so I can't answer that yet. Your message has been noted locally.";

/**
 * Floating action button + drawer, mounted once in DashboardLayout so it's
 * visible across the entire authenticated app shell. Purely client-side: no
 * real AI backend is wired up yet, hence the banner inside the drawer.
 */
export function AiAssistantWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<AssistantMessage[]>([
    { id: 1, from: 'assistant', text: WELCOME_MESSAGE, at: dayjs().toISOString() },
  ]);
  const [draft, setDraft] = useState('');
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (open) scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages.length, open]);

  const handleSend = () => {
    const text = draft.trim();
    if (!text) return;
    setDraft('');
    setMessages((prev) => [...prev, { id: prev.length + 1, from: 'me', text, at: dayjs().toISOString() }]);
    window.setTimeout(() => {
      setMessages((prev) => [
        ...prev,
        { id: prev.length + 2, from: 'assistant', text: CANNED_REPLY, at: dayjs().toISOString() },
      ]);
    }, 700);
  };

  return (
    <>
      <Fab
        color="primary"
        onClick={() => setOpen(true)}
        sx={{ position: 'fixed', bottom: 24, right: 24, zIndex: (theme) => theme.zIndex.drawer + 2 }}
        aria-label="Open AI Assistant"
      >
        <SmartToyOutlinedIcon />
      </Fab>

      <Drawer anchor="right" open={open} onClose={() => setOpen(false)}>
        <Box sx={{ width: { xs: '100vw', sm: 360 }, height: '100%', display: 'flex', flexDirection: 'column' }}>
          <Box sx={{ px: 2.5, py: 2, display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Avatar sx={{ bgcolor: 'primary.main' }}>
              <SmartToyOutlinedIcon fontSize="small" />
            </Avatar>
            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="subtitle1" fontWeight={700}>
                AI Assistant
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Always here to help
              </Typography>
            </Box>
            <IconButton onClick={() => setOpen(false)}>
              <CloseIcon />
            </IconButton>
          </Box>
          <Divider />

          <Box sx={{ px: 2, pt: 1.5 }}>
            <Alert severity="info" sx={{ mb: 1.5 }}>
              Full AI Assistant backend coming soon — responses below are canned placeholders.
            </Alert>
          </Box>

          <Box ref={scrollRef} sx={{ flexGrow: 1, overflowY: 'auto', px: 2, py: 1 }}>
            <Stack spacing={1.5}>
              {messages.map((m) => (
                <Box key={m.id} sx={{ display: 'flex', justifyContent: m.from === 'me' ? 'flex-end' : 'flex-start' }}>
                  <Box
                    sx={{
                      maxWidth: '85%',
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
                      {dayjs(m.at).format('hh:mm A')}
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
              placeholder="Ask the assistant..."
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
        </Box>
      </Drawer>
    </>
  );
}

export default AiAssistantWidget;
