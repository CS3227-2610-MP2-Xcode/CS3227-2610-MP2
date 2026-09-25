package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import java.io.Serial;
import java.util.Objects;

/** Privacy-safe validation failure for claim evidence or a decision reason. */
public final class ClaimValidationException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Invalid field category retained without the rejected input. */
    private final Field field;

    ClaimValidationException(Field invalidField, String fixedMessage) {
        super(fixedMessage);
        field = Objects.requireNonNull(invalidField, "field");
    }

    /**
     * Returns the field category without returning unsafe input.
     *
     * @return invalid field category
     */
    public Field field() {
        return field;
    }

    /** Claim text fields with user-correctable validation. */
    public enum Field {
        /** Submitted ownership evidence. */
        EVIDENCE,

        /** Approval or rejection reason. */
        DECISION_REASON
    }
}
