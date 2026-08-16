-- =====================================================================
-- 12_otp_codes.sql — one-time passcodes for password reset (and, later,
-- login or phone verification).
--
-- Safe to re-run: guarded with IF NOT EXISTS, and creates no rows.
--
-- Why a separate table rather than reusing password_reset_tokens:
--   * a reset token is a long random string mailed as a link; an OTP is six
--     digits a human retypes, so it needs an attempt counter and a much
--     shorter life, neither of which that table has,
--   * and an OTP is bound to a purpose, so a code minted for a password
--     reset can never be replayed against a login.
--
-- code_hash, not code: six digits is only a million possibilities, so the
-- column is a bcrypt hash exactly like users.password. Nothing anywhere
-- stores or logs the digits themselves.
-- =====================================================================

CREATE TABLE IF NOT EXISTS otp_codes (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT       NOT NULL,
  code_hash     VARCHAR(255) NOT NULL,
  purpose       ENUM('PASSWORD_RESET','LOGIN','PHONE_VERIFY') NOT NULL,
  channel       ENUM('EMAIL','SMS') NOT NULL,
  -- The address the code was actually sent to, kept for auditing and so a
  -- verify can confirm it is answering the same destination it was sent to.
  destination   VARCHAR(150) NOT NULL,
  expires_at    DATETIME     NOT NULL,
  consumed_at   DATETIME     NULL,
  attempts      INT          NOT NULL DEFAULT 0,
  created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  -- Verification looks up the newest live code for a user and purpose.
  KEY idx_otp_user_purpose (user_id, purpose, consumed_at),
  -- Send throttling counts recent rows per destination.
  KEY idx_otp_destination_created (destination, created_at),
  CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
