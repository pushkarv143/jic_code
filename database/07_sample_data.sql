-- =====================================================================
-- 07_sample_data.sql
-- Bulk sample-data generator. Run after 00-06.
--
-- HOW THIS FILE WORKS
-- MySQL has no arrays, so "random-ish but varied" names are produced by
-- seeding small lookup tables (name_pool_first_male/female, name_pool_last,
-- 30 rows each) and indexing into them with MOD(counter, 30) + 1 inside
-- WHILE loops. Five stored procedures do the actual generation:
--   sp_gen_teachers()   -> 50 teacher users + teacher rows, 2-4 subject
--                          assignments each via class_subject_teacher,
--                          and class_teacher_id set on a subset of
--                          sections (promoting that teacher's role to
--                          CLASS_TEACHER).
--   sp_gen_staff()      -> 30 staff users + staff rows spread across
--                          ACCOUNTANT / LIBRARIAN / RECEPTIONIST /
--                          SECURITY_GUARD.
--   sp_gen_students()   -> 500 student users + student rows spread
--                          round-robin across the 54 sections, with
--                          1-2 guardians each and medical details for
--                          about half.
--   sp_gen_attendance() -> 2000 student_attendance rows: 40 school days
--                          (Jun-Jul 2026, Sundays skipped) x a rotating,
--                          non-overlapping window of 50 students per
--                          day, which keeps every (student_id,
--                          attendance_date) pair unique automatically.
--   sp_gen_fees()       -> 1000 student_fees rows (500 students x
--                          Tuition Fee + Exam Fee) with a deterministic
--                          45% PAID / 25% PARTIAL / 30% unpaid split;
--                          PAID/PARTIAL rows get a matching fee_payments
--                          row (~700 total), letting the
--                          trg_fee_payments_after_insert trigger from
--                          04_triggers.sql compute amount_paid/status.
--
-- All five sp_gen_* procedures are DROPped at the end of this file -
-- they are one-off ETL helpers, not part of the application's permanent
-- procedure set (unlike 05_procedures.sql). The three name_pool_*
-- tables are left in place (harmless, small, occasionally useful for a
-- future re-run or extension of this generator) and are documented here
-- as such.
-- =====================================================================

USE school_management_system;

