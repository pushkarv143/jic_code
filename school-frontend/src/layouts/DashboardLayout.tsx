import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Sidebar, { SIDEBAR_WIDTH, SIDEBAR_WIDTH_COLLAPSED } from './Sidebar';
import TopBar from './TopBar';
import AiAssistantWidget from '@/components/common/AiAssistantWidget';
import AppFooter from '@/components/common/AppFooter';

/** Authenticated app shell: collapsible sidebar + top bar + routed content area. */
export function DashboardLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  const width = collapsed ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH;

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
        <Box sx={{ p: { xs: 2, sm: 3 }, flexGrow: 1 }}>
          <Outlet />
        </Box>
        <AppFooter />
      </Box>
      <AiAssistantWidget />
    </Box>
  );
}

export default DashboardLayout;
