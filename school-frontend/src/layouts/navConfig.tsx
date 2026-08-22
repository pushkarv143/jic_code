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
   * The org module this entry belongs to, keyed as `permissions.module` /
   * `org_modules.module_key` ('FEE', 'HOSTEL', 'MY_CLASS', ...).
   *
   * When an administrator switches the module off, the entry disappears for every
   * role at once — and so does the API behind it, because the backend strips that
   * module's permissions when it builds the caller's authorities. This is the
   * coarse dial: one row hides a whole area of the product.
   *
   * Omit for entries that belong to no module and should survive any
   * configuration — Dashboard, My Profile, Leave.
   */
  module?: string;
  /**
   * Show only to a user who is actually class teacher of a section.
   *
   * <p>Distinct from `roles: ['CLASS_TEACHER']`, and that distinction is the
   * point: the role is a label on the user row, while the assignment is a row in
   * `sections`. 44 users hold the role and only 17 hold an assignment, so gating
   * My Class on the role would advertise an empty screen to 27 people. The signal
   * comes from the server (`MyAccess.classTeacherOfOwnSection`), never inferred
   * here.
   */
  requiresHomeroom?: boolean;
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
        module: 'STUDENT',
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
        module: 'TEACHER',
        icon: <BadgeOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER'],
        permissions: ['TEACHER_VIEW'],
        i18nKey: 'nav.teachers',
      },
      {
        // The homeroom teacher's own section. Above Classes & Subjects because for
        // a class teacher this is the screen they actually want — theirs, editable —
        // where Classes & Subjects is the whole-school directory they can only read.
        label: 'My Class',
        path: '/app/my-class',
        icon: <ClassOutlinedIcon />,
        // No `roles`: whether this appears is decided by the homeroom assignment and
        // the permission, not by which label the user's account carries. A plain
        // TEACHER put in sections.class_teacher_id genuinely is a class teacher and
        // should see it.
        requiresHomeroom: true,
        permissions: ['MY_CLASS_VIEW'],
        module: 'MY_CLASS',
        i18nKey: 'nav.myClass',
      },
      {
        // "Sections" dropped from the label along with the picker: the school runs
        // one section per class, so subjects are what this screen is now about.
        //
        // Teachers keep read access — they need to see the timetable and subject
        // mapping — but the page's write controls are gated on CLASS_MANAGE /
        // SECTION_MANAGE inside ClassListPage. Leaving the entry visible to them and
        // the buttons hidden is deliberate: a teacher who cannot see the class list
        // at all loses information they legitimately need.
        label: 'Classes & Subjects',
        path: '/app/classes',
        icon: <ClassOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER'],
        module: 'ACADEMIC',
        i18nKey: 'nav.classesSubjects',
      },
      {
        // Self-service only: management reads a timetable through the class screen,
        // where it can also be edited.
        label: 'My Timetable',
        path: '/app/my-timetable',
        icon: <CalendarMonthOutlinedIcon />,
        roles: ['TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.myTimetable',
      },
      {
        label: 'Attendance',
        path: '/app/attendance',
        module: 'ATTENDANCE',
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
        module: 'EXAM',
        icon: <AssignmentOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.examsMarks',
      },
      {
        label: 'Assignments',
        path: '/app/assignments',
        module: 'ASSIGNMENT',
        icon: <FactCheckOutlinedIcon />,
        roles: [...MANAGEMENT, 'TEACHER', 'CLASS_TEACHER', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.assignments',
      },
      {
        label: 'Study Materials',
        path: '/app/study-materials',
        module: 'MATERIAL',
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
        module: 'STAFF',
        icon: <GroupsOutlinedIcon />,
        roles: MANAGEMENT,
        i18nKey: 'nav.staff',
      },
      {
        label: 'Fees',
        path: '/app/fees',
        module: 'FEE',
        icon: <PaidOutlinedIcon />,
        roles: [...MANAGEMENT, 'ACCOUNTANT', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.fees',
      },
      {
        label: 'Payroll',
        path: '/app/payroll/runs',
        module: 'PAYROLL',
        icon: <RequestQuoteOutlinedIcon />,
        roles: [...MANAGEMENT, 'ACCOUNTANT'],
        i18nKey: 'nav.payroll',
      },
      {
        label: 'Library',
        path: '/app/library/books',
        module: 'LIBRARY',
        icon: <MenuBookOutlinedIcon />,
        // Library is staff-only this round: Librarian + management. No student self-service view yet.
        roles: ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'LIBRARIAN'],
        i18nKey: 'nav.library',
      },
      {
        label: 'Transport',
        path: '/app/transport/assignments',
        module: 'TRANSPORT',
        icon: <DirectionsBusOutlinedIcon />,
        roles: [...MANAGEMENT, 'RECEPTIONIST', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.transport',
      },
      {
        label: 'Hostel',
        path: '/app/hostel/residents',
        module: 'HOSTEL',
        icon: <ApartmentOutlinedIcon />,
        roles: [...MANAGEMENT, 'RECEPTIONIST', 'STUDENT', 'PARENT'],
        i18nKey: 'nav.hostel',
      },
      {
        label: 'Admission Enquiries',
        path: '/app/admission',
        module: 'ADMISSION',
        icon: <HowToRegOutlinedIcon />,
        roles: ['SUPER_ADMIN', 'PRINCIPAL', 'RECEPTIONIST'],
        i18nKey: 'nav.admissionEnquiries',
      },
    ],
  },
  {
    title: 'Communication',
    items: [
      {
        label: 'Notice Board',
        path: '/app/notices',
        icon: <CampaignOutlinedIcon />,
        module: 'NOTICE',
        i18nKey: 'nav.noticeBoard',
      },
      // Calendar and Chat carry no module: they are general-purpose surfaces with
      // no permission block of their own to switch off.
      { label: 'Calendar', path: '/app/calendar', icon: <CalendarMonthOutlinedIcon />, i18nKey: 'nav.calendar' },
      {
        label: 'Notifications',
        path: '/app/communication',
        module: 'COMMUNICATION',
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
        module: 'REPORTS',
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
        module: 'USER',
        icon: <ManageAccountsOutlinedIcon />,
        roles: ['SUPER_ADMIN', 'PRINCIPAL'],
        permissions: ['USER_VIEW'],
        i18nKey: 'nav.users',
      },
      {
        label: 'Settings',
        path: '/app/settings',
        module: 'SETTINGS',
        icon: <SettingsOutlinedIcon />,
        roles: ['SUPER_ADMIN', 'PRINCIPAL'],
        i18nKey: 'nav.settings',
      },
    ],
  },
];

