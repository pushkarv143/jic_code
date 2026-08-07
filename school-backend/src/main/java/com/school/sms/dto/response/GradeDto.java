package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Grade has no dedicated CRUD endpoint of its own in this system (see the
 * entity's javadoc) but is one of the reference tables the settings backup
 * export includes, so it needs a serializable shape of its own here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeDto {

    private Long id;
    private String gradeName;
    private BigDecimal minPercentage;
    private BigDecimal maxPercentage;
    private BigDecimal gradePoint;
}
