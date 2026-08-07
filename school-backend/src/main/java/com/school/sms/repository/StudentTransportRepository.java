package com.school.sms.repository;

import com.school.sms.entity.StudentTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface StudentTransportRepository extends JpaRepository<StudentTransport, Long>,
        JpaSpecificationExecutor<StudentTransport> {

    boolean existsByStudentId(Long studentId);

    // Reports: /api/v1/reports/transport-summary.
    @Query("SELECT COUNT(DISTINCT st.student.id) FROM StudentTransport st")
    long countDistinctStudents();
}
