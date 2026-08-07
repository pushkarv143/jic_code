package com.school.sms.service;

import com.school.sms.dto.response.BirthdayCalendarDto;

public interface CalendarService {

    BirthdayCalendarDto getBirthdays(int month);
}
