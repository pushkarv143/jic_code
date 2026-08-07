-- =====================================================================
-- 06_seed_reference_data.sql
-- Reference / master data + demo login users. Run after 01-05.
-- All lookups of FK ids use name-based subqueries (never hardcoded
-- numeric ids) so this file is robust regardless of auto_increment
-- start values.
-- =====================================================================

USE school_management_system;

-- =====================================================================
-- Roles (fixed set, id 1-11 in this exact order per contract)
-- =====================================================================
INSERT INTO roles (name, description) VALUES
  ('SUPER_ADMIN',    'Full system access across every module'),
  ('PRINCIPAL',      'School principal - academic and administrative oversight'),
  ('VICE_PRINCIPAL', 'Deputy to the principal'),
  ('TEACHER',        'Subject teacher'),
  ('CLASS_TEACHER',  'Teacher additionally in charge of a section'),
  ('ACCOUNTANT',     'Handles fees, payroll and accounts'),
  ('LIBRARIAN',      'Manages the library and book issues'),
  ('RECEPTIONIST',   'Front office / admissions desk'),
  ('STUDENT',        'Student portal login'),
  ('PARENT',         'Parent/guardian portal login'),
  ('SECURITY_GUARD', 'Gate security and visitor log');

-- =====================================================================
-- Permissions (grouped by module)
-- =====================================================================
INSERT INTO permissions (name, module, description) VALUES
  ('USER_VIEW','USER','View user accounts'),
  ('USER_CREATE','USER','Create user accounts'),
  ('USER_UPDATE','USER','Update user accounts'),
  ('USER_DELETE','USER','Delete/deactivate user accounts'),
  ('ROLE_VIEW','ROLE','View roles and permissions'),
  ('ROLE_MANAGE','ROLE','Manage roles and permission mappings'),
  ('STUDENT_VIEW','STUDENT','View student records'),
  ('STUDENT_CREATE','STUDENT','Create student records'),
  ('STUDENT_UPDATE','STUDENT','Update student records'),
  ('STUDENT_DELETE','STUDENT','Delete/deactivate student records'),
  ('TEACHER_VIEW','TEACHER','View teacher records'),
  ('TEACHER_CREATE','TEACHER','Create teacher records'),
  ('TEACHER_UPDATE','TEACHER','Update teacher records'),
  ('TEACHER_DELETE','TEACHER','Delete/deactivate teacher records'),
  ('STAFF_VIEW','STAFF','View staff records'),
  ('STAFF_CREATE','STAFF','Create staff records'),
  ('STAFF_UPDATE','STAFF','Update staff records'),
  ('STAFF_DELETE','STAFF','Delete/deactivate staff records'),
  ('CLASS_MANAGE','ACADEMIC','Manage classes'),
  ('SECTION_MANAGE','ACADEMIC','Manage sections'),
  ('SUBJECT_MANAGE','ACADEMIC','Manage subjects'),
  ('ATTENDANCE_VIEW','ATTENDANCE','View attendance records'),
  ('ATTENDANCE_MARK','ATTENDANCE','Mark daily attendance'),
  ('ATTENDANCE_REPORT','ATTENDANCE','Run attendance reports'),
  ('FEE_VIEW','FEE','View fee records'),
  ('FEE_COLLECT','FEE','Collect fee payments'),
  ('FEE_STRUCTURE_MANAGE','FEE','Manage fee structures'),
  ('FEE_REPORT','FEE','Run fee collection reports'),
  ('LIBRARY_VIEW','LIBRARY','View library catalogue'),
  ('LIBRARY_ISSUE','LIBRARY','Issue/return books'),
  ('LIBRARY_MANAGE','LIBRARY','Manage books and categories'),
  ('TRANSPORT_VIEW','TRANSPORT','View transport routes/buses'),
  ('TRANSPORT_MANAGE','TRANSPORT','Manage transport routes/buses'),
  ('HOSTEL_VIEW','HOSTEL','View hostel allocations'),
  ('HOSTEL_MANAGE','HOSTEL','Manage hostel rooms/allocations'),
  ('EXAM_VIEW','EXAM','View exams and schedules'),
  ('EXAM_MANAGE','EXAM','Manage exams and schedules'),
  ('MARKS_ENTRY','EXAM','Enter student marks'),
  ('MARKS_VIEW','EXAM','View student marks'),
  ('ASSIGNMENT_VIEW','ASSIGNMENT','View assignments'),
  ('ASSIGNMENT_CREATE','ASSIGNMENT','Create/assign assignments'),
  ('ASSIGNMENT_SUBMIT','ASSIGNMENT','Submit assignment work'),
  ('ASSIGNMENT_GRADE','ASSIGNMENT','Grade assignment submissions'),
  ('NOTICE_VIEW','NOTICE','View notices'),
  ('NOTICE_PUBLISH','NOTICE','Publish notices'),
  ('ADMISSION_VIEW','ADMISSION','View admission enquiries'),
  ('ADMISSION_MANAGE','ADMISSION','Manage admission enquiries'),
  ('PAYROLL_VIEW','PAYROLL','View payroll records'),
  ('PAYROLL_MANAGE','PAYROLL','Process payroll'),
  ('NOTIFICATION_SEND','COMMUNICATION','Send notifications'),
  ('NOTIFICATION_VIEW','COMMUNICATION','View notifications'),
  ('SETTINGS_VIEW','SETTINGS','View system settings'),
  ('SETTINGS_MANAGE','SETTINGS','Manage system settings'),
  ('REPORT_VIEW','REPORTS','View cross-module reports');

