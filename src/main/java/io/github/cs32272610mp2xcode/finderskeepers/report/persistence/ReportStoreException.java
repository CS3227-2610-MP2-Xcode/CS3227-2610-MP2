package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import java.io.Serial;
import java.util.Objects;

/** Privacy-safe checked failure returned by report persistence operations. */
public final class ReportStoreException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Stable caller-visible failure category. */
    private final Reason reason;

    /**
     * Creates a failure with its fixed privacy-safe diagnostic.
     *
     * @param failureReason stable caller-visible failure category
     */
    public ReportStoreException(Reason failureReason) {
        super(messageFor(failureReason), null, false, true);
        this.reason = failureReason;
    }

    /**
     * Returns the stable failure category.
     *
     * @return failure category
     */
    public Reason reason() {
        return reason;
    }

    private static String messageFor(Reason failureReason) {
        return switch (Objects.requireNonNull(failureReason, "reason")) {
            case DUPLICATE_REPORT_ID -> "A report with that identifier already exists.";
            case REPLACEMENT_TARGET_NOT_FOUND -> "The report to replace does not exist.";
            case IMMUTABLE_FIELD_MISMATCH -> "The replacement changes immutable report identity.";
            case CORRUPT_OR_UNSUPPORTED_STORE -> "The report store is corrupt or uses an unsupported format.";
            case STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE -> "The report store could not be accessed or safely replaced.";
            case UNENCODABLE_OR_OVER_LIMIT_RESULT ->
                "The requested report state is not encodable or exceeds the supported store limit.";
        };
    }

    /** Stable failure categories for callers. */
    public enum Reason {
        /** An insertion used an existing report identifier. */
        DUPLICATE_REPORT_ID,

        /** A replacement target does not exist. */
        REPLACEMENT_TARGET_NOT_FOUND,

        /** A replacement attempted to change immutable report state. */
        IMMUTABLE_FIELD_MISMATCH,

        /** Existing storage violates the supported format. */
        CORRUPT_OR_UNSUPPORTED_STORE,

        /** Storage could not be accessed or replaced atomically. */
        STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE,

        /** Proposed state cannot be encoded or exceeds the store bound. */
        UNENCODABLE_OR_OVER_LIMIT_RESULT
    }
}
