package com.school.sms.service;

import com.school.sms.dto.request.EventRequest;
import com.school.sms.dto.response.EventDto;

import java.time.LocalDate;
import java.util.List;

public interface EventService {

    List<EventDto> getAll(LocalDate startDate, LocalDate endDate, String eventType);

    EventDto create(EventRequest request);

    EventDto update(Long id, EventRequest request);

    void delete(Long id);
}
