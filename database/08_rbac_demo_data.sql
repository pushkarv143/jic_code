-- =====================================================================
-- 08_rbac_demo_data.sql
--
-- Demo logins and relationships for exercising role-based access control
-- end to end. Run AFTER 07_sample_data.sql: it depends on the classes,
-- sections, subjects and academic years that 06/07 create.
--
-- 06_seed_reference_data.sql already ships demo logins for SUPER_ADMIN,
-- PRINCIPAL, VICE_PRINCIPAL, TEACHER, ACCOUNTANT, LIBRARIAN, RECEPTIONIST
-- and SECURITY_GUARD. What it does not ship, and what RBAC cannot be
-- demonstrated without, is added here:
--
--   * a STUDENT login linked to an actual student row,
--   * a second STUDENT in a different section — the "someone else" a
--     student must be refused,
--   * a PARENT login linked to the first student only,
--   * a CLASS_TEACHER login who is homeroom teacher of one section,
--   * class_subject_teacher rows for teacher.demo, without which the
--     teacher scope query returns nothing and the demo teacher sees an
--     empty directory.
--
-- Passwords (bcrypt hashes reused verbatim from 06_seed_reference_data.sql):
--   admin / Admin@123
--   every other demo login / Password@123
--
-- Re-runnable: every insert is guarded, so applying this twice is a no-op
-- rather than a duplicate-key failure.
-- =====================================================================

USE school_management_system;

SET @PW_HASH := '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa';
SET @ACADEMIC_YEAR_ID := (SELECT id FROM academic_years WHERE is_current = 1 LIMIT 1);

-- Two sections in the same class where possible, so "another student the
-- teacher does not teach" is a realistic neighbour rather than a far-away row.
SET @SECTION_A := (SELECT id FROM sections ORDER BY id LIMIT 1);
SET @CLASS_A   := (SELECT class_id FROM sections WHERE id = @SECTION_A);
SET @SECTION_B := (SELECT id FROM sections WHERE id <> @SECTION_A ORDER BY id LIMIT 1);
SET @CLASS_B   := (SELECT class_id FROM sections WHERE id = @SECTION_B);

-- ---------------------------------------------------------------------
-- CLASS_TEACHER demo login, homeroom teacher of section A.
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
SELECT 'classteacher.demo', 'classteacher.demo@school.edu', @PW_HASH,
       'Meera', 'Iyer', '+91-9999900009', 'FEMALE',
       (SELECT id FROM roles WHERE name = 'CLASS_TEACHER'), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'classteacher.demo');

INSERT INTO teachers (
  user_id, employee_id, department_id, designation_id, qualification,
  experience_years, joining_date, date_of_birth, gender, address, city, state, pincode,
  blood_group, emergency_contact, salary, employment_type, status
)
SELECT (SELECT id FROM users WHERE username = 'classteacher.demo'),
       'EMP-DEMO-CT-001',
       (SELECT id FROM departments WHERE name = 'Academics'),
       (SELECT id FROM designations WHERE name = 'TGT'),
       'M.A B.Ed', 12, '2014-06-01', '1986-09-20', 'FEMALE',
       '18 Hauz Khas', 'New Delhi', 'Delhi', '110016',
       'B+', '+91-9999911009', 58000.00, 'FULL_TIME', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM teachers WHERE employee_id = 'EMP-DEMO-CT-001');

SET @CLASS_TEACHER_ID := (SELECT id FROM teachers WHERE employee_id = 'EMP-DEMO-CT-001');
SET @DEMO_TEACHER_ID  := (SELECT id FROM teachers WHERE employee_id = 'EMP-DEMO-TCH-001');

-- Homeroom assignment. This alone is what makes section A's students
-- visible to classteacher.demo under StudentAccessGuard's "taught" scope.
UPDATE sections SET class_teacher_id = @CLASS_TEACHER_ID WHERE id = @SECTION_A;

