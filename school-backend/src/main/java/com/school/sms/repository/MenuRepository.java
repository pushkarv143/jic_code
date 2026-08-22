package com.school.sms.repository;

import com.school.sms.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * Every menu, headings included, in display order.
     *
     * <p>Ordered by {@code parent_id} first so a heading's children arrive
     * together, which is what lets the tree be assembled in one pass.
     */
    @Query("SELECT m FROM Menu m ORDER BY m.parentId ASC, m.sortOrder ASC, m.label ASC")
    List<Menu> findAllOrdered();

    Optional<Menu> findByMenuKey(String menuKey);

    /**
     * The menus assigned to one role.
     *
     * <p>{@code role_menus} has no JPA entity — the same "no entity needed, just a
     * native-query join" approach {@link PermissionRepository#findAllByRoleId}
     * takes for {@code role_permissions}. The table is two foreign keys and a
     * timestamp; an entity would add a class and buy nothing.
     *
     * <p>Disabled menus are excluded here rather than filtered downstream: a menu
     * switched off is switched off for everyone, and leaving it in the result
     * would invite a caller to forget the check.
     */
    @Query(value = "SELECT m.* FROM menus m " +
            "JOIN role_menus rm ON rm.menu_id = m.id " +
            "WHERE rm.role_id = :roleId AND m.is_enabled = 1 " +
            "ORDER BY m.parent_id, m.sort_order, m.label", nativeQuery = true)
    List<Menu> findAllByRoleId(@Param("roleId") Long roleId);

    /**
     * Menu ids assigned to one role, disabled rows included.
     *
     * <p>The admin screen needs these: an assignment to a retired menu still
     * exists and must round-trip through a save rather than being silently
     * dropped because the menu happens to be switched off today.
     */
    @Query(value = "SELECT rm.menu_id FROM role_menus rm WHERE rm.role_id = :roleId",
            nativeQuery = true)
    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** Assigned-role count per menu, as {@code [menuId, count]} rows. */
    @Query(value = "SELECT rm.menu_id, COUNT(*) FROM role_menus rm GROUP BY rm.menu_id",
            nativeQuery = true)
    List<Object[]> countRolesPerMenu();

    // ---------------------------------------------------------------------
    // Writes against role_menus.
    //
    // Native and @Modifying, so a caller must be @Transactional —
    // MenuServiceImpl.replaceRoleMenus is. Replace-wholesale rather than a
    // computed diff, for the same reasons as role_permissions: the screen sends
    // the full desired set, and the table is tiny.
    // ---------------------------------------------------------------------
    @Modifying
    @Query(value = "DELETE FROM role_menus WHERE role_id = :roleId", nativeQuery = true)
    void deleteAllByRoleId(@Param("roleId") Long roleId);

    /**
     * Assigns every menu in {@code menuIds} to {@code roleId}.
     *
     * <p>Ids matching no menu row are ignored rather than rejected — validating
     * the requested set belongs to the service, which can name the unknown ones.
     * Callers must not pass an empty collection: MySQL rejects {@code IN ()}.
     */
    @Modifying
    @Query(value = "INSERT INTO role_menus (role_id, menu_id) " +
            "SELECT :roleId, m.id FROM menus m WHERE m.id IN (:menuIds)", nativeQuery = true)
    int assignToRole(@Param("roleId") Long roleId, @Param("menuIds") Collection<Long> menuIds);
}
