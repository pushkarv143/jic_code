-- =====================================================================
-- 01_schema.sql
-- School Management System - Full schema (FK-safe creation order)
-- Matches SCHEMA_CONTRACT.md exactly for table/column names & enums.
-- Run with: mysql -u root -p < 01_schema.sql   (or after USE, on the db)
--
-- Conventions used in this file (documented, applied consistently):
--   * Every table: id BIGINT AUTO_INCREMENT PRIMARY KEY.
--   * Every table gets created_at / updated_at EXCEPT pure mapping/join
--     tables with no attributes of their own: role_permissions,
--     class_subject_teacher, student_parents.
--   * created_by / updated_by (FK -> users.id) are added only where the
--     contract explicitly lists them (users table). Adding them to every
--     single table would add ~120 extra FK columns with little value in
--     round 1; noted as a deviation in database/README.md.
--   * is_deleted TINYINT(1) DEFAULT 0 added exactly where the contract
--     calls for soft-delete support: teachers, staff, students, classes,
--     subjects, books, buses.
--   * ON DELETE policy: CASCADE for strict dependent/child rows
--     (guardians, student_documents, marks, fee_payments, ...),
--     RESTRICT for references into master/lookup data that must not
--     vanish silently (departments, designations, academic_years,
--     fee_categories, classes/sections referenced by students, ...),
--     SET NULL for optional "soft" references that should survive
--     deletion of the referenced row (sections.class_teacher_id,
--     assignments.teacher_id, notices.published_by, ...).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS school_management_system
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE school_management_system;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- MODULE: Core / Auth
-- =====================================================================