-- =====================================================================
-- Role -> Permission mapping (real RBAC subsets per role)
-- =====================================================================

-- SUPER_ADMIN: everything.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'SUPER_ADMIN'), id FROM permissions;

-- PRINCIPAL: broad oversight, not raw settings/role management.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'PRINCIPAL'), id FROM permissions
WHERE name IN (
  'USER_VIEW','ROLE_VIEW',
  'STUDENT_VIEW','STUDENT_CREATE','STUDENT_UPDATE','STUDENT_DELETE',
  'TEACHER_VIEW','TEACHER_CREATE','TEACHER_UPDATE','TEACHER_DELETE',
  'STAFF_VIEW','STAFF_CREATE','STAFF_UPDATE',
  'CLASS_MANAGE','SECTION_MANAGE','SUBJECT_MANAGE',
  'ATTENDANCE_VIEW','ATTENDANCE_REPORT',
  'FEE_VIEW','FEE_REPORT',
  'LIBRARY_VIEW','TRANSPORT_VIEW','HOSTEL_VIEW',
  'EXAM_VIEW','EXAM_MANAGE','MARKS_VIEW',
  'ASSIGNMENT_VIEW',
  'NOTICE_VIEW','NOTICE_PUBLISH',
  'ADMISSION_VIEW','ADMISSION_MANAGE',
  'PAYROLL_VIEW',
  'NOTIFICATION_SEND','NOTIFICATION_VIEW',
  'SETTINGS_VIEW','REPORT_VIEW'
);

-- VICE_PRINCIPAL: similar to principal, fewer administrative rights.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'VICE_PRINCIPAL'), id FROM permissions
WHERE name IN (
  'STUDENT_VIEW','STUDENT_UPDATE','TEACHER_VIEW','STAFF_VIEW',
  'ATTENDANCE_VIEW','ATTENDANCE_REPORT',
  'EXAM_VIEW','EXAM_MANAGE','MARKS_VIEW',
  'ASSIGNMENT_VIEW','NOTICE_VIEW','NOTICE_PUBLISH',
  'ADMISSION_VIEW','REPORT_VIEW'
);

-- TEACHER: day-to-day classroom duties, no fee/payroll access.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'TEACHER'), id FROM permissions
WHERE name IN (
  'STUDENT_VIEW',
  'ATTENDANCE_VIEW','ATTENDANCE_MARK',
  'EXAM_VIEW','MARKS_ENTRY','MARKS_VIEW',
  'ASSIGNMENT_VIEW','ASSIGNMENT_CREATE','ASSIGNMENT_GRADE',
  'NOTICE_VIEW','REPORT_VIEW'
);

-- CLASS_TEACHER: everything TEACHER has, plus section-level reporting
-- and the ability to publish notices / update student records for
-- their own section.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'CLASS_TEACHER'), id FROM permissions
WHERE name IN (
  'STUDENT_VIEW','STUDENT_UPDATE',
  'ATTENDANCE_VIEW','ATTENDANCE_MARK','ATTENDANCE_REPORT',
  'EXAM_VIEW','MARKS_ENTRY','MARKS_VIEW',
  'ASSIGNMENT_VIEW','ASSIGNMENT_CREATE','ASSIGNMENT_GRADE',
  'NOTICE_VIEW','NOTICE_PUBLISH','REPORT_VIEW'
);

-- ACCOUNTANT: fees + payroll, read-only student lookup.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'ACCOUNTANT'), id FROM permissions
WHERE name IN (
  'FEE_VIEW','FEE_COLLECT','FEE_STRUCTURE_MANAGE','FEE_REPORT',
  'PAYROLL_VIEW','PAYROLL_MANAGE',
  'STUDENT_VIEW','NOTICE_VIEW','REPORT_VIEW'
);

