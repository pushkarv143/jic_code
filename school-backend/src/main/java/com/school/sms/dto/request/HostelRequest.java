package com.school.sms.dto.request;

import com.school.sms.entity.HostelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HostelRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String wardenName;

    private String wardenContact;

    @NotNull(message = "Type is required")
    private HostelType type;
}
