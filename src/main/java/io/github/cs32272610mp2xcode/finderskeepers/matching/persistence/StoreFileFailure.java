package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

/** Internal filesystem failure classified before becoming a public match-store error. */
final class StoreFileFailure extends Exception {
    private static final long serialVersionUID = 1L;

    private final Kind kind;

    /**
     * Creates a classified filesystem failure without retaining sensitive path details.
     *
     * @param failureKind failure reason
     */
    StoreFileFailure(Kind failureKind) {
        kind = failureKind;
    }

    /**
     * Returns the failure reason used by the repository error boundary.
     *
     * @return failure reason
     */
    Kind kind() {
        return kind;
    }

    /** Reason a low-level store operation failed. */
    enum Kind {
        /** The path could not be accessed or used safely. */
        ACCESS,
        /** The document exceeded the configured byte limit. */
        OVER_LIMIT
    }
}
