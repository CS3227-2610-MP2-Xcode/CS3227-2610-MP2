package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;

/**
 * Privacy-safe projection of one approved Claim for downstream workflows.
 *
 * @param claimId stable Claim identity
 * @param claimReference visible Claim reference
 * @param itemName safe FOUND-item name, when the report is available
 * @param category safe FOUND-item category, when the report is available
 * @param approvedAt authoritative approval time
 */
public record ApprovedClaimSummary(ClaimId claimId, String claimReference,
        Optional<String> itemName, Optional<ItemCategory> category, Instant approvedAt) {
    /** Validates the immutable downstream projection. */
    public ApprovedClaimSummary {
        claimId = Objects.requireNonNull(claimId, "claimId");
        claimReference = requireText(claimReference, "claimReference");
        itemName = Objects.requireNonNull(itemName, "itemName");
        category = Objects.requireNonNull(category, "category");
        approvedAt = Objects.requireNonNull(approvedAt, "approvedAt");
        if (itemName.isPresent() != category.isPresent()) {
            throw new IllegalArgumentException("Item name and category must be available together.");
        }
        itemName = itemName.map(value -> requireText(value, "itemName"));
    }

    /** Returns a concise, child-friendly label for appointment selection.
     * @return item name and category, or a short non-sensitive fallback */
    public String displayLabel() {
        if (itemName.isPresent()) {
            return itemName.orElseThrow() + " · " + category.orElseThrow().displayName();
        }
        String reference = claimReference.substring(Math.max(0, claimReference.length() - 8));
        return "Approved item · " + reference;
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
