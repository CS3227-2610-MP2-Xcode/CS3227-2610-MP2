package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import java.util.Objects;
import java.util.UUID;

/** Immutable internal and user-visible identity for one claim. */
public final class ClaimId {
    private static final String REFERENCE_PREFIX = "CLM-";

    private final UUID value;

    private ClaimId(UUID claimId) {
        value = claimId;
    }

    /**
     * Wraps one claim UUID.
     *
     * @param claimId internal claim identifier
     * @return immutable claim identifier
     */
    public static ClaimId of(UUID claimId) {
        return new ClaimId(Objects.requireNonNull(claimId, "claimId"));
    }

    /**
     * Returns the persisted UUID value.
     *
     * @return persisted UUID
     */
    public UUID value() {
        return value;
    }

    /**
     * Returns the stable user-visible claim reference.
     *
     * @return reference prefixed with {@code CLM-}
     */
    public String reference() {
        return REFERENCE_PREFIX + value.toString().replace("-", "").toUpperCase();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ClaimId that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "ClaimId[redacted]";
    }
}
