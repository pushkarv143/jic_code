package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String gender;
    private String role;
    private boolean active;
    private String profileImage;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    // Self-service identity resolution: populated only on /auth/login, /auth/refresh-token
    // and /auth/me (not on the admin user-management endpoints, which have no need for it),
    // so the frontend can resolve "which student/teacher am I" without an extra round trip
    // when building self-service attendance/fee views. Null when not applicable to this role.
    private Long studentId;
    private Long teacherId;
    private Long classId;
    private Long sectionId;
}
