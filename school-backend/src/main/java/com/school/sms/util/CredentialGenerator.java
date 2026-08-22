package com.school.sms.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.List;
import java.util.function.Predicate;

/**
 * Usernames and first-time passwords for accounts the school creates.
 *
 * <p>Self-registration is gone, so nobody types either of these any more: admitting
 * a student generates both and emails them. That makes the shape of what is
 * generated a real decision rather than a detail — a username a thirteen-year-old
 * cannot read back over the phone is a support call, and a password they cannot
 * distinguish an l from a 1 in is two.
 */
@Component
@RequiredArgsConstructor
public class CredentialGenerator {

    /**
     * Deliberately excludes the characters that look like each other in the fonts
     * an email client picks: no O/0, no I/l/1, no S/5, no Z/2. A generated password
     * is read off a screen and typed on a phone keyboard exactly once, and the
     * whole point is that it works the first time.
     */
    private static final String UPPER = "ABCDEFGHJKMNPQRTUVWXY";
    private static final String LOWER = "abcdefghjkmnpqrtuvwxy";
    private static final String DIGITS = "34679";
    /** No quotes, backslashes or angle brackets: those get mangled by something. */
    private static final String SYMBOLS = "!@#$%^&*-_=+";

    private static final int PASSWORD_LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    /**
     * "Pushkar Verma" → {@code pushkarv}.
     *
     * <p>First name plus the first letter of the last name, lowercased, accents
     * folded, everything that is not a letter or digit removed. A collision appends
     * a counter — {@code pushkarv1}, {@code pushkarv2} — and keeps counting past 9
     * rather than switching to some other pattern at an arbitrary point: a school
     * with ten Pushkar V's gets {@code pushkarv10}, which is still the same rule
     * and still readable.
     *
     * <p>Falls back to the admission number when the name yields nothing usable —
     * a record with only punctuation for a name, or a mononym in a script the fold
     * cannot reduce to ASCII. That is rare and it must not produce an empty
     * username, because an empty one cannot be logged in with and cannot be
     * corrected without an administrator noticing it first.
     *
     * @param taken answers whether a candidate is already in use; asked once per
     *              candidate, so an implementation should hit an index
     */
    public String usernameFor(String firstName, String lastName, String admissionNumber,
                             Predicate<String> taken) {
        String base = sanitise(firstName) + initial(lastName);
        if (base.isEmpty()) {
            base = sanitise(admissionNumber);
        }
        if (base.isEmpty()) {
            // Nothing in the record can produce a username. Better a random one the
            // administrator can see and correct than a blank that fails silently.
            base = "student" + (1000 + random.nextInt(9000));
        }

        if (!taken.test(base)) {
            return base;
        }
        // Unbounded on purpose. A cap would need a second pattern behind it, and
        // two patterns is two things to explain when somebody asks why their
        // username does not look like everyone else's.
        for (int suffix = 1; ; suffix++) {
            String candidate = base + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * A temporary password that satisfies the policy the change-password endpoint
     * enforces: at least 8 characters with an upper, a lower, a digit and a symbol.
     *
     * <p>One of each is placed first so the policy cannot be missed by chance, the
     * rest is filled from the whole alphabet, and the result is shuffled so the
     * guaranteed characters are not always in the same positions — a password whose
     * first character is reliably uppercase has given away a character.
     */
    public String temporaryPassword() {
        List<Character> chars = new java.util.ArrayList<>(PASSWORD_LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SYMBOLS));

        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        while (chars.size() < PASSWORD_LENGTH) {
            chars.add(pick(all));
        }

        // Fisher-Yates over a SecureRandom. Collections.shuffle(list) without a
        // source would use a shared java.util.Random, which is not what a password
        // should come out of.
        for (int i = chars.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Character swap = chars.get(i);
            chars.set(i, chars.get(j));
            chars.set(j, swap);
        }

        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        chars.forEach(password::append);
        return password.toString();
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    /** Lowercased, accents folded to ASCII, anything else dropped. */
    private String sanitise(String raw) {
        if (raw == null) {
            return "";
        }
        String folded = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return folded.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String initial(String lastName) {
        String cleaned = sanitise(lastName);
        return cleaned.isEmpty() ? "" : cleaned.substring(0, 1);
    }
}
