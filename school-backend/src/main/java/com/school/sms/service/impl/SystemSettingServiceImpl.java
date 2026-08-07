package com.school.sms.service.impl;

import com.school.sms.dto.request.SystemSettingRequest;
import com.school.sms.dto.response.SystemSettingDto;
import com.school.sms.entity.SystemSetting;
import com.school.sms.repository.SystemSettingRepository;
import com.school.sms.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingDto> getAll() {
        return systemSettingRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public List<SystemSettingDto> upsertAll(List<SystemSettingRequest> requests) {
        List<SystemSetting> saved = requests.stream()
                .map(request -> {
                    SystemSetting setting = systemSettingRepository.findBySettingKey(request.getKey())
                            .orElseGet(() -> SystemSetting.builder().settingKey(request.getKey()).build());
                    setting.setSettingValue(request.getValue());
                    return systemSettingRepository.save(setting);
                })
                .toList();
        return saved.stream().map(this::toDto).toList();
    }

    private SystemSettingDto toDto(SystemSetting setting) {
        return SystemSettingDto.builder()
                .key(setting.getSettingKey())
                .value(setting.getSettingValue())
                .build();
    }
}
