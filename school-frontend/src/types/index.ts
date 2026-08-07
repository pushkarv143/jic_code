/**
 * Core shared TypeScript types for the School Management System frontend.
 * These mirror the API contract defined in SCHEMA_CONTRACT.md — keep in sync
 * with the backend DTOs. Do not rename fields without updating both sides.
 */

/** The exact 11 seeded roles, in seed order (id 1-11). */
export type Role =
  | 'SUPER_ADMIN'
  | 'PRINCIPAL'
  | 'VICE_PRINCIPAL'
  | 'TEACHER'
  | 'CLASS_TEACHER'
  | 'ACCOUNTANT'
  | 'LIBRARIAN'
  | 'RECEPTIONIST'
  | 'STUDENT'
  | 'PARENT'
  | 'SECURITY_GUARD';

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  gender: Gender | null;
  role: Role;
  isActive: boolean;
  profileImage: string | null;
  lastLogin: string | null;
  // Round-3 addition (ASSUMPTION - reconcile with backend): self-service attendance/fees
  // pages need to know "which student/teacher am I" without a dedicated lookup endpoint.
  // Expecting the backend to populate these on /auth/login and /auth/me for the relevant
  // roles (STUDENT -> studentId/classId/sectionId, TEACHER/CLASS_TEACHER -> teacherId).
  // All optional/nullable so nothing breaks if the backend hasn't wired this up yet.
  studentId?: number | null;
  teacherId?: number | null;
  classId?: number | null;
  sectionId?: number | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}

/** Generic API envelope used by most non-paginated endpoints. */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

/** Generic Spring-style page response for list endpoints. */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  success: false;
  message: string;
  errors?: Record<string, string>;
  status?: number;
  timestamp?: string;
}

/* ------------------------------------------------------------------------ */
/* Academic setup, people & module entities (Round 2).                       */
/* Field names mirror SCHEMA_CONTRACT.md tables verbatim (camelCased).       */
/* ------------------------------------------------------------------------ */

export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT';
export type StaffStatus = 'ACTIVE' | 'INACTIVE' | 'RESIGNED' | 'TERMINATED';
export type StudentStatus = 'ACTIVE' | 'INACTIVE' | 'ALUMNI' | 'TRANSFERRED';

export interface AcademicYear {
  id: number;
  yearName: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
}

export interface Department {
  id: number;
  name: string;
  description: string | null;
}

export interface Designation {
  id: number;
  name: string;
  description: string | null;
}

export interface SchoolClass {
  id: number;
  className: string;
  academicYearId: number;
  academicYearName?: string;
  sectionCount?: number;
  studentCount?: number;
}

export interface Section {
  id: number;
  sectionName: string;
  classId: number;
  className?: string;
  classTeacherId: number | null;
  classTeacherName?: string | null;
  roomNumber: string | null;
  capacity: number | null;
  studentCount?: number;
}

export interface Subject {
  id: number;
  subjectName: string;
  subjectCode: string;
  classId: number;
  isElective: boolean;
}

/** class_subject_teacher mapping row, denormalised with display-friendly names. */
export interface ClassSubjectTeacher {
  id: number;
  classId: number;
  sectionId: number;
  sectionName?: string;
  subjectId: number;
  subjectName?: string;
  teacherId: number;
  teacherName?: string;
}

/**
 * A teacher is a `users` row joined to a `teachers` row. The exact response
 * shape (flattened vs. nested `user`) is a point to reconcile with the
 * backend once it lands — this type supports both by making the flattened
 * convenience fields optional alongside an optional nested `user`.
 */
