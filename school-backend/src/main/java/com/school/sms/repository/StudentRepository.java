package com.school.sms.repository;

import com.school.sms.entity.Student;
import com.school.sms.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    boolean existsByAdmissionNumber(String admissionNumber);

    Optional<Student> findByUserId(Long userId);

    long countByAdmissionNumberStartingWith(String prefix);

    List<Student> findAllByIdIn(List<Long> ids);

    // Attendance/fees modules: roster of a section's active students (used to
    // pre-fill the attendance-marking grid and the monthly attendance view).
    List<Student> findAllBySchoolClassIdAndSectionIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
            Long classId, Long sectionId, StudentStatus status);

    // Fee generation: every active student across all sections of a class.
    List<Student> findAllBySchoolClassIdAndDeletedFalseAndStatusOrderByRollNumberAsc(
            Long classId, StudentStatus status);

    // Own-record scoping for a PARENT: student_parents/parents have no JPA
    // entities in this round, so this resolves a parent's children with a
    // native query against the raw tables per SCHEMA_CONTRACT.md.
    @Query(value = "SELECT s.* FROM students s " +
            "JOIN student_parents sp ON sp.student_id = s.id " +
            "JOIN parents p ON p.id = sp.parent_id " +
            "WHERE p.user_id = :userId", nativeQuery = true)
    List<Student> findAllByParentUserId(@Param("userId") Long userId);

    // Teacher scoping (StudentAccessGuard): the students a teacher is entitled to see —
    // those in a section they are homeroom teacher of, plus those in any class/section
    // they are mapped to teach a subject in. Returns ids only: the guard compares
    // membership and the service re-fetches whichever rows it actually needs.
    @Query("SELECT DISTINCT s.id FROM Student s " +
            "WHERE s.deleted = false AND (" +
            "  s.section.classTeacher.id = :teacherId " +
            "  OR EXISTS (SELECT 1 FROM ClassSubjectTeacher cst " +
            "             WHERE cst.teacher.id = :teacherId " +
            "               AND cst.schoolClass.id = s.schoolClass.id " +
            "               AND cst.section.id = s.section.id))")
    List<Long> findIdsTaughtByTeacherId(@Param("teacherId") Long teacherId);

    // Roll-number allocation: the highest roll number currently used in a
    // class/section, so the next admission continues the sequence. Deleted students
    // are included on purpose — reusing a withdrawn student's roll number would make
    // historical attendance and marks ambiguous.
    @Query("SELECT MAX(s.rollNumber) FROM Student s "
            + "WHERE s.schoolClass.id = :classId AND s.section.id = :sectionId")
    Integer findMaxRollNumberInSection(@Param("classId") Long classId, @Param("sectionId") Long sectionId);

    boolean existsBySchoolClassIdAndSectionIdAndRollNumber(Long classId, Long sectionId, Integer rollNumber);

    // Calendar / birthdays widget: active students born in a given month, in day-of-month order.
    @Query("SELECT s FROM Student s WHERE s.deleted = false AND s.dateOfBirth IS NOT NULL " +
            "AND MONTH(s.dateOfBirth) = :month ORDER BY DAY(s.dateOfBirth) ASC")
    List<Student> findAllByBirthMonth(@Param("month") int month);

    // Reports: /api/v1/reports/students-summary.
    long countByDeletedFalseAndStatus(StudentStatus status);

    @Query("SELECT s.schoolClass.className, COUNT(s) FROM Student s " +
            "WHERE s.deleted = false AND s.status = 'ACTIVE' " +
            "GROUP BY s.schoolClass.className ORDER BY s.schoolClass.className")
    List<Object[]> countActiveGroupByClass();

    @Query("SELECT s.status, COUNT(s) FROM Student s WHERE s.deleted = false GROUP BY s.status")
    List<Object[]> countGroupByStatus();

    /*
     * Class-module strength counts.
     *
     * Keyed on id rather than className like countActiveGroupByClass() above,
     * which groups by name for a dashboard chart and so cannot be reused here:
     * two classes may legitimately share a name across academic years, and the
     * class list needs a count it can join back to a row by id.
     *
     * Returned as one aggregate per class/section rather than a count per row,
     * so a page of 20 classes costs one query instead of 20.
     */
    @Query("SELECT s.schoolClass.id, COUNT(s) FROM Student s " +
            "WHERE s.deleted = false AND s.status = 'ACTIVE' GROUP BY s.schoolClass.id")
    List<Object[]> countActiveGroupByClassId();

    @Query("SELECT s.section.id, COUNT(s) FROM Student s " +
            "WHERE s.deleted = false AND s.status = 'ACTIVE' GROUP BY s.section.id")
    List<Object[]> countActiveGroupBySectionId();

    /** Gender split for one class, for the strength panel. [0] gender, [1] count. */
    @Query("SELECT s.gender, COUNT(s) FROM Student s " +
            "WHERE s.deleted = false AND s.status = 'ACTIVE' AND s.schoolClass.id = :classId " +
            "GROUP BY s.gender")
    List<Object[]> countActiveByGenderForClass(@Param("classId") Long classId);

    long countBySchoolClassIdAndDeletedFalseAndStatus(Long classId, StudentStatus status);

    long countBySectionIdAndDeletedFalseAndStatus(Long sectionId, StudentStatus status);

    boolean existsByIdAndSchoolClassIdAndDeletedFalseAndStatus(Long id, Long classId, StudentStatus status);
}
