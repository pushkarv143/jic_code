package com.school.sms.service;

import com.school.sms.dto.request.SalaryStructureRequest;
import com.school.sms.dto.response.SalaryStructureDto;
import com.school.sms.entity.PayrollEmployeeType;

import java.util.List;

public interface SalaryStructureService {

    List<SalaryStructureDto> getAll(Long employeeId, PayrollEmployeeType employeeType);

    SalaryStructureDto create(SalaryStructureRequest request);

    SalaryStructureDto update(Long id, SalaryStructureRequest request);

    void delete(Long id);
}
