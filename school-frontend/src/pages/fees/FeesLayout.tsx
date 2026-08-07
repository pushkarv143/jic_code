import { useMemo, type ReactElement, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import SummarizeOutlinedIcon from '@mui/icons-material/SummarizeOutlined';
import CardGiftcardOutlinedIcon from '@mui/icons-material/CardGiftcardOutlined';
import PageHeader from '@/components/common/PageHeader';
import { useAppSelector } from '@/store/hooks';
import type { Role } from '@/types';

const FEE_STAFF_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'ACCOUNTANT'];
const FEE_REPORT_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'ACCOUNTANT'];

interface FeeTab {
  value: string;
  label: string;
  icon: ReactElement;
  roles?: Role[];
}

/** Shared Tabs sub-nav for the /app/fees/* module. STUDENT/PARENT only ever see "My Fees". */
export function FeesLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const role = useAppSelector((state) => state.auth.user?.role);
  const isSelfView = role === 'STUDENT' || role === 'PARENT';

  const tabs: FeeTab[] = useMemo(
    () =>
      isSelfView
        ? [{ value: 'my', label: 'My Fees', icon: <PaidOutlinedIcon fontSize="small" /> }]
        : [
            { value: 'setup', label: 'Fee Setup', icon: <CategoryOutlinedIcon fontSize="small" />, roles: FEE_STAFF_ROLES },
            { value: 'collect', label: 'Collect Fees', icon: <PaidOutlinedIcon fontSize="small" />, roles: FEE_STAFF_ROLES },
            { value: 'reports', label: 'Reports', icon: <SummarizeOutlinedIcon fontSize="small" />, roles: FEE_REPORT_ROLES },
            { value: 'scholarships', label: 'Scholarships', icon: <CardGiftcardOutlinedIcon fontSize="small" /> },
          ],
    [isSelfView],
  );

  const visibleTabs = useMemo(() => tabs.filter((t) => !t.roles || (role && t.roles.includes(role))), [tabs, role]);

  const current =
    visibleTabs.find((t) => location.pathname.includes(`/fees/${t.value}`))?.value ?? visibleTabs[0]?.value ?? 'reports';

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/fees/${value}`);
  };

  return (
    <Box>
      <PageHeader
        title="Fees"
        subtitle="Manage fee structures, collect payments and track dues"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Fees' }]}
      />
      <Card sx={{ mb: 2.5 }}>
        <Tabs
          value={current}
          onChange={handleChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          {visibleTabs.map((t) => (
            <Tab key={t.value} value={t.value} label={t.label} icon={t.icon} iconPosition="start" sx={{ minHeight: 48 }} />
          ))}
        </Tabs>
      </Card>
      <Outlet />
    </Box>
  );
}

export default FeesLayout;
