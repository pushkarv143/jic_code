package com.school.sms.service;

import com.school.sms.dto.request.FeePaymentRequest;
import com.school.sms.dto.response.FeePaymentResultDto;
import com.school.sms.dto.response.FeeReceiptDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.FeePaymentDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface FeePaymentService {

    FeePaymentResultDto pay(FeePaymentRequest request);

    PageResponse<FeePaymentDto> getAll(Long studentId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    FeeReceiptDto getReceipt(Long paymentId);
}
