package com.school.sms.service;

import com.school.sms.dto.response.PermissionDto;
import com.school.sms.dto.response.RoleDto;

import java.util.List;

public interface RoleService {

    List<RoleDto> getAll();

    List<PermissionDto> getPermissions(Long roleId);
}
