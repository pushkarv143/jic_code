package com.school.sms.service.impl;

import com.school.sms.dto.request.BusRequest;
import com.school.sms.dto.response.BusDto;
import com.school.sms.entity.Bus;
import com.school.sms.entity.Driver;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.BusMapper;
import com.school.sms.repository.BusRepository;
import com.school.sms.repository.DriverRepository;
import com.school.sms.service.BusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final BusMapper busMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BusDto> getAll() {
        return busRepository.findAllByDeletedFalseOrderByBusNumberAsc().stream()
                .map(busMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BusDto create(BusRequest request) {
        if (busRepository.existsByBusNumber(request.getBusNumber())) {
            throw new DuplicateResourceException("Bus", "busNumber", request.getBusNumber());
        }
        if (busRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Bus", "registrationNumber", request.getRegistrationNumber());
        }
        Driver driver = request.getDriverId() != null ? findDriver(request.getDriverId()) : null;

        Bus bus = Bus.builder()
                .busNumber(request.getBusNumber())
                .capacity(request.getCapacity())
                .driver(driver)
                .vehicleModel(request.getVehicleModel())
                .registrationNumber(request.getRegistrationNumber())
                .deleted(false)
                .build();

        return busMapper.toDto(busRepository.save(bus));
    }

    @Override
    @Transactional
    public BusDto update(Long id, BusRequest request) {
        Bus bus = findEntity(id);

        if (!bus.getBusNumber().equalsIgnoreCase(request.getBusNumber())
                && busRepository.existsByBusNumber(request.getBusNumber())) {
            throw new DuplicateResourceException("Bus", "busNumber", request.getBusNumber());
        }
        if (!bus.getRegistrationNumber().equalsIgnoreCase(request.getRegistrationNumber())
                && busRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Bus", "registrationNumber", request.getRegistrationNumber());
        }
        Driver driver = request.getDriverId() != null ? findDriver(request.getDriverId()) : null;

        bus.setBusNumber(request.getBusNumber());
        bus.setCapacity(request.getCapacity());
        bus.setDriver(driver);
        bus.setVehicleModel(request.getVehicleModel());
        bus.setRegistrationNumber(request.getRegistrationNumber());

        return busMapper.toDto(busRepository.save(bus));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Bus bus = findEntity(id);
        bus.setDeleted(true);
        busRepository.save(bus);
    }

    private Bus findEntity(Long id) {
        return busRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
    }

    private Driver findDriver(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id));
    }
}
