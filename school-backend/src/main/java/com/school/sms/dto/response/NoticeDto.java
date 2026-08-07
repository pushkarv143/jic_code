package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDto {

    private Long id;
    private String title;
    private String description;
    private Long targetRoleId;
    private String targetRoleName;
    private Long publishedBy;
    private String publishedByName;
    private LocalDateTime publishedAt;
    private LocalDate expiryDate;
    private String attachmentUrl;
}
