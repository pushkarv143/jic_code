import { useEffect, useMemo, useState, type SyntheticEvent } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import ToggleButton from '@mui/material/ToggleButton';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import type { GridColDef } from '@mui/x-data-grid';
import { useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import SecurityOutlinedIcon from '@mui/icons-material/SecurityOutlined';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import CloudDownloadOutlinedIcon from '@mui/icons-material/CloudDownloadOutlined';
import CircularProgress from '@mui/material/CircularProgress';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import DataTable from '@/components/common/DataTable';
import settingsApi from '@/api/settingsApi';
import { useAppSelector } from '@/store/hooks';
import { useThemeMode } from '@/theme/ThemeModeProvider';
import { downloadBlob } from '@/utils/downloadBlob';
import type { RoleInfo, SchoolInfo, SystemSetting } from '@/types';
import RolePermissionsDialog from './components/RolePermissionsDialog';
import AuditLogsTab from './components/AuditLogsTab';

const TABS = [
  { value: 'school-info', label: 'School Information', icon: <BusinessOutlinedIcon fontSize="small" /> },
  { value: 'system', label: 'System Settings', icon: <TuneOutlinedIcon fontSize="small" /> },
  { value: 'roles', label: 'Roles & Permissions', icon: <SecurityOutlinedIcon fontSize="small" /> },
  { value: 'appearance', label: 'Appearance', icon: <PaletteOutlinedIcon fontSize="small" /> },
  { value: 'audit-logs', label: 'Audit Logs', icon: <HistoryOutlinedIcon fontSize="small" /> },
  // Backup export is restricted to SUPER_ADMIN — filtered out of the tab list for PRINCIPAL below.
  { value: 'backup', label: 'Backup', icon: <CloudDownloadOutlinedIcon fontSize="small" />, superAdminOnly: true },
];

interface SchoolInfoFormValues {
  name: string;
  address: string;
  phone: string;
  email: string;
  establishedYear: string;
  affiliationNumber: string;
}

/** School Information (bound to /settings/school-info), System Settings (key-value editor bound to
 * /settings/system), Roles & Permissions (read-only, drills into /roles/{id}/permissions) and Appearance.
 * PRINCIPAL gets read-only access — fields/buttons are disabled rather than hiding the page. */
export function SettingsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const { mode, setMode } = useThemeMode();
  const role = useAppSelector((state) => state.auth.user?.role);
  const readOnly = role === 'PRINCIPAL';

  const [tab, setTab] = useState('school-info');
  const visibleTabs = useMemo(() => TABS.filter((t) => !t.superAdminOnly || role === 'SUPER_ADMIN'), [role]);

  const [backupDownloading, setBackupDownloading] = useState(false);
  const handleDownloadBackup = async () => {
    setBackupDownloading(true);
    try {
      const blob = await settingsApi.exportBackup();
      downloadBlob(blob, `sms-backup-${new Date().toISOString().slice(0, 10)}.json`);
      enqueueSnackbar('Backup downloaded.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not download the backup.', { variant: 'error' });
    } finally {
      setBackupDownloading(false);
    }
  };

  // --- School Information ---
  const [schoolInfoLoading, setSchoolInfoLoading] = useState(true);
  const [savingSchoolInfo, setSavingSchoolInfo] = useState(false);
  const {
    register: registerSchoolInfo,
    handleSubmit: handleSchoolInfoSubmit,
    reset: resetSchoolInfoForm,
    formState: { errors: schoolInfoErrors },
  } = useForm<SchoolInfoFormValues>({
    defaultValues: { name: '', address: '', phone: '', email: '', establishedYear: '', affiliationNumber: '' },
  });

  useEffect(() => {
    setSchoolInfoLoading(true);
    settingsApi
      .getSchoolInfo()
      .then((res) => {
        const info = res.data;
        resetSchoolInfoForm({
          name: info?.name ?? '',
          address: info?.address ?? '',
          phone: info?.phone ?? '',
          email: info?.email ?? '',
          establishedYear: info?.establishedYear ? String(info.establishedYear) : '',
          affiliationNumber: info?.affiliationNumber ?? '',
        });
      })
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load school information.', { variant: 'error' });
      })
      .finally(() => setSchoolInfoLoading(false));
  }, [resetSchoolInfoForm, enqueueSnackbar]);

  const onSaveSchoolInfo = async (values: SchoolInfoFormValues) => {
    setSavingSchoolInfo(true);
    try {
      const payload: SchoolInfo = {
        name: values.name.trim(),
        address: values.address.trim(),
        phone: values.phone.trim(),
        email: values.email.trim(),
        establishedYear: values.establishedYear ? Number(values.establishedYear) : null,
        affiliationNumber: values.affiliationNumber.trim() || null,
      };
      await settingsApi.updateSchoolInfo(payload);
      enqueueSnackbar('School information saved.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save school information.', { variant: 'error' });
    } finally {
      setSavingSchoolInfo(false);
    }
  };

  // --- System Settings ---
  const [systemSettings, setSystemSettings] = useState<SystemSetting[]>([]);
  const [systemLoading, setSystemLoading] = useState(true);
  const [savingSystem, setSavingSystem] = useState(false);

  useEffect(() => {
    setSystemLoading(true);
    settingsApi
      .getSystemSettings()
      .then((res) => setSystemSettings(res.data))
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load system settings.', { variant: 'error' });
        setSystemSettings([]);
      })
      .finally(() => setSystemLoading(false));
  }, [enqueueSnackbar]);

  const updateSystemValue = (key: string, value: string) => {
    setSystemSettings((prev) => prev.map((s) => (s.key === key ? { ...s, value } : s)));
  };

  const handleSaveSystemSettings = async () => {
    setSavingSystem(true);
    try {
      const res = await settingsApi.updateSystemSettings(systemSettings);
      setSystemSettings(res.data);
      enqueueSnackbar('System settings saved.', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err?.response?.data?.message ?? 'Could not save system settings.', { variant: 'error' });
    } finally {
      setSavingSystem(false);
    }
  };

  // --- Roles & Permissions ---
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [rolesLoading, setRolesLoading] = useState(true);
  const [permissionsTarget, setPermissionsTarget] = useState<RoleInfo | null>(null);

  useEffect(() => {
    setRolesLoading(true);
    settingsApi
      .listRoles()
      .then((res) => setRoles(res.data))
      .catch((err) => {
        enqueueSnackbar(err?.response?.data?.message ?? 'Could not load roles.', { variant: 'error' });
        setRoles([]);
      })
      .finally(() => setRolesLoading(false));
  }, [enqueueSnackbar]);

  const roleColumns: GridColDef<RoleInfo>[] = useMemo(
    () => [
      { field: 'name', headerName: 'Role', flex: 1, minWidth: 180 },
      { field: 'description', headerName: 'Description', flex: 2, minWidth: 220, valueGetter: (_v, row) => row.description || '—' },
      {
        field: 'actions',
        headerName: 'Permissions',
        width: 130,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Tooltip title="View permissions">
            <IconButton size="small" onClick={() => setPermissionsTarget(params.row)}>
              <VisibilityOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
      },
    ],
    [],
  );

  const handleChange = (_e: SyntheticEvent, value: string) => setTab(value);

  return (
    <Box>
      <PageHeader
        title="Settings"
        subtitle="Configure school information, system settings and role permissions"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Settings' }]}
      />

      {readOnly && (
        <Alert severity="info" sx={{ mb: 2.5 }}>
          You have read-only access to Settings. Contact a Super Admin to make changes.
        </Alert>
      )}

      <Card sx={{ mb: 2.5 }}>
        <Tabs value={tab} onChange={handleChange} variant="scrollable" scrollButtons="auto" sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
          {visibleTabs.map((t) => (
            <Tab key={t.value} value={t.value} label={t.label} icon={t.icon} iconPosition="start" sx={{ minHeight: 48 }} />
          ))}
        </Tabs>
      </Card>

      {tab === 'school-info' && (
        schoolInfoLoading ? (
          <PageLoader label="Loading school information..." />
        ) : (
          <Card>
            <CardHeader title="School Information" subheader="Basic details shown across the platform" />
            <CardContent>
              <Box component="form" onSubmit={handleSchoolInfoSubmit(onSaveSchoolInfo)} noValidate>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="School Name"
                      fullWidth
                      disabled={readOnly}
                      {...registerSchoolInfo('name', { required: 'School name is required' })}
                      error={!!schoolInfoErrors.name}
                      helperText={schoolInfoErrors.name?.message}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField label="Affiliation Number" fullWidth disabled={readOnly} {...registerSchoolInfo('affiliationNumber')} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField label="Established Year" fullWidth disabled={readOnly} {...registerSchoolInfo('establishedYear')} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField label="Contact Phone" fullWidth disabled={readOnly} {...registerSchoolInfo('phone')} />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField label="Contact Email" fullWidth disabled={readOnly} {...registerSchoolInfo('email')} />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField label="Address" fullWidth multiline minRows={2} disabled={readOnly} {...registerSchoolInfo('address')} />
                  </Grid>
                </Grid>
                <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                  <Button type="submit" variant="contained" startIcon={<SaveOutlinedIcon />} disabled={readOnly || savingSchoolInfo}>
                    Save Changes
                  </Button>
                </Box>
              </Box>
            </CardContent>
          </Card>
        )
      )}

      {tab === 'system' && (
        systemLoading ? (
          <PageLoader label="Loading system settings..." />
        ) : (
          <Card>
            <CardHeader title="System Settings" subheader="Key-value configuration used across the platform" />
            <CardContent>
              {systemSettings.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No system settings are configured yet.
                </Typography>
              ) : (
                <Stack spacing={2}>
                  {systemSettings.map((setting) => (
                    <Grid container spacing={2} alignItems="center" key={setting.key}>
                      <Grid item xs={12} sm={4}>
                        <Typography variant="body2" fontWeight={600} sx={{ wordBreak: 'break-word' }}>
                          {setting.key}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={8}>
                        <TextField
                          fullWidth
                          size="small"
                          value={setting.value}
                          disabled={readOnly}
                          onChange={(e) => updateSystemValue(setting.key, e.target.value)}
                        />
                      </Grid>
                    </Grid>
                  ))}
                </Stack>
              )}
              <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  startIcon={<SaveOutlinedIcon />}
                  disabled={readOnly || savingSystem || systemSettings.length === 0}
                  onClick={handleSaveSystemSettings}
                >
                  Save Changes
                </Button>
              </Box>
            </CardContent>
          </Card>
        )
      )}

      {tab === 'roles' && (
        <Card>
          <CardHeader title="Roles & Permissions" subheader="Click a role to see its assigned permissions" />
          <CardContent>
            <DataTable
              rows={roles}
              columns={roleColumns}
              loading={rolesLoading}
              getRowId={(row) => row.id}
              emptyTitle="No roles found"
            />
          </CardContent>
        </Card>
      )}

      {tab === 'appearance' && (
        <Card>
          <CardHeader title="Appearance" subheader="Choose how the application looks" />
          <CardContent>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Theme mode
            </Typography>
            <ToggleButtonGroup exclusive value={mode} onChange={(_e, value) => value && setMode(value)} sx={{ maxWidth: 320 }}>
              <ToggleButton value="light">
                <LightModeOutlinedIcon sx={{ mr: 1 }} fontSize="small" /> Light
              </ToggleButton>
              <ToggleButton value="dark">
                <DarkModeOutlinedIcon sx={{ mr: 1 }} fontSize="small" /> Dark
              </ToggleButton>
            </ToggleButtonGroup>
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1.5, display: 'block' }}>
              Your preference is saved to this browser and applied automatically next time.
            </Typography>
          </CardContent>
        </Card>
      )}

      {tab === 'audit-logs' && <AuditLogsTab />}

      {tab === 'backup' && role === 'SUPER_ADMIN' && (
        <Card>
          <CardHeader title="Backup" subheader="Download a snapshot of configuration and reference data" />
          <CardContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              This downloads a JSON export of school configuration and reference data (not a full database dump).
              Keep it somewhere safe — it can be handed to support if the system ever needs to be restored.
            </Typography>
            <Button
              variant="contained"
              startIcon={backupDownloading ? <CircularProgress size={16} color="inherit" /> : <CloudDownloadOutlinedIcon />}
              onClick={handleDownloadBackup}
              disabled={backupDownloading}
            >
              Download Backup
            </Button>
          </CardContent>
        </Card>
      )}

      <RolePermissionsDialog open={!!permissionsTarget} role={permissionsTarget} onClose={() => setPermissionsTarget(null)} />
    </Box>
  );
}

export default SettingsPage;
