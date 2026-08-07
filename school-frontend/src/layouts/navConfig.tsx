import type { ReactNode } from 'react';
import type { Role } from '@/types';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import ClassOutlinedIcon from '@mui/icons-material/ClassOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined';
import PaidOutlinedIcon from '@mui/icons-material/PaidOutlined';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import DirectionsBusOutlinedIcon from '@mui/icons-material/DirectionsBusOutlined';
import ApartmentOutlinedIcon from '@mui/icons-material/ApartmentOutlined';
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined';
import RequestQuoteOutlinedIcon from '@mui/icons-material/RequestQuoteOutlined';
import BarChartOutlinedIcon from '@mui/icons-material/BarChartOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import VideoCameraFrontOutlinedIcon from '@mui/icons-material/VideoCameraFrontOutlined';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined';
import FamilyRestroomOutlinedIcon from '@mui/icons-material/FamilyRestroomOutlined';
import NotificationsOutlinedIcon from '@mui/icons-material/NotificationsOutlined';
import ChatOutlinedIcon from '@mui/icons-material/ChatOutlined';

export interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
  /** Roles allowed to see this item. Omit to allow every authenticated role. */
  roles?: Role[];
  /**
   * Optional dot-path key into i18n/translations.ts (e.g. 'nav.dashboard').
   * Sidebar looks this up via useTranslation() and falls back to `label` when
   * unset/untranslated — see i18n/LanguageProvider.tsx for the mechanism.
   */
  i18nKey?: string;
}

export interface NavGroup {
  title: string;
  items: NavItem[];
}

const MANAGEMENT: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];

export const NAV_GROUPS: NavGroup[] = [
  {
    title: 'Overview',
    items: [
      { label: 'Dashboard', path: '/app/dashboard', icon: <DashboardOutlinedIcon />, i18nKey: 'nav.dashboard' },
      {
        label: 'My Children',
        path: '/app/parent',
        icon: <FamilyRestroomOutlinedIcon />,
        roles: ['PARENT'],
        i18nKey: 'nav.myChildren',
      },
    ],
  },
  {
    title: 'Academics',
    items: [
      {
        label: 'Students',
        path: '/app/students',
        icon: <SchoolOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'RECEPTIONIST'],
        i18nKey: 'nav.students',
      },
      {
        label: 'Teachers',
        path: '/app/teachers',
        icon: <BadgeOutlinedIcon />,
        roles: MANAGEMENT,
        i18nKey: 'nav.teachers',
      },
      {
        label: 'Classes & Sections',
        path: '/app/classes',
        icon: <ClassOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER'],
        i18nKey: 'nav.classesSections',
      },
      {
        label: 'Attendance',
        path: '/app/attendance',
        icon: <EventAvailableOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.attendance',
      },
      {
        label: 'Leave',
        path: '/app/leave',
        icon: <EventBusyOutlinedIcon />,
        // No `roles` -> visible to every authenticated role, since every role applies for leave.
        i18nKey: 'nav.leave',
      },
      {
        label: 'Exams & Marks',
        path: '/app/exams',
        icon: <AssignmentOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.examsMarks',
      },
      {
        label: 'Assignments',
        path: '/app/assignments',
        icon: <FactCheckOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.assignments',
      },
      {
        label: 'Online Classes',
        path: '/app/online-classes',
        icon: <VideoCameraFrontOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.onlineClasses',
      },
    ],
  },
  {
    title: 'Administration',
    items: [
      {
        label: 'Staff',
        path: '/app/staff',
        icon: <GroupsOutlinedIcon />,
        roles: MANAGEMENT,
        i18nKey: 'nav.staff',
      },
      {
        label: 'Fees',
        path: '/app/fees',
        icon: <PaidOutlinedIcon />,
        roles: [...MANAGEMENT, 'ACCOUNTANT', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.fees',
      },
      {
        label: 'Payroll',
        path: '/app/payroll/runs',
        icon: <RequestQuoteOutlinedIcon />,
        roles: [...MANAGEMENT, 'ACCOUNTANT'],
        i18nKey: 'nav.payroll',
      },
      {
        label: 'Library',
        path: '/app/library/books',
        icon: <MenuBookOutlinedIcon />,
        // Library is staff-only this round: Librarian + management. No student self-service view yet.
        roles: ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'LIBRARIAN'],
        i18nKey: 'nav.library',
      },
      {
        label: 'Transport',
        path: '/app/transport/assignments',
        icon: <DirectionsBusOutlinedIcon />,
        roles: [...MANAGEMENT, 'RECEPTIONIST', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.transport',
      },
      {
        label: 'Hostel',
        path: '/app/hostel/residents',
        icon: <ApartmentOutlinedIcon />,
        roles: [...MANAGEMENT, 'RECEPTIONIST', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.hostel',
      },
      {
        label: 'Admission Enquiries',
        path: '/app/admission',
        icon: <HowToRegOutlinedIcon />,
        roles: ['SUPER_ADMIN', 'PRINCIPAL', 'RECEPTIONIST'],
        i18nKey: 'nav.admissionEnquiries',
      },
    ],
  },
  {
    title: 'Communication',
    items: [
      { label: 'Notice Board', path: '/app/notices', icon: <CampaignOutlinedIcon />, i18nKey: 'nav.noticeBoard' },
      { label: 'Calendar', path: '/app/calendar', icon: <CalendarMonthOutlinedIcon />, i18nKey: 'nav.calendar' },
      {
        label: 'Notifications',
        path: '/app/communication',
        icon: <NotificationsOutlinedIcon />,
        i18nKey: 'nav.notifications',
      },
      { label: 'Chat', path: '/app/chat', icon: <ChatOutlinedIcon />, i18nKey: 'nav.chat' },
    ],
  },
  {
    title: 'Insights',
    items: [
      {
        label: 'Reports',
        path: '/app/reports/overview',
        icon: <BarChartOutlinedIcon />,
        roles: [...MANAGEMENT, 'ACCOUNTANT'],
        i18nKey: 'nav.reports',
      },
    ],
  },
  {
    title: 'Account',
    items: [
      { label: 'My Profile', path: '/app/profile', icon: <PersonOutlineOutlinedIcon />, i18nKey: 'nav.myProfile' },
      {
        label: 'Settings',
        path: '/app/settings',
        icon: <SettingsOutlinedIcon />,
        roles: ['SUPER_ADMIN', 'PRINCIPAL'],
        i18nKey: 'nav.settings',
      },
    ],
  },
];

export function getNavForRole(role: Role | undefined): NavGroup[] {
  if (!role) return [];
  return NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => !item.roles || item.roles.includes(role)),
  })).filter((group) => group.items.length > 0);
}
