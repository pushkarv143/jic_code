package com.school.sms.service.impl;

import com.school.sms.dto.request.StudentTransportRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentTransportDto;
import com.school.sms.entity.PickupPoint;
import com.school.sms.entity.Route;
import com.school.sms.entity.Student;
import com.school.sms.entity.StudentTransport;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.PickupPointRepository;
import com.school.sms.repository.RouteRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.StudentTransportRepository;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.StudentTransportService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentTransportServiceImpl implements StudentTransportService {

    private final StudentTransportRepository studentTransportRepository;
    private final StudentRepository studentRepository;
    private final RouteRepository routeRepository;
    private final PickupPointRepository pickupPointRepository;
    private final StudentAccessGuard studentAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentTransportDto> getAll(Long studentId, Long routeId, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only view your own transport records");
        }

        Specification<StudentTransport> spec = new SpecificationBuilder<StudentTransport>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "student.id", SearchOperation.IN, ownIds)
                .with(routeId != null, "route.id", SearchOperation.EQUALS, routeId)
                .build();

        Page<StudentTransport> page = studentTransportRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public StudentTransportDto create(StudentTransportRequest request) {
        Student student = findStudent(request.getStudentId());
        Route route = findRoute(request.getRouteId());
        PickupPoint pickupPoint = findPickupPoint(request.getPickupPointId());

        if (!pickupPoint.getRoute().getId().equals(route.getId())) {
            throw new BadRequestException("The pickup point does not belong to the given route");
        }

        StudentTransport entity = StudentTransport.builder()
                .student(student)
                .route(route)
                .pickupPoint(pickupPoint)
                .monthlyFee(request.getMonthlyFee())
                .build();

        return toDto(studentTransportRepository.save(entity));
    }

    @Override
    @Transactional
    public StudentTransportDto update(Long id, StudentTransportRequest request) {
        StudentTransport entity = findEntity(id);
        Student student = findStudent(request.getStudentId());
        Route route = findRoute(request.getRouteId());
        PickupPoint pickupPoint = findPickupPoint(request.getPickupPointId());

        if (!pickupPoint.getRoute().getId().equals(route.getId())) {
            throw new BadRequestException("The pickup point does not belong to the given route");
        }

        entity.setStudent(student);
        entity.setRoute(route);
        entity.setPickupPoint(pickupPoint);
        entity.setMonthlyFee(request.getMonthlyFee());

        return toDto(studentTransportRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        studentTransportRepository.delete(findEntity(id));
    }

    private StudentTransportDto toDto(StudentTransport entity) {
        Student student = entity.getStudent();
        User user = student.getUser();

        return StudentTransportDto.builder()
                .id(entity.getId())
                .studentId(student.getId())
                .studentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .admissionNumber(student.getAdmissionNumber())
                .routeId(entity.getRoute().getId())
                .routeName(entity.getRoute().getRouteName())
                .pickupPointId(entity.getPickupPoint().getId())
                .pickupPointName(entity.getPickupPoint().getPointName())
                .monthlyFee(entity.getMonthlyFee())
                .build();
    }

    private StudentTransport findEntity(Long id) {
        return studentTransportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentTransport", "id", id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    private Route findRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
    }

    private PickupPoint findPickupPoint(Long id) {
        return pickupPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PickupPoint", "id", id));
    }
}
