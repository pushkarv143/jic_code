package com.school.sms.service.impl;

import com.school.sms.dto.request.PickupPointRequest;
import com.school.sms.dto.response.PickupPointDto;
import com.school.sms.entity.PickupPoint;
import com.school.sms.entity.Route;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.PickupPointMapper;
import com.school.sms.repository.PickupPointRepository;
import com.school.sms.repository.RouteRepository;
import com.school.sms.service.PickupPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PickupPointServiceImpl implements PickupPointService {

    private final PickupPointRepository pickupPointRepository;
    private final RouteRepository routeRepository;
    private final PickupPointMapper pickupPointMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PickupPointDto> getByRoute(Long routeId) {
        findRoute(routeId);
        return pickupPointRepository.findAllByRouteIdOrderByPickupTimeAsc(routeId).stream()
                .map(pickupPointMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public PickupPointDto create(Long routeId, PickupPointRequest request) {
        Route route = findRoute(routeId);

        PickupPoint pickupPoint = PickupPoint.builder()
                .route(route)
                .pointName(request.getPointName())
                .pickupTime(request.getPickupTime())
                .dropTime(request.getDropTime())
                .build();

        return pickupPointMapper.toDto(pickupPointRepository.save(pickupPoint));
    }

    @Override
    @Transactional
    public PickupPointDto update(Long id, PickupPointRequest request) {
        PickupPoint pickupPoint = findEntity(id);

        pickupPoint.setPointName(request.getPointName());
        pickupPoint.setPickupTime(request.getPickupTime());
        pickupPoint.setDropTime(request.getDropTime());

        return pickupPointMapper.toDto(pickupPointRepository.save(pickupPoint));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        pickupPointRepository.delete(findEntity(id));
    }

    private PickupPoint findEntity(Long id) {
        return pickupPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PickupPoint", "id", id));
    }

    private Route findRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
    }
}
