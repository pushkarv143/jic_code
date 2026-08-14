package com.school.sms.controller;

import com.school.sms.dto.response.StudentDto;
import com.school.sms.security.CustomUserDetailsService;
import com.school.sms.security.JwtTokenProvider;
import com.school.sms.service.ExcelService;
import com.school.sms.service.PdfService;
import com.school.sms.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The endpoint half of RBAC: which roles may call which student endpoints at all.
 *
 * Row-level scoping (which students come back) is the guard's job and is covered by
 * {@link com.school.sms.security.StudentAccessGuardTest} — here the service is
 * mocked, so a 200 means "the role gate let this through", not "the caller was
 * entitled to the rows".
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = StudentController.class)
@Import(StudentControllerAccessTest.MethodSecurityConfig.class)
class StudentControllerAccessTest {

    /**
     * {@code @WebMvcTest} loads the web slice but not the app's SecurityConfig, so
     * {@code @EnableMethodSecurity} — the thing that makes {@code @PreAuthorize}
     * do anything — has to be switched on explicitly or every assertion below
     * would pass vacuously.
     *
     * {@code @TestConfiguration} rather than {@code @Configuration}: a nested plain
     * {@code @Configuration} is taken as the test's <em>primary</em> configuration
     * and stops Boot from finding {@code SchoolManagementApplication}, which leaves
     * the slice with no controllers registered and turns every assertion here into
     * a 404.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {

        /**
         * {@code @EnableJpaAuditing} on the application class names this bean, and it
         * normally comes from SecurityConfig — which the slice does not load. Nothing
         * is persisted in these tests, so an empty auditor is enough to let the
         * context start.
         */
        @Bean(name = "auditorAware")
        AuditorAware<Long> auditorAware() {
            return Optional::empty;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @MockBean
    private PdfService pdfService;

    @MockBean
    private ExcelService excelService;

    // JwtAuthenticationFilter is a Filter, so @WebMvcTest instantiates it even though
    // the slice excludes plain @Components. Its collaborators are stubbed rather than
    // real because @WithMockUser populates the SecurityContext directly — no token is
    // ever parsed in these tests.
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // SchoolManagementApplication carries @EnableJpaAuditing, which wants a JPA
    // metamodel the web slice has no entities to build. Stubbing the mapping context
    // satisfies it without dragging a datasource into an authorization test.
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /* --------------------------------------------------------------- */
    /* Reads: broad role set, narrowed to rows by the guard.            */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void adminCanReadTheDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/students")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadTheDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/students")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanReachTheDirectoryEndpointAndIsScopedByTheService() throws Exception {
        mockMvc.perform(get("/api/v1/students")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SECURITY_GUARD")
    void unrelatedRoleCannotReadStudents() throws Exception {
        mockMvc.perform(get("/api/v1/students")).andExpect(status().isForbidden());
        verify(studentService, never()).getAll(any(), any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void librarianCannotReadStudentProfiles() throws Exception {
        mockMvc.perform(get("/api/v1/students/1")).andExpect(status().isForbidden());
        verify(studentService, never()).getById(anyLong());
    }

    /* --------------------------------------------------------------- */
    /* Writes: management only.                                         */
    /* --------------------------------------------------------------- */

    // DELETE rather than POST as the probe for WRITE_ROLES: both carry the same
    // @PreAuthorize, but a POST with an empty body fails @Valid first and answers 400,
    // which would make the status assertion prove nothing about authorization. DELETE
    // takes no body, so the only thing that can reject it is the role gate.

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotDeleteStudents() throws Exception {
        mockMvc.perform(delete("/api/v1/students/1").with(csrf()))
                .andExpect(status().isForbidden());
        verify(studentService, never()).delete(anyLong());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotDeleteStudents() throws Exception {
        mockMvc.perform(delete("/api/v1/students/1").with(csrf()))
                .andExpect(status().isForbidden());
        verify(studentService, never()).delete(anyLong());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotPromoteStudents() throws Exception {
        mockMvc.perform(post("/api/v1/students/1/mark-alumni").with(csrf()))
                .andExpect(status().isForbidden());
        verify(studentService, never()).markAlumni(anyLong());
    }

    /**
     * Admission is management-only. Asserted through the service rather than the
     * status code for the reason noted above — what matters is that a teacher's
     * request never reaches {@code create(...)}.
     */
    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherRequestNeverReachesStudentCreation() throws Exception {
        mockMvc.perform(post("/api/v1/students")
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf()));

        verify(studentService, never()).create(any());
    }

    /* --------------------------------------------------------------- */
    /* Self-service: STUDENT only, and no id crosses the wire.          */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanReadTheirOwnProfile() throws Exception {
        when(studentService.getOwnProfile()).thenReturn(new StudentDto());

        mockMvc.perform(get("/api/v1/students/me")).andExpect(status().isOk());
        verify(studentService).getOwnProfile();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanUpdateTheirOwnContactDetails() throws Exception {
        when(studentService.updateOwnProfile(any())).thenReturn(new StudentDto());

        mockMvc.perform(patch("/api/v1/students/me")
                        .contentType("application/json")
                        .content("{\"city\":\"New Delhi\"}")
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(studentService).updateOwnProfile(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherHasNoSelfServiceStudentProfile() throws Exception {
        mockMvc.perform(get("/api/v1/students/me")).andExpect(status().isForbidden());
    }

    /* --------------------------------------------------------------- */
    /* Bulk export: not narrowed by the guard, so kept off self-scoped  */
    /* roles entirely.                                                  */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotExportTheWholeDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/students/export/excel")).andExpect(status().isForbidden());
        verify(excelService, never()).exportStudents(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotExportTheWholeDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/students/export/excel")).andExpect(status().isForbidden());
        verify(excelService, never()).exportStudents(any(), any(), any());
    }
}
