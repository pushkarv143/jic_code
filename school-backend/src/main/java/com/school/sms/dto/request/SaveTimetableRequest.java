package com.school.sms.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A section's whole week, replaced in one call.
 *
 * <p>Whole-week rather than per-slot because a timetable is edited as a grid:
 * moving one period usually rewrites several, and a sequence of per-slot calls
 * would trip the (section, day, period) unique key half-way through a swap.
 * An empty list is meaningful — it clears the section's timetable.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveTimetableRequest {

    @Valid
    private List<TimetableSlotRequest> slots = new ArrayList<>();
}