/** What the menu filter needs to know about the signed-in user. */
export interface NavAccess {
  role: Role | undefined;
  /** Effective grants — already module-filtered by the backend. */
  permissions: Set<Permission>;
  /** True when the named org module is switched on. Unknown keys read as on. */
  moduleEnabled: (moduleKey: string) => boolean;
  /** True only when the user holds an actual homeroom assignment. */
  isClassTeacherOfOwnSection: boolean;
}

/**
 * Filters the menu down to what this user should be offered.
 *
 * <p>Four independent gates, applied in this order because they get progressively
 * more specific:
 *
 * <ol>
 *   <li><b>module</b> — is this area of the product switched on for the
 *       organisation at all? Checked first because it overrides everything, for
 *       everyone, including the administrator: a school that does not run a hostel
 *       does not want a Hostel entry on anybody's menu. This is the one gate
 *       SUPER_ADMIN does not bypass, and that is deliberate — it is a statement
 *       about the organisation, not about the user's authority.</li>
 *   <li><b>homeroom</b> — for My Class, does this user actually hold a section?</li>
 *   <li><b>roles</b> — coarse shape of the menu.</li>
 *   <li><b>permissions</b> — the fine-grained grant.</li>
 * </ol>
 *
 * <p>The permission gate is unconditional: an entry that declares `permissions` is
 * hidden unless the user actually holds one of them. There is no "empty grant set
 * means we don't know, so show it anyway" fallback — DashboardLayout does not render
 * the shell until `GET /api/v1/me/access` has answered, so by the time this runs the
 * grants are known and an empty set genuinely means "this role holds nothing".
 */
export function getNavForAccess(access: NavAccess): NavGroup[] {
  const { role, permissions, moduleEnabled, isClassTeacherOfOwnSection } = access;
  if (!role) return [];

  // SUPER_ADMIN is never filtered by permissions, matching useAccess() and
  // AppConstants.ADMIN_OVERRIDE on the backend. Note this exempts them from the
  // permission gate only — not from the module gate, which is a statement about
  // the organisation rather than about the user's authority.
  const isAdmin = role === 'SUPER_ADMIN';

  return NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => {
      if (item.module && !moduleEnabled(item.module)) return false;
      if (item.requiresHomeroom && !isClassTeacherOfOwnSection) return false;
      if (item.roles && !item.roles.includes(role)) return false;
      if (isAdmin || !item.permissions) return true;
      return item.permissions.some((permission) => permissions.has(permission));
    }),
  })).filter((group) => group.items.length > 0);
}