export interface Teacher {
  id: number;
  userId: number;
  employeeId: string;
  departmentId: number;
  departmentName?: string;
  designationId: number;
  designationName?: string;
  qualification: string | null;
  experienceYears: number | null;
  joiningDate: string;
  dateOfBirth: string | null;
  gender: Gender | null;
  address: string | null;
  city: string | null;
  state: string | null;
  pincode: string | null;
  bloodGroup: string | null;
  emergencyContact: string | null;
  salary: number | null;
  employmentType: EmploymentType;
  status: StaffStatus;
  // Flattened user-account convenience fields (see note above).
  username?: string;
  email?: string;
  phone?: string | null;
  firstName?: string;
  lastName?: string;
  user?: User;
}

export interface Guardian {
  id: number;
  studentId: number;
  name: string;
  relation: string;
  occupation: string | null;
  phone: string;
  email: string | null;
  address: string | null;
  isPrimary: boolean;
}

export interface MedicalDetails {
  id: number;
  studentId: number;
  heightCm: number | null;
  weightKg: number | null;
  allergies: string | null;
  medicalConditions: string | null;
  doctorName: string | null;
  doctorContact: string | null;
}

export interface StudentDocument {
  id: number;
  studentId: number;
  documentType: string;
  fileUrl: string;
  uploadedAt: string;
}

/**
 * Students may or may not have a linked `users` row (`userId` is nullable per
 * the schema contract), so first/last name are carried as flattened
 * convenience fields on the student resource itself (see reconciliation note
 * in the round-2 report). `guardians`/`medicalDetails`/`documents` are only
 * populated on the single-student GET.
 */
export interface Student {
  id: number;
  userId: number | null;
  admissionNumber: string;
  // Null when the student has no login account (user_id is nullable per schema) —
  // use getStudentDisplayName()/getStudentInitials() from '@/utils/format' instead
  // of concatenating these directly.
  firstName: string | null;
  lastName: string | null;
  email?: string | null;
  phone?: string | null;
  primaryGuardianName?: string | null;
  primaryGuardianPhone?: string | null;
  classId: number;
  className?: string;
  sectionId: number;
  sectionName?: string;
  rollNumber: string;
  admissionDate: string;
  dateOfBirth: string;
  gender: Gender;
  bloodGroup: string | null;
  religion: string | null;
  category: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  pincode: string | null;
  photoUrl: string | null;
  academicYearId: number;
  status: StudentStatus;
  guardians?: Guardian[];
  medicalDetails?: MedicalDetails | null;
  documents?: StudentDocument[];
}

/* ------------------------------------------------------------------------ */
/* Attendance, Leave & Fees (Round 3).                                       */
/* Field names mirror SCHEMA_CONTRACT.md tables verbatim (camelCased).       */
/* ------------------------------------------------------------------------ */

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'HALF_DAY' | 'LEAVE';

/** One row of the editable "mark attendance" grid — GET /attendance/students. */
export interface StudentAttendanceRow {
  studentId: number;
  firstName: string | null;
  lastName: string | null;
  rollNumber: string;
  status: AttendanceStatus | null;
  remarks: string | null;
}

/** One row of the paginated student attendance report/register. */
export interface StudentAttendanceReportRow {
  id: number;
  studentId: number;
  firstName?: string | null;
  lastName?: string | null;
  rollNumber?: string;
  classId?: number;
  className?: string;
  sectionId?: number;
  sectionName?: string;
  attendanceDate: string;
  status: AttendanceStatus;
  remarks: string | null;
}

export interface StudentAttendanceSummary {
  presentDays: number;
  absentDays: number;
  lateDays: number;
  halfDays: number;
  leaveDays: number;
  totalMarkedDays: number;
  percentage: number;
}

/** One row of the month-wise attendance register/calendar grid. */
export interface MonthlyAttendanceRow {
  studentId: number;
  firstName: string | null;
  lastName: string | null;
  rollNumber: string;
  days: Record<string, AttendanceStatus>;
}

/** One row of the teacher "mark attendance" grid — GET /attendance/teachers. */
export interface TeacherAttendanceRow {
  teacherId: number;
  firstName: string;
  lastName: string;
  employeeId?: string;
  status: AttendanceStatus | null;
  checkIn: string | null;
  checkOut: string | null;
  remarks: string | null;
}

