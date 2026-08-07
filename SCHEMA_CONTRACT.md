# School Management System — Master Schema Contract

This file is the single source of truth for table names, column names, enums and
relationships across the whole project (backend entities, SQL scripts, frontend
DTOs). Every build round (backend, database, frontend, and every module added
later) MUST read this file first and stay consistent with it. If a round needs
to add a table/column not listed here, it must APPEND it to this file (never
silently invent a name that conflicts with something already listed).

Naming conventions: snake_case in MySQL, camelCase (Java) / camelCase (TS) in code.
Every table has: id BIGINT AUTO_INCREMENT PK, created_at DATETIME, updated_at DATETIME
(and created_by / updated_by BIGINT nullable FK -> users.id) unless noted otherwise.
Soft-delete flag `is_deleted TINYINT(1) DEFAULT 0` on business tables that support
delete-restore (students, teachers, staff, books, buses...).

## Roles (fixed set, seeded, id 1-11 in this exact order)
1 SUPER_ADMIN, 2 PRINCIPAL, 3 VICE_PRINCIPAL, 4 TEACHER, 5 CLASS_TEACHER,
6 ACCOUNTANT, 7 LIBRARIAN, 8 RECEPTIONIST, 9 STUDENT, 10 PARENT, 11 SECURITY_GUARD

## Core / Auth
- roles(id, name UNIQUE, description)
- permissions(id, name UNIQUE, module, description)
- role_permissions(id, role_id FK->roles, permission_id FK->permissions)
- users(id, username UNIQUE, email UNIQUE, password, first_name, last_name, phone,
  gender ENUM(MALE,FEMALE,OTHER), role_id FK->roles, is_active TINYINT(1) DEFAULT 1,
  is_email_verified TINYINT(1) DEFAULT 0, profile_image, last_login DATETIME,
  created_at, updated_at, created_by, updated_by)
- refresh_tokens(id, user_id FK->users, token VARCHAR(512) UNIQUE, expiry_date DATETIME, revoked TINYINT(1) DEFAULT 0, created_at)
- password_reset_tokens(id, user_id FK->users, token VARCHAR(255) UNIQUE, expiry_date DATETIME, used TINYINT(1) DEFAULT 0, created_at)
- audit_logs(id, user_id FK->users NULL, action, entity_name, entity_id, old_value TEXT, new_value TEXT, ip_address, created_at)

## Academic setup
- academic_years(id, year_name UNIQUE e.g. '2025-2026', start_date, end_date, is_current TINYINT(1))
- departments(id, name UNIQUE, description)
- designations(id, name UNIQUE, description)
- classes(id, class_name e.g. 'Class 1', academic_year_id FK, is_deleted)
- sections(id, section_name e.g. 'A', class_id FK->classes, class_teacher_id FK->teachers NULL, room_number, capacity)
- subjects(id, subject_name, subject_code UNIQUE, class_id FK->classes, is_elective TINYINT(1), is_deleted)
- class_subject_teacher(id, class_id FK, section_id FK, subject_id FK, teacher_id FK->teachers)

## People
- teachers(id, user_id FK->users UNIQUE, employee_id UNIQUE, department_id FK, designation_id FK,
  qualification, experience_years, joining_date, date_of_birth, gender, address, city, state, pincode,
  blood_group, emergency_contact, salary DECIMAL(12,2), employment_type ENUM(FULL_TIME,PART_TIME,CONTRACT),
  status ENUM(ACTIVE,INACTIVE,RESIGNED,TERMINATED), is_deleted)
- staff(id, user_id FK->users UNIQUE, employee_id UNIQUE, department_id FK, designation_id FK,
  joining_date, salary DECIMAL(12,2), status ENUM(ACTIVE,INACTIVE,RESIGNED,TERMINATED), is_deleted)
  -- staff_type stored via users.role_id (ACCOUNTANT/LIBRARIAN/RECEPTIONIST/SECURITY_GUARD)
