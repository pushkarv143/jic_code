package com.school.sms.service.impl;

import com.school.sms.dto.request.ClassOfficialRequest;
import com.school.sms.dto.request.MyClassOfficialRequest;
import com.school.sms.dto.request.MyClassStudentUpdateRequest;
import com.school.sms.dto.request.StudentCreateRequest;
import com.school.sms.dto.request.StudentUpdateRequest;
import com.school.sms.dto.response.ClassOfficialDto;
import com.school.sms.dto.response.HomeroomDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentDto;
import com.school.sms.entity.Section;
import com.school.sms.entity.StudentStatus;
import com.school.sms.exception.BadRequestException;
import com.school.sms.repository.StudentRepository;
import com.school.sms.security.HomeroomGuard;
import com.school.sms.service.MyClassService;
import com.school.sms.service.ClassOfficialService;
import com.school.sms.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyClassServiceImpl implements MyClassService {

    private final HomeroomGuard homeroomGuard;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final ClassOfficialService classOfficialService;

    @Override
    @Transactional(readOnly = true)
    public HomeroomDto getMyClass() {
        return toDto(homeroomGuard.requireHomeroomSection());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentDto> getMyStudents(String search, String status, int page, int size,
                                                 String sortBy, String sortDirection) {
        Section homeroom = homeroomGuard.requireHomeroomSection();

        // Delegates to the student module with the class/section pinned to the
        // caller's own homeroom. Note that StudentServiceImpl.getAll additionally
        // applies StudentAccessGuard's row scope, so this is filtered twice: once to
        // the homeroom section here, and again to the set of students the caller may
        // see at all. Belt and braces on purpose — the pinned ids are the security
        // boundary, and the guard is what catches a future refactor that loses them.
        return studentService.getAll(search, classIdOf(homeroom), homeroom.getId(), status,
                page, size, sortBy, sortDirection);
    }

    @Override
    @Transactional
    public StudentDto updateStudent(Long studentId, MyClassStudentUpdateRequest request) {
        Section homeroom = homeroomGuard.requireHomeroomSection();
        requireOnMyRoster(studentId, homeroom);

        StudentUpdateRequest delegate = new StudentUpdateRequest();
        delegate.setFirstName(request.getFirstName());
        delegate.setLastName(request.getLastName());
        delegate.setEmail(request.getEmail());
        delegate.setPhone(request.getPhone());
        delegate.setAdmissionDate(request.getAdmissionDate());
        delegate.setDateOfBirth(request.getDateOfBirth());
        delegate.setGender(request.getGender());
        delegate.setBloodGroup(request.getBloodGroup());
        delegate.setReligion(request.getReligion());
        delegate.setCategory(request.getCategory());
        delegate.setAddress(request.getAddress());
        delegate.setCity(request.getCity());
        delegate.setState(request.getState());
        delegate.setPincode(request.getPincode());

        // Pinned to where the student already is, so this endpoint cannot become an
        // unaudited transfer. StudentUpdateRequest requires all three.
        delegate.setClassId(classIdOf(homeroom));
        delegate.setSectionId(homeroom.getId());
        delegate.setAcademicYearId(academicYearIdOf(homeroom));

        return studentService.update(studentId, delegate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassOfficialDto> getOfficials() {
        Section homeroom = homeroomGuard.requireHomeroomSection();
        return classOfficialService.getCurrent(classIdOf(homeroom));
    }

    @Override
    @Transactional
    public ClassOfficialDto appointOfficial(MyClassOfficialRequest request) {
        Section homeroom = homeroomGuard.requireHomeroomSection();

        ClassOfficialRequest delegate = new ClassOfficialRequest();
        delegate.setStudentId(request.getStudentId());
        delegate.setRole(request.getRole());
        delegate.setFromDate(request.getFromDate());
        delegate.setRemarks(request.getRemarks());
        delegate.setSectionId(homeroom.getId());

        // classIdOf(homeroom) is the security boundary: ClassOfficialServiceImpl
        // refuses to appoint a student who does not belong to the class it is given,
        // so passing the caller's own class is what stops a class teacher naming
        // somebody else's student as their head boy.
        return classOfficialService.appoint(classIdOf(homeroom), delegate);
    }

    @Override
    @Transactional
    public void endOfficial(Long officialId) {
        Section homeroom = homeroomGuard.requireHomeroomSection();
        // Scoped by the class id rather than by a separate ownership check: `end`
        // resolves the appointment within the class it is handed and rejects an id
        // from anywhere else.
        classOfficialService.end(classIdOf(homeroom), officialId);
    }

    /**
     * @throws AccessDeniedException if the student is not on this section's roster
     *
     * <p>Deliberately not "not found": whether some other section's student exists
     * is not this caller's business, and a 404-versus-403 distinction here would let
     * a class teacher enumerate student ids across the school.
     */
    private void requireOnMyRoster(Long studentId, Section homeroom) {
        if (studentId == null
                || !studentRepository.existsByIdAndSectionIdAndDeletedFalse(studentId, homeroom.getId())) {
            throw new AccessDeniedException("That student is not in your class");
        }
    }

    /*
     * A section always has a class, and that class always has an academic year —
     * both are NOT NULL foreign keys. These accessors exist so that if the data ever
     * contradicts the schema the failure is a clear message here rather than a
     * NullPointerException several frames deeper inside the student module.
     */
    private Long classIdOf(Section homeroom) {
        if (homeroom.getSchoolClass() == null) {
            throw new BadRequestException("Your section is not linked to a class; contact the office");
        }
        return homeroom.getSchoolClass().getId();
    }

    private Long academicYearIdOf(Section homeroom) {
        var schoolClass = homeroom.getSchoolClass();
        if (schoolClass == null || schoolClass.getAcademicYear() == null) {
            throw new BadRequestException("Your class has no academic year set; contact the office");
        }
        return schoolClass.getAcademicYear().getId();
    }

    private HomeroomDto toDto(Section section) {
        var schoolClass = section.getSchoolClass();
        var academicYear = schoolClass != null ? schoolClass.getAcademicYear() : null;

        return HomeroomDto.builder()
                .sectionId(section.getId())
                .sectionName(section.getSectionName())
                .classId(schoolClass != null ? schoolClass.getId() : null)
                .className(schoolClass != null ? schoolClass.getClassName() : null)
                .academicYearId(academicYear != null ? academicYear.getId() : null)
                .academicYear(academicYear != null ? academicYear.getYearName() : null)
                .studentCount(studentRepository.countBySectionIdAndDeletedFalseAndStatus(
                        section.getId(), StudentStatus.ACTIVE))
                .build();
    }
}
