-- Migration 24: WhatsApp notification opt-out
-- Adds a per-user flag to disable the daily WhatsApp timetable reminder.
-- Default is NULL (treated as enabled) so existing users receive reminders
-- without needing any data migration.
-- Set to 0 (false) to opt out.

-- ---------------------------------------------------------------------
-- 1. The flag
--
-- Guarded through information_schema: MySQL has no ADD COLUMN IF NOT
-- EXISTS (that is MariaDB), and a bare ALTER would fail the whole file on
-- a second run.
-- ---------------------------------------------------------------------
SET @has_column := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'whatsapp_notifications_enabled'
);

SET @ddl := IF(@has_column = 0,
    'ALTER TABLE users ADD COLUMN whatsapp_notifications_enabled TINYINT(1) DEFAULT NULL
        COMMENT ''1 = opted in, 0 = opted out, NULL = default (enabled)''',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. Verification
-- ---------------------------------------------------------------------

-- The column must exist. Expect 1.
SELECT COUNT(*) AS whatsapp_notifications_enabled_column
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'users'
  AND column_name = 'whatsapp_notifications_enabled';

-- Nobody is opted out on the day this runs: every existing row is NULL,
-- which the scheduler reads as enabled. Expect 0.
SELECT COUNT(*) AS users_opted_out
FROM users
WHERE whatsapp_notifications_enabled = 0;
