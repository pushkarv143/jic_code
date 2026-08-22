package com.school.sms.service.impl;

import com.school.sms.dto.response.HomeroomDto;
import com.school.sms.dto.response.MyAccessDto;
import com.school.sms.dto.response.OrgModuleDto;
import com.school.sms.entity.OrgModule;
import com.school.sms.entity.Permission;
import com.school.sms.entity.Role;
import com.school.sms.entity.Section;
import com.school.sms.entity.StudentStatus;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.OrgModuleRepository;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.security.HomeroomGuard;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Resolves what the signed-in user may actually do, and lets an administrator
 * change it at runtime.
 *
 * @see MyAccessDto for why the login response is not enough on its own
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessServiceImpl implements AccessService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrgModuleRepository orgModuleRepository;
    private final StudentRepository studentRepository;
    private final HomeroomGuard homeroomGuard;

    @Override
    @Transactional(readOnly = true)
    public MyAccessDto getMyAccess() {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new AccessDeniedException("No authenticated user found"));

        Set<String> disabledModules = Set.copyOf(orgModuleRepository.findDisabledModuleKeys());

        Role role = roleRepository.findByName(principal.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", principal.getRoleName()));

        // Read the grants from the database rather than from the principal: the
        // principal was built from the JWT at the start of this request, and an
        // administrator may have changed the role's grants since the token was
        // issued. This endpoint exists to be current.
        Set<String> effective = permissionRepository.findAllByRoleId(role.getId()).stream()
                .filter(permission -> !disabledModules.contains(permission.getModule()))
                .map(Permission::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> enabledModules = orgModuleRepository.findAllByOrderBySortOrderAscLabelAsc().stream()
                .filter(OrgModule::isEnabled)
                .map(OrgModule::getModuleKey)
                .collect(Collectors.toList());

        HomeroomDto homeroom = homeroomGuard.findHomeroomSection()
                .map(this::toHomeroomDto)
                .orElse(null);

        // Both conditions, deliberately. A homeroom assignment is what makes the
        // module meaningful; the module being switched on is what makes it
        // permitted. Either one alone is not enough.
        boolean classTeacherOfOwnSection = homeroom != null && !disabledModules.contains("MY_CLASS");

        return MyAccessDto.builder()
                .userId(principal.getId())
                .username(principal.getUsername())
                .role(role.getName())
                .permissions(effective)
                .enabledModules(enabledModules)
                .homeroom(homeroom)
                .classTeacherOfOwnSection(classTeacherOfOwnSection)
                .build();
    }

    private HomeroomDto toHomeroomDto(Section section) {
        var schoolClass = section.getSchoolClass();
        var academicYear = schoolClass != null ? schoolClass.getAcademicYear() : null;

        return HomeroomDto.builder()
                .sectionId(section.getId())
                .sectionName(section.getSectionName())
                .classId(schoolClass != null ? schoolClass.getId() : null)
                .className(schoolClass != null ? schoolClass.getClassName() : null)
                .academicYearId(academicYear != null ? academicYear.getId() : null)
                .academicYear(academicYear != null ? academicYear.getYearName() : null)
                .studentCount(studentRepository.countBySectionIdAndDeletedFalseAndStatus(
                        section.getId(), StudentStatus.ACTIVE))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgModuleDto> getModules() {
        Map<String, Long> countsByModule = new HashMap<>();
        for (Object[] row : permissionRepository.countGroupByModule()) {
            countsByModule.put((String) row[0], ((Number) row[1]).longValue());
        }

        return orgModuleRepository.findAllByOrderBySortOrderAscLabelAsc().stream()
                .map(module -> OrgModuleDto.builder()
                        .id(module.getId())
                        .moduleKey(module.getModuleKey())
                        .label(module.getLabel())
                        .description(module.getDescription())
                        .enabled(module.isEnabled())
                        .core(module.isCore())
                        .sortOrder(module.getSortOrder())
                        .permissionCount(countsByModule.getOrDefault(module.getModuleKey(), 0L))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<OrgModuleDto> updateModules(Map<String, Boolean> enabledByKey) {
        if (enabledByKey == null || enabledByKey.isEmpty()) {
            throw new BadRequestException("No modules supplied");
        }

        List<String> unknown = new ArrayList<>();
        List<String> refusedCore = new ArrayList<>();

        // Validate the whole request before writing any of it. A partially applied
        // toggle set is worse than a rejected one: the caller sees an error and has
        // no way to know which half took effect.
        Map<OrgModule, Boolean> pending = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : enabledByKey.entrySet()) {
            String key = entry.getKey();
            Boolean desired = entry.getValue();
            if (desired == null) {
                continue;
            }
            OrgModule module = orgModuleRepository.findByModuleKey(key).orElse(null);
            if (module == null) {
                unknown.add(key);
                continue;
            }
            if (module.isCore() && !desired) {
                refusedCore.add(module.getLabel());
                continue;
            }
            pending.put(module, desired);
        }

        if (!unknown.isEmpty()) {
            throw new BadRequestException("Unknown module(s): " + String.join(", ", unknown));
        }
        if (!refusedCore.isEmpty()) {
            throw new BadRequestException(
                    "These modules are required and cannot be switched off: " + String.join(", ", refusedCore));
        }

        pending.forEach((module, desired) -> {
            if (module.isEnabled() != desired) {
                log.info("Module {} {} by user {}", module.getModuleKey(), desired ? "enabled" : "disabled",
                        SecurityUtils.getCurrentUserPrincipal().map(UserPrincipal::getUsername).orElse("unknown"));
                module.setEnabled(desired);
            }
        });
        orgModuleRepository.saveAll(pending.keySet());

        return getModules();
    }
}
