package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

/** Safe, typed events retained in a collection-case audit trail. */
public enum AuditEventType {
    /** Student or officer created a booking. */
    APPOINTMENT_BOOKED,
    /** Student changed the reserved slot. */
    APPOINTMENT_RESCHEDULED,
    /** Student cancelled a booking. */
    APPOINTMENT_CANCELLED,
    /** Officer recorded a missed appointment. */
    NO_SHOW_RECORDED,
    /** Officer recorded where the item is stored. */
    STORAGE_LOCATION_RECORDED,
    /** Officer marked custody ready for collection. */
    CUSTODY_READY,
    /** Officer confirmed collection attendance. */
    COLLECTION_CONFIRMED,
    /** Officer recorded the item as returned. */
    ITEM_RETURNED,
    /** Officer closed the case. */
    CASE_CLOSED;

    /** Returns the exact persisted enum name.
     * @return exact persisted enum name */
    public String storedName() {
        return name();
    }

    /** Parses an exact persisted enum name.
     * @param value persisted enum name
     *  @return parsed event type */
    public static AuditEventType fromStoredName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Audit event type is required.");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown audit event type.", failure);
        }
    }
}