-- ---------------------------------------------------------------------
-- Subject mappings for teacher.demo, in section A only.
--
-- teacher.demo is seeded in 06 with no class_subject_teacher rows at all,
-- which under row-level scoping means an empty student list. Mapping them
-- to section A (and deliberately NOT to section B) is what makes the
-- "teacher cannot see a student they do not teach" case demonstrable.
-- ---------------------------------------------------------------------
INSERT INTO class_subject_teacher (class_id, section_id, subject_id, teacher_id)
SELECT @CLASS_A, @SECTION_A, sub.id, @DEMO_TEACHER_ID
FROM subjects sub
-- Subjects belong to a class, so only this class's subjects are valid here.
WHERE sub.class_id = @CLASS_A
  AND sub.is_deleted = 0
  AND @DEMO_TEACHER_ID IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM class_subject_teacher cst
    WHERE cst.section_id = @SECTION_A AND cst.subject_id = sub.id AND cst.teacher_id = @DEMO_TEACHER_ID
  )
ORDER BY sub.id
LIMIT 2;

-- classteacher.demo teaches a subject in section A as well as being its homeroom
-- teacher. Homeroom lives on sections.class_teacher_id, which grants the student
-- scope but produces no class_subject_teacher row — so without this their
-- /teachers/me/assignments list would come back empty even though they clearly
-- have a class.
INSERT INTO class_subject_teacher (class_id, section_id, subject_id, teacher_id)
SELECT @CLASS_A, @SECTION_A, sub.id, @CLASS_TEACHER_ID
FROM subjects sub
WHERE sub.class_id = @CLASS_A
  AND sub.is_deleted = 0
  AND @CLASS_TEACHER_ID IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM class_subject_teacher cst
    WHERE cst.section_id = @SECTION_A AND cst.subject_id = sub.id
  )
ORDER BY sub.id
LIMIT 1;

-- ---------------------------------------------------------------------
-- STUDENT demo login (section A — taught by both demo teachers).
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
SELECT 'student.demo', 'student.demo@school.edu', @PW_HASH,
       'Aarav', 'Sharma', '+91-9999900010', 'MALE',
       (SELECT id FROM roles WHERE name = 'STUDENT'), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'student.demo');

INSERT INTO students (
  user_id, admission_number, class_id, section_id, roll_number, admission_date,
  date_of_birth, gender, blood_group, religion, category,
  address, city, state, pincode, academic_year_id, status
)
SELECT (SELECT id FROM users WHERE username = 'student.demo'),
       'ADM-DEMO-0001', @CLASS_A, @SECTION_A, 901, '2026-04-01',
       '2012-05-14', 'MALE', 'O+', 'Hindu', 'General',
       '42 Vasant Vihar', 'New Delhi', 'Delhi', '110057', @ACADEMIC_YEAR_ID, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE admission_number = 'ADM-DEMO-0001');

-- ---------------------------------------------------------------------
-- A second STUDENT, in a different section on purpose.
--
-- This is the control case: student.demo must be refused this record, and
-- so must teacher.demo, who has no mapping to section B.
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
SELECT 'student.other', 'student.other@school.edu', @PW_HASH,
       'Diya', 'Kapoor', '+91-9999900011', 'FEMALE',
       (SELECT id FROM roles WHERE name = 'STUDENT'), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'student.other');

INSERT INTO students (
  user_id, admission_number, class_id, section_id, roll_number, admission_date,
  date_of_birth, gender, blood_group, religion, category,
  address, city, state, pincode, academic_year_id, status
)
SELECT (SELECT id FROM users WHERE username = 'student.other'),
       'ADM-DEMO-0002', @CLASS_B, @SECTION_B, 902, '2026-04-01',
       '2012-11-02', 'FEMALE', 'A+', 'Hindu', 'General',
       '9 Saket', 'New Delhi', 'Delhi', '110017', @ACADEMIC_YEAR_ID, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE admission_number = 'ADM-DEMO-0002');

SET @DEMO_STUDENT_ID  := (SELECT id FROM students WHERE admission_number = 'ADM-DEMO-0001');
SET @OTHER_STUDENT_ID := (SELECT id FROM students WHERE admission_number = 'ADM-DEMO-0002');

-- Guardian rows for the demo student, so the guardians tab has content.
INSERT INTO guardians (student_id, name, relation, occupation, phone, email, address, is_primary)
SELECT @DEMO_STUDENT_ID, 'Rohit Sharma', 'Father', 'Software Engineer',
       '+91-9999900012', 'rohit.sharma@example.com', '42 Vasant Vihar, New Delhi', 1
