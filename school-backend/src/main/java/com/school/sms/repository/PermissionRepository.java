package com.school.sms.repository;

import com.school.sms.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    // role_permissions has no JPA entity (same "no entity needed, just a
    // native-query join" approach as StudentRepository.findAllByParentUserId).
    @Query(value = "SELECT p.* FROM permissions p " +
            "JOIN role_permissions rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id = :roleId " +
            "ORDER BY p.module, p.name", nativeQuery = true)
    List<Permission> findAllByRoleId(@Param("roleId") Long roleId);
}
