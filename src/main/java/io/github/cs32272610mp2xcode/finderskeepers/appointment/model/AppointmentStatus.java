package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

/** Lifecycle status of one appointment attempt. */
public enum AppointmentStatus {
    /** Appointment is reserved and awaiting collection. */
    BOOKED,
    /** Student cancelled before the slot began. */
    CANCELLED,
    /** Officer recorded that the Student did not attend. */
    NO_SHOW,
    /** Officer confirmed attendance/collection at the desk. */
    COLLECTION_CONFIRMED;

    /** Returns the exact persisted enum name.
     * @return exact persisted enum name */
    public String storedName() {
        return name();
    }

    /** Parses an exact persisted enum name.
     * @param value persisted enum name
     *  @return parsed appointment status */
    public static AppointmentStatus fromStoredName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Appointment status is required.");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown appointment status.", failure);
        }
    }
}
