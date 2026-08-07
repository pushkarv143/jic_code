package com.school.sms.service.impl;

import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StaffDto;
import com.school.sms.entity.Staff;
import com.school.sms.repository.StaffRepository;
import com.school.sms.service.StaffService;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StaffDto> search(String search, int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<Staff> spec = new SpecificationBuilder<Staff>()
                .with("deleted", SearchOperation.EQUALS, false)
                .build();

        if (StringUtils.hasText(search)) {
            String term = search.trim().toLowerCase();
            Specification<Staff> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("user").get("firstName")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("user").get("lastName")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("user").get("username")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("user").get("email")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("employeeId")), "%" + term + "%")
            );
            spec = spec == null ? searchSpec : spec.and(searchSpec);
        }

        Page<Staff> page1 = staffRepository.findAll(spec, pageable);
        return PageResponse.from(page1.map(this::toDto));
    }

    private StaffDto toDto(Staff staff) {
        return StaffDto.builder()
                .id(staff.getId())
                .userId(staff.getUser() != null ? staff.getUser().getId() : null)
                .employeeId(staff.getEmployeeId())
                .firstName(staff.getUser() != null ? staff.getUser().getFirstName() : null)
                .lastName(staff.getUser() != null ? staff.getUser().getLastName() : null)
                .departmentName(staff.getDepartment() != null ? staff.getDepartment().getName() : null)
                .designationName(staff.getDesignation() != null ? staff.getDesignation().getName() : null)
                .status(staff.getStatus() != null ? staff.getStatus().name() : null)
                .build();
    }
}
