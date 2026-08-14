package com.school.sms.controller;

import com.school.sms.dto.response.StudentAttendanceSummaryDto;
import com.school.sms.security.CustomUserDetailsService;
import com.school.sms.security.JwtTokenProvider;
import com.school.sms.service.StudentAttendanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Which roles may call which attendance endpoints. Section scoping and student
 * membership are enforced in the service and covered by
 * {@link com.school.sms.security.SectionAccessGuardTest} and
 * {@link com.school.sms.service.impl.StudentAttendanceMarkingTest}; the service is
 * mocked here, so a 200 only means the role gate let the call through.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = StudentAttendanceController.class)
@Import(StudentAttendanceControllerAccessTest.MethodSecurityConfig.class)
class StudentAttendanceControllerAccessTest {

    /** See StudentControllerAccessTest for why this is @TestConfiguration, not @Configuration. */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {

        @Bean(name = "auditorAware")
        AuditorAware<Long> auditorAware() {
            return Optional::empty;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentAttendanceService studentAttendanceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /* --------------------------------------------------------------- */
    /* The marking grid: staff only, section-scoped in the service.     */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReachTheMarkingGrid() throws Exception {
        when(studentAttendanceService.getGrid(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/attendance/students")
                        .param("classId", "5")
                        .param("sectionId", "50")
                        .param("date", "2026-08-10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotReachTheMarkingGrid() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/students")
                        .param("classId", "5")
                        .param("sectionId", "50")
                        .param("date", "2026-08-10"))
                .andExpect(status().isForbidden());
        verify(studentAttendanceService, never()).getGrid(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void parentCannotReachTheMonthlyGrid() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/students/monthly")
                        .param("classId", "5")
                        .param("sectionId", "50")
                        .param("year", "2026")
                        .param("month", "8"))
                .andExpect(status().isForbidden());
        verify(studentAttendanceService, never()).getMonthly(any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    /* --------------------------------------------------------------- */
    /* Marking: teaching and management roles only.                     */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotMarkAttendance() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/students/mark")
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf()));

        verify(studentAttendanceService, never()).mark(any());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCannotMarkAttendance() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/students/mark")
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf()));

        verify(studentAttendanceService, never()).mark(any());
    }

    /* --------------------------------------------------------------- */
    /* Reports: broad role set, narrowed to rows in the service.        */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanReachTheReportAndIsScopedByTheService() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/students/report")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SECURITY_GUARD")
    void unrelatedRoleCannotReachTheReport() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/students/report")).andExpect(status().isForbidden());
    }

    /* --------------------------------------------------------------- */
    /* Self-service summary: STUDENT only, and no id crosses the wire.  */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanReadTheirOwnSummaryWithoutAnId() throws Exception {
        when(studentAttendanceService.getOwnSummary(any(), any()))
                .thenReturn(new StudentAttendanceSummaryDto());

        mockMvc.perform(get("/api/v1/attendance/students/me/summary")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk());
        verify(studentAttendanceService).getOwnSummary(any(), any());
    }

    /** "me" must route to the self-service handler, not be parsed as a {studentId}. */
    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherHasNoSelfServiceAttendanceSummary() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/students/me/summary")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isForbidden());
        verify(studentAttendanceService, never()).getOwnSummary(any(), any());
        verify(studentAttendanceService, never()).getSummary(any(), any(), any());
    }
}
