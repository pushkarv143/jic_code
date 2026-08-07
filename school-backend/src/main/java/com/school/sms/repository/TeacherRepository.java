package com.school.sms.repository;

import com.school.sms.entity.Teacher;
import com.school.sms.entity.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {

    boolean existsByEmployeeId(String employeeId);

    Optional<Teacher> findByUserId(Long userId);

    long countByEmployeeIdStartingWith(String prefix);

    // Attendance module: roster of active teachers for the marking grid.
    List<Teacher> findAllByDeletedFalseAndStatusOrderByIdAsc(TeacherStatus status);

    // Calendar / birthdays widget: active teachers born in a given month, in day-of-month order.
    @Query("SELECT t FROM Teacher t WHERE t.deleted = false AND t.dateOfBirth IS NOT NULL " +
            "AND MONTH(t.dateOfBirth) = :month ORDER BY DAY(t.dateOfBirth) ASC")
    List<Teacher> findAllByBirthMonth(@Param("month") int month);

    // Reports: /api/v1/reports/teachers-summary.
    long countByDeletedFalseAndStatus(TeacherStatus status);

    @Query("SELECT t.department.name, COUNT(t) FROM Teacher t " +
            "WHERE t.deleted = false AND t.status = 'ACTIVE' " +
            "GROUP BY t.department.name ORDER BY t.department.name")
    List<Object[]> countActiveGroupByDepartment();
}
