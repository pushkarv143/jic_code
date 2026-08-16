package com.school.sms.entity;

/**
 * What a one-time passcode may be used for.
 *
 * A code is bound to exactly one of these when it is issued and is only
 * accepted for that purpose, so a code obtained through a password-reset
 * request can never be replayed to sign in.
 */
public enum OtpPurpose {

    /** Prove ownership of the account, then choose a new password. */
    PASSWORD_RESET,

    /** Sign in with the code itself. Not wired to an endpoint yet. */
    LOGIN,

    /** Confirm a phone number belongs to the account holder. Not wired up yet. */
    PHONE_VERIFY
}