-- LIBRARIAN: library module + read-only lookups needed to issue books.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'LIBRARIAN'), id FROM permissions
WHERE name IN (
  'LIBRARY_VIEW','LIBRARY_ISSUE','LIBRARY_MANAGE',
  'STUDENT_VIEW','TEACHER_VIEW','NOTICE_VIEW'
);

-- RECEPTIONIST: front-desk admissions + basic student lookup.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'RECEPTIONIST'), id FROM permissions
WHERE name IN (
  'ADMISSION_VIEW','ADMISSION_MANAGE','STUDENT_VIEW',
  'NOTICE_VIEW','NOTIFICATION_SEND','REPORT_VIEW'
);

-- STUDENT: self-service portal.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'STUDENT'), id FROM permissions
WHERE name IN (
  'STUDENT_VIEW','ATTENDANCE_VIEW','EXAM_VIEW','MARKS_VIEW',
  'ASSIGNMENT_VIEW','ASSIGNMENT_SUBMIT','NOTICE_VIEW',
  'FEE_VIEW','LIBRARY_VIEW'
);

-- PARENT: read-only view into their child's records.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'PARENT'), id FROM permissions
WHERE name IN (
  'STUDENT_VIEW','ATTENDANCE_VIEW','FEE_VIEW','EXAM_VIEW',
  'MARKS_VIEW','NOTICE_VIEW','ASSIGNMENT_VIEW'
);

-- SECURITY_GUARD: gate/notice board visibility only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'SECURITY_GUARD'), id FROM permissions
WHERE name IN ('NOTICE_VIEW','HOSTEL_VIEW','TRANSPORT_VIEW');

-- =====================================================================
-- Academic years - "today" is 2026-08-06, so the Apr-Mar Indian school
-- cycle currently running is 2026-2027.
-- =====================================================================
INSERT INTO academic_years (year_name, start_date, end_date, is_current) VALUES
  ('2025-2026', '2025-04-01', '2026-03-31', 0),
  ('2026-2027', '2026-04-01', '2027-03-31', 1);

-- =====================================================================
-- Departments
-- =====================================================================
INSERT INTO departments (name, description) VALUES
  ('Administration', 'School administration'),
  ('Academics',       'Teaching staff and curriculum'),
  ('Accounts',        'Fees, payroll and finance'),
  ('Library',         'Library services'),
  ('Transport',       'School bus fleet and routes'),
  ('Sports',          'Physical education and sports'),
  ('Hostel',          'Boarding houses'),
  ('IT',              'Information technology and systems');

-- =====================================================================
-- Designations
-- =====================================================================
INSERT INTO designations (name, description) VALUES
  ('Principal',        'Head of institution'),
  ('Vice Principal',   'Deputy head of institution'),
  ('PGT',              'Post Graduate Teacher (senior secondary)'),
  ('TGT',              'Trained Graduate Teacher (secondary)'),
  ('PRT',              'Primary Teacher'),
  ('Librarian',        'Library in-charge'),
  ('Accountant',       'Accounts and fee collection'),
  ('Receptionist',     'Front office'),
  ('Security Guard',   'Campus security'),
  ('Lab Assistant',    'Science/computer lab support'),
  ('Peon',             'Support staff');

-- =====================================================================
-- Classes (20) - all tied to the current academic year (2026-2027).
-- =====================================================================
INSERT INTO classes (class_name, academic_year_id)
SELECT x.class_name, (SELECT id FROM academic_years WHERE is_current = 1)
FROM (
  SELECT 'Pre-Nursery' AS class_name UNION ALL SELECT 'Nursery' UNION ALL SELECT 'LKG' UNION ALL SELECT 'UKG'
  UNION ALL SELECT 'Class 1' UNION ALL SELECT 'Class 2' UNION ALL SELECT 'Class 3' UNION ALL SELECT 'Class 4'
  UNION ALL SELECT 'Class 5' UNION ALL SELECT 'Class 6' UNION ALL SELECT 'Class 7' UNION ALL SELECT 'Class 8'
  UNION ALL SELECT 'Class 9' UNION ALL SELECT 'Class 10'
  UNION ALL SELECT 'Class 11 Science' UNION ALL SELECT 'Class 11 Commerce' UNION ALL SELECT 'Class 11 Arts'
  UNION ALL SELECT 'Class 12 Science' UNION ALL SELECT 'Class 12 Commerce' UNION ALL SELECT 'Class 12 Arts'
) x;

-- =====================================================================
-- Sections - 3 (A/B/C) for Pre-Nursery..Class 10, 2 (A/B) for the
-- six 11/12 stream classes. 14*3 + 6*2 = 54 sections.
-- =====================================================================
INSERT INTO sections (section_name, class_id, room_number, capacity)
SELECT x.section_name, c.id, CONCAT(c.class_name, ' - ', x.section_name), 40
FROM classes c
CROSS JOIN (SELECT 'A' AS section_name UNION ALL SELECT 'B' UNION ALL SELECT 'C') x
WHERE c.class_name IN (
  'Pre-Nursery','Nursery','LKG','UKG',
  'Class 1','Class 2','Class 3','Class 4','Class 5',
  'Class 6','Class 7','Class 8','Class 9','Class 10'
);

