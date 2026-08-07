import { useCallback, useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import AltRouteOutlinedIcon from '@mui/icons-material/AltRouteOutlined';
import type { GridColDef } from '@mui/x-data-grid';
import { useSnackbar } from 'notistack';
import DataTable from '@/components/common/DataTable';
import ConfirmDialog from '@/components/common/ConfirmDialog';
import transportApi from '@/api/transportApi';
import type { Bus, Driver, Route } from '@/types';
import DriverFormDialog from './components/DriverFormDialog';
import BusFormDialog from './components/BusFormDialog';
import RouteFormDialog from './components/RouteFormDialog';
import RouteDetailDialog from './components/RouteDetailDialog';

function DriversTab({ drivers, loading, onChanged }: { drivers: Driver[]; loading: boolean; onChanged: () => void }) {
  const { enqueueSnackbar } = useSnackbar();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Driver | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Driver | null>(null);

  const handleSave = async (values: Parameters<typeof transportApi.drivers.create>[0]) => {
    setSaving(true);
    try {
      if (editing) {
        await transportApi.drivers.update(editing.id, values);
        enqueueSnackbar('Driver updated.', { variant: 'success' });
      } else {
        await transportApi.drivers.create(values);
        enqueueSnackbar('Driver added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this driver.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await transportApi.drivers.remove(deleteTarget.id);
      enqueueSnackbar('Driver deleted.', { variant: 'success' });
      setDeleteTarget(null);
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — this driver may be assigned to a bus.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<Driver>[] = useMemo(
    () => [
      { field: 'name', headerName: 'Name', flex: 1, minWidth: 150 },
      { field: 'phone', headerName: 'Phone', width: 140 },
      { field: 'licenseNumber', headerName: 'License No.', width: 150 },
      { field: 'address', headerName: 'Address', flex: 1, minWidth: 160, valueGetter: (_v, row) => row.address ?? '-' },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 110,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => { setEditing(params.row); setFormOpen(true); }}>
                <EditOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Delete">
              <IconButton size="small" onClick={() => setDeleteTarget(params.row)}>
                <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    [],
  );

  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
          Add Driver
        </Button>
      </Stack>
      <Card>
        <DataTable rows={drivers} columns={columns} loading={loading} emptyTitle="No drivers yet" emptyDescription="Add a driver to assign them to a bus." />
      </Card>
      <DriverFormDialog open={formOpen} editing={editing} saving={saving} onClose={() => { setFormOpen(false); setEditing(null); }} onSubmit={handleSave} />
      <ConfirmDialog
        open={!!deleteTarget}
        title={`Delete ${deleteTarget?.name ?? ''}`}
        message="This may fail if the driver is currently assigned to a bus."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

function BusesTab({
  buses,
  drivers,
  routes,
  loading,
  onChanged,
}: {
  buses: Bus[];
  drivers: Driver[];
  routes: Route[];
  loading: boolean;
  onChanged: () => void;
}) {
  const { enqueueSnackbar } = useSnackbar();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Bus | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Bus | null>(null);

  const handleSave = async (values: Parameters<typeof transportApi.buses.create>[0]) => {
    setSaving(true);
    try {
      if (editing) {
        await transportApi.buses.update(editing.id, values);
        enqueueSnackbar('Bus updated.', { variant: 'success' });
      } else {
        await transportApi.buses.create(values);
        enqueueSnackbar('Bus added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this bus.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await transportApi.buses.remove(deleteTarget.id);
      enqueueSnackbar('Bus deleted.', { variant: 'success' });
      setDeleteTarget(null);
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — this bus may have routes assigned.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const routeCountFor = (busId: number) => routes.filter((r) => r.busId === busId).length;

  const columns: GridColDef<Bus>[] = useMemo(
    () => [
      { field: 'busNumber', headerName: 'Bus No.', width: 110 },
      { field: 'registrationNumber', headerName: 'Registration', width: 140 },
      { field: 'vehicleModel', headerName: 'Model', flex: 0.8, minWidth: 120, valueGetter: (_v, row) => row.vehicleModel ?? '-' },
      { field: 'capacity', headerName: 'Capacity', width: 100 },
      {
        field: 'driverName',
        headerName: 'Driver',
        flex: 1,
        minWidth: 140,
        valueGetter: (_v, row) => row.driverName ?? (row.driverId ? drivers.find((d) => d.id === row.driverId)?.name : null) ?? 'Unassigned',
      },
      {
        field: 'routeCount',
        headerName: 'Routes',
        width: 90,
        valueGetter: (_v, row) => row.routeCount ?? routeCountFor(row.id),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 110,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => { setEditing(params.row); setFormOpen(true); }}>
                <EditOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Delete">
              <IconButton size="small" onClick={() => setDeleteTarget(params.row)}>
                <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [drivers, routes],
  );

  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
          Add Bus
        </Button>
      </Stack>
      <Card>
        <DataTable rows={buses} columns={columns} loading={loading} emptyTitle="No buses yet" emptyDescription="Add a bus to the fleet to start defining routes." />
      </Card>
      <BusFormDialog open={formOpen} editing={editing} drivers={drivers} saving={saving} onClose={() => { setFormOpen(false); setEditing(null); }} onSubmit={handleSave} />
      <ConfirmDialog
        open={!!deleteTarget}
        title={`Delete ${deleteTarget?.busNumber ?? ''}`}
        message="This may fail if the bus has routes assigned to it."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  );
}

function RoutesTab({ routes, buses, loading, onChanged }: { routes: Route[]; buses: Bus[]; loading: boolean; onChanged: () => void }) {
  const { enqueueSnackbar } = useSnackbar();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Route | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Route | null>(null);
  const [detailTarget, setDetailTarget] = useState<Route | null>(null);

  const handleSave = async (values: Parameters<typeof transportApi.routes.create>[0]) => {
    setSaving(true);
    try {
      if (editing) {
        await transportApi.routes.update(editing.id, values);
        enqueueSnackbar('Route updated.', { variant: 'success' });
      } else {
        await transportApi.routes.create(values);
        enqueueSnackbar('Route added.', { variant: 'success' });
      }
      setFormOpen(false);
      setEditing(null);
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save this route.', { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await transportApi.routes.remove(deleteTarget.id);
      enqueueSnackbar('Route deleted.', { variant: 'success' });
      setDeleteTarget(null);
      onChanged();
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not delete — students may be assigned to this route.', { variant: 'error' });
      setDeleteTarget(null);
    }
  };

  const columns: GridColDef<Route>[] = useMemo(
    () => [
      { field: 'routeName', headerName: 'Route', flex: 1, minWidth: 150 },
      { field: 'busNumber', headerName: 'Bus', width: 110, valueGetter: (_v, row) => row.busNumber ?? buses.find((b) => b.id === row.busId)?.busNumber ?? `#${row.busId}` },
      { field: 'startPoint', headerName: 'Start Point', flex: 0.9, minWidth: 130 },
      { field: 'endPoint', headerName: 'End Point', flex: 0.9, minWidth: 130 },
      {
        field: 'pickupPointCount',
        headerName: 'Pickup Points',
        width: 130,
        renderCell: (params) => <Chip size="small" label={params.row.pickupPointCount ?? '—'} variant="outlined" />,
      },
      {
        field: 'actions',
        headerName: 'Actions',
        width: 140,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Tooltip title="Manage pickup points">
              <IconButton size="small" color="primary" onClick={() => setDetailTarget(params.row)}>
                <AltRouteOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => { setEditing(params.row); setFormOpen(true); }}>
                <EditOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Delete">
              <IconButton size="small" onClick={() => setDeleteTarget(params.row)}>
                <DeleteOutlineOutlinedIcon fontSize="small" color="error" />
              </IconButton>
            </Tooltip>
          </Stack>
        ),
      },
    ],
    [buses],
  );

  return (
    <Box>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => { setEditing(null); setFormOpen(true); }}>
          Add Route
        </Button>
      </Stack>
      <Card>
        <DataTable rows={routes} columns={columns} loading={loading} emptyTitle="No routes yet" emptyDescription="Add a route and define its pickup points." />
      </Card>
      <RouteFormDialog open={formOpen} editing={editing} buses={buses} saving={saving} onClose={() => { setFormOpen(false); setEditing(null); }} onSubmit={handleSave} />
      <ConfirmDialog
        open={!!deleteTarget}
        title={`Delete ${deleteTarget?.routeName ?? ''}`}
        message="This may fail if students are currently assigned to this route."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
      <RouteDetailDialog open={!!detailTarget} route={detailTarget} onClose={() => setDetailTarget(null)} onChanged={onChanged} />
    </Box>
  );
}

/** Fleet management: Drivers, Buses, and Routes (with nested pickup-point management). Restricted to management roles. */
export function FleetPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [tab, setTab] = useState(0);

  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [buses, setBuses] = useState<Bus[]>([]);
  const [routes, setRoutes] = useState<Route[]>([]);
  const [loading, setLoading] = useState(false);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [driversRes, busesRes, routesRes] = await Promise.all([
        transportApi.drivers.list(),
        transportApi.buses.list(),
        transportApi.routes.list(),
      ]);
      setDrivers(driversRes.data);
      setBuses(busesRes.data);
      setRoutes(routesRes.data);
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not load fleet data.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [enqueueSnackbar]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  return (
    <Box>
      <Card sx={{ mb: 2.5 }}>
        <Tabs value={tab} onChange={(_e: SyntheticEvent, v: number) => setTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
          <Tab label="Drivers" />
          <Tab label="Buses" />
          <Tab label="Routes" />
        </Tabs>
      </Card>
      {tab === 0 && <DriversTab drivers={drivers} loading={loading} onChanged={loadAll} />}
      {tab === 1 && <BusesTab buses={buses} drivers={drivers} routes={routes} loading={loading} onChanged={loadAll} />}
      {tab === 2 && <RoutesTab routes={routes} buses={buses} loading={loading} onChanged={loadAll} />}
    </Box>
  );
}

export default FleetPage;
