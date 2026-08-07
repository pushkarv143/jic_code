import EditCalendarOutlinedIcon from '@mui/icons-material/EditCalendarOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import QueryStatsOutlinedIcon from '@mui/icons-material/QueryStatsOutlined';
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined';
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined';
import ModuleLandingPage from './ModuleLandingPage';

export function AttendancePage() {
  return (
    <ModuleLandingPage
      title="Attendance"
      subtitle="Mark and review daily attendance for students and staff"
      breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Attendance' }]}
      sections={[
        {
          icon: <EditCalendarOutlinedIcon />,
          title: 'Mark Attendance',
          description: 'Mark daily attendance for a class and section in one screen.',
        },
        {
          icon: <FactCheckOutlinedIcon />,
          title: 'Attendance Register',
          description: 'View a month-wise attendance register for any class.',
        },
        {
          icon: <HowToRegOutlinedIcon />,
          title: 'Staff Attendance',
          description: 'Track teacher and staff check-in / check-out times.',
        },
        {
          icon: <EventBusyOutlinedIcon />,
          title: 'Leave Applications',
          description: 'Review and approve leave requests from staff and students.',
          badge: '4 pending',
        },
        {
          icon: <QueryStatsOutlinedIcon />,
          title: 'Attendance Analytics',
          description: 'Visualize attendance trends by class, section and date range.',
        },
      ]}
    />
  );
}

export default AttendancePage;
