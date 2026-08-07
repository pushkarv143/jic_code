package com.school.sms.controller;

import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.BirthdayCalendarDto;
import com.school.sms.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Calendar", description = "Birthday calendar widget for students and teachers")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/birthdays")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List students/teachers whose birthday falls in the given month (1-12), ordered by day of month")
    public ResponseEntity<ApiResponse<BirthdayCalendarDto>> getBirthdays(@RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success("Birthdays retrieved successfully", calendarService.getBirthdays(month)));
    }
}
