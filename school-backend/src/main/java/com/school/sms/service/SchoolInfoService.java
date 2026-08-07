package com.school.sms.service;

import com.school.sms.dto.request.SchoolInfoRequest;
import com.school.sms.dto.response.SchoolInfoDto;

public interface SchoolInfoService {

    SchoolInfoDto get();

    SchoolInfoDto update(SchoolInfoRequest request);
}
