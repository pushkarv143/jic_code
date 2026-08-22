package com.school.sms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usernames and first-time passwords, now that nobody types either.
 *
 * <p>Self-registration is gone: admitting a student generates both and emails them.
 * That makes the shape of what comes out a behaviour worth pinning — a username the
 * student cannot read back over the phone, or a password with an l they read as a 1,
 * is a support call rather than a login.
 */
class CredentialGeneratorTest {

    private final CredentialGenerator generator = new CredentialGenerator();

    /** Nothing is taken. */
    private static final java.util.function.Predicate<String> FREE = name -> false;

    @Test
    @DisplayName("first name plus the initial of the last name, lowercased")
    void thePattern() {
        assertThat(generator.usernameFor("Pushkar", "Verma", "ADM-1", FREE)).isEqualTo("pushkarv");
        assertThat(generator.usernameFor("Aarav", "Sharma", "ADM-2", FREE)).isEqualTo("aaravs");
    }

    @Test
    @DisplayName("spaces, punctuation and accents are folded out")
    void awkwardNames() {
        // A name is typed by a human into an admin form; it arrives however it arrives.
        assertThat(generator.usernameFor("  Mary Jane ", "O'Brien", "ADM-3", FREE))
                .isEqualTo("maryjaneo");
        assertThat(generator.usernameFor("José", "Ñuñez", "ADM-4", FREE)).isEqualTo("josen");
        assertThat(generator.usernameFor("Anne-Marie", "de la Cruz", "ADM-5", FREE))
                .isEqualTo("annemaried");
    }

    @Test
    @DisplayName("a collision appends a counter, and keeps counting past nine")
    void collisions() {
        Set<String> taken = new HashSet<>(Set.of("pushkarv"));
        assertThat(generator.usernameFor("Pushkar", "Verma", "ADM-1", taken::contains))
                .isEqualTo("pushkarv1");

        // The interesting case: a cap would need a second pattern behind it, and two
        // patterns is two things to explain when a student asks why their username
        // does not look like their classmates'.
        for (int i = 1; i <= 9; i++) {
            taken.add("pushkarv" + i);
        }
        assertThat(generator.usernameFor("Pushkar", "Verma", "ADM-1", taken::contains))
                .isEqualTo("pushkarv10");

        taken.add("pushkarv10");
        assertThat(generator.usernameFor("Pushkar", "Verma", "ADM-1", taken::contains))
                .isEqualTo("pushkarv11");
    }

    @Test
    @DisplayName("a name with no usable letters falls back to the admission number")
    void unusableName() {
        // Rather than an empty username, which cannot be logged in with and cannot be
        // corrected until somebody notices it exists.
        assertThat(generator.usernameFor("...", "!!!", "ADM-2026-0042", FREE))
                .isEqualTo("adm20260042");
        assertThat(generator.usernameFor(null, null, "ADM-7", FREE)).isEqualTo("adm7");
    }

    @Test
    @DisplayName("with nothing usable at all, still never blank")
    void nothingUsable() {
        String username = generator.usernameFor(null, null, null, FREE);
        assertThat(username).isNotBlank().startsWith("student");
    }

    @Test
    @DisplayName("a single-name student gets just the first name")
    void mononym() {
        assertThat(generator.usernameFor("Ravi", null, "ADM-8", FREE)).isEqualTo("ravi");
        assertThat(generator.usernameFor("Ravi", "", "ADM-8", FREE)).isEqualTo("ravi");
    }

    /* ---- passwords ---------------------------------------------------- */

    /** The policy ChangePasswordRequest enforces on the replacement. */
    private static final Pattern POLICY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)"
                    + "(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");

    @Test
    @DisplayName("every generated password satisfies the policy the reset will demand")
    void passwordsMeetThePolicy() {
        // Run enough times to catch a generator that only usually includes a symbol:
        // one that fails a hundredth of the time fails for one student in a hundred,
        // and they cannot sign in at all.
        for (int i = 0; i < 500; i++) {
            String password = generator.temporaryPassword();
            assertThat(password).matches(POLICY);
        }
    }

    @Test
    @DisplayName("no characters that are read wrongly off a screen")
    void noAmbiguousCharacters() {
        // The password is read from an email and typed on a phone keyboard exactly
        // once. O/0, I/l/1, S/5 and Z/2 are where that goes wrong.
        for (int i = 0; i < 200; i++) {
            assertThat(generator.temporaryPassword())
                    .doesNotContain("O").doesNotContain("0")
                    .doesNotContain("I").doesNotContain("l").doesNotContain("1")
                    .doesNotContain("S").doesNotContain("5")
                    .doesNotContain("Z").doesNotContain("2");
        }
    }

    @Test
    @DisplayName("passwords do not repeat, and the guaranteed characters are not in fixed positions")
    void passwordsAreNotPredictable() {
        Set<String> seen = new HashSet<>();
        Set<Character> firstCharacters = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String password = generator.temporaryPassword();
            seen.add(password);
            firstCharacters.add(password.charAt(0));
        }
        assertThat(seen).hasSize(200);
        // Without the shuffle the first character would always be uppercase, which
        // gives away a character of every password the school ever issues.
        assertThat(firstCharacters).hasSizeGreaterThan(4);
    }
}
