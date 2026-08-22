package com.school.sms.service.impl;

import com.school.sms.dto.response.PermissionDto;
import com.school.sms.dto.response.RoleDto;
import com.school.sms.entity.Permission;
import com.school.sms.entity.Role;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.service.RoleService;
import com.school.sms.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * The one role whose grants may not be edited.
     *
     * <p>Every other safety net in the system assumes SUPER_ADMIN holds
     * everything — {@code AppConstants.ADMIN_OVERRIDE} ORs it into permission-gated
     * endpoints precisely so that a missing grant row, which is a data problem
     * rather than a policy decision, cannot lock the administrator out. If the role
     * editor could strip SUPER_ADMIN's own ROLE_MANAGE grant, one careless save
     * would leave nobody able to grant it back and the only repair would be SQL.
     */
    private static final String PROTECTED_ROLE = AppConstants.ROLE_SUPER_ADMIN;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAll() {
        return roleRepository.findAll().stream()
                .map(role -> RoleDto.builder().id(role.getId()).name(role.getName()).description(role.getDescription()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getPermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        return permissionRepository.findAllByRoleId(role.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getPermissionCatalogue() {
        return permissionRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Permission::getModule)
                        .thenComparing(Permission::getName))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public List<PermissionDto> replacePermissions(Long roleId, Set<String> permissionNames) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (PROTECTED_ROLE.equals(role.getName())) {
            throw new BadRequestException(
                    PROTECTED_ROLE + " always holds every permission and cannot be edited");
        }

        Set<String> requested = permissionNames == null
                ? Set.of()
                : permissionNames.stream()
                        .filter(name -> name != null && !name.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // Reject unknown names rather than silently dropping them. The insert below
        // matches on name, so a typo would simply grant nothing and the caller
        // would get back a smaller set than they sent with no hint which entry
        // vanished or why.
        if (!requested.isEmpty()) {
            Set<String> known = permissionRepository.findAllByNameIn(requested).stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());
            Set<String> unknown = requested.stream()
                    .filter(name -> !known.contains(name))
                    .collect(Collectors.toCollection(TreeSet::new));
            if (!unknown.isEmpty()) {
                throw new BadRequestException("Unknown permission(s): " + String.join(", ", unknown));
            }
        }

        // Replace wholesale rather than computing a diff: the editor sends the full
        // desired set, and a diff would need a read-compare-write round trip under
        // the same lock to be correct against a concurrent save. The table holds a
        // few hundred rows, so there is nothing to gain.
        permissionRepository.deleteAllByRoleId(roleId);
        if (!requested.isEmpty()) {
            // Guarded: MySQL rejects IN () for an empty collection.
            permissionRepository.grantToRole(roleId, requested);
        }

        log.info("Role {} permissions replaced with {} grant(s)", role.getName(), requested.size());

        return getPermissions(roleId);
    }

    private PermissionDto toDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .module(permission.getModule())
                .description(permission.getDescription())
                .build();
    }
}
