package com.school.sms.service.impl;

import com.school.sms.dto.request.HostelStudentRequest;
import com.school.sms.dto.request.HostelStudentVacateRequest;
import com.school.sms.dto.response.HostelStudentDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.HostelRoom;
import com.school.sms.entity.HostelStudent;
import com.school.sms.entity.HostelStudentStatus;
import com.school.sms.entity.Student;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.HostelRoomRepository;
import com.school.sms.repository.HostelStudentRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.security.StudentAccessGuard;
import com.school.sms.service.HostelStudentService;
import com.school.sms.util.NameUtil;
import com.school.sms.util.specification.SearchOperation;
import com.school.sms.util.specification.SpecificationBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HostelStudentServiceImpl implements HostelStudentService {

    private final HostelStudentRepository hostelStudentRepository;
    private final StudentRepository studentRepository;
    private final HostelRoomRepository hostelRoomRepository;
    private final StudentAccessGuard studentAccessGuard;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HostelStudentDto> getAll(Long studentId, Long roomId, String status, Pageable pageable) {
        List<Long> ownIds = studentAccessGuard.resolveViewableStudentIds();
        if (ownIds != null && studentId != null && !ownIds.contains(studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only view your own hostel records");
        }

        Specification<HostelStudent> spec = new SpecificationBuilder<HostelStudent>()
                .with(studentId != null, "student.id", SearchOperation.EQUALS, studentId)
                .with(studentId == null && ownIds != null, "student.id", SearchOperation.IN, ownIds)
                .with(roomId != null, "room.id", SearchOperation.EQUALS, roomId)
                .with(StringUtils.hasText(status), "status", SearchOperation.EQUALS,
                        StringUtils.hasText(status) ? HostelStudentStatus.valueOf(status.toUpperCase()) : null)
                .build();

        Page<HostelStudent> page = hostelStudentRepository.findAll(spec, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public HostelStudentDto allocate(HostelStudentRequest request) {
        Student student = findStudent(request.getStudentId());
        HostelRoom room = findRoom(request.getRoomId());

        if (room.getOccupiedCount() >= room.getCapacity()) {
            throw new BadRequestException("This room is already at full capacity");
        }

        HostelStudent hostelStudent = HostelStudent.builder()
                .student(student)
                .room(room)
                .allocationDate(request.getAllocationDate() != null ? request.getAllocationDate() : LocalDate.now())
                .status(HostelStudentStatus.ACTIVE)
                .build();

        // trg_hostel_students_after_insert (see database/04_triggers.sql) increments
        // hostel_rooms.occupied_count as soon as this INSERT executes.
        HostelStudent saved = hostelStudentRepository.saveAndFlush(hostelStudent);
        entityManager.refresh(room);

        return toDto(saved);
    }

    @Override
    @Transactional
    public HostelStudentDto vacate(Long id, HostelStudentVacateRequest request) {
        HostelStudent hostelStudent = findEntity(id);
        if (hostelStudent.getStatus() == HostelStudentStatus.VACATED) {
            throw new BadRequestException("This student has already vacated the hostel");
        }

        hostelStudent.setStatus(HostelStudentStatus.VACATED);
        hostelStudent.setVacateDate(request != null && request.getVacateDate() != null
                ? request.getVacateDate() : LocalDate.now());

        // trg_hostel_students_after_update (see database/04_triggers.sql) decrements
        // hostel_rooms.occupied_count on this ACTIVE -> VACATED transition.
        HostelStudent saved = hostelStudentRepository.saveAndFlush(hostelStudent);
        entityManager.refresh(saved.getRoom());

        return toDto(saved);
    }

    private HostelStudentDto toDto(HostelStudent entity) {
        Student student = entity.getStudent();
        User user = student.getUser();
        HostelRoom room = entity.getRoom();

        return HostelStudentDto.builder()
                .id(entity.getId())
                .studentId(student.getId())
                .studentName(user != null ? NameUtil.fullName(user.getFirstName(), user.getLastName()) : null)
                .admissionNumber(student.getAdmissionNumber())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .hostelId(room.getHostel().getId())
                .hostelName(room.getHostel().getName())
                .allocationDate(entity.getAllocationDate())
                .vacateDate(entity.getVacateDate())
                .status(entity.getStatus().name())
                .build();
    }

    private HostelStudent findEntity(Long id) {
        return hostelStudentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HostelStudent", "id", id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    private HostelRoom findRoom(Long id) {
        return hostelRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HostelRoom", "id", id));
    }
}
