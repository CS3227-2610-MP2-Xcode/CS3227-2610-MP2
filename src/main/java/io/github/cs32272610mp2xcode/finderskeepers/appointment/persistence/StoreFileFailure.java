package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

/** Internal filesystem failure classified before becoming a public store error. */
final class StoreFileFailure extends Exception {
    private static final long serialVersionUID = 1L;

    /** Reason a low-level store operation failed. */
    enum Kind {
        /** The path could not be accessed or used safely. */
        ACCESS,
        /** The document exceeded the configured byte limit. */
        OVER_LIMIT
    }

    private final Kind failureKind;

    /**
     * Creates a classified filesystem failure.
     *
     * @param kind failure reason
     */
    StoreFileFailure(Kind kind) {
        failureKind = kind;
    }

    /**
     * Returns the failure reason used by the repository error boundary.
     *
     * @return failure reason
     */
    Kind kind() {
        return failureKind;
    }
}
