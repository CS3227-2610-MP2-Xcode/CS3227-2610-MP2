package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

/** Checked failure raised when appointment storage cannot be used safely. */
public final class AppointmentStoreException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Typed storage failure reasons. */
    public enum Reason {
        /** Document is malformed or violates an invariant. */
        CORRUPT_STORE,
        /** Document version is not supported. */
        UNSUPPORTED_VERSION,
        /** File could not be read or accessed. */
        READ_FAILURE,
        /** Candidate could not be atomically committed. */
        WRITE_FAILURE,
        /** Candidate exceeded the configured byte limit. */
        RESULT_TOO_LARGE
    }

    /** Stored reason for this failure. */
    private final Reason failureReason;

    /** Creates a privacy-safe typed storage failure.
     * @param reason typed failure reason */
    public AppointmentStoreException(Reason reason) {
        this(reason, null);
    }

    /** Creates a typed failure with an internal diagnostic detail.
     * @param reason typed failure reason
     *  @param detail non-sensitive diagnostic detail */
    AppointmentStoreException(Reason reason, String detail) {
        super("Appointment storage operation failed: " + reason
                + (detail == null ? "" : " (" + detail + ")"));
        failureReason = java.util.Objects.requireNonNull(reason, "reason");
    }

    /** Returns the typed failure reason.
     * @return typed failure reason */
    public Reason reason() {
        return failureReason;
    }
}
