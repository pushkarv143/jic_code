import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';

import PublicLayout from '@/layouts/PublicLayout';
import AuthLayout from '@/layouts/AuthLayout';
import DashboardLayout from '@/layouts/DashboardLayout';

import ProtectedRoute from './ProtectedRoute';
import RoleBasedRoute from './RoleBasedRoute';

import LandingPage from '@/pages/public/LandingPage';
import AboutPage from '@/pages/public/AboutPage';
import ContactPage from '@/pages/public/ContactPage';
import AdmissionPage from '@/pages/public/AdmissionPage';
import GalleryPage from '@/pages/public/GalleryPage';
import FacilitiesPage from '@/pages/public/FacilitiesPage';
import AcademicsPage from '@/pages/public/AcademicsPage';
import FacultyPage from '@/pages/public/FacultyPage';

import LoginPage from '@/pages/auth/LoginPage';
import RegisterPage from '@/pages/auth/RegisterPage';
import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/auth/ResetPasswordPage';

import DashboardPage from '@/pages/dashboard/DashboardPage';

import StudentListPage from '@/pages/students/StudentListPage';
import StudentFormPage from '@/pages/students/StudentFormPage';
import StudentProfilePage from '@/pages/students/StudentProfilePage';
import MyStudentProfilePage from '@/pages/students/MyStudentProfilePage';

import TeacherListPage from '@/pages/teachers/TeacherListPage';
import TeacherFormPage from '@/pages/teachers/TeacherFormPage';
import TeacherProfilePage from '@/pages/teachers/TeacherProfilePage';
import MyTeacherProfilePage from '@/pages/teachers/MyTeacherProfilePage';

import ClassListPage from '@/pages/classes/ClassListPage';
import ClassDetailPage from '@/pages/classes/ClassDetailPage';

import StudyMaterialsPage from '@/pages/materials/StudyMaterialsPage';

import AttendanceLayout from '@/pages/attendance/AttendanceLayout';
import MarkAttendancePage from '@/pages/attendance/MarkAttendancePage';
import AttendanceReportPage from '@/pages/attendance/AttendanceReportPage';
import MonthlyAttendancePage from '@/pages/attendance/MonthlyAttendancePage';
import TeacherAttendancePage from '@/pages/attendance/TeacherAttendancePage';

import LeaveListPage from '@/pages/leave/LeaveListPage';

import FeesLayout from '@/pages/fees/FeesLayout';
import FeeSetupPage from '@/pages/fees/FeeSetupPage';
import FeeCollectionPage from '@/pages/fees/FeeCollectionPage';
import FeeReportsPage from '@/pages/fees/FeeReportsPage';
import FeeReceiptPage from '@/pages/fees/FeeReceiptPage';
import ScholarshipsPage from '@/pages/fees/ScholarshipsPage';
import FeesIndexRedirect from '@/pages/fees/FeesIndexRedirect';

import StaffPage from '@/pages/modules/StaffPage';

import ExamsLayout from '@/pages/exams/ExamsLayout';
import ExamSetupPage from '@/pages/exams/ExamSetupPage';
import MarksEntryPage from '@/pages/exams/MarksEntryPage';
import ResultsPage from '@/pages/exams/ResultsPage';

import AssignmentListPage from '@/pages/assignments/AssignmentListPage';
import OnlineClassesPage from '@/pages/online-classes/OnlineClassesPage';
import NoticeBoardPage from '@/pages/notices/NoticeBoardPage';
import CalendarPage from '@/pages/calendar/CalendarPage';
import AdmissionEnquiriesPage from '@/pages/admission/AdmissionEnquiriesPage';
import ChatPage from '@/pages/chat/ChatPage';

import LibraryLayout from '@/pages/library/LibraryLayout';
import BooksPage from '@/pages/library/BooksPage';
import IssueReturnPage from '@/pages/library/IssueReturnPage';
import OverdueBooksPage from '@/pages/library/OverdueBooksPage';

import TransportLayout from '@/pages/transport/TransportLayout';
import FleetPage from '@/pages/transport/FleetPage';
import StudentAssignmentsPage from '@/pages/transport/StudentAssignmentsPage';

import HostelLayout from '@/pages/hostel/HostelLayout';
import RoomsPage from '@/pages/hostel/RoomsPage';
import ResidentsPage from '@/pages/hostel/ResidentsPage';
import VisitorsPage from '@/pages/hostel/VisitorsPage';
import HostelFeesPage from '@/pages/hostel/HostelFeesPage';

