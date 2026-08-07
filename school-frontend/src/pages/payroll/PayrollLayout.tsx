import { useMemo, type SyntheticEvent } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import AccountBalanceOutlinedIcon from '@mui/icons-material/AccountBalanceOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import PageHeader from '@/components/common/PageHeader';

const TABS = [
  { value: 'structures', label: 'Salary Structures', icon: <AccountBalanceOutlinedIcon fontSize="small" /> },
  { value: 'runs', label: 'Payroll Runs', icon: <RequestQuoteOutlinedIcon fontSize="small" /> },
];

/** Shared Tabs sub-nav for the /app/payroll/* module (Salary Structures / Payroll Runs). */
export function PayrollLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const current = TABS.find((t) => location.pathname.includes(`/payroll/${t.value}`))?.value ?? TABS[0].value;

  const handleChange = (_e: SyntheticEvent, value: string) => {
    navigate(`/app/payroll/${value}`);
  };

  const tabs = useMemo(() => TABS, []);

  return (
    <Box>
      <PageHeader
        title="Payroll"
        subtitle="Manage salary structures and process monthly payroll for staff"
        breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Payroll' }]}
      />
      <Card sx={{ mb: 2.5 }}>
        <Tabs
          value={current}
          onChange={handleChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}
        >
          {tabs.map((t) => (
            <Tab key={t.value} value={t.value} label={t.label} icon={t.icon} iconPosition="start" sx={{ minHeight: 48 }} />
          ))}
        </Tabs>
      </Card>
      <Outlet />
    </Box>
  );
}

export default PayrollLayout;
