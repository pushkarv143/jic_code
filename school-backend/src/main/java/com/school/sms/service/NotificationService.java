package com.school.sms.service;

import com.school.sms.dto.request.NotificationSendRequest;
import com.school.sms.dto.response.NotificationDto;
import com.school.sms.dto.response.NotificationSendResultDto;
import com.school.sms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PageResponse<NotificationDto> getMy(Pageable pageable);

    NotificationSendResultDto send(NotificationSendRequest request);
}
