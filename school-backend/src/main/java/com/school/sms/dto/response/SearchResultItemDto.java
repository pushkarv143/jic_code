package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of a global-search dropdown result: just enough to display and link to the record. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultItemDto {

    private Long id;
    private String title;
    private String subtitle;
    private String identifier;
}
