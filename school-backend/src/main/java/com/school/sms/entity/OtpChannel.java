package com.school.sms.entity;

/**
 * How a one-time passcode was delivered.
 *
 * Deliberately narrower than {@link NotificationType}: a passcode can only go
 * somewhere the account already proves it owns. Push and in-app are useless for
 * account recovery, since both require being signed in already.
 */
public enum OtpChannel {
    EMAIL,
    SMS
}
