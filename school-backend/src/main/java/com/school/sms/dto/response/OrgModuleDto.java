package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row of the module registry, as the settings screen shows it.
 *
 * <p>{@code core} is sent so the UI can render the toggle disabled with a reason,
 * rather than offering a switch the API will reject.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgModuleDto {

    private Long id;
    private String moduleKey;
    private String label;
    private String description;
    private boolean enabled;
    private boolean core;
    private int sortOrder;
    /** How many permissions belong to this module — what switching it off would withdraw. */
    private long permissionCount;
}