import PayrollLayout from '@/pages/payroll/PayrollLayout';
import SalaryStructuresPage from '@/pages/payroll/SalaryStructuresPage';
import PayrollRunsPage from '@/pages/payroll/PayrollRunsPage';
import SalarySlipPage from '@/pages/payroll/SalarySlipPage';

import ParentDashboardPage from '@/pages/parent/ParentDashboardPage';

import NotificationsPage from '@/pages/communication/NotificationsPage';

import ReportsLayout from '@/pages/reports/ReportsLayout';
import OverviewReportPage from '@/pages/reports/OverviewReportPage';
import StudentsReportPage from '@/pages/reports/StudentsReportPage';
import TeachersReportPage from '@/pages/reports/TeachersReportPage';
import AttendanceAnalyticsPage from '@/pages/reports/AttendanceAnalyticsPage';
import FeeCollectionReportPage from '@/pages/reports/FeeCollectionReportPage';
import PayrollReportPage from '@/pages/reports/PayrollReportPage';
import LibraryReportPage from '@/pages/reports/LibraryReportPage';
import TransportReportPage from '@/pages/reports/TransportReportPage';

import SettingsPage from '@/pages/settings/SettingsPage';
import UserListPage from '@/pages/users/UserListPage';

import ProfilePage from '@/pages/misc/ProfilePage';
import NotFoundPage from '@/pages/misc/NotFoundPage';
import ForbiddenPage from '@/pages/misc/ForbiddenPage';
import ErrorPage from '@/pages/misc/ErrorPage';

import type { Role } from '@/types';
import { usePermissions } from '@/hooks/usePermissions';

const MANAGEMENT_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL'];
const EXAM_STAFF_ROLES: Role[] = [...MANAGEMENT_ROLES, 'TEACHER', 'CLASS_TEACHER'];
const ADMISSION_STAFF_ROLES: Role[] = ['SUPER_ADMIN', 'PRINCIPAL', 'RECEPTIONIST'];

/**
 * One path, two pages: /app/students is the directory for anyone who supervises
 * students and the student's own record for a STUDENT. Keeping the path shared
 * means the sidebar, breadcrumbs and deep links stay identical across roles.
 */
function StudentsIndexRoute() {
  const { is } = usePermissions();
  return is('STUDENT') ? <MyStudentProfilePage /> : <StudentListPage />;
}

/**
 * Same split for staff: /app/teachers is the directory for management and the
 * teacher's own record — plus the classes they teach — for a teacher.
 */
function TeachersIndexRoute() {
  const { isTeaching } = usePermissions();
  return isTeaching ? <MyTeacherProfilePage /> : <TeacherListPage />;
}