INSERT INTO sections (section_name, class_id, room_number, capacity)
SELECT x.section_name, c.id, CONCAT(c.class_name, ' - ', x.section_name), 35
FROM classes c
CROSS JOIN (SELECT 'A' AS section_name UNION ALL SELECT 'B') x
WHERE c.class_name IN (
  'Class 11 Science','Class 11 Commerce','Class 11 Arts',
  'Class 12 Science','Class 12 Commerce','Class 12 Arts'
);

-- =====================================================================
-- Subjects per class group (subject_code is globally UNIQUE, so every
-- code embeds a class-specific suffix).
-- =====================================================================

-- Pre-primary: Pre-Nursery, Nursery, LKG, UKG.
INSERT INTO subjects (subject_name, subject_code, class_id, is_elective)
SELECT x.subject_name,
       CONCAT(x.code_prefix, '-',
         CASE c.class_name WHEN 'Pre-Nursery' THEN 'PN' WHEN 'Nursery' THEN 'NUR'
                            WHEN 'LKG' THEN 'LKG' WHEN 'UKG' THEN 'UKG' END),
       c.id, 0
FROM classes c
CROSS JOIN (
  SELECT 'English' AS subject_name, 'ENG' AS code_prefix
  UNION ALL SELECT 'Mathematics', 'MAT'
  UNION ALL SELECT 'Art & Craft', 'ART'
  UNION ALL SELECT 'Physical Education', 'PE'
) x
WHERE c.class_name IN ('Pre-Nursery','Nursery','LKG','UKG');

-- Class 1 - Class 8: full 8-subject set, nothing elective.
INSERT INTO subjects (subject_name, subject_code, class_id, is_elective)
SELECT x.subject_name,
       CONCAT(x.code_prefix, '-',
         CASE c.class_name
           WHEN 'Class 1' THEN 'C1' WHEN 'Class 2' THEN 'C2' WHEN 'Class 3' THEN 'C3'
           WHEN 'Class 4' THEN 'C4' WHEN 'Class 5' THEN 'C5' WHEN 'Class 6' THEN 'C6'
           WHEN 'Class 7' THEN 'C7' WHEN 'Class 8' THEN 'C8'
         END),
       c.id, 0
FROM classes c
CROSS JOIN (
  SELECT 'English' AS subject_name, 'ENG' AS code_prefix
  UNION ALL SELECT 'Mathematics', 'MAT'
  UNION ALL SELECT 'Science', 'SCI'
  UNION ALL SELECT 'Social Studies', 'SST'
  UNION ALL SELECT 'Hindi', 'HIN'
  UNION ALL SELECT 'Computer Science', 'CS'
  UNION ALL SELECT 'Art & Craft', 'ART'
  UNION ALL SELECT 'Physical Education', 'PE'
) x
WHERE c.class_name IN ('Class 1','Class 2','Class 3','Class 4','Class 5','Class 6','Class 7','Class 8');

-- Class 9 - Class 10: same subjects, Computer Science & Art are electives.
INSERT INTO subjects (subject_name, subject_code, class_id, is_elective)
SELECT x.subject_name,
       CONCAT(x.code_prefix, '-', CASE c.class_name WHEN 'Class 9' THEN 'C9' WHEN 'Class 10' THEN 'C10' END),
       c.id, x.is_elective
FROM classes c
CROSS JOIN (
  SELECT 'English' AS subject_name, 'ENG' AS code_prefix, 0 AS is_elective
  UNION ALL SELECT 'Mathematics', 'MAT', 0
  UNION ALL SELECT 'Science', 'SCI', 0
  UNION ALL SELECT 'Social Studies', 'SST', 0
  UNION ALL SELECT 'Hindi', 'HIN', 0
  UNION ALL SELECT 'Computer Science', 'CS', 1
  UNION ALL SELECT 'Art & Craft', 'ART', 1
  UNION ALL SELECT 'Physical Education', 'PE', 0
) x
WHERE c.class_name IN ('Class 9','Class 10');

-- Class 11/12 Science stream.
INSERT INTO subjects (subject_name, subject_code, class_id, is_elective)
SELECT x.subject_name,
       CONCAT(x.code_prefix, '-', CASE c.class_name WHEN 'Class 11 Science' THEN '11SCI' WHEN 'Class 12 Science' THEN '12SCI' END),
       c.id, 0
