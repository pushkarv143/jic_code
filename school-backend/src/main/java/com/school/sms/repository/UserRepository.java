package com.school.sms.repository;

import com.school.sms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Phone lookup for OTP, matched on digits alone.
     *
     * The column is not stored in one format — the seed data alone holds both
     * {@code +91-9999900001} and {@code 9999900004} — and nobody types their
     * number the way it happens to sit in the database. An exact match therefore
     * finds almost nobody, which for OTP means silently sending nothing at all.
     *
     * So both sides are reduced to digits and compared on the last ten, which is
     * the national number regardless of whether a country code, a leading zero or
     * any punctuation was included. Single-country assumption, which holds here.
     *
     * Deliberately unindexed work: this scans, and at a few hundred users that
     * costs nothing. A generated normalised column would be the answer if this
     * ever ran against a much larger table.
     *
     * Returns a list rather than an Optional because the column has no unique
     * constraint — a parent may share a number with their child — and the caller
     * treats an ambiguous match as no match rather than guessing.
     */
    @Query(value = "SELECT * FROM users u "
            + "WHERE u.phone IS NOT NULL AND u.phone <> '' "
            + "AND REGEXP_REPLACE(u.phone, '[^0-9]', '') LIKE CONCAT('%', :nationalDigits)",
            nativeQuery = true)
    List<User> findAllByPhoneDigits(@Param("nationalDigits") String nationalDigits);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Notifications targetRole fan-out: every active user holding a given role.
    List<User> findAllByRole_NameAndActiveTrue(String roleName);
}
