package com.school.sms.repository;

import com.school.sms.entity.OrgModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrgModuleRepository extends JpaRepository<OrgModule, Long> {

    List<OrgModule> findAllByOrderBySortOrderAscLabelAsc();

    Optional<OrgModule> findByModuleKey(String moduleKey);

    /**
     * The keys of the modules currently switched off.
     *
     * <p>Returned as the *disabled* set rather than the enabled one on purpose: a
     * module that exists in the permission catalogue but has no registry row at
     * all must behave as enabled, not as denied. Filtering by "not in disabled"
     * gives that for free, where filtering by "in enabled" would silently hide a
     * module a later migration forgot to register.
     */
    @Query("SELECT m.moduleKey FROM OrgModule m WHERE m.enabled = false")
    List<String> findDisabledModuleKeys();
}
