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

}
