package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import java.io.Serial;
import java.util.Objects;

/** Privacy-safe checked failure from Claim persistence. */
public final class ClaimStoreException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Stable failure category. */
    private final Reason reason;

    /**
     * Creates one failure with fixed safe copy and no retained cause.
     *
     * @param failureReason stable reason
     */
    public ClaimStoreException(Reason failureReason) {
        super(messageFor(failureReason), null, false, true);
        reason = failureReason;
    }

    /**
     * Returns the stable failure category.
     *
     * @return failure reason
     */
    public Reason reason() {
        return reason;
    }

    private static String messageFor(Reason failureReason) {
        return switch (Objects.requireNonNull(failureReason, "reason")) {
            case CORRUPT_STORE -> "The Claim store is corrupt.";
            case UNSUPPORTED_VERSION -> "The Claim store uses an unsupported version.";
            case READ_FAILURE -> "The Claim store could not be read safely.";
            case WRITE_FAILURE -> "The Claim store could not be replaced safely.";
            case RESULT_TOO_LARGE -> "The requested Claim state exceeds the store limit.";
        };
    }

    /** Stable caller-visible storage failure categories. */
    public enum Reason {
        /** Existing bytes violate the supported schema or ledger rules. */
        CORRUPT_STORE,
        /** Existing bytes use another numeric schema version. */
        UNSUPPORTED_VERSION,
        /** Existing storage could not be accessed safely. */
        READ_FAILURE,
        /** New storage could not be committed atomically. */
        WRITE_FAILURE,
        /** Canonical candidate bytes exceed the configured bound. */
        RESULT_TOO_LARGE
    }
}