FROM classes c
CROSS JOIN (
  SELECT 'English' AS subject_name, 'ENG' AS code_prefix
  UNION ALL SELECT 'Physics', 'PHY'
  UNION ALL SELECT 'Chemistry', 'CHE'
  UNION ALL SELECT 'Biology', 'BIO'
  UNION ALL SELECT 'Mathematics', 'MAT'
  UNION ALL SELECT 'Physical Education', 'PE'
) x
WHERE c.class_name IN ('Class 11 Science','Class 12 Science');

-- Class 11/12 Commerce stream.
INSERT INTO subjects (subject_name, subject_code, class_id, is_elective)
SELECT x.subject_name,
       CONCAT(x.code_prefix, '-', CASE c.class_name WHEN 'Class 11 Commerce' THEN '11COM' WHEN 'Class 12 Commerce' THEN '12COM' END),
       c.id, 0
FROM classes c
CROSS JOIN (
  SELECT 'English' AS subject_name, 'ENG' AS code_prefix
  UNION ALL SELECT 'Accountancy', 'ACC'
  UNION ALL SELECT 'Business Studies', 'BST'
  UNION ALL SELECT 'Economics', 'ECO'
  UNION ALL SELECT 'Mathematics', 'MAT'
  UNION ALL SELECT 'Physical Education', 'PE'
) x
WHERE c.class_name IN ('Class 11 Commerce','Class 12 Commerce');

-- Class 11/12 Arts stream.
INSERT INTO subjects (subject_name, subject_code, class_id, is_elective)
SELECT x.subject_name,
       CONCAT(x.code_prefix, '-', CASE c.class_name WHEN 'Class 11 Arts' THEN '11ART' WHEN 'Class 12 Arts' THEN '12ART' END),
       c.id, 0
FROM classes c
CROSS JOIN (
  SELECT 'English' AS subject_name, 'ENG' AS code_prefix
  UNION ALL SELECT 'History', 'HIS'
  UNION ALL SELECT 'Political Science', 'POL'
  UNION ALL SELECT 'Economics', 'ECO'
  UNION ALL SELECT 'Psychology', 'PSY'
  UNION ALL SELECT 'Physical Education', 'PE'
) x
WHERE c.class_name IN ('Class 11 Arts','Class 12 Arts');

-- =====================================================================
-- Fee categories
-- =====================================================================
INSERT INTO fee_categories (name, description) VALUES
  ('Tuition Fee',   'Core academic tuition'),
  ('Admission Fee', 'One-time admission charge'),
  ('Transport Fee', 'School bus transport'),
  ('Library Fee',   'Library membership/usage'),
  ('Lab Fee',       'Science/computer lab usage'),
  ('Exam Fee',      'Examination charges'),
  ('Hostel Fee',    'Boarding and lodging'),
  ('Sports Fee',    'Sports and extra-curricular activities');

-- =====================================================================
-- Fee structures - every (class, current-year, category) combination,
-- flat amount per category (round-1 simplification; a later round can
-- vary tuition by class level). due_date = 2 months into the academic
-- year, i.e. 2026-06-01 (already in the past relative to "today"
-- 2026-08-06, which lets 07_sample_data.sql produce realistic OVERDUE
-- rows for students who never paid).
-- =====================================================================
INSERT INTO fee_structures (class_id, academic_year_id, fee_category_id, amount, due_date)
SELECT c.id, c.academic_year_id, fc.id,
       CASE fc.name
         WHEN 'Tuition Fee'   THEN 20000.00
         WHEN 'Admission Fee' THEN 5000.00
         WHEN 'Transport Fee' THEN 12000.00
         WHEN 'Library Fee'   THEN 1000.00
         WHEN 'Lab Fee'       THEN 1500.00
         WHEN 'Exam Fee'      THEN 2000.00
         WHEN 'Hostel Fee'    THEN 30000.00
         WHEN 'Sports Fee'    THEN 800.00
       END,
       DATE_ADD((SELECT start_date FROM academic_years WHERE is_current = 1), INTERVAL 2 MONTH)
FROM classes c
CROSS JOIN fee_categories fc;

-- =====================================================================
-- Exam types
-- =====================================================================
INSERT INTO exam_types (name) VALUES
  ('Unit Test 1'), ('Unit Test 2'), ('Half Yearly'), ('Final');

-- =====================================================================
-- Grades
-- =====================================================================
INSERT INTO grades (grade_name, min_percentage, max_percentage, grade_point) VALUES
  ('A1', 91.00, 100.00, 10.0),
  ('A2', 81.00, 90.99,  9.0),
  ('B1', 71.00, 80.99,  8.0),
  ('B2', 61.00, 70.99,  7.0),
  ('C1', 51.00, 60.99,  6.0),
  ('C2', 41.00, 50.99,  5.0),
  ('D',  33.00, 40.99,  4.0),
  ('E',   0.00, 32.99,  0.0);

