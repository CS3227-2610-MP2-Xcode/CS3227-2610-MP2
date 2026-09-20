package io.github.cs32272610mp2xcode.finderskeepers.report;

/** Whether an item was lost or found. */
public enum ReportType {
    /** A student is looking for an item. */
    LOST("Lost"),

    /** A student found an item. */
    FOUND("Found");

    private final String displayName;

    ReportType(String displayLabel) {
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
     * @return readable type name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Parses an exact stored report type.
     *
     * @param storedValue stored enum name
     * @return parsed report type
     * @throws IllegalArgumentException if the value is absent, blank, or unknown
     */
    public static ReportType fromStoredName(String storedValue) {
        if (storedValue == null) {
            throw new IllegalArgumentException("Report type is required.");
        }
        if (storedValue.isBlank()) {
            throw new IllegalArgumentException("Report type cannot be blank.");
        }
        try {
            return valueOf(storedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown report type: " + storedValue + ".", exception);
        }
    }
}
