package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import org.junit.jupiter.api.Test;

class ApprovedClaimSummaryTest {
    @Test
    void matchingItemDetailsStillProduceDistinguishableLabels() {
        ApprovedClaimSummary first = summary(1);
        ApprovedClaimSummary second = summary(2);

        assertEquals("Blue pencil case · Stationery · CLM-00000001", first.displayLabel());
        assertEquals("Blue pencil case · Stationery · CLM-00000002", second.displayLabel());
        assertNotEquals(first.displayLabel(), second.displayLabel());
    }

    private static ApprovedClaimSummary summary(long id) {
        ClaimId claimId = ClaimId.of(new UUID(0L, id));
        return new ApprovedClaimSummary(claimId, claimId.reference(),
                Optional.of("Blue pencil case"), Optional.of(ItemCategory.STATIONERY),
                Instant.parse("2030-01-01T00:00:00Z"));
    }
}
