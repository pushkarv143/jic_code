-- =====================================================================
-- 02_indexes.sql
-- Additional performance indexes, beyond what InnoDB already creates
-- implicitly for PRIMARY KEY / UNIQUE / FOREIGN KEY columns.
-- Run after 01_schema.sql.
-- =====================================================================

USE school_management_system;

-- Students: the most common lookup is "all students in a class+section"
-- (roster screens, attendance marking, fee runs).
CREATE INDEX idx_students_class_section ON students(class_id, section_id);

-- Students: admission number is looked up constantly at the front desk
-- and in the admissions/search screens (separate from the UNIQUE key's
-- own index, this keeps the intent documented and is a no-op if MySQL
-- already covers it via the UNIQUE constraint).
CREATE INDEX idx_students_admission_number ON students(admission_number);

-- Students: filtering active/alumni/transferred rosters per year.
CREATE INDEX idx_students_status ON students(status);

-- Attendance: date-range reports ("attendance for October") scan by date
-- across all students, independent of which student.
CREATE INDEX idx_student_attendance_date ON student_attendance(attendance_date);

-- Attendance: per-section daily register lookups.
CREATE INDEX idx_student_attendance_section_date ON student_attendance(section_id, attendance_date);

-- Teacher attendance: monthly payroll/HR reports scan by date.
CREATE INDEX idx_teacher_attendance_date ON teacher_attendance(attendance_date);

-- Users: login is by email, and role-based admin listings filter by role.
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);

-- Teachers: employee id is used on ID cards / HR search screens.
CREATE INDEX idx_teachers_employee_id ON teachers(employee_id);
CREATE INDEX idx_staff_employee_id ON staff(employee_id);

-- Fee payments: cashier "collections today/this month" reports.
CREATE INDEX idx_fee_payments_payment_date ON fee_payments(payment_date);

-- Student fees: dashboards filter outstanding dues by status.
CREATE INDEX idx_student_fees_status ON student_fees(status);
CREATE INDEX idx_student_fees_due_date ON student_fees(due_date);

-- Library: "my currently issued books" / overdue sweep runs by status.
CREATE INDEX idx_book_issues_status ON book_issues(status);
CREATE INDEX idx_book_issues_due_date ON book_issues(due_date);

-- Marks: report-card generation pulls all marks for a student across an
-- exam, and all marks for a schedule (class result entry) - composite
-- index covers the student-centric lookup used in vw_* / procedures.
CREATE INDEX idx_marks_student_schedule ON marks(student_id, exam_schedule_id);

-- Notices: the notice board / dashboard widget always orders by recency.
CREATE INDEX idx_notices_published_at ON notices(published_at);

-- Events/calendar: month-view calendar queries filter by event_date.
CREATE INDEX idx_events_event_date ON events(event_date);

-- Assignments: "assignments due this week" per section/subject.
CREATE INDEX idx_assignments_due_date ON assignments(due_date);

-- Study materials: every read is "what has been shared with my class/section",
-- which is the query a student's materials list runs on every page load.
CREATE INDEX idx_study_materials_class_section ON study_materials(class_id, section_id);
-- Teachers filter their own uploads to manage them.
CREATE INDEX idx_study_materials_teacher ON study_materials(teacher_id);

-- Leave applications: pending-approval queues filtered by status.
CREATE INDEX idx_leave_applications_status ON leave_applications(status);

-- Payroll: monthly payroll run lookups by (month, year) across employees.
CREATE INDEX idx_payroll_month_year ON payroll(month, year);

-- Hostel students: active occupant lookups per room during allocation.
CREATE INDEX idx_hostel_students_status ON hostel_students(status);

-- Admission enquiries: front-office pipeline filtered by status.
CREATE INDEX idx_admission_enquiries_status ON admission_enquiries(status);

-- Notifications: worker picks up PENDING rows to dispatch.
CREATE INDEX idx_notifications_status ON notifications(status);
