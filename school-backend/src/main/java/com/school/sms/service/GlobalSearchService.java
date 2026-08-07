package com.school.sms.service;

import com.school.sms.dto.response.GlobalSearchResultDto;

public interface GlobalSearchService {

    /**
     * Searches students (name/admission number), teachers (name/employee id) and
     * books (title/author/isbn), capped at 5 results per category. Any
     * authenticated user may call this; no additional per-category scoping is
     * applied beyond authentication (e.g. library results are not further
     * restricted to LIBRARIAN-only) — see the round report for this deviation.
     */
    GlobalSearchResultDto search(String query);
}
