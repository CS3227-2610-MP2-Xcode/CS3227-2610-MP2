package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

/** Officer-managed custody state for one approved item. */
public enum CustodyStatus {
    /** No storage location has been recorded. */
    AWAITING_STORAGE,
    /** Item is stored at the recorded location. */
    STORED,
    /** Item is ready for the Student's collection appointment. */
    READY_FOR_COLLECTION,
    /** Item was handed back to the Student. */
    RETURNED;

    /** Returns the exact persisted enum name.
     * @return exact persisted enum name */
    public String storedName() {
        return name();
    }

    /** Parses an exact persisted enum name.
     * @param value persisted enum name
     * @return parsed custody status */
    public static CustodyStatus fromStoredName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Custody status is required.");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown custody status.", failure);
        }
    }
}
