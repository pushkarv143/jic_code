import type { ReactNode } from 'react';
import type { Permission, Role } from '@/types';
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
import ManageAccountsOutlinedIcon from '@mui/icons-material/ManageAccountsOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import LibraryBooksOutlinedIcon from '@mui/icons-material/LibraryBooksOutlined';
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
   * Permission names, any one of which reveals this item. Applied on top of
   * `roles`: an item with both must satisfy both. Omit for items that every
   * role holding the listed roles should see regardless of permission grants
   * (e.g. Dashboard, My Profile).
   */
  permissions?: Permission[];
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
        // Renders as the full directory for management/office roles, as the taught
        // students for a teacher, and as the student's own profile for STUDENT —
        // the page decides, the backend scopes the rows either way.
        label: 'Students',
        path: '/app/students',
        icon: <SchoolOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'RECEPTIONIST', 'STUDENT'],
        permissions: ['STUDENT_VIEW'],
        i18nKey: 'nav.students',
      },
      {
        // Directory for management; the signed-in teacher's own record for a
        // teacher — same entry, TeachersIndexRoute picks the page.
        label: 'Teachers',
        path: '/app/teachers',
        icon: <BadgeOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER'],
        permissions: ['TEACHER_VIEW'],
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
        permissions: ['ATTENDANCE_VIEW'],
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
        label: 'Study Materials',
        path: '/app/study-materials',
        icon: <LibraryBooksOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        permissions: ['MATERIAL_VIEW'],
        i18nKey: 'nav.studyMaterials',
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
        label: 'Users',
        path: '/app/users',
        icon: <ManageAccountsOutlinedIcon />,
        roles: ['SUPER_ADMIN', 'PRINCIPAL'],
        permissions: ['USER_VIEW'],
        i18nKey: 'nav.users',
      },
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

/**
 * Filters the menu down to what this user should be offered.
 *
 * `granted` is the permission set from the login response. When it is empty the
 * permission dimension is skipped entirely and filtering falls back to roles
 * alone — that case means "we don't know this user's grants" (a session cached
 * before the backend started returning them), and blanking the whole menu would
 * be a worse answer than the previous role-only behaviour. It is never a
 * security shortcut: the API refuses the request regardless of what is rendered.
 */
export function getNavForRole(role: Role | undefined, granted?: Set<Permission>): NavGroup[] {
  if (!role) return [];
  // SUPER_ADMIN is never filtered by permissions, matching usePermissions() and
  // AppConstants.ADMIN_OVERRIDE on the backend.
  const checkPermissions = role !== 'SUPER_ADMIN' && Boolean(granted && granted.size > 0);

  return NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => {
      if (item.roles && !item.roles.includes(role)) return false;
      if (!checkPermissions || !item.permissions) return true;
      return item.permissions.some((permission) => granted!.has(permission));
    }),
  })).filter((group) => group.items.length > 0);
}
