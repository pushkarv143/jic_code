package com.school.sms.service.impl;

import com.school.sms.entity.OtpCode;
import com.school.sms.repository.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a failed guess against a passcode, in a transaction of its own.
 *
 * The separate transaction is the entire reason this class exists. Verification
 * increments the counter and then throws to reject the caller — and a throw rolls
 * the enclosing transaction back, taking the increment with it. The counter
 * silently stayed at zero, which meant the five-attempt cap never bit and the six
 * digits could be worked through at leisure.
 *
 * REQUIRES_NEW commits the increment before the rejection unwinds, so a wrong
 * guess costs something. It has to live on a separate bean: a self-call inside
 * OtpServiceImpl would bypass the proxy and quietly do nothing, which is the same
 * bug wearing a different hat.
 */
@Component
@RequiredArgsConstructor
public class OtpAttemptRecorder {

    private final OtpCodeRepository otpCodeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long otpId) {
        otpCodeRepository.findById(otpId).ifPresent(otp -> {
            otp.setAttempts(otp.getAttempts() + 1);
            otpCodeRepository.save(otp);
        });
    }
}
