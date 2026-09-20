package io.github.cs32272610mp2xcode.finderskeepers.report;

/** Current processing state of an item report. */
public enum ReportStatus {
    /** The report has been submitted and is waiting for staff review. */
    SUBMITTED("Submitted"),

    /** A Desk Officer has started reviewing the report. */
    UNDER_REVIEW("Under review");

    private final String displayName;

    ReportStatus(String displayLabel) {
        displayName = displayLabel;
    }

    /**
     * Returns the stable, case-sensitive value used in storage.
     *
     * @return stored enum name
     */
    public String storedName() {
        return name();
    }

    /**
     * Returns the label intended for user interfaces.
     *
     * @return readable status name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Parses an exact stored report status.
     *
     * @param storedValue stored enum name
     * @return parsed report status
     * @throws IllegalArgumentException if the value is absent, blank, or unknown
     */
    public static ReportStatus fromStoredName(String storedValue) {
        if (storedValue == null) {
            throw new IllegalArgumentException("Report status is required.");
        }
        if (storedValue.isBlank()) {
            throw new IllegalArgumentException("Report status cannot be blank.");
        }
        try {
            return valueOf(storedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown report status: " + storedValue + ".", exception);
        }
    }
}
