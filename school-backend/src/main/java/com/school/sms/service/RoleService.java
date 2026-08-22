package com.school.sms.service;

import com.school.sms.dto.response.PermissionDto;
import com.school.sms.dto.response.RoleDto;

import java.util.List;
import java.util.Set;

public interface RoleService {

    List<RoleDto> getAll();

    List<PermissionDto> getPermissions(Long roleId);

    /** Every permission that exists, so a role editor can offer the full catalogue. */
    List<PermissionDto> getPermissionCatalogue();

    /**
     * Replaces a role's grants wholesale with {@code permissionNames}.
     *
     * @return the role's grants after the write
     */
    List<PermissionDto> replacePermissions(Long roleId, Set<String> permissionNames);
}