- students(id, user_id FK->users UNIQUE NULL, admission_number UNIQUE, class_id FK, section_id FK,
  roll_number, admission_date, date_of_birth, gender, blood_group, religion, category,
  address, city, state, pincode, photo_url, academic_year_id FK,
  status ENUM(ACTIVE,INACTIVE,ALUMNI,TRANSFERRED), is_deleted)
- guardians(id, student_id FK->students, name, relation, occupation, phone, email, address, is_primary TINYINT(1))
- parents(id, user_id FK->users UNIQUE, occupation, annual_income DECIMAL(12,2))
- student_parents(id, student_id FK->students, parent_id FK->parents)
- student_medical_details(id, student_id FK->students UNIQUE, height_cm, weight_kg, allergies, medical_conditions, doctor_name, doctor_contact)
- student_documents(id, student_id FK->students, document_type, file_url, uploaded_at)

## Attendance & Leave
- student_attendance(id, student_id FK, class_id FK, section_id FK, attendance_date DATE,
  status ENUM(PRESENT,ABSENT,LATE,HALF_DAY,LEAVE), remarks, marked_by FK->users, created_at,
  UNIQUE(student_id, attendance_date))
- teacher_attendance(id, teacher_id FK, attendance_date DATE, status ENUM(PRESENT,ABSENT,LATE,HALF_DAY,LEAVE),
  check_in TIME, check_out TIME, remarks, UNIQUE(teacher_id, attendance_date))
- leave_applications(id, applicant_id FK->users, applicant_type ENUM(TEACHER,STAFF,STUDENT),
  leave_type, start_date, end_date, reason, status ENUM(PENDING,APPROVED,REJECTED),
  approved_by FK->users NULL, applied_at)

## Fees
- fee_categories(id, name UNIQUE, description)
- fee_structures(id, class_id FK, academic_year_id FK, fee_category_id FK, amount DECIMAL(12,2), due_date)
- student_fees(id, student_id FK, fee_structure_id FK, academic_year_id FK, amount_due DECIMAL(12,2),
  amount_paid DECIMAL(12,2) DEFAULT 0, due_date, status ENUM(PAID,UNPAID,PARTIAL,OVERDUE))
- fee_payments(id, student_fee_id FK, amount DECIMAL(12,2), payment_date, payment_mode ENUM(CASH,ONLINE,CHEQUE,CARD),
  transaction_id, receipt_number UNIQUE, collected_by FK->users)
- scholarships(id, student_id FK, title, amount DECIMAL(12,2), type ENUM(PERCENTAGE,FIXED), academic_year_id FK, approved_by FK->users)

## Transport
- drivers(id, name, phone, license_number UNIQUE, address)
- buses(id, bus_number UNIQUE, capacity, driver_id FK->drivers NULL, vehicle_model, registration_number UNIQUE)
- routes(id, route_name, bus_id FK->buses, start_point, end_point)
- pickup_points(id, route_id FK, point_name, pickup_time TIME, drop_time TIME)
- student_transport(id, student_id FK, route_id FK, pickup_point_id FK, monthly_fee DECIMAL(10,2))

## Library
- book_categories(id, name UNIQUE)
- books(id, title, author, isbn UNIQUE, category_id FK, publisher, total_copies, available_copies, rack_number, price DECIMAL(10,2), is_deleted)
- book_issues(id, book_id FK, student_id FK NULL, teacher_id FK NULL, issue_date, due_date, return_date NULL,
  fine_amount DECIMAL(10,2) DEFAULT 0, status ENUM(ISSUED,RETURNED,OVERDUE))

## Hostel
- hostels(id, name, warden_name, warden_contact, type ENUM(BOYS,GIRLS))
- hostel_rooms(id, hostel_id FK, room_number, capacity, occupied_count DEFAULT 0)
- hostel_students(id, student_id FK, room_id FK->hostel_rooms, allocation_date, vacate_date NULL, status ENUM(ACTIVE,VACATED))
- hostel_visitors(id, student_id FK, visitor_name, relation, phone, visit_date, purpose, check_in DATETIME, check_out DATETIME NULL)
- hostel_fees(id, student_id FK, month, year, amount DECIMAL(10,2), paid_status ENUM(PAID,UNPAID))

