import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Grid from '@mui/material/Grid';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Tooltip from '@mui/material/Tooltip';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import { menusApi, rolesApi } from '@/api/accessApi';
import { useAccess } from '@/access/AccessProvider';
import { formatRoleLabel } from '@/utils/format';
import type { MenuEntry } from '@/types';

/**
 * Privileges — which menus each role is offered.
 *
 * <p>Its own screen under its own module rather than a third tab on Roles &
 * Permissions, because it is a different authority. ROLE_MANAGE is held by the
 * principal so they can adjust what a teacher may do; deciding what appears on
 * every role's navigation shapes what the whole organisation sees, and an empty
 * assignment empties somebody's sidebar. PRIVILEGE_VIEW and PRIVILEGE_MANAGE are
 * granted to SUPER_ADMIN alone — but they are permissions, not a hard-coded role,
 * so an organisation that wants to delegate can.
 *
 * <p><b>What this screen cannot do.</b> A menu assignment is visibility, never
 * authority. Three gates still apply on top of it — the module being switched on,
 * a homeroom assignment for My Class, and the permission behind the screen — so
 * ticking a box here can only ever *narrow* what a role sees. Assigning a menu to
 * a role that lacks the permission behind it shows nothing at all, which is why
 * each entry names the grant it needs and rows that would be dead are called out.
 */
