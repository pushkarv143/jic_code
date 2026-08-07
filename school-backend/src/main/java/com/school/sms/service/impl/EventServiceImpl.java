package com.school.sms.service.impl;

import com.school.sms.dto.request.EventRequest;
import com.school.sms.dto.response.EventDto;
import com.school.sms.entity.Event;
import com.school.sms.entity.EventType;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.EventRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.service.EventService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAll(LocalDate startDate, LocalDate endDate, String eventType) {
        Specification<Event> spec = new SpecificationBuilder<Event>()
                .with(startDate != null, "eventDate", SearchOperation.GREATER_THAN_EQUAL, startDate)
                .with(endDate != null, "eventDate", SearchOperation.LESS_THAN_EQUAL, endDate)
                .with(StringUtils.hasText(eventType), "eventType", SearchOperation.EQUALS,
                        StringUtils.hasText(eventType) ? EventType.valueOf(eventType.toUpperCase()) : null)
                .build();

        List<Event> events = eventRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "eventDate"));

        Map<Long, User> creatorsById = userRepository.findAllById(
                events.stream().map(Event::getCreatedBy).filter(Objects::nonNull).toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return events.stream().map(event -> toDto(event, creatorsById)).toList();
    }

    @Override
    @Transactional
    public EventDto create(EventRequest request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .eventType(parseEventType(request.getEventType()))
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();

        return toDto(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventDto update(Long id, EventRequest request) {
        Event event = findEntity(id);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setEventType(parseEventType(request.getEventType()));
        return toDto(eventRepository.save(event));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        eventRepository.delete(findEntity(id));
    }

    private EventType parseEventType(String value) {
        try {
            return EventType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid event type: " + value);
        }
    }

    private EventDto toDto(Event event) {
        Map<Long, User> creatorMap = event.getCreatedBy() != null
                ? userRepository.findById(event.getCreatedBy()).map(u -> Map.of(u.getId(), u)).orElse(Map.of())
                : Map.of();
        return toDto(event, creatorMap);
    }

    private EventDto toDto(Event event, Map<Long, User> creatorsById) {
        User creator = event.getCreatedBy() != null ? creatorsById.get(event.getCreatedBy()) : null;
        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .eventType(event.getEventType().name())
                .createdBy(event.getCreatedBy())
                .createdByName(creator != null ? NameUtil.fullName(creator.getFirstName(), creator.getLastName()) : null)
                .build();
    }

    private Event findEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
    }
}
