package com.school.sms.service.impl;

import com.school.sms.dto.request.RouteRequest;
import com.school.sms.dto.response.RouteDto;
import com.school.sms.entity.Bus;
import com.school.sms.entity.Route;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.RouteMapper;
import com.school.sms.repository.BusRepository;
import com.school.sms.repository.RouteRepository;
import com.school.sms.service.RouteService;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final RouteMapper routeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RouteDto> getAll(Long busId) {
        Specification<Route> spec = new SpecificationBuilder<Route>()
                .with(busId != null, "bus.id", SearchOperation.EQUALS, busId)
                .build();

        return routeRepository.findAll(spec).stream()
                .map(routeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RouteDto create(RouteRequest request) {
        Bus bus = request.getBusId() != null ? findBus(request.getBusId()) : null;

        Route route = Route.builder()
                .routeName(request.getRouteName())
                .bus(bus)
                .startPoint(request.getStartPoint())
                .endPoint(request.getEndPoint())
                .build();

        return routeMapper.toDto(routeRepository.save(route));
    }

    @Override
    @Transactional
    public RouteDto update(Long id, RouteRequest request) {
        Route route = findEntity(id);
        Bus bus = request.getBusId() != null ? findBus(request.getBusId()) : null;

        route.setRouteName(request.getRouteName());
        route.setBus(bus);
        route.setStartPoint(request.getStartPoint());
        route.setEndPoint(request.getEndPoint());

        return routeMapper.toDto(routeRepository.save(route));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        routeRepository.delete(findEntity(id));
    }

    private Route findEntity(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
    }

    private Bus findBus(Long id) {
        return busRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
    }
}
