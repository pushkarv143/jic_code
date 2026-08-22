package com.school.sms.service;

import com.school.sms.dto.response.MyAccessDto;
import com.school.sms.dto.response.OrgModuleDto;

import java.util.List;
import java.util.Map;

public interface AccessService {

    /** Effective role, module-filtered permissions, enabled modules and homeroom for the caller. */
    MyAccessDto getMyAccess();

    /** The module registry, ordered for display. */
    List<OrgModuleDto> getModules();

    /**
     * Switches modules on/off by key.
     *
     * @param enabledByKey module key to desired state; keys absent from the map are left alone
     * @return the registry after the update
     */
    List<OrgModuleDto> updateModules(Map<String, Boolean> enabledByKey);
}
