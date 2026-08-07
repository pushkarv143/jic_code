import { useCallback, useEffect, useState, type SyntheticEvent } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import CircularProgress from '@mui/material/CircularProgress';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import ApartmentOutlinedIcon from '@mui/icons-material/ApartmentOutlined';
import { useSnackbar } from 'notistack';
import EmptyState from '@/components/common/EmptyState';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import hostelApi from '@/api/hostelApi';
import type { Hostel, HostelRoom } from '@/types';
import HostelFormDialog from './components/HostelFormDialog';
import RoomFormDialog from './components/RoomFormDialog';

/** Hostel selector (tabs) + card grid of rooms per hostel with occupancy, plus hostel & room CRUD. */
export function RoomsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [hostels, setHostels] = useState<Hostel[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [rooms, setRooms] = useState<HostelRoom[]>([]);
  const [loadingHostels, setLoadingHostels] = useState(true);
  const [loadingRooms, setLoadingRooms] = useState(false);

  const [hostelFormOpen, setHostelFormOpen] = useState(false);
  const [editingHostel, setEditingHostel] = useState<Hostel | null>(null);
  const [savingHostel, setSavingHostel] = useState(false);
  const [deleteHostelTarget, setDeleteHostelTarget] = useState<Hostel | null>(null);

  const [roomFormOpen, setRoomFormOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<HostelRoom | null>(null);
  const [savingRoom, setSavingRoom] = useState(false);
  const [deleteRoomTarget, setDeleteRoomTarget] = useState<HostelRoom | null>(null);

  const loadHostels = useCallback(async () => {
    setLoadingHostels(true);
    try {
      const res = await hostelApi.hostels.list();
      setHostels(res.data);
      setSelectedId((prev) => (prev && res.data.some((h) => h.id === prev) ? prev : res.data[0]?.id ?? null));
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load hostels.', { variant: 'error' });
    } finally {
      setLoadingHostels(false);
    }
  }, [enqueueSnackbar]);

  useEffect(() => {
    loadHostels();
  }, [loadHostels]);

  const loadRooms = useCallback(async () => {
    if (!selectedId) {
      setRooms([]);
      return;
    }
    setLoadingRooms(true);
    try {
      const res = await hostelApi.hostelRooms.list(selectedId);
      setRooms(res.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load rooms for this hostel.', { variant: 'error' });
      setRooms([]);
    } finally {
      setLoadingRooms(false);
    }
  }, [selectedId, enqueueSnackbar]);

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  const selectedHostel = hostels.find((h) => h.id === selectedId) ?? null;

  const handleSaveHostel = async (values: Parameters<typeof hostelApi.hostels.create>[0]) => {
    setSavingHostel(true);
    try {
      if (editingHostel) {
        await hostelApi.hostels.update(editingHostel.id, values);
        enqueueSnackbar('Hostel updated.', { variant: 'success' });
      } else {
        await hostelApi.hostels.create(values);
        enqueueSnackbar('Hostel added.', { variant: 'success' });
      }
      setHostelFormOpen(false);
      setEditingHostel(null);
      loadHostels();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this hostel.', { variant: 'error' });
    } finally {
      setSavingHostel(false);
    }
  };

  const handleDeleteHostel = async () => {
    if (!deleteHostelTarget) return;
    try {
      await hostelApi.hostels.remove(deleteHostelTarget.id);
      enqueueSnackbar('Hostel deleted.', { variant: 'success' });
      setDeleteHostelTarget(null);
      loadHostels();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — it may have rooms or residents.', { variant: 'error' });
      setDeleteHostelTarget(null);
    }
  };

  const handleSaveRoom = async (values: Parameters<typeof hostelApi.hostelRooms.create>[1]) => {
    if (!selectedId) return;
    setSavingRoom(true);
    try {
      if (editingRoom) {
        await hostelApi.hostelRooms.update(editingRoom.id, values);
        enqueueSnackbar('Room updated.', { variant: 'success' });
      } else {
        await hostelApi.hostelRooms.create(selectedId, values);
        enqueueSnackbar('Room added.', { variant: 'success' });
      }
      setRoomFormOpen(false);
      setEditingRoom(null);
      loadRooms();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this room.', { variant: 'error' });
    } finally {
      setSavingRoom(false);
    }
  };

  const handleDeleteRoom = async () => {
    if (!deleteRoomTarget) return;
    try {
      await hostelApi.hostelRooms.remove(deleteRoomTarget.id);
      enqueueSnackbar('Room deleted.', { variant: 'success' });
      setDeleteRoomTarget(null);
      loadRooms();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — students may be allocated to this room.', { variant: 'error' });
      setDeleteRoomTarget(null);
    }
  };

  if (loadingHostels) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={32} />
      </Box>
    );
  }

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ px: 2 }}>
          {hostels.length > 0 ? (
            <Tabs
              value={selectedId}
              onChange={(_e: SyntheticEvent, v: number) => setSelectedId(v)}
              variant="scrollable"
              scrollButtons="auto"
              sx={{ borderBottom: 0 }}
            >
              {hostels.map((h) => (
                <Tab key={h.id} value={h.id} label={`${h.name} (${h.type === 'BOYS' ? 'Boys' : 'Girls'})`} sx={{ minHeight: 48 }} />
              ))}
            </Tabs>
          ) : (
            <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
              No hostels yet.
            </Typography>
          )}
          <Stack direction="row" spacing={1}>
            {selectedHostel && (
              <>
                <Tooltip title="Edit hostel">
                  <IconButton
                    size="small"
                    onClick={() => {
                      setEditingHostel(selectedHostel);
                      setHostelFormOpen(true);
                    }}
                  >
                    <EditOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Delete hostel">
                  <IconButton size="small" onClick={() => setDeleteHostelTarget(selectedHostel)}>
                    <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                  </IconButton>
                </Tooltip>
              </>
            )}
            <Button
              size="small"
              variant="outlined"
              startIcon={<ApartmentOutlinedIcon />}
              onClick={() => {
                setEditingHostel(null);
                setHostelFormOpen(true);
              }}
            >
              Add Hostel
            </Button>
          </Stack>
        </Stack>
      </Card>

      {hostels.length === 0 ? (
        <EmptyState title="No hostels yet" description="Add a hostel block (Boys/Girls) to start managing rooms." />
      ) : (
        <>
          <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
            <Button
              size="small"
              variant="contained"
              startIcon={<AddOutlinedIcon />}
              onClick={() => {
                setEditingRoom(null);
                setRoomFormOpen(true);
              }}
            >
              Add Room
            </Button>
          </Stack>

          {loadingRooms ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress size={28} />
            </Box>
          ) : rooms.length === 0 ? (
            <EmptyState title="No rooms yet" description="Add a room to this hostel to start allocating students." />
          ) : (
            <Grid container spacing={2}>
              {rooms.map((room) => (
                <Grid item xs={12} sm={6} md={4} lg={3} key={room.id}>
                  <Card variant="outlined">
                    <CardContent>
                      <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                        <Box>
                          <Typography variant="subtitle1" fontWeight={700}>
                            Room {room.roomNumber}
                          </Typography>
                          <Chip
                            size="small"
                            sx={{ mt: 1 }}
                            label={`${room.occupiedCount}/${room.capacity} occupied`}
                            color={room.occupiedCount >= room.capacity ? 'error' : 'success'}
                            variant="outlined"
                          />
                        </Box>
                        <Stack direction="row" spacing={0.5}>
                          <Tooltip title="Edit">
                            <IconButton
                              size="small"
                              onClick={() => {
                                setEditingRoom(room);
                                setRoomFormOpen(true);
                              }}
                            >
                              <EditOutlinedIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton size="small" onClick={() => setDeleteRoomTarget(room)}>
                              <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </>
      )}

      <HostelFormDialog
        open={hostelFormOpen}
        editing={editingHostel}
        saving={savingHostel}
        onClose={() => {
          setHostelFormOpen(false);
          setEditingHostel(null);
        }}
        onSubmit={handleSaveHostel}
      />
      <ConfirmDialog
        open={!!deleteHostelTarget}
        title={`Delete ${deleteHostelTarget?.name ?? ''}`}
        message="This may fail if the hostel has rooms or residents."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDeleteHostel}
        onCancel={() => setDeleteHostelTarget(null)}
      />

      <RoomFormDialog
        open={roomFormOpen}
        editing={editingRoom}
        saving={savingRoom}
        onClose={() => {
          setRoomFormOpen(false);
          setEditingRoom(null);
        }}
        onSubmit={handleSaveRoom}
      />
      <ConfirmDialog
        open={!!deleteRoomTarget}
        title={`Delete Room ${deleteRoomTarget?.roomNumber ?? ''}`}
        message="This may fail if students are currently allocated to this room."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDeleteRoom}
        onCancel={() => setDeleteRoomTarget(null)}
      />
    </Box>
  );
}

export default RoomsPage;
