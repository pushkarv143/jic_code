package com.school.sms.service.impl;

import com.school.sms.dto.request.DriverRequest;
import com.school.sms.dto.response.DriverDto;
import com.school.sms.entity.Driver;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.DriverMapper;
import com.school.sms.repository.DriverRepository;
import com.school.sms.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DriverDto> getAll() {
        return driverRepository.findAllByOrderByNameAsc().stream()
                .map(driverMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DriverDto create(DriverRequest request) {
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("Driver", "licenseNumber", request.getLicenseNumber());
        }
        Driver entity = driverMapper.toEntity(request);
        return driverMapper.toDto(driverRepository.save(entity));
    }

    @Override
    @Transactional
    public DriverDto update(Long id, DriverRequest request) {
        Driver entity = findEntity(id);
        if (!entity.getLicenseNumber().equalsIgnoreCase(request.getLicenseNumber())
                && driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("Driver", "licenseNumber", request.getLicenseNumber());
        }
        driverMapper.updateEntityFromRequest(request, entity);
        return driverMapper.toDto(driverRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        driverRepository.delete(findEntity(id));
    }

    private Driver findEntity(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id));
    }
}
