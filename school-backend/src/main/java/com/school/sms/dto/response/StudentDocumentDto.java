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
public class StudentDocumentDto {

    private Long id;
    private Long studentId;
    private String documentType;
    private String fileUrl;
    private LocalDateTime uploadedAt;
}
