package com.school.sms.entity;

/**
 * Kind of teaching resource, mirroring the {@code study_materials.material_type}
 * enum. Purely descriptive — it drives the icon and the filter chips in the UI,
 * not any access decision.
 */
public enum MaterialType {
    NOTES,
    PRESENTATION,
    WORKSHEET,
    REFERENCE,
    VIDEO,
    OTHER
}
