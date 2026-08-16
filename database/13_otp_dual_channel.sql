-- =====================================================================
-- 13_otp_dual_channel.sql — a passcode may now go to both the email address
-- and the phone number on an account, rather than to whichever one the person
-- happened to type.
--
-- Safe to re-run: MODIFY COLUMN to the same definition is a no-op.
--
-- Why: someone who signs in with their phone number should not have to
-- remember which contact detail the code was sent to. Sending to every
-- contact point on the account means the code arrives wherever they are
-- looking, and the request no longer has to be refused when one channel is
-- unavailable — the other still carries it.
-- =====================================================================

ALTER TABLE otp_codes
  MODIFY COLUMN channel ENUM('EMAIL','SMS','BOTH') NOT NULL;
