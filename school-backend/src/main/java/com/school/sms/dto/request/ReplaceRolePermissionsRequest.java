package com.school.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * The complete set of permissions a role should hold after the save.
 *
 * <p>Absolute, not a delta: whatever is missing from {@code permissions} is
 * revoked. The role editor renders every permission as a checkbox and submits the
 * whole board, so sending the desired end state avoids the client and server
 * disagreeing about which boxes changed.
 *
 * <p>An empty set is valid and means "revoke everything for this role". It is
 * distinguished from a missing field by {@code @NotNull}, so a client that forgets
 * to send the list gets a validation error rather than silently stripping a role.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplaceRolePermissionsRequest {

    @NotNull(message = "permissions is required (send an empty array to revoke all)")
    private Set<String> permissions;
}
