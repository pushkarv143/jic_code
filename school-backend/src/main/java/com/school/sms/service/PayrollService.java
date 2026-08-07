package com.school.sms.service;

import com.school.sms.dto.request.PayrollGenerateRequest;
import com.school.sms.dto.request.PayrollMarkPaidRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.PayrollDashboardDto;
import com.school.sms.dto.response.PayrollDto;
import com.school.sms.dto.response.PayrollGenerateResultDto;
import com.school.sms.dto.response.SalarySlipDto;
import com.school.sms.entity.PayrollEmployeeType;
import org.springframework.data.domain.Pageable;

public interface PayrollService {

    PageResponse<PayrollDto> getAll(Long employeeId, PayrollEmployeeType employeeType, Integer month, Integer year,
                                     String status, Pageable pageable);

    PayrollGenerateResultDto generate(PayrollGenerateRequest request);

    PayrollDto markPaid(Long id, PayrollMarkPaidRequest request);

    SalarySlipDto getSalarySlip(Long id);

    PayrollDashboardDto getDashboard(Integer month, Integer year);
}
