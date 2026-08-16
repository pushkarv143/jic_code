package com.school.sms.util;

/**
 * Turning what people type into something you can match on and dial.
 *
 * The stored data is not in one shape — the seed alone holds
 * {@code +91-9999900001} and {@code 9999900004} — and nobody enters their number
 * the way it happens to sit in the column. Everything here exists to make those
 * agree.
 */
public final class PhoneNumbers {

    /** Indian mobile numbers are ten digits; the national number is what identifies them. */
    private static final int NATIONAL_LENGTH = 10;

    private PhoneNumbers() {
    }

    /** Strips everything that is not a digit: {@code +91-98100 11122} becomes {@code 919810011122}. */
    public static String digitsOnly(String raw) {
        return raw == null ? "" : raw.replaceAll("[^0-9]", "");
    }

    /**
     * The last ten digits — the part that stays the same whether the number was
     * written with a country code, a leading zero, or neither. This is what
     * lookups compare, so {@code 9810011122} finds {@code +91-9810011122}.
     *
     * Shorter input is returned as-is rather than padded, so a too-short number
     * simply fails to match instead of matching something it should not.
     */
    public static String national(String raw) {
        String digits = digitsOnly(raw);
        return digits.length() <= NATIONAL_LENGTH
                ? digits
                : digits.substring(digits.length() - NATIONAL_LENGTH);
    }

    /**
     * E.164, which is the only format an SMS gateway will accept: {@code +919810011122}.
     *
     * @param defaultCountryCode dialling code with or without its plus, applied
     *                           only when the number does not already carry one
     */
    public static String toE164(String raw, String defaultCountryCode) {
        String digits = digitsOnly(raw);
        if (digits.isEmpty()) {
            return "";
        }
        // Already longer than a national number, so it carries a country code.
        if (digits.length() > NATIONAL_LENGTH) {
            return "+" + digits;
        }
        return "+" + digitsOnly(defaultCountryCode) + digits;
    }

    /** True when there are enough digits to be a real number rather than a typo. */
    public static boolean isPlausible(String raw) {
        return digitsOnly(raw).length() >= NATIONAL_LENGTH;
    }
}
