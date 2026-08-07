package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostelRoomDto {

    private Long id;
    private Long hostelId;
    private String hostelName;
    private String roomNumber;
    private Integer capacity;
    private Integer occupiedCount;
}
