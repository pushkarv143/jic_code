import PersonAddOutlinedIcon from '@mui/icons-material/PersonAddOutlined';
import FormatListBulletedOutlinedIcon from '@mui/icons-material/FormatListBulletedOutlined';
import TrendingUpOutlinedIcon from '@mui/icons-material/TrendingUpOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import MedicalInformationOutlinedIcon from '@mui/icons-material/MedicalInformationOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import FamilyRestroomOutlinedIcon from '@mui/icons-material/FamilyRestroomOutlined';
import ModuleLandingPage from './ModuleLandingPage';

export function StudentsPage() {
  return (
    <ModuleLandingPage
      title="Students"
      subtitle="Manage student admissions, records, promotions and transfers"
      breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Students' }]}
      sections={[
        {
          icon: <PersonAddOutlinedIcon />,
          title: 'Add Student',
          description: 'Admit a new student with class, section and guardian details.',
        },
        {
          icon: <FormatListBulletedOutlinedIcon />,
          title: 'Student List',
          description: 'Browse, search and filter all enrolled students by class and status.',
          badge: '512 active',
        },
        {
          icon: <TrendingUpOutlinedIcon />,
          title: 'Promotion',
          description: 'Promote students in bulk to the next class for a new academic year.',
        },
        {
          icon: <SwapHorizOutlinedIcon />,
          title: 'Transfer / Alumni',
          description: 'Process transfer certificates and mark students as alumni.',
        },
        {
          icon: <BadgeOutlinedIcon />,
          title: 'ID Cards',
          description: 'Generate and print student identity cards with QR codes.',
        },
        {
          icon: <FamilyRestroomOutlinedIcon />,
          title: 'Guardians',
          description: 'Manage parent and guardian contact information per student.',
        },
        {
          icon: <MedicalInformationOutlinedIcon />,
          title: 'Medical Records',
          description: 'Track health details, allergies and emergency contacts.',
        },
        {
          icon: <DescriptionOutlinedIcon />,
          title: 'Documents',
          description: 'Upload and verify admission and identity documents.',
        },
      ]}
    />
  );
}

export default StudentsPage;
