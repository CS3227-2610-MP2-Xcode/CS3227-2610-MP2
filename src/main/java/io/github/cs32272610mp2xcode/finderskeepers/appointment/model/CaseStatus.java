package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

/** Lifecycle status of one Claim's collection case. */
public enum CaseStatus {
    /** Case still has work remaining. */
    OPEN,
    /** Item was returned and the case was closed. */
    CLOSED;

    /** Returns the exact persisted enum name.
     * @return exact persisted enum name */
    public String storedName() {
        return name();
    }

    /** Parses an exact persisted enum name.
     * @param value persisted enum name
     *  @return parsed case status */
    public static CaseStatus fromStoredName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Case status is required.");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown case status.", failure);
        }
    }
}
