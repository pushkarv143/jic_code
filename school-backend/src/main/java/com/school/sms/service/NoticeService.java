package com.school.sms.service;

import com.school.sms.dto.request.NoticeFormRequest;
import com.school.sms.dto.response.NoticeDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface NoticeService {

    PageResponse<NoticeDto> getAll(boolean includeExpired, String targetRole, Pageable pageable);

    NoticeDto create(NoticeFormRequest request, MultipartFile file);

    NoticeDto update(Long id, NoticeFormRequest request, MultipartFile file);

    void delete(Long id);
}
