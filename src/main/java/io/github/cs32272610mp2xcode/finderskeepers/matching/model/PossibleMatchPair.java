package io.github.cs32272610mp2xcode.finderskeepers.matching.model;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/** Canonical identity for an unordered pair of distinct reports. */
public final class PossibleMatchPair {
    /** Orders pairs by their canonical UUID string tuple. */
    public static final Comparator<PossibleMatchPair> CANONICAL_ORDER =
            Comparator.comparing((PossibleMatchPair pair) -> pair.firstId.toString())
                    .thenComparing(pair -> pair.secondId.toString());

    private final UUID firstId;

    private final UUID secondId;

    private PossibleMatchPair(UUID first, UUID second) {
        firstId = first;
        secondId = second;
    }

    /**
     * Creates the canonical representation of an unordered pair.
     *
     * @param first one report identifier
     * @param second the other report identifier
     * @return canonical pair
     * @throws NullPointerException if either identifier is absent
     * @throws IllegalArgumentException if both identifiers are equal
     */
    public static PossibleMatchPair of(UUID first, UUID second) {
        Objects.requireNonNull(first, "first report ID");
        Objects.requireNonNull(second, "second report ID");
        if (first.equals(second)) {
            throw new IllegalArgumentException("A possible-match pair requires two distinct reports.");
        }
        if (first.toString().compareTo(second.toString()) <= 0) {
            return new PossibleMatchPair(first, second);
        }
        return new PossibleMatchPair(second, first);
    }

    /** Returns the lexically first canonical report identifier. */
    public UUID firstId() {
        return firstId;
    }

    /** Returns the lexically second canonical report identifier. */
    public UUID secondId() {
        return secondId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PossibleMatchPair that)) {
            return false;
        }
        return firstId.equals(that.firstId) && secondId.equals(that.secondId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstId, secondId);
    }

    @Override
    public String toString() {
        return "PossibleMatchPair[redacted]";
    }
}
