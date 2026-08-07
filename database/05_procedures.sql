-- =====================================================================
-- 05_procedures.sql
-- Stored procedures. Run after 01_schema.sql.
--
-- NOTE on parameter naming: MySQL resolves an unqualified identifier
-- that is spelled the same as both a routine parameter and a table
-- column in favor of the column, which silently breaks WHERE/SET
-- clauses that meant the parameter. To avoid that footgun, every IN/OUT
-- parameter below is prefixed with p_ (e.g. p_student_id instead of
-- student_id). Order and data types match the contract's requested
-- signatures exactly; only the cosmetic names differ.
-- =====================================================================

USE school_management_system;

-- Helper table backing sp_generate_fee_receipt_number(): a dedicated
-- auto_increment sequence gives us an atomic, gap-tolerant counter
-- without racing against concurrent fee_payments inserts.
CREATE TABLE IF NOT EXISTS receipt_number_sequence (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$

-- ---------------------------------------------------------------------
-- sp_calculate_student_attendance_percentage
-- Percentage of marked days in [p_start_date, p_end_date] on which the
-- student was PRESENT, LATE or HALF_DAY (all three count as "in
-- attendance" for the percentage; ABSENT/LEAVE do not). Returns 0.00
-- when there are no marked days in range, rather than dividing by zero.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_calculate_student_attendance_percentage $$
CREATE PROCEDURE sp_calculate_student_attendance_percentage(
  IN  p_student_id  BIGINT,
  IN  p_start_date  DATE,
  IN  p_end_date    DATE,
  OUT p_percentage  DECIMAL(5,2)
)
BEGIN
  DECLARE v_total   INT DEFAULT 0;
  DECLARE v_present INT DEFAULT 0;

  SELECT COUNT(*) INTO v_total
  FROM student_attendance
  WHERE student_id = p_student_id
    AND attendance_date BETWEEN p_start_date AND p_end_date;

  SELECT COUNT(*) INTO v_present
  FROM student_attendance
  WHERE student_id = p_student_id
    AND attendance_date BETWEEN p_start_date AND p_end_date
    AND status IN ('PRESENT','LATE','HALF_DAY');

  IF v_total = 0 THEN
    SET p_percentage = 0.00;
  ELSE
    SET p_percentage = ROUND((v_present * 100.0) / v_total, 2);
  END IF;
END $$

-- ---------------------------------------------------------------------
-- sp_generate_fee_receipt_number
-- Produces a unique, human-readable receipt number of the form
-- RCPT-YYYYMMDD-NNNNNN using the receipt_number_sequence auto_increment
-- counter, e.g. RCPT-20260806-000123. Implemented as a procedure with
-- an OUT parameter (rather than a FUNCTION) so it does not require
-- log_bin_trust_function_creators to be enabled on the server - a
-- FUNCTION that both writes data and is used inside INSERT statements
-- would need that flag; a PROCEDURE does not.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_generate_fee_receipt_number $$
CREATE PROCEDURE sp_generate_fee_receipt_number(
  OUT p_receipt_number VARCHAR(50)
)
BEGIN
  DECLARE v_seq BIGINT;

  INSERT INTO receipt_number_sequence () VALUES ();
  SET v_seq = LAST_INSERT_ID();

  SET p_receipt_number = CONCAT('RCPT-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-', LPAD(v_seq, 6, '0'));
END $$

-- ---------------------------------------------------------------------
-- sp_promote_students
-- Bulk-promotes every ACTIVE, non-deleted student currently in
-- p_from_class_id into p_to_class_id under p_academic_year_id.
-- Section mapping: keeps the same section_name (e.g. 'A' -> 'A') in
-- the destination class when one exists there; otherwise falls back to
-- the lowest-id section of the destination class. roll_number is reset
-- to NULL so the receiving class teacher re-numbers the new roster.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_promote_students $$
CREATE PROCEDURE sp_promote_students(
  IN p_from_class_id     BIGINT,
  IN p_to_class_id       BIGINT,
  IN p_academic_year_id  BIGINT
)
BEGIN
  DECLARE v_done                   INT DEFAULT 0;
  DECLARE v_student_id             BIGINT;
  DECLARE v_current_section_name   VARCHAR(20);
  DECLARE v_target_section_id      BIGINT;
  DECLARE v_fallback_section_id    BIGINT;

  DECLARE cur_students CURSOR FOR
    SELECT s.id, sec.section_name
    FROM students s
    JOIN sections sec ON sec.id = s.section_id
    WHERE s.class_id = p_from_class_id
      AND s.is_deleted = 0
      AND s.status = 'ACTIVE';

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

  SELECT MIN(id) INTO v_fallback_section_id
  FROM sections
  WHERE class_id = p_to_class_id;

  OPEN cur_students;

  promote_loop: LOOP
    FETCH cur_students INTO v_student_id, v_current_section_name;
    IF v_done = 1 THEN
      LEAVE promote_loop;
    END IF;

    SET v_target_section_id = NULL;

    SELECT MIN(id) INTO v_target_section_id
    FROM sections
    WHERE class_id = p_to_class_id
      AND section_name = v_current_section_name;

    IF v_target_section_id IS NULL THEN
      SET v_target_section_id = v_fallback_section_id;
    END IF;

    UPDATE students
       SET class_id         = p_to_class_id,
           section_id       = v_target_section_id,
           academic_year_id = p_academic_year_id,
           roll_number      = NULL
     WHERE id = v_student_id;

  END LOOP;

  CLOSE cur_students;
END $$

DELIMITER ;
