package com.school.sms.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Module keys mapped to the state they should be in.
 *
 * <p>A partial map, unlike {@link ReplaceRolePermissionsRequest}: modules absent
 * from it are left as they are. The settings screen can therefore send just the
 * toggle the user flipped, and two administrators editing different modules do not
 * overwrite each other.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrgModulesRequest {

    @NotEmpty(message = "At least one module must be supplied")
    private Map<String, Boolean> modules;
}
