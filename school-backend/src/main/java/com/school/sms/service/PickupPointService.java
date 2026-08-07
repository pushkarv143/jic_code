package com.school.sms.service;

import com.school.sms.dto.request.PickupPointRequest;
import com.school.sms.dto.response.PickupPointDto;

import java.util.List;

public interface PickupPointService {

    List<PickupPointDto> getByRoute(Long routeId);

    PickupPointDto create(Long routeId, PickupPointRequest request);

    PickupPointDto update(Long id, PickupPointRequest request);

    void delete(Long id);
}
