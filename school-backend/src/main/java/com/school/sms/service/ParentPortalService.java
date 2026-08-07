package com.school.sms.service;

import com.school.sms.dto.response.ChildDto;

import java.util.List;

public interface ParentPortalService {

    List<ChildDto> getMyChildren();
}
