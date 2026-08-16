package com.school.sms.repository;

import com.school.sms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Phone lookup for OTP. Returns a list rather than an Optional on purpose:
     * users.phone carries no unique constraint, so a number can legitimately sit
     * on more than one row — a parent sharing a number with their child, say.
     * The caller treats an ambiguous match as no match rather than guessing which
     * account someone meant.
     */
    List<User> findAllByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Notifications targetRole fan-out: every active user holding a given role.
    List<User> findAllByRole_NameAndActiveTrue(String roleName);
}