WHERE NOT EXISTS (
  SELECT 1 FROM guardians WHERE student_id = @DEMO_STUDENT_ID AND relation = 'Father'
);

INSERT INTO guardians (student_id, name, relation, occupation, phone, email, address, is_primary)
SELECT @DEMO_STUDENT_ID, 'Neha Sharma', 'Mother', 'Doctor',
       '+91-9999900013', 'neha.sharma@example.com', '42 Vasant Vihar, New Delhi', 0
WHERE NOT EXISTS (
  SELECT 1 FROM guardians WHERE student_id = @DEMO_STUDENT_ID AND relation = 'Mother'
);

-- ---------------------------------------------------------------------
-- PARENT demo login, linked to student.demo only.
--
-- The link goes through parents + student_parents, which is exactly the
-- path StudentRepository.findAllByParentUserId walks, so this is what
-- makes the parent scope real rather than theoretical.
-- ---------------------------------------------------------------------
INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
SELECT 'parent.demo', 'parent.demo@school.edu', @PW_HASH,
       'Rohit', 'Sharma', '+91-9999900012', 'MALE',
       (SELECT id FROM roles WHERE name = 'PARENT'), 1, 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'parent.demo');

INSERT INTO parents (user_id, occupation, annual_income)
SELECT (SELECT id FROM users WHERE username = 'parent.demo'), 'Software Engineer', 1800000.00
WHERE NOT EXISTS (
  SELECT 1 FROM parents WHERE user_id = (SELECT id FROM users WHERE username = 'parent.demo')
);

SET @DEMO_PARENT_ID := (SELECT id FROM parents WHERE user_id = (SELECT id FROM users WHERE username = 'parent.demo'));

INSERT INTO student_parents (student_id, parent_id)
SELECT @DEMO_STUDENT_ID, @DEMO_PARENT_ID
WHERE @DEMO_STUDENT_ID IS NOT NULL AND @DEMO_PARENT_ID IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM student_parents WHERE student_id = @DEMO_STUDENT_ID AND parent_id = @DEMO_PARENT_ID
  );

-- ---------------------------------------------------------------------
-- Verification: what each demo login should see.
--
-- /api/v1/students — row-level scoping:
--   admin / principal      -> every student
--   classteacher.demo      -> section A only (includes ADM-DEMO-0001,
--                             excludes ADM-DEMO-0002)
--   teacher.demo           -> section A only, same as above
--   student.demo           -> exactly 1 row, ADM-DEMO-0001
--   parent.demo            -> exactly 1 row, ADM-DEMO-0001
--
-- /api/v1/teachers — field-level redaction (all rows, varying detail):
--   admin / principal      -> every teacher, salary and personal fields intact
--   accountant.demo        -> same (payroll is computed from salary)
--   teacher.demo           -> every teacher, but salary / date_of_birth /
--                             address / emergency_contact / blood_group null
--                             on everyone except their own row
--   receptionist.demo      -> every teacher, all sensitive fields null
--   student.demo           -> 403, no staff directory access at all
--
-- /api/v1/teachers/me — teacher.demo and classteacher.demo only; returns their
-- own record unredacted. /teachers/me/assignments returns their section A
-- subject mappings. Note these are two different grants: homeroom
-- (sections.class_teacher_id) is what widens a class teacher's *student* scope,
-- while class_subject_teacher is what populates their *assignment* list.
--
-- /api/v1/attendance/students — section-level scoping:
--   teacher.demo / classteacher.demo -> may pull the grid and mark section A;
--                             section B returns 403 (SectionAccessGuard)
--   marking a section-B student while addressing section A returns 400 — the
--   student is not enrolled in the section being marked
--   student.demo           -> 403 on the grid; /me/summary returns their own
--                             percentage with no id in the request
--   admin / principal      -> any section
-- ---------------------------------------------------------------------
SELECT 'RBAC demo data loaded' AS status,
       @DEMO_STUDENT_ID       AS demo_student_id,
       @OTHER_STUDENT_ID      AS other_student_id,
       @SECTION_A             AS taught_section_id,
       @SECTION_B             AS untaught_section_id,
       @CLASS_TEACHER_ID      AS class_teacher_id,
       @DEMO_TEACHER_ID       AS subject_teacher_id;
