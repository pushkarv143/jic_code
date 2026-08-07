package com.school.sms.service;

import com.school.sms.dto.request.ExamTypeRequest;
import com.school.sms.dto.response.ExamTypeDto;

import java.util.List;

public interface ExamTypeService {

    List<ExamTypeDto> getAll();

    ExamTypeDto create(ExamTypeRequest request);

    ExamTypeDto update(Long id, ExamTypeRequest request);

    void delete(Long id);
}
