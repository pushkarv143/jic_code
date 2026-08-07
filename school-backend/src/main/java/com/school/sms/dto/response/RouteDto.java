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
public class RouteDto {

    private Long id;
    private String routeName;
    private Long busId;
    private String busNumber;
    private String startPoint;
    private String endPoint;
}