export function PrivilegesPage() {
  const { enqueueSnackbar } = useSnackbar();
  const { refresh } = useAccess();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [roles, setRoles] = useState<Array<{ id: number; name: string; description: string | null }>>([]);
  const [menuTree, setMenuTree] = useState<MenuEntry[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [assigned, setAssigned] = useState<Set<number>>(new Set());
  /** Grants the selected role holds — used to flag assignments that would show nothing. */
  const [granted, setGranted] = useState<Set<string>>(new Set());

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [roleList, menus] = await Promise.all([rolesApi.list(), menusApi.catalogue()]);
      setRoles(roleList);
      setMenuTree(menus);
      setSelectedRoleId((current) => current ?? roleList[0]?.id ?? null);
    } catch {
      enqueueSnackbar('Could not load the menu catalogue.', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [enqueueSnackbar]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  // The assignment and the role's grants together: the board needs both to say
  // which ticked boxes would actually show something.
  useEffect(() => {
    if (selectedRoleId == null) return;
    let cancelled = false;
    (async () => {
      const [menuIds, permissions] = await Promise.all([
        menusApi.assignedTo(selectedRoleId).catch(() => []),
        rolesApi.permissionsFor(selectedRoleId).catch(() => []),
      ]);
      if (cancelled) return;
      setAssigned(new Set(menuIds));
      setGranted(new Set(permissions.map((row) => row.name)));
    })();
    return () => {
      cancelled = true;
    };
  }, [selectedRoleId]);

  const selectedRole = roles.find((role) => role.id === selectedRoleId) ?? null;
  // SUPER_ADMIN bypasses the permission gate, matching the backend, so nothing is
  // ever flagged as dead for them.
  const bypassesPermissions = selectedRole?.name === 'SUPER_ADMIN';

  /**
   * Assigned entries whose permission the role does not hold.
   *
   * Not an error and not blocked — a grant can be added afterwards — but these are
   * the rows to look at first when someone reports a menu they cannot see.
   */
  const deadAssignments = useMemo(() => {
    if (bypassesPermissions) return [];
    return menuTree
      .flatMap((section) => section.children)
      .filter((menu) => assigned.has(menu.id))
      .filter((menu) => menu.requiredPermission && !granted.has(menu.requiredPermission));
  }, [menuTree, assigned, granted, bypassesPermissions]);

  function toggle(menu: MenuEntry, on: boolean) {
    setAssigned((prev) => {
      const next = new Set(prev);
      if (on) next.add(menu.id);
      else next.delete(menu.id);
      return next;
    });
  }

  function toggleSection(section: MenuEntry, on: boolean) {
    setAssigned((prev) => {
      const next = new Set(prev);
      // The heading goes with its children. A heading with nothing under it is
      // never rendered, so leaving it assigned alone would be invisible state.
      [section, ...section.children].forEach((menu) => (on ? next.add(menu.id) : next.delete(menu.id)));
      return next;
    });
  }

  async function save() {
    if (selectedRoleId == null) return;
    setSaving(true);
    try {
      const saved = await menusApi.replaceFor(selectedRoleId, [...assigned]);
      setAssigned(new Set(saved));
      enqueueSnackbar(`${formatRoleLabel(selectedRole?.name ?? '')} menus saved.`, { variant: 'success' });
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

  if (loading) return <PageLoader />;

  const assignedEntryCount = menuTree
    .flatMap((section) => section.children)
    .filter((menu) => assigned.has(menu.id)).length;

  return (
    <Box>
      <PageHeader
        title="Privileges"
        subtitle="Which menus each role is offered"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Privileges' }]}
      />

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
                    {assignedEntryCount} menu{assignedEntryCount === 1 ? '' : 's'} assigned
                  </Typography>
                </Box>
                <Button
                  variant="contained"
                  onClick={() => void save()}
                  disabled={saving || selectedRoleId == null}
                >
                  {saving ? 'Saving…' : 'Save menus'}
                </Button>
              </Stack>

              <Alert severity="info" sx={{ mb: 2 }}>
                A menu decides what <strong>appears</strong>, never what may be done. An entry still
                needs its module switched on and its permission granted, so assigning one to a role
                that holds neither shows nothing at all. Unticking every box is valid and means this
                role signs in to an empty sidebar.
              </Alert>

              {deadAssignments.length > 0 && (
                <Alert severity="warning" sx={{ mb: 2 }}>
                  {deadAssignments.length} assigned{' '}
                  {deadAssignments.length === 1 ? 'menu' : 'menus'} will not appear for this role,
                  because it does not hold the permission behind{' '}
                  {deadAssignments.length === 1 ? 'it' : 'them'}:{' '}
                  {deadAssignments.map((menu) => `${menu.label} (${menu.requiredPermission})`).join(', ')}.
                  Grant the permission on Roles &amp; Permissions, or leave the menu unassigned.
                </Alert>
              )}

              {menuTree.map((section) => {
                const childIds = section.children.map((child) => child.id);
                const count = childIds.filter((id) => assigned.has(id)).length;
                return (
                  <Box key={section.id} sx={{ mb: 2.5 }}>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={count === childIds.length && childIds.length > 0}
                          indeterminate={count > 0 && count < childIds.length}
                          onChange={(e) => toggleSection(section, e.target.checked)}
                        />
                      }
                      label={
                        <Typography variant="subtitle2">
                          {section.label}{' '}
                          <Typography component="span" variant="caption" color="text.secondary">
                            ({count}/{childIds.length})
                          </Typography>
                        </Typography>
                      }
                    />
                    <Divider sx={{ mb: 1 }} />
                    <Box sx={{ pl: 4, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                      {section.children.map((menu) => {
                        const isDead =
                          !bypassesPermissions &&
                          !!menu.requiredPermission &&
                          !granted.has(menu.requiredPermission);
                        return (
                          <FormControlLabel
                            key={menu.id}
                            sx={{ width: { xs: '100%', sm: '48%', lg: '31%' }, mr: 0, alignItems: 'flex-start' }}
                            control={
                              <Checkbox
                                size="small"
                                checked={assigned.has(menu.id)}
                                onChange={(e) => toggle(menu, e.target.checked)}
                              />
                            }
                            label={
                              <Box sx={{ pt: 0.5 }}>
                                <Typography variant="body2" component="span">
                                  {menu.label}
                                </Typography>
                                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
                                  {!menu.enabled && (
                                    <Tooltip title="Switched off for every role — the row is kept so its assignments survive">
                                      <Chip size="small" color="warning" variant="outlined" label="retired" />
                                    </Tooltip>
                                  )}
                                  {menu.requiredPermission && (
                                    <Tooltip title={`Hidden unless the role holds ${menu.requiredPermission}`}>
                                      <Chip
                                        size="small"
                                        variant="outlined"
                                        color={isDead && assigned.has(menu.id) ? 'warning' : 'default'}
                                        label={menu.requiredPermission}
                                      />
                                    </Tooltip>
                                  )}
                                  {menu.moduleKey && (
                                    <Tooltip title={`Hidden for everyone when the ${menu.moduleKey} module is off`}>
                                      <Chip size="small" variant="outlined" label={menu.moduleKey} />
                                    </Tooltip>
                                  )}
                                  {menu.requiresHomeroom && (
                                    <Tooltip title="Only for a teacher who actually holds a class">
                                      <Chip size="small" variant="outlined" label="homeroom" />
                                    </Tooltip>
                                  )}
                                </Box>
                              </Box>
                            }
                          />
                        );
                      })}
                    </Box>
                  </Box>
                );
              })}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default PrivilegesPage;