/** One row of the paginated teacher attendance report. */
export interface TeacherAttendanceReportRow {
  id: number;
  teacherId: number;
  firstName?: string;
  lastName?: string;
  employeeId?: string;
  attendanceDate: string;
  status: AttendanceStatus;
  checkIn: string | null;
  checkOut: string | null;
  remarks: string | null;
}

export type LeaveApplicantType = 'TEACHER' | 'STAFF' | 'STUDENT';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface LeaveApplication {
  id: number;
  applicantId: number;
  applicantType: LeaveApplicantType;
  // Denormalised convenience fields (same pattern as Student/Teacher list rows).
  applicantName?: string;
  applicantEmail?: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  reason: string;
  status: LeaveStatus;
  approvedBy: number | null;
  approvedByName?: string | null;
  appliedAt: string;
}

export interface FeeCategory {
  id: number;
  name: string;
  description: string | null;
}

export interface FeeStructure {
  id: number;
  classId: number;
  className?: string;
  academicYearId: number;
  academicYearName?: string;
  feeCategoryId: number;
  feeCategoryName?: string;
  amount: number;
  dueDate: string;
}

export type StudentFeeStatus = 'PAID' | 'UNPAID' | 'PARTIAL' | 'OVERDUE';

export interface StudentFee {
  id: number;
  studentId: number;
  // Denormalised convenience fields per the schema contract note for this endpoint.
  studentName?: string;
  admissionNumber?: string;
  classId?: number;
  className?: string;
  sectionId?: number;
  sectionName?: string;
  feeStructureId: number;
  feeCategoryId?: number;
  feeCategoryName?: string;
  academicYearId: number;
  academicYearName?: string;
  amountDue: number;
  amountPaid: number;
  dueDate: string;
  status: StudentFeeStatus;
  /** Only populated on GET /student-fees/{id}. */
  feePayments?: FeePayment[];
}

export type PaymentMode = 'CASH' | 'ONLINE' | 'CHEQUE' | 'CARD';

export interface FeePayment {
  id: number;
  studentFeeId: number;
  amount: number;
  paymentDate: string;
  paymentMode: PaymentMode;
  transactionId: string | null;
  receiptNumber: string;
  collectedBy: number | null;
  collectedByName?: string | null;
}

/** Printable receipt DTO — GET /fee-payments/{id}/receipt. Shape is a best-effort
 * guess (ASSUMPTION - reconcile with backend); every field is rendered defensively. */
export interface FeeReceipt {
  paymentId: number;
  receiptNumber: string;
  paymentDate: string;
  amount: number;
  paymentMode: PaymentMode;
  transactionId: string | null;
  studentName?: string;
  admissionNumber?: string;
  className?: string;
  sectionName?: string;
  feeCategoryName?: string;
  academicYearName?: string;
  collectedByName?: string | null;
  schoolName?: string;
  schoolAddress?: string;
}

export interface DuesSummary {
  totalDue: number;
  totalCollected: number;
  totalOutstanding: number;
  studentCount: number;
}

export interface GenerateDuesResult {
  generatedCount: number;
  skippedCount: number;
}

export type ScholarshipType = 'PERCENTAGE' | 'FIXED';

export interface Scholarship {
  id: number;
  studentId: number;
  studentName?: string;
  admissionNumber?: string;
  title: string;
  amount: number;
  type: ScholarshipType;
  academicYearId: number;
  academicYearName?: string;
  approvedBy: number | null;
}

/* ------------------------------------------------------------------------ */
/* Library, Transport & Hostel (Round 4).                                    */
/* Field names mirror SCHEMA_CONTRACT.md tables verbatim (camelCased).       */
/* ------------------------------------------------------------------------ */

export interface BookCategory {
  id: number;
  name: string;
}

