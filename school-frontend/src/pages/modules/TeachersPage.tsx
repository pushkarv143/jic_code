import PersonAddOutlinedIcon from '@mui/icons-material/PersonAddOutlined';
import FormatListBulletedOutlinedIcon from '@mui/icons-material/FormatListBulletedOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import WorkspacePremiumOutlinedIcon from '@mui/icons-material/WorkspacePremiumOutlined';
import ScheduleOutlinedIcon from '@mui/icons-material/ScheduleOutlined';
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined';
import ModuleLandingPage from './ModuleLandingPage';

export function TeachersPage() {
  return (
    <ModuleLandingPage
      title="Teachers"
      subtitle="Manage teaching staff records, qualifications and class assignments"
      breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Teachers' }]}
      sections={[
        {
          icon: <PersonAddOutlinedIcon />,
          title: 'Add Teacher',
          description: 'Onboard a new teacher with department, designation and salary details.',
        },
        {
          icon: <FormatListBulletedOutlinedIcon />,
          title: 'Teacher Directory',
          description: 'Search and filter all teaching staff by department and status.',
          badge: '50 active',
        },
        {
          icon: <AssignmentIndOutlinedIcon />,
          title: 'Class-Subject Mapping',
          description: 'Assign teachers to classes, sections and subjects.',
        },
        {
          icon: <ScheduleOutlinedIcon />,
          title: 'Timetable',
          description: 'View and edit weekly teaching schedules per teacher.',
        },
        {
          icon: <EventAvailableOutlinedIcon />,
          title: 'Attendance & Leave',
          description: 'Track daily attendance and approve leave applications.',
        },
        {
          icon: <WorkspacePremiumOutlinedIcon />,
          title: 'Performance & Appraisal',
          description: 'Record qualifications, experience and annual appraisals.',
        },
      ]}
    />
  );
}

export default TeachersPage;
