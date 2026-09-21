package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import java.io.Serial;
import java.util.Objects;

/** Privacy-safe checked failure returned by possible-match persistence. */
public final class PossibleMatchStoreException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Reason reason;

    /** Creates a failure with a fixed privacy-safe diagnostic. */
    public PossibleMatchStoreException(Reason failureReason) {
        super(messageFor(failureReason), null, false, true);
        reason = failureReason;
    }

    /** Returns the stable failure category. */
    public Reason reason() {
        return reason;
    }

    private static String messageFor(Reason failureReason) {
        return switch (Objects.requireNonNull(failureReason, "reason")) {
            case CORRUPT_STORE -> "The possible-match store is corrupt.";
            case UNSUPPORTED_VERSION -> "The possible-match store uses an unsupported version.";
            case READ_FAILURE -> "Possible-match relationships could not be read safely.";
            case WRITE_FAILURE -> "Possible-match relationships could not be saved safely.";
            case RESULT_TOO_LARGE -> "The possible-match relationship state exceeds the supported limit.";
        };
    }

    /** Stable caller-visible failure categories. */
    public enum Reason {
        /** Existing bytes violate the version-one grammar. */
        CORRUPT_STORE,
        /** The header identifies a numeric version other than one. */
        UNSUPPORTED_VERSION,
        /** Storage could not be read safely. */
        READ_FAILURE,
        /** Storage could not be replaced atomically. */
        WRITE_FAILURE,
        /** Proposed encoded state exceeds the supported bound. */
        RESULT_TOO_LARGE
    }
}