export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  categoryId: number;
  categoryName?: string;
  publisher: string | null;
  totalCopies: number;
  availableCopies: number;
  rackNumber: string | null;
  price: number | null;
}

export type BookIssueStatus = 'ISSUED' | 'RETURNED' | 'OVERDUE';

export interface BookIssue {
  id: number;
  bookId: number;
  bookTitle?: string;
  isbn?: string;
  studentId: number | null;
  studentName?: string | null;
  teacherId: number | null;
  teacherName?: string | null;
  issueDate: string;
  dueDate: string;
  returnDate: string | null;
  fineAmount: number;
  status: BookIssueStatus;
}

export interface LibraryDashboard {
  totalBooks: number;
  totalCopies: number;
  availableCopies: number;
  issuedCount: number;
  overdueCount: number;
}

export interface Driver {
  id: number;
  name: string;
  phone: string;
  licenseNumber: string;
  address: string | null;
}

export interface Bus {
  id: number;
  busNumber: string;
  capacity: number;
  driverId: number | null;
  driverName?: string | null;
  vehicleModel: string | null;
  registrationNumber: string;
  routeCount?: number;
}

export interface Route {
  id: number;
  routeName: string;
  busId: number;
  busNumber?: string;
  startPoint: string;
  endPoint: string;
  pickupPointCount?: number;
}

export interface PickupPoint {
  id: number;
  routeId: number;
  pointName: string;
  pickupTime: string;
  dropTime: string;
}

export interface StudentTransport {
  id: number;
  studentId: number;
  studentName?: string;
  admissionNumber?: string;
  routeId: number;
  routeName?: string;
  pickupPointId: number;
  pickupPointName?: string;
  monthlyFee: number;
}

export type HostelType = 'BOYS' | 'GIRLS';

export interface Hostel {
  id: number;
  name: string;
  wardenName: string | null;
  wardenContact: string | null;
  type: HostelType;
}

export interface HostelRoom {
  id: number;
  hostelId: number;
  hostelName?: string;
  roomNumber: string;
  capacity: number;
  occupiedCount: number;
}

export type HostelStudentStatus = 'ACTIVE' | 'VACATED';

export interface HostelStudent {
  id: number;
  studentId: number;
  studentName?: string;
  admissionNumber?: string;
  roomId: number;
  roomNumber?: string;
  hostelId?: number;
  hostelName?: string;
  allocationDate: string;
  vacateDate: string | null;
  status: HostelStudentStatus;
}

export interface HostelVisitor {
  id: number;
  studentId: number;
  studentName?: string;
  visitorName: string;
  relation: string;
  phone: string | null;
  visitDate: string;
  purpose: string | null;
  checkIn: string;
  checkOut: string | null;
}

export type HostelFeePaidStatus = 'PAID' | 'UNPAID';

export interface HostelFee {
  id: number;
  studentId: number;
  studentName?: string;
  month: number;
  year: number;
  amount: number;
  paidStatus: HostelFeePaidStatus;
}

/* ------------------------------------------------------------------------ */
/* Exams, Assignments, Online Classes, Notices, Calendar & Admission         */
/* (Round 5). Field names mirror SCHEMA_CONTRACT.md tables verbatim.        */
/* ------------------------------------------------------------------------ */

export interface ExamType {
  id: number;
  name: string;
}

export interface Exam {
  id: number;
  examTypeId: number;
  examTypeName?: string;
  classId: number;
  className?: string;
  academicYearId: number;
  academicYearName?: string;
  startDate: string;
  endDate: string;
}

export interface ExamSchedule {
  id: number;
  examId: number;
  subjectId: number;
  subjectName?: string;
  examDate: string;
  startTime: string;
  endTime: string;
  maxMarks: number;
  roomNumber: string | null;
}

export interface Grade {
  id: number;
  gradeName: string;
  minPercentage: number;
  maxPercentage: number;
  gradePoint: number;
}

