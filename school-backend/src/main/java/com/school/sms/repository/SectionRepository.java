package com.school.sms.repository;

import com.school.sms.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllBySchoolClassIdOrderBySectionNameAsc(Long classId);

    /**
     * Every section of a page of classes, with its class teacher and that
     * teacher's account already joined.
     *
     * <p>The fetch joins are the point: the class list renders a class-teacher
     * dropdown per section, so a plain finder would lazy-load teacher and user for
     * each of ~30 sections a page. One query instead of sixty.
     */
    @Query("SELECT s FROM Section s "
            + "LEFT JOIN FETCH s.classTeacher t "
            + "LEFT JOIN FETCH t.user "
            + "WHERE s.schoolClass.id IN :classIds "
            + "ORDER BY s.sectionName ASC")
    List<Section> findAllWithClassTeacherBySchoolClassIdIn(@Param("classIds") List<Long> classIds);

    boolean existsBySchoolClassIdAndSectionNameIgnoreCase(Long classId, String sectionName);

    /**
     * The section this teacher is homeroom of, if any.
     *
     * <p>Singular because a teacher heads at most one section — enforced by
     * uq_sections_class_teacher (13_assignment_uniqueness.sql). Used to turn that
     * constraint into a message naming the section they already hold, rather than
     * letting a duplicate-key error reach the caller.
     */
    Optional<Section> findByClassTeacherId(Long teacherId);

    /**
     * Every section that currently has a class teacher, with the teacher and their
     * account joined. Drives the "already taken" state of the class-teacher
     * dropdown, so the whole set is wanted at once rather than one lookup per
     * option.
     */
    @Query("SELECT s FROM Section s "
            + "JOIN FETCH s.classTeacher t "
            + "JOIN FETCH t.user "
            + "LEFT JOIN FETCH s.schoolClass "
            + "WHERE s.classTeacher IS NOT NULL")
    List<Section> findAllAssignedHomerooms();

    // Excel student import: resolves a row's plain-text section name to an id within its class.
    Optional<Section> findFirstBySchoolClassIdAndSectionNameIgnoreCase(Long classId, String sectionName);

    // Section scoping (SectionAccessGuard): is this teacher the homeroom teacher of
    // this section? Homeroom is a separate grant from the class_subject_teacher
    // subject mappings — a class teacher may own a section without teaching a
    // subject in it.
    boolean existsByIdAndSchoolClassIdAndClassTeacherId(Long sectionId, Long classId, Long teacherId);

    // Study materials: the sections a teacher reaches. Split in two because the two
    // grants live in different tables — homeroom on sections, subject teaching on
    // class_subject_teacher — and the caller unions them.
    @Query("SELECT s.id FROM Section s WHERE s.classTeacher.id = :teacherId")
    List<Long> findIdsByClassTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT cst.section.id FROM ClassSubjectTeacher cst WHERE cst.teacher.id = :teacherId")
    List<Long> findIdsTaughtByTeacherId(@Param("teacherId") Long teacherId);

    // The classes a teacher reaches, by either grant. Kept as two queries unioned by
    // the caller rather than one: JPQL has no UNION, and a native query would have to
    // hard-code the column names these entity mappings already own.
    @Query("SELECT DISTINCT s.schoolClass.id FROM Section s WHERE s.classTeacher.id = :teacherId")
    List<Long> findClassIdsByClassTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT cst.schoolClass.id FROM ClassSubjectTeacher cst WHERE cst.teacher.id = :teacherId")
    List<Long> findClassIdsTaughtByTeacherId(@Param("teacherId") Long teacherId);
}
