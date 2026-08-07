package com.school.sms.service;

import com.school.sms.dto.request.RouteRequest;
import com.school.sms.dto.response.RouteDto;

import java.util.List;

public interface RouteService {

    List<RouteDto> getAll(Long busId);

    RouteDto create(RouteRequest request);

    RouteDto update(Long id, RouteRequest request);

    void delete(Long id);
}
