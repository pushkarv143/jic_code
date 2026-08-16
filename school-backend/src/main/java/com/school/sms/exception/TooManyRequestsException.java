package com.school.sms.exception;

/**
 * Thrown when a caller has asked for something too often — currently only
 * one-time passcodes.
 *
 * Deliberately its own type rather than a BadRequestException: 429 is the status
 * a client can act on, and the Android app already treats it as "wait and retry"
 * rather than "the input was wrong".
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
