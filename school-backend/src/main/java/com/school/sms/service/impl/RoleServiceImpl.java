package com.school.sms.service.impl;

import com.school.sms.dto.response.PermissionDto;
import com.school.sms.dto.response.RoleDto;
import com.school.sms.entity.Permission;
import com.school.sms.entity.Role;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

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

    private PermissionDto toDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .module(permission.getModule())
                .description(permission.getDescription())
                .build();
    }
}
