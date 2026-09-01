import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import PageLoader from '@/components/common/PageLoader';
import { useAccess } from '@/access/AccessProvider';
import Sidebar, { SIDEBAR_WIDTH, SIDEBAR_WIDTH_COLLAPSED } from './Sidebar';
import TopBar from './TopBar';
import AiAssistantWidget from '@/components/common/AiAssistantWidget';
import AppFooter from '@/components/common/AppFooter';

/** Authenticated app shell: collapsible sidebar + top bar + routed content area. */
export function DashboardLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const { settled, error, access, refresh } = useAccess();

  const width = collapsed ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH;

  /*
   * Nothing is rendered until the server has said what this user may do.
   *
   * The alternative — paint the shell immediately and fill permissions in when
   * they arrive — is what produced the behaviour this change removes: for the
   * few hundred milliseconds before the answer lands, every gated control has to
   * be guessed at, and guessing "allow" shows a class teacher an Add Class
   * button that 403s. Holding the shell for one request makes every `can()` call
   * downstream a real answer, so no screen has to handle a maybe.
   *
   * This is a fetch per login, per token refresh and per page load, not per
   * navigation.
   *
   * Gated on `settled` and NOT on `loading`: `settled` latches true after the
   * first answer, so a later re-read — the settings screen calling `refresh()`
   * after saving a role, or a background token refresh — updates the menu in
   * place instead of tearing the shell down and losing the user's scroll
   * position and open dialogs mid-edit.
   */
  if (!settled) {
    return <PageLoader label="Checking your permissions…" />;
  }

  /*
   * Only when there is nothing to fall back on. A refresh that fails while we
   * still hold a good answer keeps showing that answer — replacing a working
   * screen with an error card because a background re-read timed out would be
   * worse than briefly running on grants a few seconds old.
   *
   * Failing closed silently is the other trap: it leaves a usable-looking app
   * with every action mysteriously missing, which reads as "the product is
   * broken" rather than "we could not check". Say which it is, and offer a retry.
   */
  if (error && !access) {
    return (
      <Box sx={{ p: 4, maxWidth: 560, mx: 'auto' }}>
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={() => void refresh()}>
              Retry
            </Button>
          }
        >
          Could not load your permissions, so no actions are being offered. Check your connection
          and retry.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <TopBar
        onMenuClick={() => setMobileOpen(true)}
        onCollapseClick={() => setCollapsed((c) => !c)}
        collapsed={collapsed}
      />
      <Sidebar mobileOpen={mobileOpen} collapsed={collapsed} onClose={() => setMobileOpen(false)} />
      <Box
        component="main"
        sx={{
          // Column layout with the content area flexing: this is what keeps the
          // footer at the bottom of the viewport on short pages instead of
          // floating up under the content.
          display: 'flex',
          flexDirection: 'column',
          flexGrow: 1,
          minWidth: 0,
          bgcolor: 'background.default',
          minHeight: '100vh',
          transition: 'width 0.2s ease',
          width: { md: `calc(100% - ${width}px)` },
        }}
      >
        <Toolbar />
        <Box key={location.pathname} className="page-enter" sx={{ p: { xs: 2, sm: 3 }, flexGrow: 1 }}>
          <Outlet />
        </Box>
        <AppFooter />
      </Box>
      <AiAssistantWidget />
    </Box>
  );
}

export default DashboardLayout;
