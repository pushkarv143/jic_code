import PersonAddOutlinedIcon from '@mui/icons-material/PersonAddOutlined';
import FormatListBulletedOutlinedIcon from '@mui/icons-material/FormatListBulletedOutlined';
import LocalPoliceOutlinedIcon from '@mui/icons-material/LocalPoliceOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import SupportAgentOutlinedIcon from '@mui/icons-material/SupportAgentOutlined';
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined';
import ModuleLandingPage from './ModuleLandingPage';

export function StaffPage() {
  return (
    <ModuleLandingPage
      title="Staff"
      subtitle="Manage non-teaching staff: accountants, librarians, receptionists and security"
      breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Staff' }]}
      sections={[
        {
          icon: <PersonAddOutlinedIcon />,
          title: 'Add Staff',
          description: 'Onboard new non-teaching staff with department and designation.',
        },
        {
          icon: <FormatListBulletedOutlinedIcon />,
          title: 'Staff Directory',
          description: 'Browse all staff members grouped by role and department.',
          badge: '30 active',
        },
        {
          icon: <AccountBalanceWalletOutlinedIcon />,
          title: 'Accounts Team',
          description: 'Manage accountants responsible for fee collection and payroll.',
        },
        {
          icon: <MenuBookOutlinedIcon />,
          title: 'Library Staff',
          description: 'Manage librarians and library operation assignments.',
        },
        {
          icon: <SupportAgentOutlinedIcon />,
          title: 'Front Office',
          description: 'Manage receptionists handling enquiries and visitor logs.',
        },
        {
          icon: <LocalPoliceOutlinedIcon />,
          title: 'Security',
          description: 'Manage security guards, shift rosters and gate logs.',
        },
      ]}
    />
  );
}

export default StaffPage;
