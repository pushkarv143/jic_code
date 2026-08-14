package com.school.sms.service;

import com.school.sms.dto.request.StudyMaterialRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StudyMaterialDto;
import org.springframework.web.multipart.MultipartFile;

public interface StudyMaterialService {

    /** Materials visible to the caller, narrowed by their role. */
    PageResponse<StudyMaterialDto> getAll(Long classId, Long sectionId, Long subjectId, String materialType,
                                           String search, int page, int size, String sortBy, String sortDirection);

    StudyMaterialDto getById(Long id);

    StudyMaterialDto create(StudyMaterialRequest request, MultipartFile file);

    StudyMaterialDto update(Long id, StudyMaterialRequest request, MultipartFile file);

    void delete(Long id);

    /**
     * Resolves a material for download, applying the same visibility check as the
     * read endpoints so a direct link cannot be used to reach past them.
     */
    StudyMaterialDto getForDownload(Long id);
}
