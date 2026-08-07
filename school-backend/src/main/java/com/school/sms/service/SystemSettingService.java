package com.school.sms.service;

import com.school.sms.dto.request.SystemSettingRequest;
import com.school.sms.dto.response.SystemSettingDto;

import java.util.List;

public interface SystemSettingService {

    List<SystemSettingDto> getAll();

    List<SystemSettingDto> upsertAll(List<SystemSettingRequest> requests);
}