export interface Mark {
  id: number;
  examScheduleId: number;
  studentId: number;
  studentName?: string;
  rollNumber?: string;
  marksObtained: number | null;
  gradeId: number | null;
  gradeName?: string | null;
  remarks: string | null;
  enteredBy?: number | null;
}

/**
 * One row per active student in an exam schedule's class, with their existing
 * mark if any (null otherwise) — the roster powers the marks-entry grid.
 * Unlike `Mark`, this always includes every student, entered or not, the
 * same "roster with nullable status" shape as student attendance.
 */
export interface MarkRosterRow {
  studentId: number;
  firstName: string | null;
  lastName: string | null;
  rollNumber: number | null;
  sectionName: string;
  marksObtained: number | null;
  maxMarks: number;
  gradeName: string | null;
  remarks: string | null;
}

export interface ReportCardSubjectRow {
  subjectName: string;
  marksObtained: number;
  maxMarks: number;
  gradeName?: string | null;
}

export interface ReportCard {
  studentName: string;
  className: string;
  sectionName: string;
  examName: string;
  subjects: ReportCardSubjectRow[];
  totalObtained: number;
  totalMax: number;
  overallPercentage: number;
  overallGrade: string;
}

export interface ExamResultRow {
  studentId: number;
  studentName: string;
  rollNumber: string;
  totalObtained: number;
  totalMax: number;
  percentage: number;
  rank: number;
}

export interface Assignment {
  id: number;
  classId: number;
  className?: string;
  sectionId: number;
  sectionName?: string;
  subjectId: number;
  subjectName?: string;
  teacherId: number;
  teacherName?: string;
  title: string;
  description: string | null;
  fileUrl: string | null;
  assignedDate: string;
  dueDate: string;
}

export type AssignmentSubmissionStatus = 'SUBMITTED' | 'LATE' | 'GRADED';

export interface AssignmentSubmission {
  id: number;
  assignmentId: number;
  studentId: number;
  studentName?: string;
  rollNumber?: string;
  fileUrl: string;
  submittedAt: string;
  marksObtained: number | null;
  feedback: string | null;
  status: AssignmentSubmissionStatus;
}

export interface OnlineClass {
  id: number;
  classId: number;
  className?: string;
  sectionId: number;
  sectionName?: string;
  subjectId: number;
  subjectName?: string;
  teacherId: number;
  teacherName?: string;
  title: string;
  meetingLink: string;
  scheduledAt: string;
  durationMinutes: number;
}

export interface Notice {
  id: number;
  title: string;
  description: string;
  targetRole: Role | null;
  publishedBy: number;
  publishedByName?: string;
  publishedAt: string;
  expiryDate: string | null;
  attachmentUrl: string | null;
}

export type CalendarEventType = 'HOLIDAY' | 'EVENT' | 'EXAM' | 'OTHER';

export interface CalendarEvent {
  id: number;
  title: string;
  description: string | null;
  eventDate: string;
  eventType: CalendarEventType;
  createdBy?: number | null;
}

/** One row of GET /calendar/birthdays?month= — students/teachers with a birthday that month. */
export interface BirthdayPerson {
  id: number;
  firstName: string | null;
  lastName: string | null;
  dateOfBirth: string;
  className?: string;
  sectionName?: string;
  designationName?: string;
}

export interface BirthdaysResponse {
  students: BirthdayPerson[];
  teachers: BirthdayPerson[];
}

export type AdmissionEnquiryStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AdmissionEnquiry {
  id: number;
  studentName: string;
  parentName: string;
  phone: string;
  email: string;
  classApplying: string;
  dob: string;
  address: string;
  status: AdmissionEnquiryStatus;
  documentsUrl: string | null;
  appliedAt: string;
}

/* ------------------------------------------------------------------------ */
/* Payroll/HR, Parent Portal, Communication, Reports/Analytics & Settings    */
/* (Round 6). Field names mirror SCHEMA_CONTRACT.md tables verbatim.        */
/* ------------------------------------------------------------------------ */

