-- =====================================================================
-- 03_views.sql
-- Reporting views built on top of 01_schema.sql. Run after 01 & 02.
-- =====================================================================

USE school_management_system;

-- ---------------------------------------------------------------------
-- vw_student_summary
-- One row per (non-deleted) student with class/section and the single
-- primary guardian's contact details (MIN(id) subquery guarantees at
-- most one guardian row joins in, even if data ever has >1 is_primary
-- row for the same student).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_student_summary AS
SELECT
  st.id                    AS student_id,
  st.admission_number,
  u.first_name             AS student_first_name,
  u.last_name              AS student_last_name,
  st.roll_number,
  c.class_name,
  sec.section_name,
  st.gender,
  st.date_of_birth,
  st.status                AS student_status,
  g.name                   AS primary_guardian_name,
  g.relation               AS primary_guardian_relation,
  g.phone                  AS primary_guardian_phone,
  g.email                  AS primary_guardian_email
FROM students st
LEFT JOIN users u   ON u.id = st.user_id
JOIN classes c       ON c.id = st.class_id
JOIN sections sec    ON sec.id = st.section_id
LEFT JOIN guardians g
  ON g.id = (
       SELECT MIN(g2.id) FROM guardians g2
       WHERE g2.student_id = st.id AND g2.is_primary = 1
     )
WHERE st.is_deleted = 0;

-- ---------------------------------------------------------------------
-- vw_fee_collection_summary
-- Total fee collected per class, per calendar month.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_fee_collection_summary AS
SELECT
  c.id                    AS class_id,
  c.class_name,
  YEAR(fp.payment_date)   AS payment_year,
  MONTH(fp.payment_date)  AS payment_month,
  COUNT(fp.id)            AS total_payments,
  SUM(fp.amount)          AS total_collected
FROM fee_payments fp
JOIN student_fees sf ON sf.id = fp.student_fee_id
JOIN students st     ON st.id = sf.student_id
JOIN classes c       ON c.id = st.class_id
GROUP BY c.id, c.class_name, YEAR(fp.payment_date), MONTH(fp.payment_date);

-- ---------------------------------------------------------------------
-- vw_attendance_summary
-- Per-student attendance percentage across all marked days on record.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_attendance_summary AS
SELECT
  st.id                     AS student_id,
  st.admission_number,
  u.first_name              AS student_first_name,
  u.last_name               AS student_last_name,
  COUNT(*)                  AS total_marked_days,
  SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 ELSE 0 END)  AS days_present,
  SUM(CASE WHEN sa.status = 'ABSENT'  THEN 1 ELSE 0 END)  AS days_absent,
  SUM(CASE WHEN sa.status = 'LATE'    THEN 1 ELSE 0 END)  AS days_late,
  SUM(CASE WHEN sa.status = 'LEAVE'   THEN 1 ELSE 0 END)  AS days_on_leave,
  ROUND(
    SUM(CASE WHEN sa.status IN ('PRESENT','LATE','HALF_DAY') THEN 1 ELSE 0 END) * 100.0
    / COUNT(*), 2
  )                          AS attendance_percentage
FROM students st
LEFT JOIN users u          ON u.id = st.user_id
JOIN student_attendance sa ON sa.student_id = st.id
GROUP BY st.id, st.admission_number, u.first_name, u.last_name;

-- ---------------------------------------------------------------------
-- vw_teacher_workload
-- How many distinct subjects/sections each teacher is assigned to, and
-- how many sections they are the class_teacher of.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_teacher_workload AS
SELECT
  t.id                          AS teacher_id,
  u.first_name                  AS teacher_first_name,
  u.last_name                   AS teacher_last_name,
  t.employee_id,
  d.name                        AS department_name,
  COUNT(DISTINCT cst.subject_id) AS subjects_count,
  COUNT(DISTINCT cst.section_id) AS sections_count,
  COUNT(DISTINCT sec2.id)        AS class_teacher_of_sections
FROM teachers t
JOIN users u                        ON u.id = t.user_id
JOIN departments d                  ON d.id = t.department_id
LEFT JOIN class_subject_teacher cst ON cst.teacher_id = t.id
LEFT JOIN sections sec2             ON sec2.class_teacher_id = t.id
WHERE t.is_deleted = 0
GROUP BY t.id, u.first_name, u.last_name, t.employee_id, d.name;

-- ---------------------------------------------------------------------
-- vw_library_overdue
-- Currently overdue book issues (not yet returned, past due_date) with
-- computed days overdue and the recorded fine.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_library_overdue AS
SELECT
  bi.id                   AS issue_id,
  b.title,
  b.author,
  bi.student_id,
  su.first_name           AS student_first_name,
  su.last_name            AS student_last_name,
  bi.teacher_id,
  tu.first_name           AS teacher_first_name,
  tu.last_name            AS teacher_last_name,
  bi.issue_date,
  bi.due_date,
  DATEDIFF(CURDATE(), bi.due_date) AS days_overdue,
  bi.fine_amount,
  bi.status
FROM book_issues bi
JOIN books b            ON b.id = bi.book_id
LEFT JOIN students st   ON st.id = bi.student_id
LEFT JOIN users su      ON su.id = st.user_id
LEFT JOIN teachers t    ON t.id = bi.teacher_id
LEFT JOIN users tu      ON tu.id = t.user_id
WHERE bi.return_date IS NULL
  AND bi.due_date < CURDATE();

-- ---------------------------------------------------------------------
-- vw_class_section_strength (bonus)
-- Head-count per class/section, handy for admission planning.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_class_section_strength AS
SELECT
  c.id            AS class_id,
  c.class_name,
  sec.id          AS section_id,
  sec.section_name,
  sec.capacity,
  COUNT(st.id)    AS current_strength
FROM sections sec
JOIN classes c ON c.id = sec.class_id
LEFT JOIN students st
  ON st.section_id = sec.id AND st.is_deleted = 0 AND st.status = 'ACTIVE'
GROUP BY c.id, c.class_name, sec.id, sec.section_name, sec.capacity;

-- ---------------------------------------------------------------------
-- vw_student_fee_due (bonus)
-- Outstanding balance per student per fee structure, for collections.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_student_fee_due AS
SELECT
  sf.id            AS student_fee_id,
  st.id            AS student_id,
  st.admission_number,
  c.class_name,
  sec.section_name,
  fc.name          AS fee_category,
  sf.amount_due,
  sf.amount_paid,
  (sf.amount_due - sf.amount_paid) AS balance_due,
  sf.due_date,
  sf.status
FROM student_fees sf
JOIN students st            ON st.id = sf.student_id
JOIN classes c               ON c.id = st.class_id
JOIN sections sec             ON sec.id = st.section_id
JOIN fee_structures fs        ON fs.id = sf.fee_structure_id
JOIN fee_categories fc        ON fc.id = fs.fee_category_id
WHERE sf.status <> 'PAID';
