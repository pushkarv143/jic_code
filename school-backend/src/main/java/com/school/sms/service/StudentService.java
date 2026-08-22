package com.school.sms.service;

import com.school.sms.dto.request.GuardianRequest;
import com.school.sms.dto.request.MedicalDetailsRequest;
import com.school.sms.dto.request.PromoteStudentsRequest;
import com.school.sms.dto.request.StudentCreateRequest;
import com.school.sms.dto.request.StudentSelfUpdateRequest;
import com.school.sms.dto.request.StudentStatusRequest;
import com.school.sms.dto.request.StudentUpdateRequest;
import com.school.sms.dto.request.TransferStudentRequest;
import com.school.sms.dto.response.GuardianDto;
import com.school.sms.dto.response.MedicalDetailsDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudentDocumentDto;
import com.school.sms.dto.response.StudentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {

    PageResponse<StudentDto> getAll(String search, Long classId, Long sectionId, String status,
                                     int page, int size, String sortBy, String sortDirection);

    StudentDto getById(Long id);

    /** The caller's own student record, resolved from the security context. */
    StudentDto getOwnProfile();

    /** Updates the contact fields a student is permitted to maintain themselves. */
    StudentDto updateOwnProfile(StudentSelfUpdateRequest request);

    StudentDto create(StudentCreateRequest request);

    StudentDto update(Long id, StudentUpdateRequest request);

    void delete(Long id);

    /**
     * Regenerates a student's temporary password and sends it to them again.
     *
     * <p>For the case the whole provisioning flow has no other answer to: the
     * credentials email never arrived, or arrived and was lost. The password is
     * stored only as a bcrypt hash, so there is nothing to look up and re-send —
     * the only way to tell a student their password is to make a new one.
     *
     * <p>The username is left alone. It is the student's identity, a lost password
     * says nothing about it, and regenerating would collide with the existing row
     * and come back suffixed — "pushkarv" as "pushkarv1", which reads as somebody
     * else.
     *
     * <p>Every session on the account is ended. Otherwise a device already signed in
     * would keep working on a password that no longer exists, and the forced reset
     * this re-arms would apply to everyone except the person holding that device.
     */
    void resendCredentials(Long id);

    StudentDto updateStatus(Long id, StudentStatusRequest request);

    String uploadPhoto(Long id, MultipartFile file);

    List<GuardianDto> getGuardians(Long studentId);

    GuardianDto addGuardian(Long studentId, GuardianRequest request);

    GuardianDto updateGuardian(Long studentId, Long guardianId, GuardianRequest request);

    void deleteGuardian(Long studentId, Long guardianId);

    MedicalDetailsDto getMedicalDetails(Long studentId);

    MedicalDetailsDto upsertMedicalDetails(Long studentId, MedicalDetailsRequest request);

    List<StudentDocumentDto> getDocuments(Long studentId);

    StudentDocumentDto addDocument(Long studentId, String documentType, MultipartFile file);

    void deleteDocument(Long studentId, Long docId);

    int promote(PromoteStudentsRequest request);

    void transfer(Long id, TransferStudentRequest request);

    void markAlumni(Long id);
}
