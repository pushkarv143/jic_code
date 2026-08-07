package com.school.sms.service.impl;

import com.school.sms.dto.request.NoticeFormRequest;
import com.school.sms.dto.response.NoticeDto;
import com.school.sms.dto.response.PageResponse;
import com.school.sms.entity.Notice;
import com.school.sms.entity.Role;
import com.school.sms.entity.User;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.repository.NoticeRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.SecurityUtils;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.FileStorageService;
import com.school.sms.service.NoticeService;
import com.school.sms.util.AppConstants;
import com.school.sms.util.NameUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private static final String ROLE_PREFIX = AppConstants.JWT_ROLE_PREFIX;
    private static final Set<String> ADMIN_ROLES = Set.of(
            ROLE_PREFIX + AppConstants.ROLE_SUPER_ADMIN,
            ROLE_PREFIX + AppConstants.ROLE_PRINCIPAL,
            ROLE_PREFIX + AppConstants.ROLE_VICE_PRINCIPAL
    );

    private final NoticeRepository noticeRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NoticeDto> getAll(boolean includeExpired, String targetRole, Pageable pageable) {
        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("No authenticated user found"));
        boolean isAdmin = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ADMIN_ROLES::contains);
        boolean adminOverride = isAdmin && (includeExpired || StringUtils.hasText(targetRole));

        Specification<Notice> spec;
        if (adminOverride) {
            spec = StringUtils.hasText(targetRole)
                    ? (root, query, cb) -> cb.equal(
                            root.join("targetRole", JoinType.LEFT).get("name"), targetRole.toUpperCase())
                    : null;
        } else {
            User currentUser = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
            String callerRoleName = currentUser.getRole().getName();
            LocalDate today = LocalDate.now();
            spec = (root, query, cb) -> {
                Join<Notice, Role> roleJoin = root.join("targetRole", JoinType.LEFT);
                Predicate roleOk = cb.or(cb.isNull(root.get("targetRole")), cb.equal(roleJoin.get("name"), callerRoleName));
                Predicate expiryOk = cb.or(cb.isNull(root.get("expiryDate")), cb.greaterThanOrEqualTo(root.get("expiryDate"), today));
                return cb.and(roleOk, expiryOk);
            };
        }

        Page<Notice> page = noticeRepository.findAll(spec, pageable);

        Map<Long, User> publishersById = userRepository.findAllById(
                page.getContent().stream().map(Notice::getPublishedBy).filter(java.util.Objects::nonNull).toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return PageResponse.from(page, page.getContent().stream().map(n -> toDto(n, publishersById)).toList());
    }

    @Override
    @Transactional
    public NoticeDto create(NoticeFormRequest request, MultipartFile file) {
        Role targetRole = resolveTargetRole(request.getTargetRole());
        String attachmentUrl = (file != null && !file.isEmpty()) ? fileStorageService.storeDocument(file) : null;

        Notice notice = Notice.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .targetRole(targetRole)
                .publishedBy(SecurityUtils.getCurrentUserId())
                .publishedAt(LocalDateTime.now())
                .expiryDate(request.getExpiryDate())
                .attachmentUrl(attachmentUrl)
                .build();

        return toDto(noticeRepository.save(notice));
    }

    @Override
    @Transactional
    public NoticeDto update(Long id, NoticeFormRequest request, MultipartFile file) {
        Notice notice = findEntity(id);
        notice.setTitle(request.getTitle());
        notice.setDescription(request.getDescription());
        notice.setTargetRole(resolveTargetRole(request.getTargetRole()));
        notice.setExpiryDate(request.getExpiryDate());

        if (file != null && !file.isEmpty()) {
            String oldUrl = notice.getAttachmentUrl();
            notice.setAttachmentUrl(fileStorageService.storeDocument(file));
            if (StringUtils.hasText(oldUrl)) {
                fileStorageService.delete(oldUrl);
            }
        }

        return toDto(noticeRepository.save(notice));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Notice notice = findEntity(id);
        if (StringUtils.hasText(notice.getAttachmentUrl())) {
            fileStorageService.delete(notice.getAttachmentUrl());
        }
        noticeRepository.delete(notice);
    }

    private Role resolveTargetRole(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return null;
        }
        return roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
    }

    private NoticeDto toDto(Notice notice) {
        Map<Long, User> publisherMap = notice.getPublishedBy() != null
                ? userRepository.findById(notice.getPublishedBy()).map(u -> Map.of(u.getId(), u)).orElse(Map.of())
                : Map.of();
        return toDto(notice, publisherMap);
    }

    private NoticeDto toDto(Notice notice, Map<Long, User> publishersById) {
        Role targetRole = notice.getTargetRole();
        User publisher = notice.getPublishedBy() != null ? publishersById.get(notice.getPublishedBy()) : null;
        String publishedByName = publisher != null ? NameUtil.fullName(publisher.getFirstName(), publisher.getLastName()) : null;

        return NoticeDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .description(notice.getDescription())
                .targetRoleId(targetRole != null ? targetRole.getId() : null)
                .targetRoleName(targetRole != null ? targetRole.getName() : null)
                .publishedBy(notice.getPublishedBy())
                .publishedByName(publishedByName)
                .publishedAt(notice.getPublishedAt())
                .expiryDate(notice.getExpiryDate())
                .attachmentUrl(notice.getAttachmentUrl())
                .build();
    }

    private Notice findEntity(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", "id", id));
    }
}
