package com.school.sms.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The formats here are the ones actually in the users table — the seed data alone
 * holds both {@code +91-9999900001} and {@code 9999900004} — plus the ways someone
 * types a number into a form. All of them have to arrive at the same account.
 */
class PhoneNumbersTest {

    @Test
    void every_way_of_writing_one_number_reduces_to_the_same_national_digits() {
        assertThat(PhoneNumbers.national("+91-9999900001")).isEqualTo("9999900001");
        assertThat(PhoneNumbers.national("9999900001")).isEqualTo("9999900001");
        assertThat(PhoneNumbers.national("+91 99999 00001")).isEqualTo("9999900001");
        assertThat(PhoneNumbers.national("(+91) 99999-00001")).isEqualTo("9999900001");
        assertThat(PhoneNumbers.national("09999900001")).isEqualTo("9999900001");
    }

    @Test
    void a_number_already_carrying_a_country_code_is_not_given_a_second_one() {
        assertThat(PhoneNumbers.toE164("+91-9999900001", "+91")).isEqualTo("+919999900001");
        assertThat(PhoneNumbers.toE164("919999900001", "+91")).isEqualTo("+919999900001");
    }

    @Test
    void a_bare_national_number_gets_the_default_country_code() {
        assertThat(PhoneNumbers.toE164("9999900001", "+91")).isEqualTo("+919999900001");
        // The default is accepted with or without its plus.
        assertThat(PhoneNumbers.toE164("9999900001", "91")).isEqualTo("+919999900001");
    }

    @Test
    void something_too_short_to_be_a_number_is_left_to_fail_rather_than_padded() {
        assertThat(PhoneNumbers.isPlausible("12345")).isFalse();
        assertThat(PhoneNumbers.isPlausible("9999900001")).isTrue();
        // Returned as-is, so it matches nothing instead of matching the wrong row.
        assertThat(PhoneNumbers.national("12345")).isEqualTo("12345");
    }

    @Test
    void empty_and_null_are_handled_rather_than_thrown_on() {
        assertThat(PhoneNumbers.digitsOnly(null)).isEmpty();
        assertThat(PhoneNumbers.toE164("", "+91")).isEmpty();
        assertThat(PhoneNumbers.isPlausible("")).isFalse();
    }
}