export type PayrollEmployeeType = 'TEACHER' | 'STAFF';
export type PayrollStatus = 'PENDING' | 'PAID';

/** salary_structures row. `employeeId` is the person's `users.id`, NOT their teacher/staff-table id. */
export interface SalaryStructure {
  id: number;
  employeeId: number;
  employeeType: PayrollEmployeeType;
  // Denormalised convenience field (same pattern as Teacher/Student list rows) - ASSUMPTION,
  // reconcile with backend: expecting the employee's display name to come back with the row.
  employeeName?: string;
  basicSalary: number;
  hra: number;
  da: number;
  otherAllowances: number;
  pfPercentage: number;
  esiPercentage: number;
}

/** payroll row for one employee/month/year. */
export interface PayrollRun {
  id: number;
  employeeId: number;
  employeeType: PayrollEmployeeType;
  employeeName?: string;
  month: number;
  year: number;
  basicSalary: number;
  allowances: number;
  deductions: number;
  pf: number;
  esi: number;
  netSalary: number;
  paymentDate: string | null;
  status: PayrollStatus;
}

export interface GeneratePayrollResult {
  generatedCount: number;
  skippedCount: number;
}

export interface PayrollDashboardSummary {
  totalPaid: number;
  totalPending: number;
  employeeCount: number;
}

/** Printable salary slip DTO — matches SalarySlipDto exactly (GET /payroll/{id}/salary-slip). */
export interface SalarySlip {
  employeeName: string | null;
  employeeId: string | null;
  departmentName: string | null;
  designationName: string | null;
  month: number;
  year: number;
  basicSalary: number;
  hra: number | null;
  da: number | null;
  otherAllowances: number | null;
  pf: number;
  esi: number;
  netSalary: number;
  schoolName?: string;
}

/** One row of GET /parents/me/children. */
export interface ParentChild {
  studentId: number;
  firstName: string | null;
  lastName: string | null;
  admissionNumber: string;
  className?: string;
  sectionName?: string;
  photoUrl: string | null;
}

export type NotificationChannel = 'SMS' | 'EMAIL' | 'PUSH' | 'IN_APP';
export type NotificationDeliveryStatus = 'PENDING' | 'SENT' | 'FAILED';

/** notifications row — named NotificationRecord (not `Notification`/`AppNotification`) to avoid
 * colliding with the unrelated client-side toast type in store/notificationSlice.ts. */
export interface NotificationRecord {
  id: number;
  recipientId: number;
  recipientName?: string;
  type: NotificationChannel;
  subject: string;
  message: string;
  status: NotificationDeliveryStatus;
  sentAt: string | null;
}

/** POST /notifications/send body — exactly one of recipientId/targetRole must be set. */
export interface SendNotificationPayload {
  recipientId?: number;
  targetRole?: Role;
  type: NotificationChannel;
  subject: string;
  message: string;
}

export interface ClassCountBreakdown {
  className: string;
  count: number;
}

export interface StatusCountBreakdown {
  status: string;
  count: number;
}

export interface StudentsSummaryReport {
  totalActive: number;
  byClass: ClassCountBreakdown[];
  byStatus: StatusCountBreakdown[];
}

export interface DepartmentCountBreakdown {
  departmentName: string;
  count: number;
}

export interface TeachersSummaryReport {
  totalActive: number;
  byDepartment: DepartmentCountBreakdown[];
}

export interface ClassPercentageBreakdown {
  className: string;
  percentage: number;
}

export interface AttendanceSummaryReport {
  averagePercentage: number;
  byClass: ClassPercentageBreakdown[];
}

export interface CategoryCollectedBreakdown {
  categoryName: string;
  collected: number;
}

export interface MonthCollectedBreakdown {
  month: string | number;
  collected: number;
}

