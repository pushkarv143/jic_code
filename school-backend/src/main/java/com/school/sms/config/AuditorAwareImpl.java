package com.school.sms.config;

import com.school.sms.security.SecurityUtils;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Supplies the current authenticated user's id for @CreatedBy/@LastModifiedBy
 * style auditing. Falls back to empty (column stays null) for system/anonymous
 * operations such as the initial login itself.
 */
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        return SecurityUtils.getCurrentUserPrincipal()
                .map(principal -> principal.getId());
    }
}
