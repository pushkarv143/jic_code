package com.school.sms.service.impl;

import com.school.sms.dto.response.MenuDto;
import com.school.sms.entity.Menu;
import com.school.sms.exception.BadRequestException;
import com.school.sms.repository.MenuRepository;
import com.school.sms.repository.OrgModuleRepository;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.security.HomeroomGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Which menus survive the filter, and why.
 *
 * <p>The point of the menu tables is that the assignment is data. The point of
 * these tests is that the assignment still cannot hand anybody access: every gate
 * below can hide a menu a role has been explicitly given, and one of them can do
 * it to SUPER_ADMIN.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MenuVisibilityTest {

    private static final Long ROLE_ID = 5L;

    @Mock private MenuRepository menuRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private OrgModuleRepository orgModuleRepository;
    @Mock private HomeroomGuard homeroomGuard;

    @InjectMocks private MenuServiceImpl service;

    private static final Long ACADEMICS_ID = 100L;

    private static Menu heading(Long id, String key) {
        Menu menu = Menu.builder().menuKey(key).label(key).sortOrder(10).enabled(true).build();
        menu.setId(id);
        return menu;
    }

    private static Menu entry(Long id, String key, Long parentId) {
        Menu menu = Menu.builder()
                .menuKey(key).label(key).path("/app/" + key.toLowerCase())
                .parentId(parentId).sortOrder(10).enabled(true)
                .build();
        menu.setId(id);
        return menu;
    }

    /** Convenience: a non-admin role, so the permission gate is not bypassed. */
    private List<MenuDto> visibleFor(List<Menu> assigned,
                                     Set<String> permissions,
                                     Set<String> enabledModules,
                                     boolean holdsHomeroom) {
        when(menuRepository.findAllByRoleId(ROLE_ID)).thenReturn(assigned);
        return service.getMenusFor(ROLE_ID, "TEACHER", permissions, enabledModules, holdsHomeroom);
    }

    @Nested
    @DisplayName("the four gates")
    class Gates {

        @Test
        @DisplayName("an assigned entry with every gate satisfied is offered, under its heading")
        void assignedAndPermittedIsOffered() {
            Menu students = entry(1L, "STUDENTS", ACADEMICS_ID);
            students.setModuleKey("STUDENT");
            students.setRequiredPermission("STUDENT_VIEW");

            List<MenuDto> tree = visibleFor(
                    List.of(heading(ACADEMICS_ID, "SECTION_ACADEMICS"), students),
                    Set.of("STUDENT_VIEW"), Set.of("STUDENT"), false);

            assertThat(tree).singleElement()
                    .satisfies(section -> {
                        assertThat(section.getMenuKey()).isEqualTo("SECTION_ACADEMICS");
                        assertThat(section.getChildren()).extracting(MenuDto::getMenuKey)
                                .containsExactly("STUDENTS");
                    });
        }

        @Test
        @DisplayName("a disabled module hides the entry even though the role is assigned it")
        void disabledModuleHidesAnAssignedEntry() {
            Menu hostel = entry(2L, "HOSTEL", ACADEMICS_ID);
            hostel.setModuleKey("HOSTEL");

            List<MenuDto> tree = visibleFor(
                    List.of(heading(ACADEMICS_ID, "SECTION_ACADEMICS"), hostel),
                    Set.of(), Set.of("STUDENT"), false);

            // The heading goes with its only child rather than rendering empty.
            assertThat(tree).isEmpty();
        }

        @Test
        @DisplayName("a disabled module hides the entry from SUPER_ADMIN too")
        void theModuleGateAppliesToAdminAsWell() {
            Menu hostel = entry(2L, "HOSTEL", ACADEMICS_ID);
            hostel.setModuleKey("HOSTEL");
            when(menuRepository.findAllByRoleId(ROLE_ID))
                    .thenReturn(List.of(heading(ACADEMICS_ID, "SECTION_ACADEMICS"), hostel));

            // A school that runs no hostel wants no Hostel entry on anybody's menu.
            // This is the one gate the administrator does not bypass.
            List<MenuDto> tree = service.getMenusFor(
                    ROLE_ID, "SUPER_ADMIN", Set.of(), Set.of("STUDENT"), false);

            assertThat(tree).isEmpty();
        }

        @Test
        @DisplayName("My Class is hidden from a class teacher who holds no section")
        void homeroomGateHidesMyClassWithoutAnAssignment() {
            Menu myClass = entry(3L, "MY_CLASS", ACADEMICS_ID);
            myClass.setModuleKey("MY_CLASS");
            myClass.setRequiredPermission("MY_CLASS_VIEW");
            myClass.setRequiresHomeroom(true);

            List<Menu> assigned = List.of(heading(ACADEMICS_ID, "SECTION_ACADEMICS"), myClass);

            // Far more users carry the CLASS_TEACHER label than hold a section, so
            // the role is not the signal - the assignment is.
            assertThat(visibleFor(assigned, Set.of("MY_CLASS_VIEW"), Set.of("MY_CLASS"), false))
                    .isEmpty();
            assertThat(visibleFor(assigned, Set.of("MY_CLASS_VIEW"), Set.of("MY_CLASS"), true))
                    .singleElement()
                    .satisfies(section -> assertThat(section.getChildren())
                            .extracting(MenuDto::getMenuKey).containsExactly("MY_CLASS"));
        }

        @Test
        @DisplayName("an assigned entry whose permission the role lacks shows nothing")
        void assignmentDoesNotGrantAccess() {
            Menu teachers = entry(4L, "TEACHERS", ACADEMICS_ID);
            teachers.setModuleKey("TEACHER");
            teachers.setRequiredPermission("TEACHER_VIEW");

            // This is the real state of TEACHER: it is assigned the Teachers menu
            // and does not hold TEACHER_VIEW. Showing the entry would put a teacher
            // one click from a 403.
            assertThat(visibleFor(
                    List.of(heading(ACADEMICS_ID, "SECTION_ACADEMICS"), teachers),
                    Set.of("STUDENT_VIEW"), Set.of("TEACHER"), false))
                    .isEmpty();
        }

        @Test
        @DisplayName("SUPER_ADMIN bypasses the permission gate")
        void adminSkipsThePermissionGate() {
            Menu users = entry(5L, "USERS", ACADEMICS_ID);
            users.setModuleKey("USER");
            users.setRequiredPermission("USER_VIEW");
            when(menuRepository.findAllByRoleId(ROLE_ID))
                    .thenReturn(List.of(heading(ACADEMICS_ID, "SECTION_ACADEMICS"), users));

            List<MenuDto> tree = service.getMenusFor(
                    ROLE_ID, "SUPER_ADMIN", Set.of(), Set.of("USER"), false);

            assertThat(tree).singleElement()
                    .satisfies(section -> assertThat(section.getChildren())
                            .extracting(MenuDto::getMenuKey).containsExactly("USERS"));
        }

        @Test
        @DisplayName("a menu the role is not assigned never appears, whatever it holds")
        void unassignedNeverAppears() {
            // findAllByRoleId is the assignment gate: it returns only assigned rows,
            // so an unassigned Payroll cannot be reached by holding its permission.
            List<MenuDto> tree = visibleFor(
                    List.of(heading(ACADEMICS_ID, "SECTION_ACADEMICS")),
                    Set.of("PAYROLL_VIEW", "PAYROLL_MANAGE"), Set.of("PAYROLL"), false);

            assertThat(tree).isEmpty();
        }

        @Test
        @DisplayName("a role assigned nothing gets an empty menu rather than a default one")
        void noAssignmentMeansNoMenu() {
            assertThat(visibleFor(List.of(), Set.of("STUDENT_VIEW"), Set.of("STUDENT"), false))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("assigning menus to a role")
    class Assignment {

        @Test
        @DisplayName("an unknown menu id is refused before anything is written")
        void unknownIdIsRefusedWholesale() {
            when(roleRepository.findById(ROLE_ID))
                    .thenReturn(java.util.Optional.of(new com.school.sms.entity.Role()));
            Menu known = entry(1L, "STUDENTS", ACADEMICS_ID);
            when(menuRepository.findAllById(Set.of(1L, 999L))).thenReturn(List.of(known));

            assertThatThrownBy(() -> service.replaceRoleMenus(ROLE_ID, Set.of(1L, 999L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("999");

            // A partially applied assignment is worse than a rejected one: the caller
            // sees an error and cannot tell which half took effect.
            verify(menuRepository, never()).deleteAllByRoleId(anyLong());
        }

        @Test
        @DisplayName("an empty set unassigns everything without running an IN () insert")
        void emptySetClearsTheMenu() {
            when(roleRepository.findById(ROLE_ID))
                    .thenReturn(java.util.Optional.of(new com.school.sms.entity.Role()));
            when(menuRepository.findAllById(Set.of())).thenReturn(List.of());
            when(menuRepository.findMenuIdsByRoleId(ROLE_ID)).thenReturn(List.of());

            assertThat(service.replaceRoleMenus(ROLE_ID, Set.of())).isEmpty();

            verify(menuRepository).deleteAllByRoleId(ROLE_ID);
            // MySQL rejects IN () as a syntax error, so the insert has to be skipped.
            verify(menuRepository, never()).assignToRole(anyLong(), org.mockito.ArgumentMatchers.anyCollection());
        }
    }
}