export interface FeeCollectionReport {
  totalDue: number;
  totalCollected: number;
  totalOutstanding: number;
  byCategory: CategoryCollectedBreakdown[];
  byMonth: MonthCollectedBreakdown[];
}

export interface MonthPaidBreakdown {
  month: string | number;
  paidAmount: number;
}

export interface PayrollSummaryReport {
  totalPaidAmount: number;
  totalPendingAmount: number;
  byMonth: MonthPaidBreakdown[];
}

export interface CategoryCountBreakdown {
  categoryName: string;
  count: number;
}

export interface LibrarySummaryReport {
  totalBooks: number;
  totalIssued: number;
  totalOverdue: number;
  byCategory: CategoryCountBreakdown[];
}

export interface TransportSummaryReport {
  totalBuses: number;
  totalRoutes: number;
  studentsUsingTransport: number;
}

/**
 * GET /analytics/dashboard — "combined headline numbers" per the contract, with no
 * exact shape specified (ASSUMPTION - reconcile with backend). Modelled as the most
 * likely cross-module rollup; every field optional and rendered defensively with a
 * '-' fallback so the Overview page never breaks if a field is missing.
 */
/** Matches AnalyticsDashboardDto exactly (composed by the backend from ReportService's own methods). */
export interface AnalyticsDashboard {
  totalActiveStudents: number;
  totalActiveTeachers: number;
  totalFeeCollected: number;
  totalFeeOutstanding: number;
  averageAttendancePercentage: number;
}

/** Singleton school_info row — GET/PUT /settings/school-info always returns something, even if empty. */
export interface SchoolInfo {
  id?: number;
  name: string;
  address: string;
  phone: string;
  email: string;
  logoUrl?: string | null;
  establishedYear?: number | null;
  affiliationNumber?: string | null;
}

/** system_settings row, as returned by GET /settings/system: [{key, value}]. */
export interface SystemSetting {
  key: string;
  value: string;
}

export interface RoleInfo {
  id: number;
  name: string;
  description: string | null;
}

export interface PermissionInfo {
  id: number;
  name: string;
  module: string;
  description: string | null;
}

/** Minimal read-only staff-directory row — GET /staff, powers Payroll's STAFF employee search. */
export interface Staff {
  id: number;
  userId: number | null;
  employeeId: string;
  firstName: string | null;
  lastName: string | null;
  departmentName: string | null;
  designationName: string | null;
  status: StaffStatus;
}

/* ------------------------------------------------------------------------ */
/* PDF/Excel export-import, Audit Logs & Global Search (Round 7).           */
/* ------------------------------------------------------------------------ */

/** One skipped row from POST /students/import/excel, with the reason it wasn't imported. */
export interface ImportSkippedRow {
  rowNumber: number;
  reason: string;
}

export interface ImportResult {
  importedCount: number;
  skippedRows: ImportSkippedRow[];
}

/** audit_logs row — GET /audit-logs (paginated). oldValue/newValue are raw JSON strings from the backend. */
export interface AuditLog {
  id: number;
  userId: number | null;
  userName?: string | null;
  action: string;
  entityName: string;
  entityId: number | null;
  oldValue: string | null;
  newValue: string | null;
  ipAddress: string | null;
  createdAt: string;
}

/**
 * A single result row from GET /search/global. Shape is a best-effort guess
 * (ASSUMPTION - reconcile with backend): every display-ish field is optional
 * so the dropdown can defensively fall back across students/teachers/books
 * without knowing exactly which fields the backend chose to populate.
 */
export interface GlobalSearchResultItem {
  id: number;
  displayName?: string;
  name?: string;
  title?: string;
  subtitle?: string | null;
  admissionNumber?: string;
  employeeId?: string;
  isbn?: string;
  className?: string;
  sectionName?: string;
}

export interface GlobalSearchResponse {
  students: GlobalSearchResultItem[];
  teachers: GlobalSearchResultItem[];
  books: GlobalSearchResultItem[];
}
