package com.school.sms.repository;

import com.school.sms.entity.ClassOfficial;
import com.school.sms.entity.ClassOfficialRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassOfficialRepository extends JpaRepository<ClassOfficial, Long> {

    /** Current holders only — the ones a class overview shows. */
    List<ClassOfficial> findAllBySchoolClassIdAndToDateIsNullOrderByRoleAsc(Long classId);

    /** Every holder a class has ever had, newest appointment first. */
    List<ClassOfficial> findAllBySchoolClassIdOrderByFromDateDescIdDesc(Long classId);

    Optional<ClassOfficial> findBySchoolClassIdAndRoleAndToDateIsNull(Long classId, ClassOfficialRole role);

    /** Every current post this student holds, in any class. */
    List<ClassOfficial> findAllByStudentIdAndToDateIsNull(Long studentId);

    @Query("SELECT co.schoolClass.id, COUNT(co) FROM ClassOfficial co " +
            "WHERE co.toDate IS NULL GROUP BY co.schoolClass.id")
    List<Object[]> countCurrentGroupByClassId();

    /**
     * Current holders across a page of classes, with the student joined.
     *
     * <p>Fetch-joined for the same reason as the sections query: the class list
     * shows each post-holder's name, and lazy-loading the student per row would
     * turn one screen into dozens of queries.
     */
    @Query("SELECT co FROM ClassOfficial co "
            + "JOIN FETCH co.student "
            + "WHERE co.schoolClass.id IN :classIds AND co.toDate IS NULL")
    List<ClassOfficial> findCurrentWithStudentBySchoolClassIdIn(@Param("classIds") List<Long> classIds);
}
