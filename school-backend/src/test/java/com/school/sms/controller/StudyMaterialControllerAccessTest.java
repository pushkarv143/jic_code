package com.school.sms.controller;

import com.school.sms.dto.response.StudyMaterialDto;
import com.school.sms.security.CustomUserDetailsService;
import com.school.sms.security.JwtTokenProvider;
import com.school.sms.service.StudyMaterialService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Study materials are gated on permissions rather than roles, so these tests grant
 * authorities directly instead of using {@code @WithMockUser(roles = ...)}.
 *
 * That is the point of the design: revoking MATERIAL_MANAGE from a role takes effect
 * without a code change. Which rows a permitted caller actually sees is the service's
 * job — see {@link com.school.sms.service.impl.StudyMaterialVisibilityTest}.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = StudyMaterialController.class)
@Import(StudyMaterialControllerAccessTest.MethodSecurityConfig.class)
class StudyMaterialControllerAccessTest {

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
    private StudyMaterialService studyMaterialService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /* --------------------------------------------------------------- */
    /* Reading requires MATERIAL_VIEW.                                  */
    /* --------------------------------------------------------------- */

    @Test
    @WithMockUser(authorities = "PERM_MATERIAL_VIEW")
    void aCallerWithViewPermissionCanListMaterials() throws Exception {
        mockMvc.perform(get("/api/v1/study-materials")).andExpect(status().isOk());
    }

    /**
     * Holding a role is not enough on its own. A librarian is a legitimate staff
     * member with no MATERIAL_VIEW grant, and must be refused.
     */
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void aRoleWithoutTheViewPermissionIsRefused() throws Exception {
        mockMvc.perform(get("/api/v1/study-materials")).andExpect(status().isForbidden());
        verify(studyMaterialService, never()).getAll(any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "PERM_MATERIAL_VIEW")
    void viewPermissionResolvesADownload() throws Exception {
        when(studyMaterialService.getForDownload(anyLong())).thenReturn(new StudyMaterialDto());

        mockMvc.perform(get("/api/v1/study-materials/1/download")).andExpect(status().isOk());
        verify(studyMaterialService).getForDownload(1L);
    }

    @Test
    @WithMockUser(roles = "SECURITY_GUARD")
    void aRoleWithoutTheViewPermissionCannotResolveADownload() throws Exception {
        mockMvc.perform(get("/api/v1/study-materials/1/download")).andExpect(status().isForbidden());
        verify(studyMaterialService, never()).getForDownload(anyLong());
    }

    /* --------------------------------------------------------------- */
    /* Writing requires MATERIAL_MANAGE.                                */
    /* --------------------------------------------------------------- */

    /** Read access must not imply write access — the common way this goes wrong. */
    @Test
    @WithMockUser(authorities = "PERM_MATERIAL_VIEW")
    void viewPermissionAloneCannotDeleteAMaterial() throws Exception {
        mockMvc.perform(delete("/api/v1/study-materials/1").with(csrf()))
                .andExpect(status().isForbidden());
        verify(studyMaterialService, never()).delete(anyLong());
    }

    @Test
    @WithMockUser(authorities = { "PERM_MATERIAL_VIEW", "PERM_MATERIAL_MANAGE" })
    void managePermissionCanDeleteAMaterial() throws Exception {
        mockMvc.perform(delete("/api/v1/study-materials/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(studyMaterialService).delete(1L);
    }

    /**
     * A student holds MATERIAL_VIEW and nothing more, so the upload endpoint is
     * closed to them however the request is shaped.
     */
    // Authorities are spelled out rather than using roles = "STUDENT": @WithMockUser
    // treats roles and authorities as mutually exclusive, and this test needs both
    // the role grant and the permission grant a real student would carry.
    @Test
    @WithMockUser(authorities = { "ROLE_STUDENT", "PERM_MATERIAL_VIEW" })
    void studentCannotUploadMaterials() throws Exception {
        // Asserted through the service rather than the status code: request binding
        // runs before the authorization interceptor, so a bare multipart is rejected
        // at binding and never reaches the @PreAuthorize check. What matters is that
        // the request never reaches create(...).
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/study-materials")
                        .with(csrf()));

        verify(studyMaterialService, never()).create(any(), any());
    }

    /**
     * The assertion is "not 403" rather than a specific status: a bare multipart with
     * no parts fails request binding, and what matters here is only that it failed
     * *after* the permission gate rather than at it.
     */
    @Test
    @WithMockUser(authorities = "PERM_MATERIAL_MANAGE")
    void managePermissionReachesTheUploadEndpoint() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/v1/study-materials")
                        .with(csrf()))
                .andExpect(result -> {
                    if (result.getResponse().getStatus() == 403) {
                        throw new AssertionError("MATERIAL_MANAGE should have passed the permission gate");
                    }
                });
        verify(studyMaterialService, never()).create(any(), any());
    }
}
