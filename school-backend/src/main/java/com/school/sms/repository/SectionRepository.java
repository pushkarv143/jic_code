package com.school.sms.repository;

import com.school.sms.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findAllBySchoolClassIdOrderBySectionNameAsc(Long classId);

    boolean existsBySchoolClassIdAndSectionNameIgnoreCase(Long classId, String sectionName);

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
