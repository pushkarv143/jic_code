-- Migration 24: WhatsApp notification opt-out
-- Adds a per-user flag to disable the daily WhatsApp timetable reminder.
-- Default is NULL (treated as enabled) so existing users receive reminders
-- without needing any data migration.
-- Set to 0 (false) to opt out.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS whatsapp_notifications_enabled TINYINT(1) DEFAULT NULL
        COMMENT '1 = opted in, 0 = opted out, NULL = default (enabled)';
