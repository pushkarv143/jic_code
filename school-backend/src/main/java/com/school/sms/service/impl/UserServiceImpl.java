package com.school.sms.service.impl;

import com.school.sms.dto.request.CreateUserRequest;
import com.school.sms.dto.request.UpdateUserRequest;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.dto.response.UserDto;
import com.school.sms.entity.Role;
import com.school.sms.entity.User;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.mapper.UserMapper;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.EmailService;
import com.school.sms.service.UserService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.specification.GenericSpecification;
import com.school.sms.util.specification.SearchCriteria;
import com.school.sms.util.specification.SearchOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(true);
        user.setEmailVerified(false);

        User saved = userRepository.save(user);
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getFirstName(), saved.getUsername());
        auditLogService.record("CREATE_USER", "User", saved.getId(), null, null);

        return userMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getAllUsers(String search, String role, int page, int size,
                                              String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<User> spec = Specification.where(null);

        if (StringUtils.hasText(role)) {
            spec = spec.and(new GenericSpecification<>(new SearchCriteria("role.name", SearchOperation.EQUALS, role)));
        }

        if (StringUtils.hasText(search)) {
            String term = search.trim();
            Specification<User> searchSpec = (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), "%" + term.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("lastName")), "%" + term.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("username")), "%" + term.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + term.toLowerCase() + "%")
            );
            spec = spec.and(searchSpec);
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);
        Page<UserDto> dtoPage = userPage.map(userMapper::toDto);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));

        userMapper.updateEntityFromRequest(request, user);
        user.setRole(role);

        User saved = userRepository.save(user);
        auditLogService.record("UPDATE_USER", "User", saved.getId(), null, null);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(false);
        userRepository.save(user);
        log.info("User {} soft-deleted (deactivated) rather than hard-deleted", id);
    }

    @Override
    @Transactional
    public void promoteToClassTeacher(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!AppConstants.ROLE_TEACHER.equals(user.getRole().getName())) {
            return;
        }

        Role classTeacherRole = roleRepository.findByName(AppConstants.ROLE_CLASS_TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", AppConstants.ROLE_CLASS_TEACHER));

        user.setRole(classTeacherRole);
        userRepository.save(user);
    }
}
