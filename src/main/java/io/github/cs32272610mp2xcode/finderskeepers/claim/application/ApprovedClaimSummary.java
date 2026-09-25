package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.time.Instant;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/**
 * Privacy-safe projection of one approved Claim for downstream workflows.
 *
 * @param claimId stable Claim identity
 * @param claimReference visible Claim reference
 * @param approvedAt authoritative approval time
 */
public record ApprovedClaimSummary(ClaimId claimId, String claimReference,
        Instant approvedAt) {
    /** Validates the immutable downstream projection. */
    public ApprovedClaimSummary {
        claimId = Objects.requireNonNull(claimId, "claimId");
        claimReference = requireText(claimReference, "claimReference");
        approvedAt = Objects.requireNonNull(approvedAt, "approvedAt");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    /** Suppresses Claim-derived values in diagnostics. */
    @Override
    public String toString() {
        return "ApprovedClaimSummary[redacted]";
    }
}
