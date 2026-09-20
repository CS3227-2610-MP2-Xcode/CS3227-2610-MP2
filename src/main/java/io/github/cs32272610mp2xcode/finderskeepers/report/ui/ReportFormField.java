package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.Optional;

/** Editable fields displayed by the Student report form. */
public enum ReportFormField {
    /** Lost-or-found selection. */
    REPORT_TYPE("reportType"),

    /** Short item name. */
    ITEM_NAME("itemName"),

    /** Item-category selection. */
    CATEGORY("category"),

    /** Place where the item was lost or found. */
    LOCATION("location"),

    /** Date when the item was lost or found. */
    OCCURRENCE_DATE("occurrenceDate"),

    /** Description safe for matching. */
    PUBLIC_DESCRIPTION("publicDescription"),

    /** Detail reserved for staff verification. */
    PRIVATE_IDENTIFYING_DETAIL("privateIdentifyingDetail");

    private final String domainName;

    ReportFormField(String fieldName) {
        domainName = fieldName;
    }

    /**
     * Finds the form field for a stable domain field name.
     *
     * @param fieldName domain validation field name
     * @return matching editable field, when one exists
     */
    public static Optional<ReportFormField> fromDomainName(String fieldName) {
        for (ReportFormField field : values()) {
            if (field.domainName.equals(fieldName)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }
}
