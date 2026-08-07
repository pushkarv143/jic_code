package com.school.sms.service;

import com.school.sms.dto.request.GuardianRequest;
import com.school.sms.dto.request.MedicalDetailsRequest;
import com.school.sms.dto.request.PromoteStudentsRequest;
import com.school.sms.dto.request.StudentCreateRequest;
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

    StudentDto create(StudentCreateRequest request);

    StudentDto update(Long id, StudentUpdateRequest request);

    void delete(Long id);

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