-- =====================================================================
-- Book categories
-- =====================================================================
INSERT INTO book_categories (name) VALUES
  ('Fiction'), ('Non-Fiction'), ('Science'), ('Mathematics'),
  ('History'), ('Biography'), ('Reference'), ('Children');

-- =====================================================================
-- School info & system settings
-- =====================================================================
INSERT INTO school_info (name, address, phone, email, logo_url, established_year, affiliation_number) VALUES
  ('Greenwood International School', '12 Lake View Road, New Delhi, India', '+91-11-45678900',
   'info@greenwoodschool.edu', '/assets/logo.png', 1998, 'CBSE/AFF/10452');

INSERT INTO system_settings (setting_key, setting_value) VALUES
  ('academic_year_start_month', '4'),
  ('default_currency', 'INR'),
  ('timezone', 'Asia/Kolkata'),
  ('attendance_lock_after_days', '7'),
  ('late_fee_per_day', '10.00'),
  ('school_working_days', 'MON,TUE,WED,THU,FRI,SAT');

-- =====================================================================
-- Transport: 5 drivers, 5 buses, 5 routes, pickup points per route.
-- =====================================================================
INSERT INTO drivers (name, phone, license_number, address) VALUES
  ('Ramesh Yadav',   '+91-9810011122', 'DL-0120180012345', 'Rohini, New Delhi'),
  ('Suresh Kumar',   '+91-9810022233', 'DL-0120190023456', 'Pitampura, New Delhi'),
  ('Mahesh Chand',   '+91-9810033344', 'DL-0120170034567', 'Shalimar Bagh, New Delhi'),
  ('Dinesh Prasad',  '+91-9810044455', 'DL-0120200045678', 'Model Town, New Delhi'),
  ('Naresh Singh',   '+91-9810055566', 'DL-0120160056789', 'Ashok Vihar, New Delhi');

INSERT INTO buses (bus_number, capacity, driver_id, vehicle_model, registration_number) VALUES
  ('BUS-01', 45, (SELECT id FROM drivers WHERE license_number = 'DL-0120180012345'), 'Tata Starbus', 'DL1PC1001'),
  ('BUS-02', 45, (SELECT id FROM drivers WHERE license_number = 'DL-0120190023456'), 'Ashok Leyland', 'DL1PC1002'),
  ('BUS-03', 35, (SELECT id FROM drivers WHERE license_number = 'DL-0120170034567'), 'Tata Starbus', 'DL1PC1003'),
  ('BUS-04', 35, (SELECT id FROM drivers WHERE license_number = 'DL-0120200045678'), 'Force Traveller', 'DL1PC1004'),
  ('BUS-05', 50, (SELECT id FROM drivers WHERE license_number = 'DL-0120160056789'), 'Ashok Leyland', 'DL1PC1005');

INSERT INTO routes (route_name, bus_id, start_point, end_point) VALUES
  ('Route 1 - Rohini',        (SELECT id FROM buses WHERE bus_number = 'BUS-01'), 'Rohini Sector 7',  'Greenwood International School'),
  ('Route 2 - Pitampura',     (SELECT id FROM buses WHERE bus_number = 'BUS-02'), 'Pitampura',         'Greenwood International School'),
  ('Route 3 - Shalimar Bagh', (SELECT id FROM buses WHERE bus_number = 'BUS-03'), 'Shalimar Bagh',     'Greenwood International School'),
  ('Route 4 - Model Town',    (SELECT id FROM buses WHERE bus_number = 'BUS-04'), 'Model Town',        'Greenwood International School'),
  ('Route 5 - Ashok Vihar',   (SELECT id FROM buses WHERE bus_number = 'BUS-05'), 'Ashok Vihar',       'Greenwood International School');

INSERT INTO pickup_points (route_id, point_name, pickup_time, drop_time)
SELECT r.id, x.point_name, x.pickup_time, x.drop_time
FROM routes r
CROSS JOIN (
  SELECT 'Stop 1' AS point_name, '06:45:00' AS pickup_time, '14:45:00' AS drop_time
  UNION ALL SELECT 'Stop 2', '07:00:00', '14:30:00'
  UNION ALL SELECT 'Stop 3', '07:15:00', '14:15:00'
) x;

-- =====================================================================
-- Hostels (2) + hostel_rooms (20 each = 40 total).
-- =====================================================================
INSERT INTO hostels (name, warden_name, warden_contact, type) VALUES
  ('Greenwood Boys Hostel',  'Anil Sharma',  '+91-9811100011', 'BOYS'),
  ('Greenwood Girls Hostel', 'Sunita Verma', '+91-9811100022', 'GIRLS');