-- =====================================================================
-- Name pools (helper tables, kept after generation - see note above)
-- =====================================================================
DROP TABLE IF EXISTS name_pool_first_male;
CREATE TABLE name_pool_first_male (
  id INT PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO name_pool_first_male (id, first_name) VALUES
  (1,'Aarav'),(2,'Vivaan'),(3,'Aditya'),(4,'Vihaan'),(5,'Arjun'),
  (6,'Sai'),(7,'Reyansh'),(8,'Ayaan'),(9,'Krishna'),(10,'Ishaan'),
  (11,'Shaurya'),(12,'Atharv'),(13,'Advik'),(14,'Kabir'),(15,'Ansh'),
  (16,'Rudra'),(17,'Vivek'),(18,'Rohan'),(19,'Karan'),(20,'Aryan'),
  (21,'Dev'),(22,'Yash'),(23,'Harsh'),(24,'Nikhil'),(25,'Rahul'),
  (26,'Amit'),(27,'Sanjay'),(28,'Vikram'),(29,'Manish'),(30,'Deepak');

DROP TABLE IF EXISTS name_pool_first_female;
CREATE TABLE name_pool_first_female (
  id INT PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO name_pool_first_female (id, first_name) VALUES
  (1,'Ananya'),(2,'Diya'),(3,'Ira'),(4,'Myra'),(5,'Aadhya'),
  (6,'Anika'),(7,'Navya'),(8,'Kiara'),(9,'Saanvi'),(10,'Pari'),
  (11,'Riya'),(12,'Ishita'),(13,'Priya'),(14,'Sneha'),(15,'Neha'),
  (16,'Pooja'),(17,'Kavya'),(18,'Tara'),(19,'Meera'),(20,'Anaya'),
  (21,'Aditi'),(22,'Divya'),(23,'Shreya'),(24,'Nisha'),(25,'Rani'),
  (26,'Sunita'),(27,'Kavita'),(28,'Suman'),(29,'Anjali'),(30,'Preeti');

DROP TABLE IF EXISTS name_pool_last;
CREATE TABLE name_pool_last (
  id INT PRIMARY KEY,
  last_name VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO name_pool_last (id, last_name) VALUES
  (1,'Sharma'),(2,'Verma'),(3,'Gupta'),(4,'Singh'),(5,'Kumar'),
  (6,'Patel'),(7,'Yadav'),(8,'Mehta'),(9,'Agarwal'),(10,'Nair'),
  (11,'Reddy'),(12,'Rao'),(13,'Iyer'),(14,'Menon'),(15,'Chopra'),
  (16,'Malhotra'),(17,'Kapoor'),(18,'Bhatia'),(19,'Joshi'),(20,'Desai'),
  (21,'Pillai'),(22,'Chauhan'),(23,'Rathore'),(24,'Saxena'),(25,'Mishra'),
  (26,'Tiwari'),(27,'Pandey'),(28,'Dubey'),(29,'Trivedi'),(30,'Bansal');

-- =====================================================================
-- Generator procedures
-- =====================================================================
DELIMITER $$

-- ---------------------------------------------------------------------
-- sp_gen_teachers: 50 teachers, 2-4 subject assignments each, class
-- teacher assignment on a subset of sections.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_teachers $$
CREATE PROCEDURE sp_gen_teachers()
BEGIN
  DECLARE v_i INT DEFAULT 1;
  DECLARE v_gender VARCHAR(6);
  DECLARE v_first VARCHAR(50);
  DECLARE v_last VARCHAR(50);
  DECLARE v_dept_id BIGINT;
  DECLARE v_desig_id BIGINT;
  DECLARE v_desig_name VARCHAR(20);
  DECLARE v_grp VARCHAR(10);
  DECLARE v_username VARCHAR(50);
  DECLARE v_email VARCHAR(150);
  DECLARE v_employee_id VARCHAR(30);
  DECLARE v_user_id BIGINT;
  DECLARE v_teacher_id BIGINT;
  DECLARE v_dob DATE;
  DECLARE v_joining DATE;
  DECLARE v_exp INT;
  DECLARE v_salary DECIMAL(12,2);
  DECLARE v_blood VARCHAR(5);
  DECLARE v_status VARCHAR(15);
  DECLARE v_emp_type VARCHAR(15);
  DECLARE v_k INT;
  DECLARE v_subj_count INT;
  DECLARE v_section_id BIGINT;
  DECLARE v_subject_id BIGINT;
  DECLARE v_class_id BIGINT;

  DROP TEMPORARY TABLE IF EXISTS tmp_generated_teachers;
  CREATE TEMPORARY TABLE tmp_generated_teachers (
    rn INT PRIMARY KEY,
    teacher_id BIGINT,
    grp VARCHAR(10)
  );

  WHILE v_i <= 50 DO
    SET v_gender = IF(MOD(v_i,2)=0,'FEMALE','MALE');
    IF v_gender = 'MALE' THEN
      SELECT first_name INTO v_first FROM name_pool_first_male WHERE id = MOD(v_i-1,30)+1;
    ELSE
      SELECT first_name INTO v_first FROM name_pool_first_female WHERE id = MOD(v_i-1,30)+1;
    END IF;
    SELECT last_name INTO v_last FROM name_pool_last WHERE id = MOD((v_i-1)*7+3,30)+1;

    SET v_grp = CASE MOD(v_i,3) WHEN 0 THEN 'PRT' WHEN 1 THEN 'TGT' ELSE 'PGT' END;
    SET v_desig_name = v_grp; -- designations table has rows named exactly 'PRT'/'TGT'/'PGT'
    SELECT id INTO v_desig_id FROM designations WHERE name = v_desig_name;

    SET v_dept_id = CASE
        WHEN MOD(v_i,17) = 0 THEN (SELECT id FROM departments WHERE name = 'Sports')
        WHEN MOD(v_i,13) = 0 THEN (SELECT id FROM departments WHERE name = 'IT')
        ELSE (SELECT id FROM departments WHERE name = 'Academics')
      END;

    SET v_username    = CONCAT('teacher', v_i);
    SET v_email       = CONCAT('teacher', v_i, '@school.edu');
    SET v_employee_id = CONCAT('EMP-TCH-', LPAD(v_i,4,'0'));

    SET v_exp     = MOD(v_i,20) + 1;
    SET v_joining = DATE_SUB('2026-04-01', INTERVAL (MOD(v_i,15)+1) YEAR);
    SET v_dob     = DATE_SUB('2026-08-06', INTERVAL (28 + MOD(v_i,25)) YEAR);
    SET v_blood   = ELT(MOD(v_i,8)+1,'A+','A-','B+','B-','O+','O-','AB+','AB-');
    SET v_salary  = CASE v_grp
        WHEN 'PGT' THEN 55000 + MOD(v_i,10)*1500
        WHEN 'TGT' THEN 42000 + MOD(v_i,10)*1200
        ELSE 32000 + MOD(v_i,10)*1000
      END;
    SET v_emp_type = IF(MOD(v_i,9)=0,'CONTRACT', IF(MOD(v_i,11)=0,'PART_TIME','FULL_TIME'));
    SET v_status   = IF(v_i=49,'RESIGNED', IF(v_i=50,'INACTIVE','ACTIVE'));

    INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
    VALUES (
      v_username, v_email,
      '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
      v_first, v_last, CONCAT('+91-98', LPAD(v_i,8,'0')), v_gender,
      (SELECT id FROM roles WHERE name = 'TEACHER'), 1, 1
    );
    SET v_user_id = LAST_INSERT_ID();

    INSERT INTO teachers (
      user_id, employee_id, department_id, designation_id, qualification,
      experience_years, joining_date, date_of_birth, gender, address, city, state, pincode,
      blood_group, emergency_contact, salary, employment_type, status
    ) VALUES (
      v_user_id, v_employee_id, v_dept_id, v_desig_id,
      CASE v_grp WHEN 'PGT' THEN 'M.Sc, B.Ed' WHEN 'TGT' THEN 'M.A, B.Ed' ELSE 'B.El.Ed' END,
      v_exp, v_joining, v_dob, v_gender,
      CONCAT(MOD(v_i,200)+1,' Model Town'), 'New Delhi', 'Delhi', CONCAT('1100', LPAD(MOD(v_i,99),2,'0')),
      v_blood, CONCAT('+91-97', LPAD(v_i,8,'0')), v_salary, v_emp_type, v_status
    );
    SET v_teacher_id = LAST_INSERT_ID();

    INSERT INTO tmp_generated_teachers VALUES (v_i, v_teacher_id, v_grp);

    -- Assign 2-4 subjects, each a distinct (section, subject) pair drawn
    -- from this teacher's designation group, skipping pairs already taken.
    SET v_subj_count = 2 + MOD(v_i,3);
    SET v_k = 1;
    WHILE v_k <= v_subj_count DO
      SET v_section_id = NULL;
      SET v_subject_id = NULL;
      SET v_class_id   = NULL;

      SELECT sec.id, sub.id, sec.class_id
        INTO v_section_id, v_subject_id, v_class_id
      FROM sections sec
      JOIN classes c  ON c.id = sec.class_id
      JOIN subjects sub ON sub.class_id = c.id
      WHERE (
        (v_grp = 'PRT' AND c.class_name IN ('Pre-Nursery','Nursery','LKG','UKG','Class 1','Class 2','Class 3','Class 4','Class 5'))
        OR (v_grp = 'TGT' AND c.class_name IN ('Class 6','Class 7','Class 8','Class 9','Class 10'))
        OR (v_grp = 'PGT' AND c.class_name IN ('Class 11 Science','Class 11 Commerce','Class 11 Arts','Class 12 Science','Class 12 Commerce','Class 12 Arts'))
      )
      AND NOT EXISTS (
        SELECT 1 FROM class_subject_teacher cst
        WHERE cst.section_id = sec.id AND cst.subject_id = sub.id
      )
      ORDER BY sec.id, sub.id
      LIMIT 1;

      IF v_section_id IS NOT NULL THEN
        INSERT INTO class_subject_teacher (class_id, section_id, subject_id, teacher_id)
        VALUES (v_class_id, v_section_id, v_subject_id, v_teacher_id);
      END IF;

      SET v_k = v_k + 1;
    END WHILE;

    SET v_i = v_i + 1;
  END WHILE;

  -- Promote a subset of these teachers to class_teacher of a section
  -- (matched within the same designation group) and flip their user
  -- role from TEACHER to CLASS_TEACHER accordingly.
  DROP TEMPORARY TABLE IF EXISTS tmp_section_teacher_pairs;
  CREATE TEMPORARY TABLE tmp_section_teacher_pairs (
    section_id BIGINT PRIMARY KEY,
    teacher_id BIGINT
  );

  INSERT INTO tmp_section_teacher_pairs (section_id, teacher_id)
  SELECT s.section_id, t.teacher_id
  FROM (
    SELECT sec.id AS section_id, ROW_NUMBER() OVER (ORDER BY sec.id) AS rn
    FROM sections sec JOIN classes c ON c.id = sec.class_id
    WHERE c.class_name IN ('Pre-Nursery','Nursery','LKG','UKG','Class 1','Class 2','Class 3','Class 4','Class 5')
  ) s
  JOIN (
    SELECT teacher_id, ROW_NUMBER() OVER (ORDER BY teacher_id) AS rn
    FROM tmp_generated_teachers WHERE grp = 'PRT'
  ) t ON t.rn = s.rn;

  INSERT INTO tmp_section_teacher_pairs (section_id, teacher_id)
  SELECT s.section_id, t.teacher_id
  FROM (
    SELECT sec.id AS section_id, ROW_NUMBER() OVER (ORDER BY sec.id) AS rn
    FROM sections sec JOIN classes c ON c.id = sec.class_id
    WHERE c.class_name IN ('Class 6','Class 7','Class 8','Class 9','Class 10')
  ) s
  JOIN (
    SELECT teacher_id, ROW_NUMBER() OVER (ORDER BY teacher_id) AS rn
    FROM tmp_generated_teachers WHERE grp = 'TGT'
  ) t ON t.rn = s.rn;

  INSERT INTO tmp_section_teacher_pairs (section_id, teacher_id)
  SELECT s.section_id, t.teacher_id
  FROM (
    SELECT sec.id AS section_id, ROW_NUMBER() OVER (ORDER BY sec.id) AS rn
    FROM sections sec JOIN classes c ON c.id = sec.class_id
    WHERE c.class_name IN ('Class 11 Science','Class 11 Commerce','Class 11 Arts','Class 12 Science','Class 12 Commerce','Class 12 Arts')
  ) s
  JOIN (
    SELECT teacher_id, ROW_NUMBER() OVER (ORDER BY teacher_id) AS rn
    FROM tmp_generated_teachers WHERE grp = 'PGT'
  ) t ON t.rn = s.rn;

  UPDATE sections sec
  JOIN tmp_section_teacher_pairs p ON p.section_id = sec.id
  SET sec.class_teacher_id = p.teacher_id;

  UPDATE users u
  JOIN teachers t ON t.user_id = u.id
  JOIN tmp_section_teacher_pairs p ON p.teacher_id = t.id
  SET u.role_id = (SELECT id FROM roles WHERE name = 'CLASS_TEACHER');

  DROP TEMPORARY TABLE IF EXISTS tmp_section_teacher_pairs;
  DROP TEMPORARY TABLE IF EXISTS tmp_generated_teachers;
END $$

-- ---------------------------------------------------------------------
-- sp_gen_staff: 30 staff, split across ACCOUNTANT / LIBRARIAN /
-- RECEPTIONIST / SECURITY_GUARD.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_staff $$
CREATE PROCEDURE sp_gen_staff()
BEGIN
  DECLARE v_i INT DEFAULT 1;
  DECLARE v_gender VARCHAR(6);
  DECLARE v_first VARCHAR(50);
  DECLARE v_last VARCHAR(50);
  DECLARE v_role_name VARCHAR(30);
  DECLARE v_dept_id BIGINT;
  DECLARE v_desig_id BIGINT;
  DECLARE v_username VARCHAR(50);
  DECLARE v_email VARCHAR(150);
  DECLARE v_employee_id VARCHAR(30);
  DECLARE v_user_id BIGINT;
  DECLARE v_joining DATE;
  DECLARE v_salary DECIMAL(12,2);

  WHILE v_i <= 30 DO
    SET v_gender = IF(MOD(v_i,2)=0,'FEMALE','MALE');
    IF v_gender = 'MALE' THEN
      SELECT first_name INTO v_first FROM name_pool_first_male WHERE id = MOD(v_i+4,30)+1;
    ELSE
      SELECT first_name INTO v_first FROM name_pool_first_female WHERE id = MOD(v_i+4,30)+1;
    END IF;
    SELECT last_name INTO v_last FROM name_pool_last WHERE id = MOD(v_i*5+11,30)+1;

    SET v_role_name = CASE MOD(v_i,4)
        WHEN 0 THEN 'ACCOUNTANT'
        WHEN 1 THEN 'LIBRARIAN'
        WHEN 2 THEN 'RECEPTIONIST'
        ELSE 'SECURITY_GUARD'
      END;

    SET v_dept_id = CASE v_role_name
        WHEN 'ACCOUNTANT' THEN (SELECT id FROM departments WHERE name = 'Accounts')
        WHEN 'LIBRARIAN'  THEN (SELECT id FROM departments WHERE name = 'Library')
        ELSE (SELECT id FROM departments WHERE name = 'Administration')
      END;

    SET v_desig_id = CASE v_role_name
        WHEN 'ACCOUNTANT'     THEN (SELECT id FROM designations WHERE name = 'Accountant')
        WHEN 'LIBRARIAN'      THEN (SELECT id FROM designations WHERE name = 'Librarian')
        WHEN 'RECEPTIONIST'   THEN (SELECT id FROM designations WHERE name = 'Receptionist')
        ELSE (SELECT id FROM designations WHERE name = 'Security Guard')
      END;

    SET v_username    = CONCAT('staff', v_i);
    SET v_email       = CONCAT('staff', v_i, '@school.edu');
    SET v_employee_id = CONCAT('EMP-STF-', LPAD(v_i,4,'0'));
    SET v_joining     = DATE_SUB('2026-04-01', INTERVAL (MOD(v_i,10)+1) YEAR);
    SET v_salary = CASE v_role_name
        WHEN 'ACCOUNTANT'   THEN 35000 + MOD(v_i,10)*800
        WHEN 'LIBRARIAN'    THEN 28000 + MOD(v_i,10)*600
        WHEN 'RECEPTIONIST' THEN 22000 + MOD(v_i,10)*500
        ELSE 16000 + MOD(v_i,10)*400
      END;

    INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
    VALUES (
      v_username, v_email,
      '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
      v_first, v_last, CONCAT('+91-96', LPAD(v_i,8,'0')), v_gender,
      (SELECT id FROM roles WHERE name = v_role_name), 1, 1
    );
    SET v_user_id = LAST_INSERT_ID();

    INSERT INTO staff (user_id, employee_id, department_id, designation_id, joining_date, salary, status)
    VALUES (v_user_id, v_employee_id, v_dept_id, v_desig_id, v_joining, v_salary, 'ACTIVE');

    SET v_i = v_i + 1;
  END WHILE;
END $$

-- ---------------------------------------------------------------------
-- sp_gen_students: 500 students round-robin across the 54 sections,
-- sequential roll numbers per section, 1-2 guardians each, medical
-- details for about half.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_students $$
CREATE PROCEDURE sp_gen_students()
BEGIN
  DECLARE v_i INT DEFAULT 1;
  DECLARE v_section_rn INT;
  DECLARE v_section_id BIGINT;
  DECLARE v_class_id BIGINT;
  DECLARE v_class_name VARCHAR(50);
  DECLARE v_roll INT;
  DECLARE v_base_age INT;
  DECLARE v_gender VARCHAR(6);
  DECLARE v_first VARCHAR(50);
  DECLARE v_last VARCHAR(50);
  DECLARE v_dob DATE;
  DECLARE v_admission_date DATE;
  DECLARE v_admission_number VARCHAR(30);
  DECLARE v_username VARCHAR(50);
  DECLARE v_email VARCHAR(150);
  DECLARE v_blood VARCHAR(5);
  DECLARE v_religion VARCHAR(50);
  DECLARE v_category VARCHAR(50);
  DECLARE v_address VARCHAR(255);
  DECLARE v_pincode VARCHAR(10);
  DECLARE v_user_id BIGINT;
  DECLARE v_student_id BIGINT;
  DECLARE v_current_ay_id BIGINT;
  DECLARE v_relation1 VARCHAR(20);
  DECLARE v_relation2 VARCHAR(20);
  DECLARE v_g1_first VARCHAR(50);
  DECLARE v_g2_first VARCHAR(50);

  SELECT id INTO v_current_ay_id FROM academic_years WHERE is_current = 1;

  DROP TEMPORARY TABLE IF EXISTS tmp_sections;
  CREATE TEMPORARY TABLE tmp_sections (
    rn INT PRIMARY KEY,
    section_id BIGINT,
    class_id BIGINT,
    class_name VARCHAR(50),
    next_roll INT DEFAULT 1
  );

  INSERT INTO tmp_sections (rn, section_id, class_id, class_name)
  SELECT ROW_NUMBER() OVER (ORDER BY sec.id), sec.id, sec.class_id, c.class_name
  FROM sections sec JOIN classes c ON c.id = sec.class_id;

  WHILE v_i <= 500 DO
    SET v_section_rn = MOD(v_i - 1, 54) + 1;

    SELECT section_id, class_id, class_name, next_roll
      INTO v_section_id, v_class_id, v_class_name, v_roll
    FROM tmp_sections WHERE rn = v_section_rn;

    UPDATE tmp_sections SET next_roll = next_roll + 1 WHERE rn = v_section_rn;

    SET v_base_age = CASE v_class_name
        WHEN 'Pre-Nursery' THEN 3 WHEN 'Nursery' THEN 4 WHEN 'LKG' THEN 5 WHEN 'UKG' THEN 6
        WHEN 'Class 1' THEN 6 WHEN 'Class 2' THEN 7 WHEN 'Class 3' THEN 8 WHEN 'Class 4' THEN 9 WHEN 'Class 5' THEN 10
        WHEN 'Class 6' THEN 11 WHEN 'Class 7' THEN 12 WHEN 'Class 8' THEN 13 WHEN 'Class 9' THEN 14 WHEN 'Class 10' THEN 15
        WHEN 'Class 11 Science' THEN 16 WHEN 'Class 11 Commerce' THEN 16 WHEN 'Class 11 Arts' THEN 16
        WHEN 'Class 12 Science' THEN 17 WHEN 'Class 12 Commerce' THEN 17 WHEN 'Class 12 Arts' THEN 17
        ELSE 10
      END;

    SET v_gender = IF(MOD(v_i,2)=0,'FEMALE','MALE');
    IF v_gender = 'MALE' THEN
      SELECT first_name INTO v_first FROM name_pool_first_male WHERE id = MOD(v_i+2,30)+1;
    ELSE
      SELECT first_name INTO v_first FROM name_pool_first_female WHERE id = MOD(v_i+2,30)+1;
    END IF;
    SELECT last_name INTO v_last FROM name_pool_last WHERE id = MOD(v_i*3+1,30)+1;

    SET v_dob = DATE_SUB(DATE_SUB('2026-04-01', INTERVAL v_base_age YEAR), INTERVAL MOD(v_i*13,365) DAY);
    SET v_admission_date = DATE_SUB('2026-04-01', INTERVAL MOD(v_i,5) YEAR);
    SET v_admission_number = CONCAT('ADM2026', LPAD(v_i,4,'0'));
    SET v_username = CONCAT('student', v_i);
    SET v_email    = CONCAT('student', v_i, '@school.edu');
    SET v_blood    = ELT(MOD(v_i,8)+1,'A+','A-','B+','B-','O+','O-','AB+','AB-');
    SET v_religion = CASE MOD(v_i,4) WHEN 0 THEN 'Hindu' WHEN 1 THEN 'Muslim' WHEN 2 THEN 'Sikh' ELSE 'Christian' END;
    SET v_category = CASE MOD(v_i,4) WHEN 0 THEN 'General' WHEN 1 THEN 'OBC' WHEN 2 THEN 'SC' ELSE 'ST' END;
    SET v_address  = CONCAT(MOD(v_i,300)+1, ' ',
        CASE MOD(v_i,5) WHEN 0 THEN 'Rohini' WHEN 1 THEN 'Pitampura' WHEN 2 THEN 'Model Town' WHEN 3 THEN 'Ashok Vihar' ELSE 'Shalimar Bagh' END
      );
    SET v_pincode  = CONCAT('1100', LPAD(MOD(v_i,99),2,'0'));

    INSERT INTO users (username, email, password, first_name, last_name, phone, gender, role_id, is_active, is_email_verified)
    VALUES (
      v_username, v_email,
      '$2b$10$k1TFzKvmWn1ntt3ZflVeyew9gH/gFNjdLsREDELwqZWjrfWR7B7Aa',
      v_first, v_last, CONCAT('+91-95', LPAD(v_i,8,'0')), v_gender,
      (SELECT id FROM roles WHERE name = 'STUDENT'), 1, 1
    );
    SET v_user_id = LAST_INSERT_ID();

    INSERT INTO students (
      user_id, admission_number, class_id, section_id, roll_number, admission_date,
      date_of_birth, gender, blood_group, religion, category, address, city, state, pincode,
      academic_year_id, status
    ) VALUES (
      v_user_id, v_admission_number, v_class_id, v_section_id, v_roll, v_admission_date,
      v_dob, v_gender, v_blood, v_religion, v_category, v_address, 'New Delhi', 'Delhi', v_pincode,
      v_current_ay_id, 'ACTIVE'
    );
    SET v_student_id = LAST_INSERT_ID();

    -- Guardian 1: always present, always primary.
    SET v_relation1 = IF(MOD(v_i,2)=0,'Mother','Father');
    IF v_relation1 = 'Mother' THEN
      SELECT first_name INTO v_g1_first FROM name_pool_first_female WHERE id = MOD(v_i+9,30)+1;
    ELSE
      SELECT first_name INTO v_g1_first FROM name_pool_first_male WHERE id = MOD(v_i+9,30)+1;
    END IF;

    INSERT INTO guardians (student_id, name, relation, occupation, phone, email, address, is_primary)
    VALUES (
      v_student_id, CONCAT(v_g1_first, ' ', v_last), v_relation1,
      CASE MOD(v_i,6) WHEN 0 THEN 'Business' WHEN 1 THEN 'Government Service' WHEN 2 THEN 'Private Job'
                      WHEN 3 THEN 'Teacher' WHEN 4 THEN 'Doctor' ELSE 'Homemaker' END,
      CONCAT('+91-90', LPAD(v_i,8,'0')), CONCAT('guardian', v_i, '1@example.com'), v_address, 1
    );

    -- Guardian 2: ~50% of students, opposite relation, not primary.
    IF MOD(v_i,2) = 0 THEN
      SET v_relation2 = IF(v_relation1 = 'Mother','Father','Mother');
      IF v_relation2 = 'Mother' THEN
        SELECT first_name INTO v_g2_first FROM name_pool_first_female WHERE id = MOD(v_i+17,30)+1;
      ELSE
        SELECT first_name INTO v_g2_first FROM name_pool_first_male WHERE id = MOD(v_i+17,30)+1;
      END IF;

      INSERT INTO guardians (student_id, name, relation, occupation, phone, email, address, is_primary)
      VALUES (
        v_student_id, CONCAT(v_g2_first, ' ', v_last), v_relation2,
        CASE MOD(v_i,6) WHEN 0 THEN 'Business' WHEN 1 THEN 'Government Service' WHEN 2 THEN 'Private Job'
                        WHEN 3 THEN 'Teacher' WHEN 4 THEN 'Doctor' ELSE 'Homemaker' END,
        CONCAT('+91-91', LPAD(v_i,8,'0')), CONCAT('guardian', v_i, '2@example.com'), v_address, 0
      );
    END IF;

    -- Medical details for ~half the students (odd i).
    IF MOD(v_i,2) = 1 THEN
      INSERT INTO student_medical_details (student_id, height_cm, weight_kg, allergies, medical_conditions, doctor_name, doctor_contact)
      VALUES (
        v_student_id, (70 + v_base_age*6), (10 + v_base_age*3),
        CASE MOD(v_i,5) WHEN 0 THEN 'Dust' WHEN 1 THEN 'Pollen' ELSE 'None' END,
        'None', 'Dr. Ashok Verma', '+91-9811122334'
      );
    END IF;

    SET v_i = v_i + 1;
  END WHILE;

  DROP TEMPORARY TABLE IF EXISTS tmp_sections;
END $$

-- ---------------------------------------------------------------------
-- sp_gen_attendance: 2000 rows = 40 school days (Sundays skipped,
-- starting 2026-06-01) x a rotating window of 50 distinct students.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_attendance $$
CREATE PROCEDURE sp_gen_attendance()
BEGIN
  DECLARE v_date DATE DEFAULT '2026-06-01';
  DECLARE v_day_count INT DEFAULT 0;
  DECLARE v_window_start INT;
  DECLARE v_k INT;
  DECLARE v_rn INT;
  DECLARE v_student_id BIGINT;
  DECLARE v_class_id BIGINT;
  DECLARE v_section_id BIGINT;
  DECLARE v_status_roll INT;
  DECLARE v_status VARCHAR(10);
  DECLARE v_admin_id BIGINT;

  SELECT id INTO v_admin_id FROM users WHERE username = 'admin';

  DROP TEMPORARY TABLE IF EXISTS tmp_students_ordered;
  CREATE TEMPORARY TABLE tmp_students_ordered (
    rn INT PRIMARY KEY,
    student_id BIGINT,
    class_id BIGINT,
    section_id BIGINT
  );

  INSERT INTO tmp_students_ordered (rn, student_id, class_id, section_id)
  SELECT ROW_NUMBER() OVER (ORDER BY id), id, class_id, section_id
  FROM students;

  WHILE v_day_count < 40 DO
    IF DAYOFWEEK(v_date) <> 1 THEN  -- skip Sundays (1 = Sunday)
      SET v_window_start = MOD(v_day_count * 50, 500) + 1;
      SET v_k = 0;
      WHILE v_k < 50 DO
        SET v_rn = MOD(v_window_start - 1 + v_k, 500) + 1;

        SELECT student_id, class_id, section_id
          INTO v_student_id, v_class_id, v_section_id
        FROM tmp_students_ordered WHERE rn = v_rn;

        SET v_status_roll = MOD(v_day_count * 7 + v_k * 13, 100);
        SET v_status = CASE
            WHEN v_status_roll < 90 THEN 'PRESENT'
            WHEN v_status_roll < 95 THEN 'ABSENT'
            WHEN v_status_roll < 97 THEN 'LATE'
            WHEN v_status_roll < 99 THEN 'HALF_DAY'
            ELSE 'LEAVE'
          END;

        INSERT INTO student_attendance (student_id, class_id, section_id, attendance_date, status, marked_by)
        VALUES (v_student_id, v_class_id, v_section_id, v_date, v_status, v_admin_id);

        SET v_k = v_k + 1;
      END WHILE;

      SET v_day_count = v_day_count + 1;
    END IF;

    SET v_date = DATE_ADD(v_date, INTERVAL 1 DAY);
  END WHILE;

  DROP TEMPORARY TABLE IF EXISTS tmp_students_ordered;
END $$

-- ---------------------------------------------------------------------
-- sp_gen_fees: 1000 student_fees rows (500 students x Tuition Fee +
-- Exam Fee), ~700 of which get a matching fee_payments row (45% PAID in
-- full, 25% PARTIAL at half the due amount, 30% left unpaid/overdue).
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_gen_fees $$
CREATE PROCEDURE sp_gen_fees()
BEGIN
  DECLARE v_rn INT DEFAULT 1;
  DECLARE v_cat_idx INT;
  DECLARE v_student_id BIGINT;
  DECLARE v_class_id BIGINT;
  DECLARE v_cat_name VARCHAR(50);
  DECLARE v_fs_id BIGINT;
  DECLARE v_amount DECIMAL(12,2);
  DECLARE v_due_date DATE;
  DECLARE v_sf_id BIGINT;
  DECLARE v_fee_seq INT;
  DECLARE v_scenario INT;
  DECLARE v_partial_amount DECIMAL(12,2);
  DECLARE v_receipt VARCHAR(50);
  DECLARE v_payment_mode VARCHAR(10);
  DECLARE v_payment_date DATE;
  DECLARE v_current_ay_id BIGINT;
  DECLARE v_accountant_id BIGINT;

  SELECT id INTO v_current_ay_id FROM academic_years WHERE is_current = 1;
  SELECT id INTO v_accountant_id FROM users WHERE username = 'accountant.demo';

  DROP TEMPORARY TABLE IF EXISTS tmp_students_for_fees;
  CREATE TEMPORARY TABLE tmp_students_for_fees (
    rn INT PRIMARY KEY,
    student_id BIGINT,
    class_id BIGINT
  );

  INSERT INTO tmp_students_for_fees (rn, student_id, class_id)
  SELECT ROW_NUMBER() OVER (ORDER BY id), id, class_id
  FROM students;

  WHILE v_rn <= 500 DO
    SELECT student_id, class_id INTO v_student_id, v_class_id
    FROM tmp_students_for_fees WHERE rn = v_rn;

    SET v_cat_idx = 1;
    WHILE v_cat_idx <= 2 DO
      SET v_cat_name = IF(v_cat_idx = 1, 'Tuition Fee', 'Exam Fee');
      SET v_fee_seq  = (v_rn - 1) * 2 + v_cat_idx;

      SELECT fs.id, fs.amount, fs.due_date
        INTO v_fs_id, v_amount, v_due_date
      FROM fee_structures fs
      JOIN fee_categories fc ON fc.id = fs.fee_category_id
      WHERE fs.class_id = v_class_id
        AND fs.academic_year_id = v_current_ay_id
        AND fc.name = v_cat_name;

      INSERT INTO student_fees (student_id, fee_structure_id, academic_year_id, amount_due, amount_paid, due_date, status)
      VALUES (v_student_id, v_fs_id, v_current_ay_id, v_amount, 0, v_due_date, 'UNPAID');
      SET v_sf_id = LAST_INSERT_ID();

      SET v_scenario     = MOD(v_fee_seq, 20);
      SET v_payment_date = DATE_ADD(v_due_date, INTERVAL MOD(v_fee_seq * 3, 60) DAY);
      SET v_payment_mode = ELT(MOD(v_fee_seq,4)+1,'CASH','ONLINE','CHEQUE','CARD');

      IF v_scenario <= 8 THEN
        -- 45%: paid in full.
        CALL sp_generate_fee_receipt_number(v_receipt);
        INSERT INTO fee_payments (student_fee_id, amount, payment_date, payment_mode, transaction_id, receipt_number, collected_by)
        VALUES (
          v_sf_id, v_amount, v_payment_date, v_payment_mode,
          IF(v_payment_mode = 'ONLINE', CONCAT('TXN', LPAD(v_fee_seq,8,'0')), NULL),
          v_receipt, v_accountant_id
        );
      ELSEIF v_scenario <= 13 THEN
        -- 25%: partial payment (half the due amount).
        SET v_partial_amount = ROUND(v_amount * 0.5, 2);
        CALL sp_generate_fee_receipt_number(v_receipt);
        INSERT INTO fee_payments (student_fee_id, amount, payment_date, payment_mode, transaction_id, receipt_number, collected_by)
        VALUES (
          v_sf_id, v_partial_amount, v_payment_date, v_payment_mode,
          IF(v_payment_mode = 'ONLINE', CONCAT('TXN', LPAD(v_fee_seq,8,'0')), NULL),
          v_receipt, v_accountant_id
        );
      ELSE
        -- 30%: no payment yet - OVERDUE if past due_date, else UNPAID.
        UPDATE student_fees
           SET status = IF(due_date < CURDATE(), 'OVERDUE', 'UNPAID')
         WHERE id = v_sf_id;
      END IF;

      SET v_cat_idx = v_cat_idx + 1;
    END WHILE;

    SET v_rn = v_rn + 1;
  END WHILE;

  DROP TEMPORARY TABLE IF EXISTS tmp_students_for_fees;
END $$

DELIMITER ;

-- =====================================================================
-- Run the generators, in dependency order.
-- =====================================================================
CALL sp_gen_teachers();
CALL sp_gen_staff();
CALL sp_gen_students();
CALL sp_gen_attendance();
CALL sp_gen_fees();

-- =====================================================================
-- Clean up: these are one-off ETL procedures, not part of the
-- application's permanent procedure set (05_procedures.sql), so they
-- are dropped once the data has been generated. The name_pool_* tables
-- are intentionally left in place (see header note).
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_gen_teachers;
DROP PROCEDURE IF EXISTS sp_gen_staff;
DROP PROCEDURE IF EXISTS sp_gen_students;
DROP PROCEDURE IF EXISTS sp_gen_attendance;
DROP PROCEDURE IF EXISTS sp_gen_fees;

-- =====================================================================
-- Verification queries (optional - run interactively to sanity check
-- row counts after loading this file).
-- =====================================================================
-- SELECT COUNT(*) FROM teachers;                 -- expect 51 (50 + 1 demo)
-- SELECT COUNT(*) FROM staff;                     -- expect 34 (30 + 4 demo)
-- SELECT COUNT(*) FROM students;                  -- expect 500
-- SELECT COUNT(*) FROM guardians;                 -- expect ~750
-- SELECT COUNT(*) FROM student_medical_details;   -- expect ~250
-- SELECT COUNT(*) FROM student_attendance;        -- expect 2000
-- SELECT COUNT(*) FROM student_fees;              -- expect 1000
-- SELECT COUNT(*) FROM fee_payments;               -- expect ~700
-- SELECT COUNT(*) FROM class_subject_teacher;      -- expect ~151
-- SELECT COUNT(*) FROM sections WHERE class_teacher_id IS NOT NULL; -- expect ~43
