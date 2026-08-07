import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import CardGiftcardOutlinedIcon from '@mui/icons-material/CardGiftcardOutlined';
import SummarizeOutlinedIcon from '@mui/icons-material/SummarizeOutlined';
import ModuleLandingPage from './ModuleLandingPage';

export function FeesPage() {
  return (
    <ModuleLandingPage
      title="Fees"
      subtitle="Manage fee structures, collect payments and track dues"
      breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Fees' }]}
      sections={[
        {
          icon: <CategoryOutlinedIcon />,
          title: 'Fee Structure',
          description: 'Define fee categories and amounts per class and academic year.',
        },
        {
          icon: <PaidOutlinedIcon />,
          title: 'Collect Fees',
          description: 'Record a fee payment by cash, cheque, card or online mode.',
        },
        {
          icon: <ReceiptLongOutlinedIcon />,
          title: 'Payment History',
          description: 'View all fee payments and reprint receipts.',
        },
        {
          icon: <WarningAmberOutlinedIcon />,
          title: 'Pending / Overdue',
          description: 'Track students with partial, unpaid or overdue fees.',
          badge: '38 overdue',
        },
        {
          icon: <CardGiftcardOutlinedIcon />,
          title: 'Scholarships',
          description: 'Manage scholarship approvals and fee waivers.',
        },
        {
          icon: <SummarizeOutlinedIcon />,
          title: 'Collection Reports',
          description: 'Generate day-wise and month-wise fee collection summaries.',
        },
      ]}
    />
  );
}

export default FeesPage;
