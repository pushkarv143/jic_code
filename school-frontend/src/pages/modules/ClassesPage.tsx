import AddBoxOutlinedIcon from '@mui/icons-material/AddBoxOutlined';
import ClassOutlinedIcon from '@mui/icons-material/ClassOutlined';
import MeetingRoomOutlinedIcon from '@mui/icons-material/MeetingRoomOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import ModuleLandingPage from './ModuleLandingPage';

export function ClassesPage() {
  return (
    <ModuleLandingPage
      title="Classes & Sections"
      subtitle="Manage academic years, classes, sections and subjects"
      breadcrumbs={[{ label: 'Dashboard', to: '/app/dashboard' }, { label: 'Classes & Sections' }]}
      sections={[
        {
          icon: <AddBoxOutlinedIcon />,
          title: 'Add Class / Section',
          description: 'Create new classes and sections for the current academic year.',
        },
        {
          icon: <ClassOutlinedIcon />,
          title: 'Class List',
          description: 'View all classes with section count and enrolled strength.',
          badge: '12 classes',
        },
        {
          icon: <MeetingRoomOutlinedIcon />,
          title: 'Section & Room Allotment',
          description: 'Assign rooms and capacity to each section.',
        },
        {
          icon: <PeopleAltOutlinedIcon />,
          title: 'Class Teachers',
          description: 'Assign a class teacher to each section.',
        },
        {
          icon: <MenuBookOutlinedIcon />,
          title: 'Subjects',
          description: 'Manage subjects, subject codes and electives per class.',
        },
        {
          icon: <CalendarMonthOutlinedIcon />,
          title: 'Academic Years',
          description: 'Manage academic year records and mark the current session.',
        },
      ]}
    />
  );
}

export default ClassesPage;
