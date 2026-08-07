package com.school.sms.dto.request;

import com.school.sms.entity.StudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatusRequest {

    @NotNull(message = "Status is required")
    private StudentStatus status;
}
