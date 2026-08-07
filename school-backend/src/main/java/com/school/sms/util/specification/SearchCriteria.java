package com.school.sms.util.specification;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A single filter condition: field name (may be dotted for a nested/joined
 * property e.g. "role.name"), the comparison operation, and the value to
 * compare against.
 */
@Getter
@AllArgsConstructor
public class SearchCriteria {

    private final String key;
    private final SearchOperation operation;
    private final Object value;
}
