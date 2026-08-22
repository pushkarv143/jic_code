package com.school.sms.repository;

import com.school.sms.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    // role_permissions has no JPA entity (same "no entity needed, just a
    // native-query join" approach as StudentRepository.findAllByParentUserId).
    @Query(value = "SELECT p.* FROM permissions p " +
            "JOIN role_permissions rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id = :roleId " +
            "ORDER BY p.module, p.name", nativeQuery = true)
    List<Permission> findAllByRoleId(@Param("roleId") Long roleId);

    List<Permission> findAllByNameIn(Collection<String> names);

    /**
     * Permission count per module, as {@code [module, count]} rows.
     *
     * <p>Drives the "switching this off withdraws N permissions" label on the
     * module registry screen. Grouped in one query rather than counted per module,
     * since the screen shows every module at once.
     */
    @Query("SELECT p.module, COUNT(p) FROM Permission p GROUP BY p.module")
    List<Object[]> countGroupByModule();

    // ---------------------------------------------------------------------
    // Writes against role_permissions.
    //
    // Kept here, on the repository that already owns the join, because the table
    // has no entity of its own. Both are native and @Modifying, so a caller must
    // be @Transactional — AccessServiceImpl.replaceRolePermissions is.
    //
    // Replace-wholesale (delete then insert) rather than a computed diff: the
    // screen sends the full desired set, a diff would need a read-compare-write
    // round trip under the same lock to be correct, and the table is tiny.
    // ---------------------------------------------------------------------
    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE role_id = :roleId", nativeQuery = true)
    void deleteAllByRoleId(@Param("roleId") Long roleId);

    /**
     * Grants every permission named in {@code names} to {@code roleId}.
     *
     * <p>Names that match no permission row are ignored rather than rejected —
     * validation of the requested set belongs to the service, which can report
     * which names were unknown. Callers must not pass an empty collection: MySQL
     * rejects {@code IN ()} as a syntax error.
     */
    @Modifying
    @Query(value = "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT :roleId, p.id FROM permissions p WHERE p.name IN (:names)", nativeQuery = true)
    int grantToRole(@Param("roleId") Long roleId, @Param("names") Collection<String> names);
}
