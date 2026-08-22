import { useCallback, useEffect, useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import InputAdornment from '@mui/material/InputAdornment';
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import Tooltip from '@mui/material/Tooltip';
import IconButton from '@mui/material/IconButton';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { useSnackbar } from 'notistack';
import PageHeader from '@/components/common/PageHeader';
import PageLoader from '@/components/common/PageLoader';
import { menusApi, rolesApi } from '@/api/accessApi';
import { useAccess } from '@/access/AccessProvider';
import { formatRoleLabel } from '@/utils/format';
import type { MenuEntry } from '@/types';

interface RoleRow {
  id: number;
  name: string;
  description: string | null;
}

/**
 * Privileges — which menus each role is offered.
 *
 * <p>Its own screen under its own module rather than a tab on Roles & Permissions,
 * because it is a different authority. ROLE_MANAGE is held by the principal so they
 * can adjust what a teacher may do; deciding what appears on every role's
 * navigation shapes what the whole organisation sees, and an empty assignment
 * empties somebody's sidebar. PRIVILEGE_VIEW and PRIVILEGE_MANAGE are granted to
 * SUPER_ADMIN alone — but they are permissions, not a hard-coded role, so an
 * organisation that wants to delegate can.
 *
 * <p><b>What this screen cannot do.</b> A menu assignment is visibility, never
 * authority. Three gates still apply on top — the module being switched on, a
 * homeroom assignment for My Class, and the permission behind the screen — so
 * ticking a box here can only ever *narrow* what a role sees. An assignment whose
 * permission the role lacks shows nothing, which is the one mistake this board
 * makes easy to create, so those rows are flagged individually and counted at the
 * top.
 *
 * <h2>On the layout</h2>
 *
 * <p>The first version put every role in a tall list of description cards and every
 * menu in a three-column grid with a chip per gate. Thirty menus times three chips
 * is a wall, and the answer to "what does the librarian get" was somewhere in the
 * middle of it. So: the role is a dropdown, the sections are collapsed accordions
 * carrying their own counts, and the gate labels are off by default behind one
 * switch — the counts are what you read, the detail is what you ask for.
 */
export function PrivilegesPage() {
  const { enqueueSnackbar } = useSnackbar();
  const { refresh } = useAccess();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [roles, setRoles] = useState<RoleRow[]>([]);
  const [menuTree, setMenuTree] = useState<MenuEntry[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState<number | ''>('');

  /** The board being edited. */
  const [assigned, setAssigned] = useState<Set<number>>(new Set());
  /** What the server last confirmed — the baseline for "unsaved changes". */
  const [savedAssigned, setSavedAssigned] = useState<Set<number>>(new Set());
  /** The role's grants, so an assignment that would show nothing can be flagged. */
  const [granted, setGranted] = useState<Set<string>>(new Set());

  const [search, setSearch] = useState('');
  const [showGates, setShowGates] = useState(false);
  const [expanded, setExpanded] = useState<string | false>(false);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [roleList, menus] = await Promise.all([rolesApi.list(), menusApi.catalogue()]);
      setRoles(roleList);
      setMenuTree(menus);
      // Open the first section, so the page does not read as empty on arrival —
      // six collapsed rows and nothing else looks like a failure to load.
      setExpanded((current) => current || menus[0]?.menuKey || false);
      setSelectedRoleId((current) => (current === '' ? roleList[0]?.id ?? '' : current));
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
    if (selectedRoleId === '') return;
    let cancelled = false;
    (async () => {
      const [menuIds, permissions] = await Promise.all([
        menusApi.assignedTo(selectedRoleId).catch(() => []),
        rolesApi.permissionsFor(selectedRoleId).catch(() => []),
      ]);
      if (cancelled) return;
      setAssigned(new Set(menuIds));
      setSavedAssigned(new Set(menuIds));
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

  const allEntries = useMemo(() => menuTree.flatMap((section) => section.children), [menuTree]);

  const isDead = useCallback(
    (menu: MenuEntry) =>
      !bypassesPermissions && !!menu.requiredPermission && !granted.has(menu.requiredPermission),
    [bypassesPermissions, granted],
  );

  const deadAssignments = useMemo(
    () => allEntries.filter((menu) => assigned.has(menu.id) && isDead(menu)),
    [allEntries, assigned, isDead],
  );

  const dirty = useMemo(() => {
    if (assigned.size !== savedAssigned.size) return true;
    for (const id of assigned) if (!savedAssigned.has(id)) return true;
    return false;
  }, [assigned, savedAssigned]);

  /** Sections filtered by the search box; a section with no match drops out. */
  const visibleTree = useMemo(() => {
    const needle = search.trim().toLowerCase();
    if (!needle) return menuTree;
    return menuTree
      .map((section) => ({
        ...section,
        children: section.children.filter((menu) => menu.label.toLowerCase().includes(needle)),
      }))
      .filter((section) => section.children.length > 0);
  }, [menuTree, search]);

  const assignedCount = allEntries.filter((menu) => assigned.has(menu.id)).length;

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
    if (selectedRoleId === '') return;
    setSaving(true);
    try {
      const saved = await menusApi.replaceFor(selectedRoleId, [...assigned]);
      setAssigned(new Set(saved));
      setSavedAssigned(new Set(saved));
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

  return (
    <Box>
      <PageHeader
        title="Privileges"
        subtitle="Which menus each role is offered"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Privileges' }]}
      />

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
            <TextField
              select
              size="small"
              label="Role"
              value={selectedRoleId}
              onChange={(e) => setSelectedRoleId(Number(e.target.value))}
              sx={{ minWidth: 240 }}
            >
              {roles.map((role) => (
                <MenuItem key={role.id} value={role.id}>
                  {formatRoleLabel(role.name)}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              size="small"
              placeholder="Find a menu"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              sx={{ minWidth: 200 }}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchOutlinedIcon fontSize="small" />
                    </InputAdornment>
                  ),
                },
              }}
            />

            <Box sx={{ flexGrow: 1 }} />

            <Stack direction="row" spacing={1} alignItems="center">
              <Button size="small" disabled={!dirty || saving} onClick={() => setAssigned(new Set(savedAssigned))}>
                Reset
              </Button>
              <Button
                variant="contained"
                onClick={() => void save()}
                disabled={saving || selectedRoleId === '' || !dirty}
              >
                {saving ? 'Saving…' : 'Save menus'}
              </Button>
            </Stack>
          </Stack>

          <Stack
            direction="row"
            spacing={1.5}
            alignItems="center"
            flexWrap="wrap"
            useFlexGap
            sx={{ mt: 1.5 }}
          >
            <Typography variant="body2" color="text.secondary">
              <strong>{assignedCount}</strong> of {allEntries.length} menus assigned
              {selectedRole?.description ? ` · ${selectedRole.description}` : ''}
            </Typography>
            {dirty && <Chip size="small" color="warning" variant="outlined" label="unsaved changes" />}
            <Box sx={{ flexGrow: 1 }} />
            <FormControlLabel
              control={
                <Switch size="small" checked={showGates} onChange={(e) => setShowGates(e.target.checked)} />
              }
              label={
                <Typography variant="caption" color="text.secondary">
                  Show requirements
                </Typography>
              }
            />
            <Tooltip
              title="A menu controls what appears, never what may be done. An entry also needs its module switched on and its permission granted, so assigning one to a role that holds neither shows nothing. Assigning nothing is valid: that role signs in to an empty sidebar."
              placement="left"
            >
              <IconButton size="small">
                <InfoOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        </CardContent>
      </Card>

      {deadAssignments.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {deadAssignments.length} assigned {deadAssignments.length === 1 ? 'menu' : 'menus'} will not
          appear for this role, because it does not hold the permission behind{' '}
          {deadAssignments.length === 1 ? 'it' : 'them'}:{' '}
          {deadAssignments.map((menu) => menu.label).join(', ')}. Grant the permission on Roles &amp;
          Permissions, or leave the menu unassigned.
        </Alert>
      )}

      {visibleTree.length === 0 && (
        <Card>
          <CardContent>
            <Typography variant="body2" color="text.secondary">
              No menu matches “{search}”.
            </Typography>
          </CardContent>
        </Card>
      )}

      {visibleTree.map((section) => {
        const childIds = section.children.map((child) => child.id);
        const count = childIds.filter((id) => assigned.has(id)).length;
        const all = count === childIds.length && childIds.length > 0;
        // While searching, open everything: a collapsed section that contains the
        // hit reads as no result at all.
        const open = search.trim() ? true : expanded === section.menuKey;

        return (
          <Accordion
            key={section.id}
            expanded={open}
            onChange={() => setExpanded(open && !search.trim() ? false : section.menuKey)}
            disableGutters
            sx={{ mb: 1, '&:before': { display: 'none' }, borderRadius: 1, overflow: 'hidden' }}
          >
            <AccordionSummary expandIcon={<ExpandMoreOutlinedIcon />}>
              <Stack direction="row" spacing={1.5} alignItems="center" sx={{ width: '100%', pr: 1 }}>
                <Checkbox
                  size="small"
                  checked={all}
                  indeterminate={count > 0 && !all}
                  // Without this the click reaches the summary and collapses the
                  // section the moment you tick it.
                  onClick={(e) => e.stopPropagation()}
                  onChange={(e) => toggleSection(section, e.target.checked)}
                  sx={{ p: 0.5 }}
                />
                <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
                  {section.label}
                </Typography>
                <Chip
                  size="small"
                  variant={count > 0 ? 'filled' : 'outlined'}
                  color={count > 0 ? 'primary' : 'default'}
                  label={`${count} / ${childIds.length}`}
                />
              </Stack>
            </AccordionSummary>
            <AccordionDetails sx={{ pt: 0 }}>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1fr 1fr 1fr' },
                  columnGap: 2,
                }}
              >
                {section.children.map((menu) => {
                  const ticked = assigned.has(menu.id);
                  const dead = ticked && isDead(menu);
                  const requirements = [
                    menu.requiredPermission ? `needs ${menu.requiredPermission}` : null,
                    menu.moduleKey ? `module ${menu.moduleKey}` : null,
                    menu.requiresHomeroom ? 'class teachers only' : null,
                    !menu.enabled ? 'retired' : null,
                  ].filter(Boolean);

                  return (
                    <Stack
                      key={menu.id}
                      direction="row"
                      spacing={0.5}
                      alignItems="flex-start"
                      sx={{ py: 0.25 }}
                    >
                      <Checkbox
                        size="small"
                        checked={ticked}
                        onChange={(e) => toggle(menu, e.target.checked)}
                        sx={{ p: 0.5, mt: 0.1 }}
                      />
                      <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                        <Stack direction="row" spacing={0.5} alignItems="center">
                          <Typography variant="body2" noWrap title={menu.label}>
                            {menu.label}
                          </Typography>
                          {dead && (
                            <Tooltip title={`Hidden: this role does not hold ${menu.requiredPermission}`}>
                              <WarningAmberOutlinedIcon color="warning" sx={{ fontSize: 16 }} />
                            </Tooltip>
                          )}
                        </Stack>
                        {showGates && requirements.length > 0 && (
                          <Typography variant="caption" color="text.secondary" display="block">
                            {requirements.join(' · ')}
                          </Typography>
                        )}
                      </Box>
                    </Stack>
                  );
                })}
              </Box>
            </AccordionDetails>
          </Accordion>
        );
      })}
    </Box>
  );
}

export default PrivilegesPage;
