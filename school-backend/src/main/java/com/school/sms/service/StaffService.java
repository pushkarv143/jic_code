package com.school.sms.service;

import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.StaffDto;

public interface StaffService {

    PageResponse<StaffDto> search(String search, int page, int size, String sortBy, String sortDirection);
}
