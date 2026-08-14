package com.school.sms.controller;

import com.school.sms.dto.response.TeacherDto;
import com.school.sms.security.CustomUserDetailsService;
import com.school.sms.security.JwtTokenProvider;
import com.school.sms.service.ExcelService;
import com.school.sms.service.PdfService;
import com.school.sms.service.TeacherService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Which roles may call which teacher endpoints. Field-level redaction — what comes
 * back inside an allowed response — is the guard's job and is covered by
 * {@link com.school.sms.security.TeacherAccessGuardTest}; the service is mocked
 * here, so a 200 only means the role gate let the call through.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TeacherController.class)
@Import(TeacherControllerAccessTest.MethodSecurityConfig.class)
class TeacherControllerAccessTest {

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
    private TeacherService teacherService;

    @MockBean
    private PdfService pdfService;

    @MockBean
    private ExcelService excelService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /* --------------------------------------------------------------- */
    /* Reads: the whole staff directory, redacted per row.              */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void adminCanReadTheStaffDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadTheStaffDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotReadTheStaffDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")).andExpect(status().isForbidden());
        verify(teacherService, never()).getAll(any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    @WithMockUser(roles = "PARENT")
    void parentCannotReadTeacherProfiles() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/1")).andExpect(status().isForbidden());
        verify(teacherService, never()).getById(anyLong());
    }

    /* --------------------------------------------------------------- */
    /* Writes: management only.                                         */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotDeleteColleagues() throws Exception {
        mockMvc.perform(delete("/api/v1/teachers/1").with(csrf()))
                .andExpect(status().isForbidden());
        verify(teacherService, never()).delete(anyLong());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void accountantCannotDeleteTeachers() throws Exception {
        mockMvc.perform(delete("/api/v1/teachers/1").with(csrf()))
                .andExpect(status().isForbidden());
        verify(teacherService, never()).delete(anyLong());
    }

    /* --------------------------------------------------------------- */
    /* Self-service: teaching roles only, and no id crosses the wire.   */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadTheirOwnProfile() throws Exception {
        when(teacherService.getOwnProfile()).thenReturn(new TeacherDto());

        mockMvc.perform(get("/api/v1/teachers/me")).andExpect(status().isOk());
        verify(teacherService).getOwnProfile();
    }

    @Test
    @WithMockUser(roles = "CLASS_TEACHER")
    void classTeacherCanReadTheirOwnProfile() throws Exception {
        when(teacherService.getOwnProfile()).thenReturn(new TeacherDto());

        mockMvc.perform(get("/api/v1/teachers/me")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanUpdateTheirOwnContactDetails() throws Exception {
        when(teacherService.updateOwnProfile(any())).thenReturn(new TeacherDto());

        mockMvc.perform(patch("/api/v1/teachers/me")
                        .contentType("application/json")
                        .content("{\"city\":\"New Delhi\"}")
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(teacherService).updateOwnProfile(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanListTheirOwnAssignments() throws Exception {
        when(teacherService.getOwnAssignments()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/teachers/me/assignments")).andExpect(status().isOk());
        verify(teacherService).getOwnAssignments();
    }

    /**
     * Management has no teacher record of their own, so /me would have nothing to
     * resolve. They read colleagues by id instead, unredacted.
     */
    @Test
    @WithMockUser(roles = "PRINCIPAL")
    void managementHasNoSelfServiceTeacherProfile() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me")).andExpect(status().isForbidden());
        verify(teacherService, never()).getOwnProfile();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentHasNoSelfServiceTeacherProfile() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me")).andExpect(status().isForbidden());
    }

    /* --------------------------------------------------------------- */
    /* Bulk export: unredacted, so kept off the directory-only roles.   */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotExportTheStaffListWithSalaries() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/export/excel")).andExpect(status().isForbidden());
        verify(excelService, never()).exportTeachers();
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCannotExportTheStaffListWithSalaries() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/export/excel")).andExpect(status().isForbidden());
        verify(excelService, never()).exportTeachers();
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void accountantCanExportTheStaffListForPayroll() throws Exception {
        when(excelService.exportTeachers()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/teachers/export/excel")).andExpect(status().isOk());
    }
}