CREATE TABLE roles (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(50)  NOT NULL,
  description   VARCHAR(255),
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  module        VARCHAR(50)  NOT NULL,
  description   VARCHAR(255),
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_permissions_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pure join table: no created_at/updated_at (documented above).
CREATE TABLE role_permissions (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id        BIGINT NOT NULL,
  permission_id  BIGINT NOT NULL,
  UNIQUE KEY uq_role_permission (role_id, permission_id),
  CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
  CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  username            VARCHAR(50)  NOT NULL,
  email               VARCHAR(150) NOT NULL,
  password            VARCHAR(255) NOT NULL,
  first_name          VARCHAR(100) NOT NULL,
  last_name           VARCHAR(100),
  phone               VARCHAR(20),
  gender              ENUM('MALE','FEMALE','OTHER'),
  role_id             BIGINT NOT NULL,
  is_active           TINYINT(1) NOT NULL DEFAULT 1,
  is_email_verified   TINYINT(1) NOT NULL DEFAULT 0,
  profile_image       VARCHAR(255),
  last_login          DATETIME,
  created_by          BIGINT NULL,
  updated_by          BIGINT NULL,
  created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email),
  CONSTRAINT fk_users_role       FOREIGN KEY (role_id)    REFERENCES roles(id) ON DELETE RESTRICT,
  CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT NOT NULL,
  token        VARCHAR(512) NOT NULL,
  expiry_date  DATETIME NOT NULL,
  revoked      TINYINT(1) NOT NULL DEFAULT 0,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_refresh_tokens_token (token),
  CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT NOT NULL,
  token        VARCHAR(255) NOT NULL,
  expiry_date  DATETIME NOT NULL,
  used         TINYINT(1) NOT NULL DEFAULT 0,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_password_reset_tokens_token (token),
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT NULL,
  action       VARCHAR(100) NOT NULL,
  entity_name  VARCHAR(100) NOT NULL,
  entity_id    BIGINT,
  old_value    TEXT,
  new_value    TEXT,
  ip_address   VARCHAR(64),
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Academic setup
-- =====================================================================

CREATE TABLE academic_years (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  year_name   VARCHAR(20) NOT NULL,
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL,
  is_current  TINYINT(1) NOT NULL DEFAULT 0,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_academic_years_name (year_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE departments (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  description  VARCHAR(255),
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_departments_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE designations (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  description  VARCHAR(255),
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_designations_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- teachers is created here (ahead of classes/sections) because
-- sections.class_teacher_id references teachers, and teachers itself
-- only depends on users/departments/designations, none of which depend
-- on classes/sections. This breaks the classes<->teachers circular
-- dependency without deferred FK tricks.
CREATE TABLE teachers (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id             BIGINT NOT NULL,
  employee_id         VARCHAR(30) NOT NULL,
  department_id       BIGINT NOT NULL,
  designation_id      BIGINT NOT NULL,
  qualification       VARCHAR(255),
  experience_years     INT,
  joining_date        DATE,
  date_of_birth       DATE,
  gender              ENUM('MALE','FEMALE','OTHER'),
  address             VARCHAR(255),
  city                VARCHAR(100),
  state               VARCHAR(100),
  pincode             VARCHAR(10),
  blood_group         VARCHAR(5),
  emergency_contact   VARCHAR(20),
  salary              DECIMAL(12,2),
  employment_type     ENUM('FULL_TIME','PART_TIME','CONTRACT') NOT NULL DEFAULT 'FULL_TIME',
  status              ENUM('ACTIVE','INACTIVE','RESIGNED','TERMINATED') NOT NULL DEFAULT 'ACTIVE',
  is_deleted          TINYINT(1) NOT NULL DEFAULT 0,
  created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_teachers_user (user_id),
  UNIQUE KEY uq_teachers_employee_id (employee_id),
  CONSTRAINT fk_teachers_user        FOREIGN KEY (user_id)        REFERENCES users(id)        ON DELETE CASCADE,
  CONSTRAINT fk_teachers_department  FOREIGN KEY (department_id)  REFERENCES departments(id)  ON DELETE RESTRICT,
  CONSTRAINT fk_teachers_designation FOREIGN KEY (designation_id) REFERENCES designations(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classes (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_name        VARCHAR(50) NOT NULL,
  academic_year_id  BIGINT NOT NULL,
  is_deleted        TINYINT(1) NOT NULL DEFAULT 0,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_classes_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sections (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  section_name      VARCHAR(20) NOT NULL,
  class_id          BIGINT NOT NULL,
  class_teacher_id  BIGINT NULL,
  room_number       VARCHAR(50),
  capacity          INT,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_section_per_class (class_id, section_name),
  CONSTRAINT fk_sections_class         FOREIGN KEY (class_id)         REFERENCES classes(id)  ON DELETE CASCADE,
  CONSTRAINT fk_sections_class_teacher FOREIGN KEY (class_teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subjects (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject_name  VARCHAR(100) NOT NULL,
  subject_code  VARCHAR(20) NOT NULL,
  class_id      BIGINT NOT NULL,
  is_elective   TINYINT(1) NOT NULL DEFAULT 0,
  is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_subjects_code (subject_code),
  CONSTRAINT fk_subjects_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pure join/assignment table: no created_at/updated_at (documented above).
CREATE TABLE class_subject_teacher (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id    BIGINT NOT NULL,
  section_id  BIGINT NOT NULL,
  subject_id  BIGINT NOT NULL,
  teacher_id  BIGINT NOT NULL,
  UNIQUE KEY uq_cst (section_id, subject_id),
  CONSTRAINT fk_cst_class   FOREIGN KEY (class_id)   REFERENCES classes(id)  ON DELETE CASCADE,
  CONSTRAINT fk_cst_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
  CONSTRAINT fk_cst_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT fk_cst_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: People (staff, students, guardians, parents)
-- =====================================================================

CREATE TABLE staff (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT NOT NULL,
  employee_id     VARCHAR(30) NOT NULL,
  department_id   BIGINT NOT NULL,
  designation_id  BIGINT NOT NULL,
  joining_date    DATE,
  salary          DECIMAL(12,2),
  status          ENUM('ACTIVE','INACTIVE','RESIGNED','TERMINATED') NOT NULL DEFAULT 'ACTIVE',
  is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_staff_user (user_id),
  UNIQUE KEY uq_staff_employee_id (employee_id),
  CONSTRAINT fk_staff_user        FOREIGN KEY (user_id)        REFERENCES users(id)        ON DELETE CASCADE,
  CONSTRAINT fk_staff_department  FOREIGN KEY (department_id)  REFERENCES departments(id)  ON DELETE RESTRICT,
  CONSTRAINT fk_staff_designation FOREIGN KEY (designation_id) REFERENCES designations(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE students (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id           BIGINT NULL,
  admission_number  VARCHAR(30) NOT NULL,
  class_id          BIGINT NOT NULL,
  section_id        BIGINT NOT NULL,
  roll_number       INT,
  admission_date    DATE,
  date_of_birth     DATE,
  gender            ENUM('MALE','FEMALE','OTHER'),
  blood_group       VARCHAR(5),
  religion          VARCHAR(50),
  category          VARCHAR(50),
  address           VARCHAR(255),
  city              VARCHAR(100),
  state             VARCHAR(100),
  pincode           VARCHAR(10),
  photo_url         VARCHAR(255),
  academic_year_id  BIGINT NOT NULL,
  status            ENUM('ACTIVE','INACTIVE','ALUMNI','TRANSFERRED') NOT NULL DEFAULT 'ACTIVE',
  is_deleted        TINYINT(1) NOT NULL DEFAULT 0,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_students_user (user_id),
  UNIQUE KEY uq_students_admission_number (admission_number),
  CONSTRAINT fk_students_user          FOREIGN KEY (user_id)          REFERENCES users(id)          ON DELETE SET NULL,
  CONSTRAINT fk_students_class         FOREIGN KEY (class_id)         REFERENCES classes(id)        ON DELETE RESTRICT,
  CONSTRAINT fk_students_section       FOREIGN KEY (section_id)       REFERENCES sections(id)       ON DELETE RESTRICT,
  CONSTRAINT fk_students_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE guardians (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id  BIGINT NOT NULL,
  name        VARCHAR(150) NOT NULL,
  relation    VARCHAR(50),
  occupation  VARCHAR(100),
  phone       VARCHAR(20),
  email       VARCHAR(150),
  address     VARCHAR(255),
  is_primary  TINYINT(1) NOT NULL DEFAULT 0,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_guardians_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE parents (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT NOT NULL,
  occupation      VARCHAR(100),
  annual_income   DECIMAL(12,2),
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_parents_user (user_id),
  CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pure join table: no created_at/updated_at (documented above).
CREATE TABLE student_parents (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id  BIGINT NOT NULL,
  parent_id   BIGINT NOT NULL,
  UNIQUE KEY uq_student_parent (student_id, parent_id),
  CONSTRAINT fk_sp_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  CONSTRAINT fk_sp_parent  FOREIGN KEY (parent_id)  REFERENCES parents(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_medical_details (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id          BIGINT NOT NULL,
  height_cm           DECIMAL(5,2),
  weight_kg           DECIMAL(5,2),
  allergies           VARCHAR(255),
  medical_conditions  VARCHAR(255),
  doctor_name         VARCHAR(150),
  doctor_contact      VARCHAR(20),
  created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_medical_student (student_id),
  CONSTRAINT fk_medical_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_documents (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id     BIGINT NOT NULL,
  document_type  VARCHAR(100) NOT NULL,
  file_url       VARCHAR(255) NOT NULL,
  uploaded_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_docs_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Attendance & Leave
-- =====================================================================

CREATE TABLE student_attendance (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id       BIGINT NOT NULL,
  class_id         BIGINT NOT NULL,
  section_id       BIGINT NOT NULL,
  attendance_date  DATE NOT NULL,
  status           ENUM('PRESENT','ABSENT','LATE','HALF_DAY','LEAVE') NOT NULL,
  remarks          VARCHAR(255),
  marked_by        BIGINT NULL,
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_student_attendance_date (student_id, attendance_date),
  CONSTRAINT fk_satt_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  CONSTRAINT fk_satt_class   FOREIGN KEY (class_id)   REFERENCES classes(id)  ON DELETE RESTRICT,
  CONSTRAINT fk_satt_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT,
  CONSTRAINT fk_satt_marked_by FOREIGN KEY (marked_by) REFERENCES users(id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teacher_attendance (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  teacher_id       BIGINT NOT NULL,
  attendance_date  DATE NOT NULL,
  status           ENUM('PRESENT','ABSENT','LATE','HALF_DAY','LEAVE') NOT NULL,
  check_in         TIME,
  check_out        TIME,
  remarks          VARCHAR(255),
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_teacher_attendance_date (teacher_id, attendance_date),
  CONSTRAINT fk_tatt_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE leave_applications (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  applicant_id    BIGINT NOT NULL,
  applicant_type  ENUM('TEACHER','STAFF','STUDENT') NOT NULL,
  leave_type      VARCHAR(50),
  start_date      DATE NOT NULL,
  end_date        DATE NOT NULL,
  reason          VARCHAR(255),
  status          ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  approved_by     BIGINT NULL,
  applied_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_leave_applicant FOREIGN KEY (applicant_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_leave_approver  FOREIGN KEY (approved_by)  REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Fees
-- =====================================================================

CREATE TABLE fee_categories (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  description  VARCHAR(255),
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_fee_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_structures (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id          BIGINT NOT NULL,
  academic_year_id  BIGINT NOT NULL,
  fee_category_id   BIGINT NOT NULL,
  amount            DECIMAL(12,2) NOT NULL,
  due_date          DATE,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_fee_structure (class_id, academic_year_id, fee_category_id),
  CONSTRAINT fk_fs_class         FOREIGN KEY (class_id)         REFERENCES classes(id)        ON DELETE RESTRICT,
  CONSTRAINT fk_fs_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT,
  CONSTRAINT fk_fs_fee_category  FOREIGN KEY (fee_category_id)  REFERENCES fee_categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_fees (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id         BIGINT NOT NULL,
  fee_structure_id   BIGINT NOT NULL,
  academic_year_id   BIGINT NOT NULL,
  amount_due         DECIMAL(12,2) NOT NULL,
  amount_paid        DECIMAL(12,2) NOT NULL DEFAULT 0,
  due_date           DATE,
  status             ENUM('PAID','UNPAID','PARTIAL','OVERDUE') NOT NULL DEFAULT 'UNPAID',
  created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_student_fee (student_id, fee_structure_id),
  CONSTRAINT fk_sf_student        FOREIGN KEY (student_id)       REFERENCES students(id)       ON DELETE CASCADE,
  CONSTRAINT fk_sf_fee_structure  FOREIGN KEY (fee_structure_id) REFERENCES fee_structures(id) ON DELETE RESTRICT,
  CONSTRAINT fk_sf_academic_year  FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_payments (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_fee_id   BIGINT NOT NULL,
  amount           DECIMAL(12,2) NOT NULL,
  payment_date     DATE NOT NULL,
  payment_mode     ENUM('CASH','ONLINE','CHEQUE','CARD') NOT NULL,
  transaction_id   VARCHAR(100),
  receipt_number   VARCHAR(50) NOT NULL,
  collected_by     BIGINT NULL,
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_fee_payments_receipt (receipt_number),
  CONSTRAINT fk_fp_student_fee  FOREIGN KEY (student_fee_id) REFERENCES student_fees(id) ON DELETE CASCADE,
  CONSTRAINT fk_fp_collected_by FOREIGN KEY (collected_by)   REFERENCES users(id)        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE scholarships (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id        BIGINT NOT NULL,
  title             VARCHAR(150) NOT NULL,
  amount            DECIMAL(12,2) NOT NULL,
  type              ENUM('PERCENTAGE','FIXED') NOT NULL,
  academic_year_id  BIGINT NOT NULL,
  approved_by       BIGINT NULL,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_sch_student        FOREIGN KEY (student_id)       REFERENCES students(id)       ON DELETE CASCADE,
  CONSTRAINT fk_sch_academic_year  FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT,
  CONSTRAINT fk_sch_approved_by    FOREIGN KEY (approved_by)      REFERENCES users(id)          ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Transport
-- =====================================================================

CREATE TABLE drivers (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(150) NOT NULL,
  phone           VARCHAR(20),
  license_number  VARCHAR(50) NOT NULL,
  address         VARCHAR(255),
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_drivers_license (license_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE buses (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  bus_number            VARCHAR(30) NOT NULL,
  capacity              INT,
  driver_id             BIGINT NULL,
  vehicle_model         VARCHAR(100),
  registration_number   VARCHAR(50) NOT NULL,
  is_deleted            TINYINT(1) NOT NULL DEFAULT 0,
  created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_buses_bus_number (bus_number),
  UNIQUE KEY uq_buses_registration (registration_number),
  CONSTRAINT fk_buses_driver FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE routes (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  route_name   VARCHAR(150) NOT NULL,
  bus_id       BIGINT NULL,
  start_point  VARCHAR(150),
  end_point    VARCHAR(150),
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_routes_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pickup_points (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  route_id    BIGINT NOT NULL,
  point_name  VARCHAR(150) NOT NULL,
  pickup_time TIME,
  drop_time   TIME,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_pp_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_transport (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id        BIGINT NOT NULL,
  route_id          BIGINT NOT NULL,
  pickup_point_id   BIGINT NOT NULL,
  monthly_fee       DECIMAL(10,2),
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_st_student      FOREIGN KEY (student_id)      REFERENCES students(id)     ON DELETE CASCADE,
  CONSTRAINT fk_st_route        FOREIGN KEY (route_id)        REFERENCES routes(id)       ON DELETE RESTRICT,
  CONSTRAINT fk_st_pickup_point FOREIGN KEY (pickup_point_id) REFERENCES pickup_points(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Library
-- =====================================================================

CREATE TABLE book_categories (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_book_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE books (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  title             VARCHAR(255) NOT NULL,
  author            VARCHAR(150),
  isbn              VARCHAR(30) NOT NULL,
  category_id       BIGINT NOT NULL,
  publisher         VARCHAR(150),
  total_copies      INT NOT NULL DEFAULT 1,
  available_copies  INT NOT NULL DEFAULT 1,
  rack_number       VARCHAR(20),
  price             DECIMAL(10,2),
  is_deleted        TINYINT(1) NOT NULL DEFAULT 0,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_books_isbn (isbn),
  CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES book_categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE book_issues (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  book_id      BIGINT NOT NULL,
  student_id   BIGINT NULL,
  teacher_id   BIGINT NULL,
  issue_date   DATE NOT NULL,
  due_date     DATE NOT NULL,
  return_date  DATE NULL,
  fine_amount  DECIMAL(10,2) NOT NULL DEFAULT 0,
  status       ENUM('ISSUED','RETURNED','OVERDUE') NOT NULL DEFAULT 'ISSUED',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_bi_book    FOREIGN KEY (book_id)    REFERENCES books(id)    ON DELETE RESTRICT,
  CONSTRAINT fk_bi_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE SET NULL,
  CONSTRAINT fk_bi_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Hostel
-- =====================================================================

CREATE TABLE hostels (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(150) NOT NULL,
  warden_name     VARCHAR(150),
  warden_contact  VARCHAR(20),
  type            ENUM('BOYS','GIRLS') NOT NULL,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hostel_rooms (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  hostel_id       BIGINT NOT NULL,
  room_number     VARCHAR(20) NOT NULL,
  capacity        INT NOT NULL DEFAULT 1,
  occupied_count  INT NOT NULL DEFAULT 0,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_room_per_hostel (hostel_id, room_number),
  CONSTRAINT fk_hr_hostel FOREIGN KEY (hostel_id) REFERENCES hostels(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hostel_students (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id      BIGINT NOT NULL,
  room_id         BIGINT NOT NULL,
  allocation_date DATE NOT NULL,
  vacate_date     DATE NULL,
  status          ENUM('ACTIVE','VACATED') NOT NULL DEFAULT 'ACTIVE',
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_hs_student FOREIGN KEY (student_id) REFERENCES students(id)    ON DELETE CASCADE,
  CONSTRAINT fk_hs_room    FOREIGN KEY (room_id)    REFERENCES hostel_rooms(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hostel_visitors (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id     BIGINT NOT NULL,
  visitor_name   VARCHAR(150) NOT NULL,
  relation       VARCHAR(50),
  phone          VARCHAR(20),
  visit_date     DATE NOT NULL,
  purpose        VARCHAR(255),
  check_in       DATETIME,
  check_out      DATETIME NULL,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_hv_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hostel_fees (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id   BIGINT NOT NULL,
  month        INT NOT NULL,
  year         INT NOT NULL,
  amount       DECIMAL(10,2) NOT NULL,
  paid_status  ENUM('PAID','UNPAID') NOT NULL DEFAULT 'UNPAID',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_hostel_fee_month (student_id, month, year),
  CONSTRAINT fk_hf_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Exam
-- =====================================================================

CREATE TABLE exam_types (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_exam_types_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exams (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_type_id      BIGINT NOT NULL,
  class_id          BIGINT NOT NULL,
  academic_year_id  BIGINT NOT NULL,
  start_date        DATE,
  end_date          DATE,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_exams_type          FOREIGN KEY (exam_type_id)     REFERENCES exam_types(id)     ON DELETE RESTRICT,
  CONSTRAINT fk_exams_class         FOREIGN KEY (class_id)         REFERENCES classes(id)        ON DELETE RESTRICT,
  CONSTRAINT fk_exams_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exam_schedules (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id      BIGINT NOT NULL,
  subject_id   BIGINT NOT NULL,
  exam_date    DATE,
  start_time   TIME,
  end_time     TIME,
  max_marks    INT NOT NULL DEFAULT 100,
  room_number  VARCHAR(20),
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_es_exam    FOREIGN KEY (exam_id)    REFERENCES exams(id)    ON DELETE CASCADE,
  CONSTRAINT fk_es_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE grades (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  grade_name     VARCHAR(10) NOT NULL,
  min_percentage DECIMAL(5,2) NOT NULL,
  max_percentage DECIMAL(5,2) NOT NULL,
  grade_point    DECIMAL(3,1) NOT NULL,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_grades_name (grade_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE marks (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_schedule_id  BIGINT NOT NULL,
  student_id        BIGINT NOT NULL,
  marks_obtained    DECIMAL(5,2) NOT NULL,
  grade_id          BIGINT NULL,
  remarks           VARCHAR(255),
  entered_by        BIGINT NULL,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_marks_schedule_student (exam_schedule_id, student_id),
  CONSTRAINT fk_marks_schedule   FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedules(id) ON DELETE CASCADE,
  CONSTRAINT fk_marks_student    FOREIGN KEY (student_id)       REFERENCES students(id)       ON DELETE CASCADE,
  CONSTRAINT fk_marks_grade      FOREIGN KEY (grade_id)         REFERENCES grades(id)         ON DELETE SET NULL,
  CONSTRAINT fk_marks_entered_by FOREIGN KEY (entered_by)       REFERENCES users(id)          ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Assignments / Online classes
-- =====================================================================

CREATE TABLE assignments (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id      BIGINT NOT NULL,
  section_id    BIGINT NOT NULL,
  subject_id    BIGINT NOT NULL,
  teacher_id    BIGINT NULL,
  title         VARCHAR(255) NOT NULL,
  description   TEXT,
  file_url      VARCHAR(255),
  assigned_date DATE,
  due_date      DATE,
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_assign_class   FOREIGN KEY (class_id)   REFERENCES classes(id)  ON DELETE RESTRICT,
  CONSTRAINT fk_assign_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT,
  CONSTRAINT fk_assign_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT,
  CONSTRAINT fk_assign_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE assignment_submissions (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id    BIGINT NOT NULL,
  student_id       BIGINT NOT NULL,
  file_url         VARCHAR(255),
  submitted_at     DATETIME,
  marks_obtained   DECIMAL(5,2) NULL,
  feedback         VARCHAR(255),
  status           ENUM('SUBMITTED','LATE','GRADED') NOT NULL DEFAULT 'SUBMITTED',
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_submission (assignment_id, student_id),
  CONSTRAINT fk_asub_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
  CONSTRAINT fk_asub_student    FOREIGN KEY (student_id)    REFERENCES students(id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE online_classes (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id          BIGINT NOT NULL,
  section_id        BIGINT NOT NULL,
  subject_id        BIGINT NOT NULL,
  teacher_id        BIGINT NULL,
  title             VARCHAR(255) NOT NULL,
  meeting_link      VARCHAR(255),
  scheduled_at      DATETIME,
  duration_minutes  INT,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_oc_class   FOREIGN KEY (class_id)   REFERENCES classes(id)  ON DELETE RESTRICT,
  CONSTRAINT fk_oc_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT,
  CONSTRAINT fk_oc_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT,
  CONSTRAINT fk_oc_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Notice / Calendar / Admission
-- =====================================================================

CREATE TABLE notices (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  title         VARCHAR(255) NOT NULL,
  description   TEXT,
  target_role   BIGINT NULL,
  published_by  BIGINT NULL,
  published_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  expiry_date   DATE NULL,
  attachment_url VARCHAR(255),
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_notices_role FOREIGN KEY (target_role)  REFERENCES roles(id) ON DELETE SET NULL,
  CONSTRAINT fk_notices_user FOREIGN KEY (published_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE events (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(255) NOT NULL,
  description  TEXT,
  event_date   DATE NOT NULL,
  event_type   ENUM('HOLIDAY','EVENT','EXAM','OTHER') NOT NULL DEFAULT 'EVENT',
  created_by   BIGINT NULL,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admission_enquiries (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_name    VARCHAR(150) NOT NULL,
  parent_name     VARCHAR(150),
  phone           VARCHAR(20),
  email           VARCHAR(150),
  class_applying  VARCHAR(50),
  dob             DATE,
  address         VARCHAR(255),
  status          ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  documents_url   VARCHAR(255),
  applied_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Payroll / HR
-- =====================================================================

CREATE TABLE salary_structures (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id       BIGINT NOT NULL,
  employee_type     ENUM('TEACHER','STAFF') NOT NULL,
  basic_salary      DECIMAL(12,2) NOT NULL,
  hra               DECIMAL(12,2) NOT NULL DEFAULT 0,
  da                DECIMAL(12,2) NOT NULL DEFAULT 0,
  other_allowances  DECIMAL(12,2) NOT NULL DEFAULT 0,
  pf_percentage     DECIMAL(5,2) NOT NULL DEFAULT 0,
  esi_percentage    DECIMAL(5,2) NOT NULL DEFAULT 0,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_salary_structure_employee (employee_id),
  CONSTRAINT fk_ss_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payroll (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id    BIGINT NOT NULL,
  employee_type  ENUM('TEACHER','STAFF') NOT NULL,
  month          INT NOT NULL,
  year           INT NOT NULL,
  basic_salary   DECIMAL(12,2) NOT NULL,
  allowances     DECIMAL(12,2) NOT NULL DEFAULT 0,
  deductions     DECIMAL(12,2) NOT NULL DEFAULT 0,
  pf             DECIMAL(12,2) NOT NULL DEFAULT 0,
  esi            DECIMAL(12,2) NOT NULL DEFAULT 0,
  net_salary     DECIMAL(12,2) NOT NULL,
  payment_date   DATE NULL,
  status         ENUM('PENDING','PAID') NOT NULL DEFAULT 'PENDING',
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_payroll_month (employee_id, month, year),
  CONSTRAINT fk_payroll_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Communication
-- =====================================================================

CREATE TABLE notifications (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_id  BIGINT NOT NULL,
  type          ENUM('SMS','EMAIL','PUSH','IN_APP') NOT NULL,
  subject       VARCHAR(255),
  message       TEXT,
  status        ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING',
  sent_at       DATETIME NULL,
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- MODULE: Settings
-- =====================================================================

CREATE TABLE school_info (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  name                  VARCHAR(255) NOT NULL,
  address               VARCHAR(255),
  phone                 VARCHAR(20),
  email                 VARCHAR(150),
  logo_url              VARCHAR(255),
  established_year      INT,
  affiliation_number    VARCHAR(100),
  created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_settings (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  setting_key    VARCHAR(100) NOT NULL,
  setting_value  VARCHAR(500),
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_system_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
