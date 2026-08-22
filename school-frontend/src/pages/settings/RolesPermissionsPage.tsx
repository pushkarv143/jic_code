import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Divider from '@mui/material/Divider';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import Tooltip from '@mui/material/Tooltip';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import accessApi, { menusApi, rolesApi, type PermissionRow } from '@/api/accessApi';
import { useAccess } from '@/access/AccessProvider';
import { formatRoleLabel } from '@/utils/format';
import type { MenuEntry, OrgModule, Permission } from '@/types';

interface RoleRow {
  id: number;
  name: string;
  description: string | null;
}

/** SUPER_ADMIN always holds everything; the API rejects edits to it. */
const PROTECTED_ROLE = 'SUPER_ADMIN';

/**
 * Runtime authorization configuration — the screen that makes the permission model
 * a setting rather than a migration.
 *
 * <p>Two tabs, two different scopes:
 *
 * <ul>
 *   <li><b>Roles</b> — per-role permission grants. Fine-grained: which of the ~60
 *       permissions this role holds.</li>
 *   <li><b>Modules</b> — whole functional areas. Coarse: switching one off strips
 *       every permission in it from every role at once, and hides the matching menu
 *       entries. The API and the UI go dark together, because the backend applies
 *       the same filter when it builds a caller's authorities.</li>
 * </ul>
 *
 * <p>Both were previously only changeable by editing seed SQL and redeploying:
 * `RoleController` had no write endpoint at all and the menu was hardcoded role
 * `roles` arrays that used to sit in the clients' own nav config.
 */
