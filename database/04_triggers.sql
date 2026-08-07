-- =====================================================================
-- 04_triggers.sql
-- Business-rule triggers. Run after 01_schema.sql (and ideally after
-- 02/03, order does not matter for these since they don't touch views).
-- =====================================================================

USE school_management_system;

DELIMITER $$

-- ---------------------------------------------------------------------
-- Library: block issuing a book that has no available copies left.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_book_issues_before_insert $$
CREATE TRIGGER trg_book_issues_before_insert
BEFORE INSERT ON book_issues
FOR EACH ROW
BEGIN
  DECLARE v_available INT;
  SELECT available_copies INTO v_available FROM books WHERE id = NEW.book_id;
  IF v_available IS NULL OR v_available <= 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cannot issue book: no available copies left.';
  END IF;
END $$

-- ---------------------------------------------------------------------
-- Library: issuing a book takes one copy out of circulation.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_book_issues_after_insert $$
CREATE TRIGGER trg_book_issues_after_insert
AFTER INSERT ON book_issues
FOR EACH ROW
BEGIN
  UPDATE books
     SET available_copies = available_copies - 1
   WHERE id = NEW.book_id;
END $$

-- ---------------------------------------------------------------------
-- Library: setting return_date (transition NULL -> NOT NULL) returns
-- the copy to circulation. Guarded by the OLD/NEW comparison so this
-- only fires once per issue, not on every subsequent update of the row
-- (e.g. a later fine adjustment update won't double-increment).
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_book_issues_after_update $$
CREATE TRIGGER trg_book_issues_after_update
AFTER UPDATE ON book_issues
FOR EACH ROW
BEGIN
  IF OLD.return_date IS NULL AND NEW.return_date IS NOT NULL THEN
    UPDATE books
       SET available_copies = available_copies + 1
     WHERE id = NEW.book_id;
  END IF;
END $$

-- ---------------------------------------------------------------------
-- Fees: every payment posted against a student_fees row rolls forward
-- amount_paid and recomputes status. Two UPDATE statements are used
-- (rather than one referencing the freshly-written value) purely for
-- readability; both target student_fees, never fee_payments, so there
-- is no risk of re-triggering this same AFTER INSERT trigger.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_fee_payments_after_insert $$
CREATE TRIGGER trg_fee_payments_after_insert
AFTER INSERT ON fee_payments
FOR EACH ROW
BEGIN
  UPDATE student_fees
     SET amount_paid = amount_paid + NEW.amount
   WHERE id = NEW.student_fee_id;

  UPDATE student_fees
     SET status = CASE
                     WHEN amount_paid >= amount_due THEN 'PAID'
                     WHEN amount_paid > 0            THEN 'PARTIAL'
                     WHEN due_date IS NOT NULL AND due_date < CURDATE() THEN 'OVERDUE'
                     ELSE 'UNPAID'
                   END
   WHERE id = NEW.student_fee_id;
END $$

-- ---------------------------------------------------------------------
-- Hostel: allocating a student to a room (as ACTIVE) increments the
-- room's occupied_count.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_hostel_students_after_insert $$
CREATE TRIGGER trg_hostel_students_after_insert
AFTER INSERT ON hostel_students
FOR EACH ROW
BEGIN
  IF NEW.status = 'ACTIVE' THEN
    UPDATE hostel_rooms
       SET occupied_count = occupied_count + 1
     WHERE id = NEW.room_id;
  END IF;
END $$

-- ---------------------------------------------------------------------
-- Hostel: keep occupied_count correct across status changes (vacating,
-- re-allocating) and room transfers while still ACTIVE.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_hostel_students_after_update $$
CREATE TRIGGER trg_hostel_students_after_update
AFTER UPDATE ON hostel_students
FOR EACH ROW
BEGIN
  IF OLD.status = 'ACTIVE' AND NEW.status = 'VACATED' THEN
    UPDATE hostel_rooms SET occupied_count = occupied_count - 1 WHERE id = OLD.room_id;
  ELSEIF OLD.status = 'VACATED' AND NEW.status = 'ACTIVE' THEN
    UPDATE hostel_rooms SET occupied_count = occupied_count + 1 WHERE id = NEW.room_id;
  ELSEIF NEW.status = 'ACTIVE' AND OLD.status = 'ACTIVE' AND OLD.room_id <> NEW.room_id THEN
    UPDATE hostel_rooms SET occupied_count = occupied_count - 1 WHERE id = OLD.room_id;
    UPDATE hostel_rooms SET occupied_count = occupied_count + 1 WHERE id = NEW.room_id;
  END IF;
END $$

DELIMITER ;
