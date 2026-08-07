package com.school.sms.service;

import com.school.sms.dto.request.HostelRequest;
import com.school.sms.dto.response.HostelDto;

import java.util.List;

public interface HostelService {

    List<HostelDto> getAll();

    HostelDto create(HostelRequest request);

    HostelDto update(Long id, HostelRequest request);

    void delete(Long id);
}
