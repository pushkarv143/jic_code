package com.school.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * The complete set of menus a role should be assigned after the save.
 *
 * <p>Absolute, not a delta: whatever is missing from {@code menuIds} is
 * unassigned. The screen renders every menu as a checkbox and submits the whole
 * board, so sending the desired end state avoids the client and server
 * disagreeing about which boxes changed.
 *
 * <p>An empty set is valid and means "this role sees no menu at all" — a real
 * configuration for a role that exists only to hold an account. It is
 * distinguished from a missing field by {@code @NotNull}, so a client that forgets
 * the list gets a validation error rather than silently blanking a role's menu.
 *
 * <p>Ids rather than menu keys, unlike the permission editor's names: the menu
 * catalogue is administered on the same screen, where a row is identified by the
 * id it was listed with. Keys are for code that names a specific menu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplaceRoleMenusRequest {

    @NotNull(message = "menuIds is required (send an empty array to unassign all)")
    private Set<Long> menuIds;
}
