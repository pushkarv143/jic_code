import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Divider from '@mui/material/Divider';
import Box from '@mui/material/Box';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import type { Dayjs } from 'dayjs';
import EmptyState from '@/components/common/EmptyState';
import StatusChip from '@/components/common/StatusChip';
import type { CalendarEvent } from '@/types';

export interface DayEventsDialogProps {
  open: boolean;
  date: Dayjs | null;
  events: CalendarEvent[];
  canWrite: boolean;
  onClose: () => void;
  onAdd: () => void;
  onEdit: (event: CalendarEvent) => void;
  onDelete: (event: CalendarEvent) => void;
}

/** Side panel/dialog listing a single day's events, with add/edit/delete for admin roles. */
export function DayEventsDialog({ open, date, events, canWrite, onClose, onAdd, onEdit, onDelete }: DayEventsDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{date ? date.format('DD MMMM YYYY') : 'Events'}</DialogTitle>
      <DialogContent dividers>
        {events.length === 0 ? (
          <EmptyState title="No events" description="Nothing is scheduled for this day." />
        ) : (
          <List dense disablePadding>
            {events.map((ev, idx) => (
              <Box key={ev.id}>
                {idx > 0 && <Divider component="li" />}
                <ListItem
                  secondaryAction={
                    canWrite && (
                      <Stack direction="row" spacing={0.25}>
                        <IconButton size="small" onClick={() => onEdit(ev)}>
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => onDelete(ev)}>
                          <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                        </IconButton>
                      </Stack>
                    )
                  }
                >
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <span>{ev.title}</span>
                        <StatusChip status={ev.eventType} />
                      </Stack>
                    }
                    secondary={ev.description}
                  />
                </ListItem>
              </Box>
            ))}
          </List>
        )}
      </DialogContent>
      <DialogActions>
        {canWrite && (
          <Button startIcon={<AddOutlinedIcon />} onClick={onAdd} sx={{ mr: 'auto' }}>
            Add Event
          </Button>
        )}
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

export default DayEventsDialog;
