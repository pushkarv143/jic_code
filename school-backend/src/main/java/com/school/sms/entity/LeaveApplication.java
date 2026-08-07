package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * applicant_id/approved_by are raw FKs to users.id — per the round brief, no
 * JPA relationship is needed; the resolved applicant name/role is looked up
 * on demand (via UserRepository) and placed on LeaveApplicationDto.
 */
@Entity
@Table(name = "leave_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LeaveApplication extends AuditableEntity {

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_type", nullable = false, length = 20)
    private LeaveApplicantType applicantType;

    @Column(name = "leave_type", length = 50)
    private String leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveStatus status;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeaveApplication that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