const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    errorElement: <ErrorPage />,
    children: [
      { path: '/', element: <LandingPage /> },
      { path: '/about', element: <AboutPage /> },
      { path: '/contact', element: <ContactPage /> },
      { path: '/admission', element: <AdmissionPage /> },
      { path: '/gallery', element: <GalleryPage /> },
      { path: '/facilities', element: <FacilitiesPage /> },
      { path: '/academics', element: <AcademicsPage /> },
      { path: '/faculty', element: <FacultyPage /> },
    ],
  },
  {
    element: <AuthLayout />,
    errorElement: <ErrorPage />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
      { path: '/reset-password', element: <ResetPasswordPage /> },
    ],
  },
  {
    path: '/403',
    element: <ForbiddenPage />,
    errorElement: <ErrorPage />,
  },
  {
    element: <ProtectedRoute />,
    errorElement: <ErrorPage />,
    children: [
      {
        path: '/app',
        element: <DashboardLayout />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'dashboard', element: <DashboardPage /> },
          {
            // STUDENT is allowed in, but lands on their own profile rather than the
            // directory — StudentsIndexRoute picks the page. The create/edit routes
            // stay behind MANAGEMENT_ROLES, matching the backend's WRITE_ROLES.
            path: 'students',
            element: (
              <RoleBasedRoute
                allowedRoles={[...MANAGEMENT_ROLES, 'TEACHER', 'CLASS_TEACHER', 'RECEPTIONIST', 'STUDENT']}
              />
            ),
            children: [
              { index: true, element: <StudentsIndexRoute /> },
              {
                element: <RoleBasedRoute allowedRoles={MANAGEMENT_ROLES} />,
                children: [
                  { path: 'new', element: <StudentFormPage /> },
                  { path: ':id/edit', element: <StudentFormPage /> },
                ],
              },
              { path: ':id', element: <StudentProfilePage /> },
            ],
          },
          {
            // Teaching roles are allowed in, but land on their own profile rather
            // than the staff directory. The create/edit routes stay behind
            // MANAGEMENT_ROLES, matching the backend's WRITE_ROLES.
            path: 'teachers',
            element: <RoleBasedRoute allowedRoles={[...MANAGEMENT_ROLES, 'TEACHER', 'CLASS_TEACHER']} />,
            children: [
              { index: true, element: <TeachersIndexRoute /> },
              {
                element: <RoleBasedRoute allowedRoles={MANAGEMENT_ROLES} />,
                children: [
                  { path: 'new', element: <TeacherFormPage /> },
                  { path: ':id/edit', element: <TeacherFormPage /> },
                ],
              },
              { path: ':id', element: <TeacherProfilePage /> },
            ],
          },
          {
            path: 'staff',
            element: <RoleBasedRoute allowedRoles={MANAGEMENT_ROLES} />,
            children: [{ index: true, element: <StaffPage /> }],
          },
          {
            path: 'classes',
            element: (
              <RoleBasedRoute allowedRoles={[...MANAGEMENT_ROLES, 'TEACHER', 'CLASS_TEACHER']} />
            ),
            children: [
              { index: true, element: <ClassListPage /> },
              { path: ':id', element: <ClassDetailPage /> },
            ],
          },
          {
            path: 'attendance',
            element: <AttendanceLayout />,
            children: [
              { index: true, element: <Navigate to="reports" replace /> },
              {
                path: 'mark',
                element: <RoleBasedRoute allowedRoles={[...MANAGEMENT_ROLES, 'TEACHER', 'CLASS_TEACHER']} />,
                children: [{ index: true, element: <MarkAttendancePage /> }],
              },
              { path: 'reports', element: <AttendanceReportPage /> },
              { path: 'monthly', element: <MonthlyAttendancePage /> },
              {
                path: 'teachers',
                element: <RoleBasedRoute allowedRoles={[...MANAGEMENT_ROLES, 'TEACHER', 'CLASS_TEACHER']} />,
                children: [{ index: true, element: <TeacherAttendancePage /> }],
              },
            ],
          },
          { path: 'leave', element: <LeaveListPage /> },
          {
            path: 'fees',
            element: <FeesLayout />,
            children: [
              { index: true, element: <FeesIndexRedirect /> },
              {
                path: 'setup',
                element: <RoleBasedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ACCOUNTANT']} />,
                children: [{ index: true, element: <FeeSetupPage /> }],
              },
              {
                path: 'collect',
                element: <RoleBasedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'ACCOUNTANT']} />,
                children: [{ index: true, element: <FeeCollectionPage mode="staff" /> }],
              },
              { path: 'my', element: <FeeCollectionPage mode="self" /> },
              {
                path: 'reports',
                element: <RoleBasedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'ACCOUNTANT']} />,
                children: [{ index: true, element: <FeeReportsPage /> }],
              },
              { path: 'scholarships', element: <ScholarshipsPage /> },
              { path: 'receipt/:paymentId', element: <FeeReceiptPage /> },
            ],
          },
          {
            path: 'library',
            element: <RoleBasedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL', 'VICE_PRINCIPAL', 'LIBRARIAN']} />,
            children: [
              {
                element: <LibraryLayout />,
                children: [
                  { index: true, element: <Navigate to="books" replace /> },
                  { path: 'books', element: <BooksPage /> },
                  { path: 'issue-return', element: <IssueReturnPage /> },
                  { path: 'overdue', element: <OverdueBooksPage /> },
                ],
              },
            ],
          },
          {
            path: 'transport',
            element: <TransportLayout />,
            children: [
              { index: true, element: <Navigate to="assignments" replace /> },
              {
                path: 'fleet',
                element: <RoleBasedRoute allowedRoles={MANAGEMENT_ROLES} />,
                children: [{ index: true, element: <FleetPage /> }],
              },
              { path: 'assignments', element: <StudentAssignmentsPage /> },
            ],
          },
          {
            path: 'hostel',
            element: <HostelLayout />,
            children: [
              { index: true, element: <Navigate to="residents" replace /> },
              {
                path: 'rooms',
                element: <RoleBasedRoute allowedRoles={MANAGEMENT_ROLES} />,
                children: [{ index: true, element: <RoomsPage /> }],
              },
              { path: 'residents', element: <ResidentsPage /> },
              { path: 'visitors', element: <VisitorsPage /> },
              { path: 'fees', element: <HostelFeesPage /> },
            ],
          },
          {
            path: 'exams',
            element: <ExamsLayout />,
            children: [
              { index: true, element: <Navigate to="results" replace /> },
              {
                path: 'setup',
                element: <RoleBasedRoute allowedRoles={EXAM_STAFF_ROLES} />,
                children: [{ index: true, element: <ExamSetupPage /> }],
              },
              {
                path: 'marks-entry',
                element: <RoleBasedRoute allowedRoles={EXAM_STAFF_ROLES} />,
                children: [{ index: true, element: <MarksEntryPage /> }],
              },
              { path: 'results', element: <ResultsPage /> },
            ],
          },
          { path: 'assignments', element: <AssignmentListPage /> },
          // No RoleBasedRoute: access is decided by the MATERIAL_VIEW permission,
          // which the sidebar already filters on and the API enforces. Adding a
          // role list here would be a second, drifting copy of the same rule.
          { path: 'study-materials', element: <StudyMaterialsPage /> },
          { path: 'online-classes', element: <OnlineClassesPage /> },
          { path: 'notices', element: <NoticeBoardPage /> },
          { path: 'calendar', element: <CalendarPage /> },
          { path: 'chat', element: <ChatPage /> },
          {
            path: 'admission',
            element: <RoleBasedRoute allowedRoles={ADMISSION_STAFF_ROLES} />,
            children: [{ index: true, element: <AdmissionEnquiriesPage /> }],
          },
          {
            path: 'payroll',
            element: <RoleBasedRoute allowedRoles={[...MANAGEMENT_ROLES, 'ACCOUNTANT']} />,
            children: [
              {
                element: <PayrollLayout />,
                children: [
                  { index: true, element: <Navigate to="runs" replace /> },
                  { path: 'structures', element: <SalaryStructuresPage /> },
                  { path: 'runs', element: <PayrollRunsPage /> },
                ],
              },
              // Kept as a sibling of the tabbed layout (not nested under it) so the printable
              // slip's @media print output doesn't also include the Payroll tab bar.
              { path: 'slip/:payrollId', element: <SalarySlipPage /> },
            ],
          },
          {
            path: 'reports',
            // DEVIATION: task allowed either "ACCOUNTANT only for fees/payroll tabs" or the
            // simpler "ACCOUNTANT gets the whole Reports section" - chose the latter.
            element: <RoleBasedRoute allowedRoles={[...MANAGEMENT_ROLES, 'ACCOUNTANT']} />,
            children: [
              {
                element: <ReportsLayout />,
                children: [
                  { index: true, element: <Navigate to="overview" replace /> },
                  { path: 'overview', element: <OverviewReportPage /> },
                  { path: 'students', element: <StudentsReportPage /> },
                  { path: 'teachers', element: <TeachersReportPage /> },
                  { path: 'attendance', element: <AttendanceAnalyticsPage /> },
                  { path: 'fees', element: <FeeCollectionReportPage /> },
                  { path: 'payroll', element: <PayrollReportPage /> },
                  { path: 'library', element: <LibraryReportPage /> },
                  { path: 'transport', element: <TransportReportPage /> },
                ],
              },
            ],
          },
          {
            path: 'settings',
            element: <RoleBasedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL']} />,
            children: [{ index: true, element: <SettingsPage /> }],
          },
          {
            path: 'users',
            element: <RoleBasedRoute allowedRoles={['SUPER_ADMIN', 'PRINCIPAL']} />,
            children: [{ index: true, element: <UserListPage /> }],
          },
          // Open to all roles - the page itself shows a graceful message for non-PARENT accounts.
          { path: 'parent', element: <ParentDashboardPage /> },
          // Open to all roles - "Compose" is gated inside the page for admin roles.
          { path: 'communication', element: <NotificationsPage /> },
          { path: 'profile', element: <ProfilePage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);

/** Root application router: public marketing site, auth flows, and the protected /app/* shell. */
export function AppRouter() {
  return <RouterProvider router={router} />;
}

export default AppRouter;