## Exam
- exam_types(id, name UNIQUE)
- exams(id, exam_type_id FK, class_id FK, academic_year_id FK, start_date, end_date)
- exam_schedules(id, exam_id FK, subject_id FK, exam_date, start_time TIME, end_time TIME, max_marks INT, room_number)
- grades(id, grade_name, min_percentage DECIMAL(5,2), max_percentage DECIMAL(5,2), grade_point DECIMAL(3,1))
- marks(id, exam_schedule_id FK, student_id FK, marks_obtained DECIMAL(5,2), grade_id FK->grades NULL, remarks, entered_by FK->users)

## Assignments / Online classes
- assignments(id, class_id FK, section_id FK, subject_id FK, teacher_id FK, title, description, file_url, assigned_date, due_date)
- assignment_submissions(id, assignment_id FK, student_id FK, file_url, submitted_at,
  marks_obtained DECIMAL(5,2) NULL, feedback, status ENUM(SUBMITTED,LATE,GRADED))
- online_classes(id, class_id FK, section_id FK, subject_id FK, teacher_id FK, title, meeting_link, scheduled_at DATETIME, duration_minutes)

## Notice / Calendar / Admission
- notices(id, title, description, target_role FK->roles NULL, published_by FK->users, published_at, expiry_date NULL, attachment_url)
- events(id, title, description, event_date DATE, event_type ENUM(HOLIDAY,EVENT,EXAM,OTHER), created_by FK->users)
- admission_enquiries(id, student_name, parent_name, phone, email, class_applying, dob, address,
  status ENUM(PENDING,APPROVED,REJECTED), documents_url, applied_at)

## Payroll / HR
- salary_structures(id, employee_id FK->users, employee_type ENUM(TEACHER,STAFF), basic_salary DECIMAL(12,2),
  hra DECIMAL(12,2), da DECIMAL(12,2), other_allowances DECIMAL(12,2), pf_percentage DECIMAL(5,2), esi_percentage DECIMAL(5,2))
- payroll(id, employee_id FK->users, employee_type ENUM(TEACHER,STAFF), month INT, year INT,
  basic_salary DECIMAL(12,2), allowances DECIMAL(12,2), deductions DECIMAL(12,2), pf DECIMAL(12,2),
  esi DECIMAL(12,2), net_salary DECIMAL(12,2), payment_date NULL, status ENUM(PENDING,PAID))

## Communication
- notifications(id, recipient_id FK->users, type ENUM(SMS,EMAIL,PUSH,IN_APP), subject, message, status ENUM(PENDING,SENT,FAILED), sent_at)

## Settings
- school_info(id, name, address, phone, email, logo_url, established_year, affiliation_number)
- system_settings(id, `setting_key` UNIQUE, `setting_value`)

## Backend package root
`com.school.sms` — base package for all Java code, artifact `school-backend`.

## Frontend
Base API path: `/api/v1`. Auth endpoints under `/api/v1/auth/**`.
JWT delivered as `{accessToken, refreshToken, tokenType, user}`.

## Seed credentials (use these EXACT bcrypt hashes in SQL seed data — verified against
Spring Security's BCryptPasswordEncoder, do not regenerate/guess new ones)
- Password `Admin@123` -> bcrypt hash `$2b$10$Ih9JH8QwmYKKqexbzqQRhOz6b8WE3i1QYFOABFL8F6l9pXFOqEP.q`
  Used for: the one seeded SUPER_ADMIN account, username `admin`, email `admin@school.edu`.
- Password `Password@123` -> bcrypt hash `$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa`
  Used for: every other seeded demo user (principal, vice principal, teachers, class teachers,
  accountant, librarian, receptionist, students, parents, security guard) — all 500 students,
  50 teachers, 30 staff etc. get this same hash as their `users.password` value.
Document both credentials clearly in the root README "Demo Logins" section.
