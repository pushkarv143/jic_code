package com.school.sms.security;

import com.school.sms.util.AppConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the rule "the administrator can do everything" across every module.
 *
 * <p>The other authorization tests each cover one controller. This one reads the
 * controller sources and asserts the property holds everywhere — including in
 * modules written after it. A new module that forgets SUPER_ADMIN fails the build
 * instead of shipping a feature its administrator cannot use, which is the failure
 * mode nobody notices until someone complains a button does nothing.
 *
 * <p>Two shapes of gate are accepted, because both are legitimate:
 * <ul>
 *   <li>role-gated — the expression names SUPER_ADMIN directly;</li>
 *   <li>permission-gated — the expression ORs in {@link AppConstants#ADMIN_OVERRIDE},
 *       so a missing role_permissions row cannot lock the administrator out.</li>
 * </ul>
 *
 * <p>Self-service endpoints are exempt: {@code /students/me} resolves the caller's
 * own student record, and an administrator does not have one. Excluding them is a
 * statement about identity, not about privilege.
 */
class AdminAuthorizationAuditTest {

    private static final Path CONTROLLER_DIR =
            Paths.get("src/main/java/com/school/sms/controller");

    private static final Pattern PRE_AUTHORIZE =
            Pattern.compile("@PreAuthorize\\(\\s*(?:\"([^\"]*)\"|(\\w+))\\s*\\)");

    private static final Pattern CONSTANT_DECLARATION =
            Pattern.compile("static final String (\\w+)\\s*=\\s*([^;]+);", Pattern.DOTALL);

    /**
     * Expressions that intentionally exclude the administrator because they resolve
     * "me" from the caller's own linked record, which an administrator has not got.
     */
    private static final List<String> SELF_SERVICE_EXPRESSIONS = List.of(
            "hasRole('STUDENT')",
            "hasRole('PARENT')",
            "hasAnyRole('TEACHER')"
    );

    /**
     * A floor on how many gates the audit expects to find. Without it, a regex that
     * silently stops matching would make every assertion below pass by examining
     * nothing — an audit that cannot fail is worse than no audit, because it reads
     * like a guarantee.
     */
    private static final int MINIMUM_EXPECTED_GATES = 150;

    @Test
    @DisplayName("every @PreAuthorize gate admits SUPER_ADMIN, or is a self-service endpoint")
    void everyEndpointAdmitsTheAdministrator() throws IOException {
        List<String> violations = new ArrayList<>();
        int examined = 0;

        for (Path controller : controllerFiles()) {
            String source = Files.readString(controller, StandardCharsets.UTF_8);
            String fileName = controller.getFileName().toString();

            for (String expression : resolvedExpressions(source)) {
                examined++;
                if (admitsAdministrator(expression) || isSelfService(expression)) {
                    continue;
                }
                violations.add(fileName + " -> " + expression);
            }
        }

        assertThat(examined)
                .as("The audit found suspiciously few authorization gates - has the "
                        + "@PreAuthorize regex stopped matching?")
                .isGreaterThanOrEqualTo(MINIMUM_EXPECTED_GATES);

        assertThat(violations)
                .as("These authorization expressions lock SUPER_ADMIN out. Either name "
                        + "SUPER_ADMIN in the role list, or OR in AppConstants.ADMIN_OVERRIDE "
                        + "for a permission-gated endpoint.")
                .isEmpty();
    }

    /**
     * Every endpoint carries an authorization gate.
     *
     * The two tests either side of this one only inspect gates that exist — so an
     * endpoint with no {@code @PreAuthorize} at all passed both of them silently.
     * That is exactly how {@code GET /api/v1/users} shipped readable by any
     * authenticated caller, students included, while its sibling write endpoints
     * were correctly restricted: an audit that only checks the rules it can see is
     * blind to the absence of a rule.
     *
     * A handler that genuinely should be open to all authenticated users must say
     * so explicitly with {@code isAuthenticated()}, so the decision is visible in
     * the source rather than inferred from an omission.
     */
    @Test
    @DisplayName("no endpoint is left without an authorization gate")
    void everyEndpointHasAnAuthorizationGate() throws IOException {
        // Public by design: these are pre-auth or intentionally anonymous, and are
        // covered by the permitAll() matchers in SecurityConfig instead.
        List<String> publicControllers = List.of(
                "AuthController.java",
                "PublicAdmissionEnquiryController.java");

        // Only the verb-specific mapping annotations, and only where the declaration
        // that follows is a method rather than a type — a bare class-level
        // @RequestMapping("/api/v1/...") is the controller's base path, not a handler,
        // and matching it flags every controller in the project.
        Pattern handler = Pattern.compile(
                "@(?:Get|Post|Put|Patch|Delete)Mapping[^\\n]*\\n(\\s*@[^\\n]*\\n)*\\s*public\\s+(?!class\\b)");

        List<String> ungated = new ArrayList<>();

        for (Path controller : controllerFiles()) {
            String fileName = controller.getFileName().toString();
            if (publicControllers.contains(fileName)) {
                continue;
            }
            String source = Files.readString(controller, StandardCharsets.UTF_8);

            Matcher matcher = handler.matcher(source);
            while (matcher.find()) {
                String declaration = matcher.group();
                if (!declaration.contains("@PreAuthorize")) {
                    String firstLine = declaration.strip().split("\\R")[0];
                    ungated.add(fileName + " -> " + firstLine);
                }
            }
        }

        assertThat(ungated)
                .as("These handlers have no @PreAuthorize, so any authenticated user can call "
                        + "them. Add a gate, or isAuthenticated() if that is genuinely intended.")
                .isEmpty();
    }

    /** The administrator must never be gated by a permission grant alone. */
    @Test
    @DisplayName("permission-gated endpoints carry the admin override")
    void permissionGatedEndpointsCarryTheAdminOverride() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path controller : controllerFiles()) {
            String source = Files.readString(controller, StandardCharsets.UTF_8);

            for (String expression : resolvedExpressions(source)) {
                boolean permissionGated = expression.contains("hasAuthority('"
                        + AppConstants.PERMISSION_AUTHORITY_PREFIX);
                if (permissionGated && !expression.contains(AppConstants.ADMIN_OVERRIDE)) {
                    violations.add(controller.getFileName() + " -> " + expression);
                }
            }
        }

        assertThat(violations)
                .as("A permission-gated endpoint must OR in AppConstants.ADMIN_OVERRIDE, "
                        + "so a missing role_permissions row cannot lock the administrator out.")
                .isEmpty();
    }

    /* --------------------------------------------------------------- */

    private List<Path> controllerFiles() throws IOException {
        try (Stream<Path> paths = Files.list(CONTROLLER_DIR)) {
            return paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    /**
     * Every distinct authorization expression in a controller, with constant
     * references (the common {@code @PreAuthorize(WRITE_ROLES)} form) resolved back
     * to the literal they stand for.
     */
    private List<String> resolvedExpressions(String source) {
        List<String> expressions = new ArrayList<>();
        Matcher matcher = PRE_AUTHORIZE.matcher(source);

        while (matcher.find()) {
            String literal = matcher.group(1);
            String constantName = matcher.group(2);
            expressions.add(literal != null ? literal : resolveConstant(source, constantName));
        }
        return expressions;
    }

    private String resolveConstant(String source, String constantName) {
        Matcher matcher = CONSTANT_DECLARATION.matcher(source);
        while (matcher.find()) {
            if (matcher.group(1).equals(constantName)) {
                // Flatten the declaration's string concatenation and line breaks into
                // the effective expression, e.g. "a" + AppConstants.ADMIN_OVERRIDE.
                return matcher.group(2)
                        .replace("AppConstants.ADMIN_OVERRIDE", AppConstants.ADMIN_OVERRIDE)
                        .replaceAll("[\"+]", "")
                        .replaceAll("\\s+", " ")
                        .trim();
            }
        }
        // An unresolvable reference is reported rather than silently passed.
        return "UNRESOLVED_CONSTANT:" + constantName;
    }

    /**
     * True when the administrator gets through. Either the expression names
     * SUPER_ADMIN, or it does not discriminate by role at all — {@code
     * isAuthenticated()} and {@code permitAll()} admit every signed-in user, the
     * administrator included, so they are not violations.
     */
    private boolean admitsAdministrator(String expression) {
        String normalised = expression.replaceAll("\\s+", "");
        return expression.contains(AppConstants.ROLE_SUPER_ADMIN)
                || normalised.equals("isAuthenticated()")
                || normalised.equals("permitAll()");
    }

    private boolean isSelfService(String expression) {
        String normalised = expression.replaceAll("\\s+", "");
        return SELF_SERVICE_EXPRESSIONS.stream()
                .anyMatch(self -> normalised.equals(self.replaceAll("\\s+", "")));
    }
}
