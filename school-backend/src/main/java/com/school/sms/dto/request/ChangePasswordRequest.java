package com.school.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /**
     * The password being replaced.
     *
     * <p>Not {@code @NotBlank}: an account still on a system-generated password may
     * omit it. That is not a loophole — the caller is already authenticated, and the
     * case it serves is the one this flow exists for. When the credentials email
     * fails, the student signs in with a one-time code and has never seen the
     * password they are being told to change; requiring it would leave them
     * authenticated, obliged to change something they cannot supply, and refused by
     * every other endpoint.
     *
     * <p>Still verified whenever it <em>is</em> supplied, and still required for an
     * ordinary voluntary change — see {@code AuthServiceImpl.changePassword}.
     */
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, digit and special character"
    )
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
