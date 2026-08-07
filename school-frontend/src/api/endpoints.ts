/**
 * Central registry of API endpoint paths (relative to VITE_API_BASE_URL, which
 * already includes /api/v1). Add new module endpoints here in later rounds
 * instead of hard-coding path strings inside components.
 */
export const ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH_TOKEN: '/auth/refresh-token',
    LOGOUT: '/auth/logout',
    FORGOT_PASSWORD: '/auth/forgot-password',
    RESET_PASSWORD: '/auth/reset-password',
    CHANGE_PASSWORD: '/auth/change-password',
    ME: '/auth/me',
  },

  ACADEMIC_YEARS: {
    BASE: '/academic-years',
    BY_ID: (id: number | string) => `/academic-years/${id}`,
    SET_CURRENT: (id: number | string) => `/academic-years/${id}/set-current`,
  },

  DEPARTMENTS: {
    BASE: '/departments',
    BY_ID: (id: number | string) => `/departments/${id}`,
  },

  DESIGNATIONS: {
    BASE: '/designations',
    BY_ID: (id: number | string) => `/designations/${id}`,
  },

  CLASSES: {
    BASE: '/classes',
    BY_ID: (id: number | string) => `/classes/${id}`,
    SECTIONS: (classId: number | string) => `/classes/${classId}/sections`,
    SUBJECTS: (classId: number | string) => `/classes/${classId}/subjects`,
  },

  SECTIONS: {
    BY_ID: (id: number | string) => `/sections/${id}`,
    ASSIGN_CLASS_TEACHER: (id: number | string) => `/sections/${id}/assign-class-teacher`,
  },

  SUBJECTS: {
    BY_ID: (id: number | string) => `/subjects/${id}`,
  },

  CLASS_SUBJECT_TEACHER: {
    BASE: '/class-subject-teacher',
    BY_ID: (id: number | string) => `/class-subject-teacher/${id}`,
  },

  TEACHERS: {
    BASE: '/teachers',
    BY_ID: (id: number | string) => `/teachers/${id}`,
    STATUS: (id: number | string) => `/teachers/${id}/status`,
    ID_CARD_PDF: (id: number | string) => `/teachers/${id}/id-card/pdf`,
    EXPORT_EXCEL: '/teachers/export/excel',
  },

  STAFF: {
    BASE: '/staff',
  },

  STUDENTS: {
    BASE: '/students',
    BY_ID: (id: number | string) => `/students/${id}`,
    STATUS: (id: number | string) => `/students/${id}/status`,
    PHOTO: (id: number | string) => `/students/${id}/photo`,
    GUARDIANS: (id: number | string) => `/students/${id}/guardians`,
    GUARDIAN_BY_ID: (id: number | string, guardianId: number | string) =>
      `/students/${id}/guardians/${guardianId}`,
    MEDICAL_DETAILS: (id: number | string) => `/students/${id}/medical-details`,
    DOCUMENTS: (id: number | string) => `/students/${id}/documents`,
    DOCUMENT_BY_ID: (id: number | string, docId: number | string) =>
      `/students/${id}/documents/${docId}`,
    PROMOTE: '/students/promote',
    TRANSFER: (id: number | string) => `/students/${id}/transfer`,
    MARK_ALUMNI: (id: number | string) => `/students/${id}/mark-alumni`,
    ID_CARD_PDF: (id: number | string) => `/students/${id}/id-card/pdf`,
    EXPORT_EXCEL: '/students/export/excel',
    IMPORT_EXCEL: '/students/import/excel',
  },

  ATTENDANCE: {
    STUDENTS: '/attendance/students',
    STUDENTS_MARK: '/attendance/students/mark',
    STUDENTS_REPORT: '/attendance/students/report',
    STUDENT_SUMMARY: (studentId: number | string) => `/attendance/students/${studentId}/summary`,
    STUDENTS_MONTHLY: '/attendance/students/monthly',
    TEACHERS: '/attendance/teachers',
    TEACHERS_MARK: '/attendance/teachers/mark',
    TEACHERS_REPORT: '/attendance/teachers/report',
  },

  LEAVE_APPLICATIONS: {
    BASE: '/leave-applications',
    MY: '/leave-applications/my',
    BY_ID: (id: number | string) => `/leave-applications/${id}`,
    APPROVE: (id: number | string) => `/leave-applications/${id}/approve`,
    REJECT: (id: number | string) => `/leave-applications/${id}/reject`,
  },

  FEE_CATEGORIES: {
    BASE: '/fee-categories',
    BY_ID: (id: number | string) => `/fee-categories/${id}`,
  },

  FEE_STRUCTURES: {
    BASE: '/fee-structures',
    BY_ID: (id: number | string) => `/fee-structures/${id}`,
  },

  STUDENT_FEES: {
    BASE: '/student-fees',
    BY_ID: (id: number | string) => `/student-fees/${id}`,
    GENERATE: '/student-fees/generate',
  },

  FEE_PAYMENTS: {
    BASE: '/fee-payments',
    RECEIPT: (id: number | string) => `/fee-payments/${id}/receipt`,
    RECEIPT_PDF: (id: number | string) => `/fee-payments/${id}/receipt/pdf`,
  },

  FEES: {
    DUES_SUMMARY: '/fees/dues-summary',
  },

  SCHOLARSHIPS: {
    BASE: '/scholarships',
    BY_ID: (id: number | string) => `/scholarships/${id}`,
  },

  BOOK_CATEGORIES: {
    BASE: '/book-categories',
    BY_ID: (id: number | string) => `/book-categories/${id}`,
  },

  BOOKS: {
    BASE: '/books',
    BY_ID: (id: number | string) => `/books/${id}`,
  },

  BOOK_ISSUES: {
    BASE: '/book-issues',
    ISSUE: '/book-issues/issue',
    RETURN: (id: number | string) => `/book-issues/${id}/return`,
    OVERDUE: '/book-issues/overdue',
  },

  LIBRARY: {
    DASHBOARD: '/library/dashboard',
  },

  DRIVERS: {
    BASE: '/drivers',
    BY_ID: (id: number | string) => `/drivers/${id}`,
  },

  BUSES: {
    BASE: '/buses',
    BY_ID: (id: number | string) => `/buses/${id}`,
  },

  ROUTES: {
    BASE: '/routes',
    BY_ID: (id: number | string) => `/routes/${id}`,
    PICKUP_POINTS: (routeId: number | string) => `/routes/${routeId}/pickup-points`,
  },

  PICKUP_POINTS: {
    BY_ID: (id: number | string) => `/pickup-points/${id}`,
  },

  STUDENT_TRANSPORT: {
    BASE: '/student-transport',
    BY_ID: (id: number | string) => `/student-transport/${id}`,
  },

  HOSTELS: {
    BASE: '/hostels',
    BY_ID: (id: number | string) => `/hostels/${id}`,
    ROOMS: (hostelId: number | string) => `/hostels/${hostelId}/rooms`,
  },

  HOSTEL_ROOMS: {
    BY_ID: (id: number | string) => `/hostel-rooms/${id}`,
  },

  HOSTEL_STUDENTS: {
    BASE: '/hostel-students',
    BY_ID: (id: number | string) => `/hostel-students/${id}`,
    VACATE: (id: number | string) => `/hostel-students/${id}/vacate`,
  },

  HOSTEL_VISITORS: {
    BASE: '/hostel-visitors',
    CHECKOUT: (id: number | string) => `/hostel-visitors/${id}/checkout`,
  },

  HOSTEL_FEES: {
    BASE: '/hostel-fees',
    MARK_PAID: (id: number | string) => `/hostel-fees/${id}/mark-paid`,
  },

  EXAM_TYPES: {
    BASE: '/exam-types',
    BY_ID: (id: number | string) => `/exam-types/${id}`,
  },

  EXAMS: {
    BASE: '/exams',
    BY_ID: (id: number | string) => `/exams/${id}`,
    SCHEDULES: (examId: number | string) => `/exams/${examId}/schedules`,
    RESULTS: (examId: number | string) => `/exams/${examId}/results`,
  },

  EXAM_SCHEDULES: {
    BY_ID: (id: number | string) => `/exam-schedules/${id}`,
  },

  MARKS: {
    BASE: '/marks',
    ROSTER: '/marks/roster',
    ENTRY: '/marks/entry',
    REPORT_CARD: (studentId: number | string) => `/marks/report-card/${studentId}`,
    REPORT_CARD_PDF: (studentId: number | string) => `/marks/report-card/${studentId}/pdf`,
  },

  ASSIGNMENTS: {
    BASE: '/assignments',
    BY_ID: (id: number | string) => `/assignments/${id}`,
    SUBMISSIONS: (assignmentId: number | string) => `/assignments/${assignmentId}/submissions`,
    MY_SUBMISSION: (assignmentId: number | string) => `/assignments/${assignmentId}/submissions/my`,
    SUBMIT: (assignmentId: number | string) => `/assignments/${assignmentId}/submit`,
  },

  ASSIGNMENT_SUBMISSIONS: {
    GRADE: (id: number | string) => `/assignment-submissions/${id}/grade`,
  },

  ONLINE_CLASSES: {
    BASE: '/online-classes',
    BY_ID: (id: number | string) => `/online-classes/${id}`,
  },

  NOTICES: {
    BASE: '/notices',
    BY_ID: (id: number | string) => `/notices/${id}`,
  },

  EVENTS: {
    BASE: '/events',
    BY_ID: (id: number | string) => `/events/${id}`,
  },

  CALENDAR: {
    BIRTHDAYS: '/calendar/birthdays',
  },

  ADMISSION_ENQUIRIES: {
    PUBLIC_CREATE: '/public/admission-enquiries',
    BASE: '/admission-enquiries',
    STATUS: (id: number | string) => `/admission-enquiries/${id}/status`,
    BY_ID: (id: number | string) => `/admission-enquiries/${id}`,
  },

  SALARY_STRUCTURES: {
    BASE: '/salary-structures',
    BY_ID: (id: number | string) => `/salary-structures/${id}`,
  },

  PAYROLL: {
    BASE: '/payroll',
    GENERATE: '/payroll/generate',
    MARK_PAID: (id: number | string) => `/payroll/${id}/mark-paid`,
    SALARY_SLIP: (id: number | string) => `/payroll/${id}/salary-slip`,
    SALARY_SLIP_PDF: (id: number | string) => `/payroll/${id}/salary-slip/pdf`,
    DASHBOARD: '/payroll/dashboard',
  },

  PARENTS: {
    MY_CHILDREN: '/parents/me/children',
  },

  NOTIFICATIONS: {
    MY: '/notifications/my',
    SEND: '/notifications/send',
  },

  REPORTS: {
    STUDENTS_SUMMARY: '/reports/students-summary',
    TEACHERS_SUMMARY: '/reports/teachers-summary',
    ATTENDANCE_SUMMARY: '/reports/attendance-summary',
    FEE_COLLECTION: '/reports/fee-collection',
    FEE_COLLECTION_EXPORT_EXCEL: '/reports/fee-collection/export/excel',
    PAYROLL_SUMMARY: '/reports/payroll-summary',
    LIBRARY_SUMMARY: '/reports/library-summary',
    TRANSPORT_SUMMARY: '/reports/transport-summary',
  },

  ANALYTICS: {
    DASHBOARD: '/analytics/dashboard',
  },

  SETTINGS: {
    SCHOOL_INFO: '/settings/school-info',
    SYSTEM: '/settings/system',
    BACKUP_EXPORT: '/settings/backup/export',
  },

  ROLES: {
    BASE: '/roles',
    PERMISSIONS: (id: number | string) => `/roles/${id}/permissions`,
  },

  AUDIT_LOGS: {
    BASE: '/audit-logs',
  },

  SEARCH: {
    GLOBAL: '/search/global',
  },
} as const;