export function RolesPermissionsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const { refresh } = useAccess();

  const [tab, setTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [roles, setRoles] = useState<RoleRow[]>([]);
  const [catalogue, setCatalogue] = useState<PermissionRow[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [granted, setGranted] = useState<Set<Permission>>(new Set());
  const [modules, setModules] = useState<OrgModule[]>([]);
  /** The menu catalogue, as sections with their entries. */
  const [menuTree, setMenuTree] = useState<MenuEntry[]>([]);
  /** Menu ids assigned to the selected role. */
  const [assignedMenus, setAssignedMenus] = useState<Set<number>>(new Set());

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [roleList, permissionCatalogue, moduleList, menus] = await Promise.all([
        rolesApi.list(),
        rolesApi.permissionCatalogue(),
        accessApi.getModules(),
        menusApi.catalogue(),
      ]);
      setRoles(roleList);
      setCatalogue(permissionCatalogue);
      setModules(moduleList);
      setMenuTree(menus);
      // Open on the first editable role rather than SUPER_ADMIN, whose board is
      // read-only and would look broken as a first impression.
      const firstEditable = roleList.find((role) => role.name !== PROTECTED_ROLE) ?? roleList[0];
      setSelectedRoleId(firstEditable?.id ?? null);
    } catch {
      enqueueSnackbar('Could not load roles and permissions.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [enqueueSnackbar]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  // Reload the selected role's grants and menus whenever the selection changes.
  // Both together: they are edited on two tabs of one screen, and fetching them
  // separately let one tab show a stale board after the other saved.
  useEffect(() => {
    if (selectedRoleId == null) return;
    let cancelled = false;
    (async () => {
      const [rows, menuIds] = await Promise.all([
        rolesApi.permissionsFor(selectedRoleId).catch(() => []),
        menusApi.assignedTo(selectedRoleId).catch(() => []),
      ]);
      if (cancelled) return;
      setGranted(new Set(rows.map((row) => row.name)));
      setAssignedMenus(new Set(menuIds));
    })();
    return () => {
      cancelled = true;
    };
  }, [selectedRoleId]);

  const selectedRole = roles.find((role) => role.id === selectedRoleId) ?? null;
  const isProtected = selectedRole?.name === PROTECTED_ROLE;

  /** Permission catalogue grouped by module, so the board reads as areas of the product. */
  const byModule = useMemo(() => {
    const map = new Map<string, PermissionRow[]>();
    catalogue.forEach((row) => {
      const list = map.get(row.module) ?? [];
      list.push(row);
      map.set(row.module, list);
    });
    return [...map.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [catalogue]);

  const moduleLabel = useCallback(
    (key: string) => modules.find((module) => module.moduleKey === key)?.label ?? key,
    [modules],
  );

  const disabledModules = useMemo(
    () => new Set(modules.filter((module) => !module.enabled).map((module) => module.moduleKey)),
    [modules],
  );

  function toggle(name: Permission) {
    setGranted((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  }

  function toggleModuleBlock(moduleKey: string, rows: PermissionRow[], on: boolean) {
    setGranted((prev) => {
      const next = new Set(prev);
      rows.forEach((row) => (on ? next.add(row.name) : next.delete(row.name)));
      return next;
    });
  }

  async function saveRole() {
    if (selectedRoleId == null || isProtected) return;
    setSaving(true);
    try {
      await rolesApi.replacePermissions(selectedRoleId, [...granted]);
      enqueueSnackbar(`${formatRoleLabel(selectedRole?.name ?? '')} permissions saved.`, {
        variant: 'success',
      });
      // The signed-in user may have just changed their own role's grants, so their
      // menu and page actions need to catch up without a re-login.
      await refresh();
    } catch (error) {
      const message =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not save the permissions.';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setSaving(false);
    }
  }

  /**
   * Saves the selected role's menu assignment.
   *
   * SUPER_ADMIN is editable here, unlike its permissions: an administrator may
   * legitimately want a shorter menu without giving up any authority, and the
   * assignment cannot lock anyone out — Settings can always be reassigned from
   * another admin account, and the API refuses none of it.
   */
  async function saveMenus() {
    if (selectedRoleId == null) return;
    setSaving(true);
    try {
      const saved = await menusApi.replaceFor(selectedRoleId, [...assignedMenus]);
      setAssignedMenus(new Set(saved));
      enqueueSnackbar(`${formatRoleLabel(selectedRole?.name ?? '')} menus saved.`, {
        variant: 'success',
      });
      // The signed-in user may have just changed their own role's menu, so the
      // sidebar has to catch up without a re-login.
      await refresh();
    } catch (error) {
      const message =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not save the menus.';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setSaving(false);
    }
  }

  function toggleMenu(menu: MenuEntry, on: boolean) {
    setAssignedMenus((prev) => {
      const next = new Set(prev);
      if (on) next.add(menu.id);
      else next.delete(menu.id);
      return next;
    });
  }

  function toggleMenuSection(section: MenuEntry, on: boolean) {
    setAssignedMenus((prev) => {
      const next = new Set(prev);
      // The heading goes with its children: a heading with nothing under it is
      // never rendered anyway, and leaving it assigned would be invisible state.
      [section, ...section.children].forEach((menu) => (on ? next.add(menu.id) : next.delete(menu.id)));
      return next;
    });
  }

  async function toggleModule(module: OrgModule, enabled: boolean) {
    setSaving(true);
    try {
      setModules(await accessApi.updateModules({ [module.moduleKey]: enabled }));
      enqueueSnackbar(`${module.label} ${enabled ? 'enabled' : 'disabled'}.`, { variant: 'success' });
      await refresh();
    } catch (error) {
      const message =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Could not update that module.';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <PageLoader />;

  return (
    <Box>
      <PageHeader
        title="Roles & Permissions"
        subtitle="Decide what each role may do, and which modules this school runs"
        breadcrumbs={[
          { label: 'Dashboard', to: '/app/dashboard' },
          { label: 'Settings', to: '/app/settings' },
          { label: 'Roles & Permissions' },
        ]}
      />

      <Tabs value={tab} onChange={(_e, next) => setTab(next)} sx={{ mb: 2 }}>
        <Tab label="Role permissions" />
        <Tab label={`Menus (${assignedMenus.size} assigned)`} />
        <Tab label={`Modules (${modules.filter((m) => m.enabled).length}/${modules.length} on)`} />
      </Tabs>

      {tab === 0 && (
        <Grid container spacing={2.5}>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent sx={{ p: 0 }}>
                <List dense disablePadding>
                  {roles.map((role) => (
                    <ListItemButton
                      key={role.id}
                      selected={role.id === selectedRoleId}
                      onClick={() => setSelectedRoleId(role.id)}
                    >
                      <ListItemText
                        primary={formatRoleLabel(role.name)}
                        secondary={role.description ?? undefined}
                      />
                      {role.name === PROTECTED_ROLE && (
                        <Tooltip title="Always holds every permission">
                          <LockOutlinedIcon fontSize="small" color="disabled" />
                        </Tooltip>
                      )}
                    </ListItemButton>
                  ))}
                </List>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={9}>
            <Card>
              <CardContent>
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  spacing={2}
                  alignItems={{ sm: 'center' }}
                  justifyContent="space-between"
                  sx={{ mb: 2 }}
                >
                  <Box>
                    <Typography variant="h6">
                      {selectedRole ? formatRoleLabel(selectedRole.name) : 'Select a role'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {granted.size} permission{granted.size === 1 ? '' : 's'} granted
                    </Typography>
                  </Box>
                  <Button
                    variant="contained"
                    onClick={() => void saveRole()}
                    disabled={saving || isProtected || selectedRoleId == null}
                  >
                    {saving ? 'Saving…' : 'Save permissions'}
                  </Button>
                </Stack>

                {isProtected && (
                  <Alert severity="info" sx={{ mb: 2 }}>
                    {PROTECTED_ROLE} always holds every permission and cannot be edited. This keeps at
                    least one account able to grant permissions back, whatever else is changed here.
                  </Alert>
                )}

                <Alert severity="warning" sx={{ mb: 2 }}>
                  Saving replaces this role&apos;s grants with exactly what is ticked — anything
                  unticked is revoked. It takes effect on the affected users&apos; next request.
                </Alert>

                {byModule.map(([moduleKey, rows]) => {
                  const allOn = rows.every((row) => granted.has(row.name));
                  const moduleOff = disabledModules.has(moduleKey);
                  return (
                    <Box key={moduleKey} sx={{ mb: 2.5 }}>
                      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                        <Typography variant="subtitle2">{moduleLabel(moduleKey)}</Typography>
                        {moduleOff && (
                          <Tooltip title="This module is switched off, so these grants have no effect until it is switched back on">
                            <Chip size="small" color="warning" variant="outlined" label="module off" />
                          </Tooltip>
                        )}
                        {!isProtected && (
                          <Button
                            size="small"
                            onClick={() => toggleModuleBlock(moduleKey, rows, !allOn)}
                          >
                            {allOn ? 'Clear all' : 'Select all'}
                          </Button>
                        )}
                      </Stack>
                      <Grid container spacing={0.5}>
                        {rows.map((row) => (
                          <Grid item xs={12} sm={6} md={4} key={row.name}>
                            <Tooltip title={row.description ?? ''} placement="top-start">
                              <FormControlLabel
                                control={
                                  <Checkbox
                                    size="small"
                                    checked={isProtected || granted.has(row.name)}
                                    disabled={isProtected}
                                    onChange={() => toggle(row.name)}
                                  />
                                }
                                label={<Typography variant="body2">{row.name}</Typography>}
                              />
                            </Tooltip>
                          </Grid>
                        ))}
                      </Grid>
                      <Divider sx={{ mt: 1 }} />
                    </Box>
                  );
                })}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {tab === 1 && (
        <Grid container spacing={2.5}>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent sx={{ p: 0 }}>
                <List dense disablePadding>
                  {roles.map((role) => (
                    <ListItemButton
                      key={role.id}
                      selected={role.id === selectedRoleId}
                      onClick={() => setSelectedRoleId(role.id)}
                    >
                      <ListItemText primary={formatRoleLabel(role.name)} />
                    </ListItemButton>
                  ))}
                </List>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={9}>
            <Card>
              <CardContent>
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  spacing={2}
                  alignItems={{ sm: 'center' }}
                  justifyContent="space-between"
                  sx={{ mb: 2 }}
                >
                  <Box>
                    <Typography variant="h6">
                      {selectedRole ? formatRoleLabel(selectedRole.name) : 'Select a role'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {assignedMenus.size} menu{assignedMenus.size === 1 ? '' : 's'} assigned
                    </Typography>
                  </Box>
                  <Button
                    variant="contained"
                    onClick={() => void saveMenus()}
                    disabled={saving || selectedRoleId == null}
                  >
                    {saving ? 'Saving…' : 'Save menus'}
                  </Button>
                </Stack>

                <Alert severity="info" sx={{ mb: 2 }}>
                  A menu decides what <strong>appears</strong>, never what may be done. An entry still
                  needs its module switched on and its permission granted — so assigning one to a role
                  that lacks the permission behind it shows nothing at all. The permission each entry
                  needs is noted beside it.
                </Alert>

                {menuTree.map((section) => {
                  const childIds = section.children.map((child) => child.id);
                  const assignedCount = childIds.filter((id) => assignedMenus.has(id)).length;
                  return (
                    <Box key={section.id} sx={{ mb: 2.5 }}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            checked={assignedCount === childIds.length && childIds.length > 0}
                            indeterminate={assignedCount > 0 && assignedCount < childIds.length}
                            onChange={(e) => toggleMenuSection(section, e.target.checked)}
                          />
                        }
                        label={
                          <Typography variant="subtitle2">
                            {section.label}{' '}
                            <Typography component="span" variant="caption" color="text.secondary">
                              ({assignedCount}/{childIds.length})
                            </Typography>
                          </Typography>
                        }
                      />
                      <Box sx={{ pl: 4, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                        {section.children.map((menu) => (
                          <FormControlLabel
                            key={menu.id}
                            sx={{ width: { xs: '100%', sm: '48%', lg: '31%' }, mr: 0 }}
                            control={
                              <Checkbox
                                size="small"
                                checked={assignedMenus.has(menu.id)}
                                onChange={(e) => toggleMenu(menu, e.target.checked)}
                              />
                            }
                            label={
                              <Box>
                                <Typography variant="body2" component="span">
                                  {menu.label}
                                </Typography>
                                {!menu.enabled && (
                                  <Typography variant="caption" color="warning.main" display="block">
                                    switched off for everyone
                                  </Typography>
                                )}
                                {menu.requiredPermission && (
                                  <Typography variant="caption" color="text.secondary" display="block">
                                    needs {menu.requiredPermission}
                                  </Typography>
                                )}
                                {menu.requiresHomeroom && (
                                  <Typography variant="caption" color="text.secondary" display="block">
                                    only for a teacher holding a class
                                  </Typography>
                                )}
                              </Box>
                            }
                          />
                        ))}
                      </Box>
                    </Box>
                  );
                })}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {tab === 2 && (
        <Card>
          <CardContent>
            <Alert severity="warning" sx={{ mb: 2 }}>
              Switching a module off withdraws every permission in it from every role, hides its menu
              entries, and makes its API endpoints refuse requests. Core modules cannot be switched
              off — without them there would be no way to switch anything back on.
            </Alert>
            <Grid container spacing={1}>
              {modules.map((module) => (
                <Grid item xs={12} md={6} key={module.moduleKey}>
                  <Card variant="outlined">
                    <CardContent sx={{ py: 1.5 }}>
                      <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                        <Box sx={{ minWidth: 0 }}>
                          <Stack direction="row" spacing={1} alignItems="center">
                            <Typography variant="subtitle2" noWrap>
                              {module.label}
                            </Typography>
                            {module.core && (
                              <Tooltip title="Required — cannot be switched off">
                                <Chip size="small" variant="outlined" label="core" />
                              </Tooltip>
                            )}
                          </Stack>
                          <Typography variant="caption" color="text.secondary">
                            {module.description ?? module.moduleKey} · {module.permissionCount} permission
                            {module.permissionCount === 1 ? '' : 's'}
                          </Typography>
                        </Box>
                        <Switch
                          checked={module.enabled}
                          disabled={saving || module.core}
                          onChange={(e) => void toggleModule(module, e.target.checked)}
                        />
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}

export default RolesPermissionsPage;
