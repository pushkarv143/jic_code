package com.school.sms.util.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates {@link SearchCriteria} and combines them into a single AND-ed
 * {@link Specification}. Any future module can reuse this to build dynamic
 * "list with filters" queries without hand-rolling Criteria API code.
 */
public class SpecificationBuilder<T> {

    private final List<SearchCriteria> params = new ArrayList<>();

    public SpecificationBuilder<T> with(String key, SearchOperation operation, Object value) {
        if (value != null && !(value instanceof String s && s.isBlank())) {
            params.add(new SearchCriteria(key, operation, value));
        }
        return this;
    }

    public SpecificationBuilder<T> with(boolean condition, String key, SearchOperation operation, Object value) {
        if (condition) {
            with(key, operation, value);
        }
        return this;
    }

    public Specification<T> build() {
        if (params.isEmpty()) {
            return null;
        }
        Specification<T> result = new GenericSpecification<>(params.get(0));
        for (int i = 1; i < params.size(); i++) {
            result = Specification.where(result).and(new GenericSpecification<>(params.get(i)));
        }
        return result;
    }
}
