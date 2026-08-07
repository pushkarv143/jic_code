package com.school.sms.service;

import com.school.sms.dto.request.BusRequest;
import com.school.sms.dto.response.BusDto;

import java.util.List;

public interface BusService {

    List<BusDto> getAll();

    BusDto create(BusRequest request);

    BusDto update(Long id, BusRequest request);

    void delete(Long id);
}
