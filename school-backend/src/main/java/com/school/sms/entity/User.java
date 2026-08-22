package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "role")
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    /**
     * True while this account is still on the password the system generated for it.
     *
     * <p>Set when the school provisions an account and cleared the moment the owner
     * chooses their own. While it is set, {@code PasswordChangeRequiredFilter}
     * refuses every request except changing the password and signing out — so a
     * generated password can be used for exactly one thing: replacing itself.
     *
     * <p>Note what is <em>not</em> stored: the generated password. It is emailed
     * once and kept only as a bcrypt hash like any other, so a lost one is reset
     * through forgot-password rather than looked up.
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Named without the "is" prefix on purpose: Lombok still generates isActive()/setActive()
    // for a boolean field named "active", but keeping the field itself prefix-free avoids
    // MapStruct/bean-introspection ambiguity between the field name and the accessor name.
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return getId() != null && getId().equals(user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