INSERT INTO hostel_rooms (hostel_id, room_number, capacity, occupied_count)
SELECT h.id, CONCAT('R-', LPAD(n.num, 3, '0')), 4, 0
FROM hostels h
CROSS JOIN (
  SELECT 1 num UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) n;

-- =====================================================================
-- Demo login users (Password@123 hash for all except the super admin,
-- which uses Admin@123). Hashes copied verbatim from SCHEMA_CONTRACT.md.
-- =====================================================================

-- SUPER_ADMIN
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'admin', 'admin@school.edu',
  '$2b$10$Ih9JH8QwmYKKqexbzqQRhOz6b8WE3i1QYFOABFL8F6l9pXFOqEP.q',
  'System', 'Administrator', '+91-9999900001', 'OTHER',
  (SELECT id FROM roles WHERE name = 'SUPER_ADMIN'), 1, 1
);

-- PRINCIPAL
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'principal', 'principal@school.edu',
  '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
  'Rajesh', 'Khanna', '+91-9999900002', 'MALE',
  (SELECT id FROM roles WHERE name = 'PRINCIPAL'), 1, 1
);

-- VICE_PRINCIPAL
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'viceprincipal', 'viceprincipal@school.edu',
  '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
  'Anita', 'Desai', '+91-9999900003', 'FEMALE',
  (SELECT id FROM roles WHERE name = 'VICE_PRINCIPAL'), 1, 1
);

-- Demo TEACHER (extra, on top of the 50 bulk-generated in 07)
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'teacher.demo', 'teacher.demo@school.edu',
  '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
  'Vikram', 'Mehta', '+91-9999900004', 'MALE',
  (SELECT id FROM roles WHERE name = 'TEACHER'), 1, 1
);

INSERT INTO teachers (
  user_id, employee_id, department_id, designation_id, qualification,
  experience_years, joining_date, date_of_birth, gender, address, city, state, pincode,
  blood_group, emergency_contact, salary, employment_type, status
) VALUES (
  (SELECT id FROM users WHERE username = 'teacher.demo'),
  'EMP-DEMO-TCH-001',
  (SELECT id FROM departments WHERE name = 'Academics'),
  (SELECT id FROM designations WHERE name = 'TGT'),
  'M.Sc B.Ed', 10, '2016-06-01', '1988-03-15', 'MALE',
  '221 Green Park', 'New Delhi', 'Delhi', '110016',
  'O+', '+91-9999911004', 55000.00, 'FULL_TIME', 'ACTIVE'
);

-- ACCOUNTANT
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'accountant.demo', 'accountant.demo@school.edu',
  '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
  'Suman', 'Agarwal', '+91-9999900005', 'FEMALE',
  (SELECT id FROM roles WHERE name = 'ACCOUNTANT'), 1, 1
);
INSERT INTO staff (user_id, employee_id, department_id, designation_id, joining_date, salary, status)
VALUES (
  (SELECT id FROM users WHERE username = 'accountant.demo'),
  'EMP-DEMO-ACC-001',
  (SELECT id FROM departments WHERE name = 'Accounts'),
  (SELECT id FROM designations WHERE name = 'Accountant'),
  '2018-04-10', 42000.00, 'ACTIVE'
);

-- LIBRARIAN
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'librarian.demo', 'librarian.demo@school.edu',
  '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
  'Kavita', 'Nair', '+91-9999900006', 'FEMALE',
  (SELECT id FROM roles WHERE name = 'LIBRARIAN'), 1, 1
);
INSERT INTO staff (user_id, employee_id, department_id, designation_id, joining_date, salary, status)
VALUES (
  (SELECT id FROM users WHERE username = 'librarian.demo'),
  'EMP-DEMO-LIB-001',
  (SELECT id FROM departments WHERE name = 'Library'),
  (SELECT id FROM designations WHERE name = 'Librarian'),
  '2019-07-15', 35000.00, 'ACTIVE'
);

-- RECEPTIONIST
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'receptionist.demo', 'receptionist.demo@school.edu',
  '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
  'Priya', 'Menon', '+91-9999900007', 'FEMALE',
  (SELECT id FROM roles WHERE name = 'RECEPTIONIST'), 1, 1
);
INSERT INTO staff (user_id, employee_id, department_id, designation_id, joining_date, salary, status)
VALUES (
  (SELECT id FROM users WHERE username = 'receptionist.demo'),
  'EMP-DEMO-REC-001',
  (SELECT id FROM departments WHERE name = 'Administration'),
  (SELECT id FROM designations WHERE name = 'Receptionist'),
  '2020-01-20', 25000.00, 'ACTIVE'
);

