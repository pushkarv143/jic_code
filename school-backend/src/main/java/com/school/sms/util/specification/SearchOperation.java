package com.school.sms.util.specification;

/**
 * Supported comparison operations for {@link SearchCriteria}, used by
 * {@link GenericSpecification} to build a JPA {@code Predicate} dynamically.
 * Reusable by any future module (students, teachers, fees...) that needs
 * dynamic filtering on top of Spring Data JPA Specifications.
 */
public enum SearchOperation {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_EQUAL,
    LESS_THAN,
    LESS_THAN_EQUAL,
    LIKE,
    STARTS_WITH,
    ENDS_WITH,
    CONTAINS,
    IN
}
