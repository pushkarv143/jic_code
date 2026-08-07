package com.school.sms.service;

import com.school.sms.dto.request.CreateUserRequest;
import com.school.sms.dto.request.UpdateUserRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.UserDto;

public interface UserService {

    UserDto createUser(CreateUserRequest request);

    UserDto getUserById(Long id);

    PageResponse<UserDto> getAllUsers(String search, String role, int page, int size, String sortBy, String sortDirection);

    UserDto updateUser(Long id, UpdateUserRequest request);

    void activateUser(Long id);

    void deactivateUser(Long id);

    void deleteUser(Long id);

    /**
     * Promotes a user's role from TEACHER to CLASS_TEACHER when they are
     * assigned as a section's class teacher. No-ops if the user's current
     * role is anything other than plain TEACHER (e.g. already CLASS_TEACHER,
     * or a VICE_PRINCIPAL/PRINCIPAL who also teaches).
     */
    void promoteToClassTeacher(Long userId);
}
