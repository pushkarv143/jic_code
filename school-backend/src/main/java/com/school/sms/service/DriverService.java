package com.school.sms.service;

import com.school.sms.dto.request.DriverRequest;
import com.school.sms.dto.response.DriverDto;

import java.util.List;

public interface DriverService {

    List<DriverDto> getAll();

    DriverDto create(DriverRequest request);

    DriverDto update(Long id, DriverRequest request);

    void delete(Long id);
}
