package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSearchResultDto {

    private List<SearchResultItemDto> students;
    private List<SearchResultItemDto> teachers;
    private List<SearchResultItemDto> books;
}
