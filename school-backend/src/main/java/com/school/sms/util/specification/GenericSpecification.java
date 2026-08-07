package com.school.sms.util.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

/**
 * A generic, reusable {@link Specification} implementation driven by a single
 * {@link SearchCriteria}. Supports dotted keys (e.g. "role.name") to reach
 * properties across a to-one association without any entity-specific code.
 * Combine several instances with {@link SpecificationBuilder} for AND-ed
 * dynamic filtering — the pattern any future module (students, teachers,
 * fees...) can reuse for search/filter endpoints.
 */
public class GenericSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;

    public GenericSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Path<?> path = resolvePath(root, criteria.getKey());
        Object value = criteria.getValue();

        return switch (criteria.getOperation()) {
            case EQUALS -> builder.equal(path, value);
            case NOT_EQUALS -> builder.notEqual(path, value);
            case GREATER_THAN -> builder.greaterThan((Path<Comparable>) path, (Comparable) value);
            case GREATER_THAN_EQUAL -> builder.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
            case LESS_THAN -> builder.lessThan((Path<Comparable>) path, (Comparable) value);
            case LESS_THAN_EQUAL -> builder.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
            case LIKE -> builder.like(builder.lower(path.as(String.class)), value.toString().toLowerCase());
            case STARTS_WITH -> builder.like(builder.lower(path.as(String.class)), value.toString().toLowerCase() + "%");
            case ENDS_WITH -> builder.like(builder.lower(path.as(String.class)), "%" + value.toString().toLowerCase());
            case CONTAINS -> builder.like(builder.lower(path.as(String.class)), "%" + value.toString().toLowerCase() + "%");
            case IN -> path.in((Collection<?>) value);
        };
    }

    private Path<?> resolvePath(Root<T> root, String key) {
        if (!key.contains(".")) {
            return root.get(key);
        }
        String[] parts = key.split("\\.");
        Path<?> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }
}
