package com.school.sms.service.impl;

import com.school.sms.dto.request.SalaryStructureRequest;
import com.school.sms.dto.response.SalaryStructureDto;
import com.school.sms.entity.PayrollEmployeeType;
import com.school.sms.entity.SalaryStructure;
import com.school.sms.entity.User;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.SalaryStructureRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.SalaryStructureService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryStructureServiceImpl implements SalaryStructureService {

    private final SalaryStructureRepository salaryStructureRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStructureDto> getAll(Long employeeId, PayrollEmployeeType employeeType) {
        Specification<SalaryStructure> spec = new SpecificationBuilder<SalaryStructure>()
                .with(employeeId != null, "employeeId", SearchOperation.EQUALS, employeeId)
                .with(employeeType != null, "employeeType", SearchOperation.EQUALS, employeeType)
                .build();

        List<SalaryStructure> structures = salaryStructureRepository.findAll(spec);
        Map<Long, User> usersById = userRepository.findAllById(
                structures.stream().map(SalaryStructure::getEmployeeId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return structures.stream().map(s -> toDto(s, usersById)).toList();
    }

    @Override
    @Transactional
    public SalaryStructureDto create(SalaryStructureRequest request) {
        if (salaryStructureRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("SalaryStructure", "employeeId", request.getEmployeeId());
        }
        userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getEmployeeId()));

        SalaryStructure structure = SalaryStructure.builder()
                .employeeId(request.getEmployeeId())
                .employeeType(request.getEmployeeType())
                .basicSalary(request.getBasicSalary())
                .hra(request.getHra())
                .da(request.getDa())
                .otherAllowances(request.getOtherAllowances())
                .pfPercentage(request.getPfPercentage())
                .esiPercentage(request.getEsiPercentage())
                .build();

        return toDto(salaryStructureRepository.save(structure));
    }

    @Override
    @Transactional
    public SalaryStructureDto update(Long id, SalaryStructureRequest request) {
        SalaryStructure structure = findEntity(id);

        if (!structure.getEmployeeId().equals(request.getEmployeeId())
                && salaryStructureRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException("SalaryStructure", "employeeId", request.getEmployeeId());
        }

        structure.setEmployeeId(request.getEmployeeId());
        structure.setEmployeeType(request.getEmployeeType());
        structure.setBasicSalary(request.getBasicSalary());
        structure.setHra(request.getHra());
        structure.setDa(request.getDa());
        structure.setOtherAllowances(request.getOtherAllowances());
        structure.setPfPercentage(request.getPfPercentage());
        structure.setEsiPercentage(request.getEsiPercentage());

        return toDto(salaryStructureRepository.save(structure));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        salaryStructureRepository.delete(findEntity(id));
    }

    private SalaryStructure findEntity(Long id) {
        return salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "id", id));
    }

    private SalaryStructureDto toDto(SalaryStructure structure) {
        User user = userRepository.findById(structure.getEmployeeId()).orElse(null);
        return toDto(structure, user != null ? Map.of(user.getId(), user) : Map.of());
    }

    private SalaryStructureDto toDto(SalaryStructure structure, Map<Long, User> usersById) {
        User user = usersById.get(structure.getEmployeeId());
        return SalaryStructureDto.builder()
                .id(structure.getId())
                .employeeId(structure.getEmployeeId())
                .employeeName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .employeeType(structure.getEmployeeType().name())
                .basicSalary(structure.getBasicSalary())
                .hra(structure.getHra())
                .da(structure.getDa())
                .otherAllowances(structure.getOtherAllowances())
                .pfPercentage(structure.getPfPercentage())
                .esiPercentage(structure.getEsiPercentage())
                .build();
    }
}