-- SECURITY_GUARD
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
VALUES (
  'security.demo', 'security.demo@school.edu',
  '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
  'Bhupendra', 'Rawat', '+91-9999900008', 'MALE',
  (SELECT id FROM roles WHERE name = 'SECURITY_GUARD'), 1, 1
);
INSERT INTO staff (user_id, employee_id, department_id, designation_id, joining_date, salary, status)
VALUES (
  (SELECT id FROM users WHERE username = 'security.demo'),
  'EMP-DEMO-SEC-001',
  (SELECT id FROM departments WHERE name = 'Administration'),
  (SELECT id FROM designations WHERE name = 'Security Guard'),
  '2021-03-01', 18000.00, 'ACTIVE'
);

-- =====================================================================
-- Notices, events/holidays, and a few admission enquiries.
-- =====================================================================
INSERT INTO notices (title, description, target_role, published_by, published_at, expiry_date) VALUES
  ('Welcome to the 2026-2027 Academic Year',
   'Classes resume from 1st April 2026. Please ensure all fee dues and documents are submitted.',
   NULL, (SELECT id FROM users WHERE username = 'admin'), '2026-04-01 09:00:00', '2026-08-31'),
  ('Half Yearly Examination Schedule Released',
   'The half-yearly examination datesheet has been published on the notice board. Please check your class schedule.',
   (SELECT id FROM roles WHERE name = 'STUDENT'), (SELECT id FROM users WHERE username = 'principal'), '2026-08-01 10:00:00', '2026-09-30'),
  ('Staff Meeting - August',
   'All teaching staff to attend the monthly staff meeting in the auditorium.',
   (SELECT id FROM roles WHERE name = 'TEACHER'), (SELECT id FROM users WHERE username = 'principal'), '2026-08-03 08:00:00', '2026-08-10'),
  ('Library Books Return Reminder',
   'Students holding books issued before June are requested to return them before the new issue cycle begins.',
   (SELECT id FROM roles WHERE name = 'STUDENT'), (SELECT id FROM users WHERE username = 'librarian.demo'), '2026-08-05 11:00:00', '2026-08-20');

INSERT INTO events (title, description, event_date, event_type, created_by) VALUES
  ('Summer Break Ends',        'School reopens for the new academic year.', '2026-04-01', 'EVENT',   (SELECT id FROM users WHERE username = 'admin')),
  ('Independence Day',         'National holiday.',                          '2026-08-15', 'HOLIDAY', (SELECT id FROM users WHERE username = 'admin')),
  ('Half Yearly Exams Begin',  'Half yearly examinations for all classes.',  '2026-09-15', 'EXAM',    (SELECT id FROM users WHERE username = 'principal')),
  ('Gandhi Jayanti',           'National holiday.',                          '2026-10-02', 'HOLIDAY', (SELECT id FROM users WHERE username = 'admin')),
  ('Annual Sports Day',        'Inter-house sports competition.',            '2026-11-20', 'EVENT',   (SELECT id FROM users WHERE username = 'principal')),
  ('Winter Break Begins',      'School closes for winter vacation.',         '2026-12-22', 'HOLIDAY', (SELECT id FROM users WHERE username = 'admin')),
  ('Republic Day',             'National holiday.',                          '2027-01-26', 'HOLIDAY', (SELECT id FROM users WHERE username = 'admin')),
  ('Final Exams Begin',        'Final examinations for all classes.',        '2027-02-10', 'EXAM',    (SELECT id FROM users WHERE username = 'principal'));

INSERT INTO admission_enquiries (student_name, parent_name, phone, email, class_applying, dob, address, status, applied_at) VALUES
  ('Aarav Sharma',   'Rohit Sharma',   '+91-9800011111', 'rohit.sharma@example.com',   'Class 1',  '2019-05-12', 'Sector 12, Rohini, Delhi',       'PENDING',  '2026-07-20 10:00:00'),
  ('Diya Patel',     'Nikhil Patel',   '+91-9800022222', 'nikhil.patel@example.com',   'Nursery',  '2022-01-25', 'Model Town, Delhi',               'APPROVED', '2026-07-15 09:30:00'),
  ('Kabir Singh',    'Manpreet Singh', '+91-9800033333', 'manpreet.singh@example.com', 'Class 6',  '2015-09-02', 'Ashok Vihar, Delhi',               'PENDING',  '2026-07-25 14:15:00'),
  ('Anaya Gupta',    'Vivek Gupta',    '+91-9800044444', 'vivek.gupta@example.com',    'UKG',      '2021-03-18', 'Pitampura, Delhi',                 'REJECTED', '2026-07-10 11:45:00'),
  ('Reyansh Verma',  'Alok Verma',     '+91-9800055555', 'alok.verma@example.com',     'Class 9',  '2011-11-30', 'Shalimar Bagh, Delhi',             'PENDING',  '2026-08-01 16:00:00');
