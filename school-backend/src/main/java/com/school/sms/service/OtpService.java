package com.school.sms.service;

import com.school.sms.dto.request.SendOtpRequest;
import com.school.sms.dto.request.VerifyOtpRequest;
import com.school.sms.dto.response.OtpSendResponse;
import com.school.sms.dto.response.OtpVerifyResponse;

public interface OtpService {

    /**
     * Issues a code and sends it, or quietly does nothing when the destination
     * belongs to no account. The response is the same either way — see
     * {@link OtpSendResponse} for why.
     *
     * @param clientIp used only for throttling; never stored
     */
    OtpSendResponse send(SendOtpRequest request, String clientIp);

    /**
     * Checks a code and spends it. Every failure — unknown destination, expired,
     * already used, too many attempts, simply wrong — produces the same message,
     * so the reply cannot be used to map out which accounts exist.
     */
    OtpVerifyResponse verify(VerifyOtpRequest request);
}
