package com.school.sms.service;

import com.school.sms.dto.request.ClassOfficialRequest;
import com.school.sms.dto.response.ClassOfficialDto;

import java.util.List;

public interface ClassOfficialService {

    /** Current holders only. */
    List<ClassOfficialDto> getCurrent(Long classId);

    /** Every holder the class has had, newest first. */
    List<ClassOfficialDto> getHistory(Long classId);

    /** Appoints a student, ending the sitting holder of the same post if there is one. */
    ClassOfficialDto appoint(Long classId, ClassOfficialRequest request);

    /** Ends an appointment without appointing a successor. */
    void end(Long classId, Long officialId);
}
