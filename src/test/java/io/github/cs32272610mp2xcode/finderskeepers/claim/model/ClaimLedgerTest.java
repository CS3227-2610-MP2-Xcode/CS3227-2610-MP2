package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimLedger.SubmissionEligibility;
import org.junit.jupiter.api.Test;

class ClaimLedgerTest {
    private static final UUID LOST_A = uuid(101);

    private static final UUID LOST_B = uuid(102);

    private static final UUID FOUND_A = uuid(201);

    private static final UUID FOUND_B = uuid(202);

    private static final Instant BASE_TIME = Instant.parse("2026-09-22T01:00:00.000Z");

    @Test
    void derivesPendingLocksAndReturnsOnlyOwnBlocker() {
        Claim own = pending(1, "student-1", LOST_A, FOUND_A, 0);
        ClaimLedger ledger = ClaimLedger.from(List.of(own));

        assertEquals(SubmissionEligibility.OWN_ACTIVE_CLAIM,
                ledger.evaluate("student-1", LOST_A, FOUND_B).eligibility());
        assertEquals(own, ledger.evaluate("student-1", LOST_A, FOUND_B)
                .ownActiveClaim().orElseThrow());
        assertEquals(SubmissionEligibility.BLOCKED,
                ledger.evaluate("student-2", LOST_B, FOUND_A).eligibility());
        assertTrue(ledger.evaluate("student-2", LOST_B, FOUND_A)
                .ownActiveClaim().isEmpty());
        assertEquals(SubmissionEligibility.ELIGIBLE,
                ledger.evaluate("student-2", LOST_B, FOUND_B).eligibility());
    }

    @Test
    void withdrawalReleasesAndPermitsSamePairResubmission() {
        Claim withdrawn = pending(1, "student-1", LOST_A, FOUND_A, 0)
                .withdraw(BASE_TIME.plusSeconds(10));
        Claim replacement = pending(2, "student-1", LOST_A, FOUND_A, 20);

        ClaimLedger ledger = ClaimLedger.from(List.of(withdrawn, replacement));

        assertEquals(SubmissionEligibility.OWN_ACTIVE_CLAIM,
                ledger.evaluate("student-1", LOST_A, FOUND_A).eligibility());
    }

    @Test
    void rejectionClosesOnlyClaimantToFoundAndApprovalClosesEndpoints() {
        Claim rejected = pending(1, "student-1", LOST_A, FOUND_A, 0)
                .reject("Synthetic rejection.", BASE_TIME.plusSeconds(10));
        Claim approved = pending(2, "student-2", LOST_B, FOUND_B, 20)
                .approve(java.util.Optional.empty(), BASE_TIME.plusSeconds(30));
        ClaimLedger ledger = ClaimLedger.from(List.of(rejected, approved));

        assertEquals(SubmissionEligibility.BLOCKED,
                ledger.evaluate("student-1", uuid(103), FOUND_A).eligibility());
        assertEquals(SubmissionEligibility.ELIGIBLE,
                ledger.evaluate("student-2", uuid(103), FOUND_A).eligibility());
        assertEquals(SubmissionEligibility.BLOCKED,
                ledger.evaluate("student-3", LOST_B, uuid(203)).eligibility());
        assertEquals(SubmissionEligibility.BLOCKED,
                ledger.evaluate("student-3", uuid(103), FOUND_B).eligibility());
    }

    @Test
    void rejectsContradictoryCompleteSnapshots() {
        Claim pending = pending(1, "student-1", LOST_A, FOUND_A, 0);
        Claim duplicateId = pending(1, "student-2", LOST_B, FOUND_B, 20);
        Claim sharedLost = pending(2, "student-2", LOST_A, FOUND_B, 20);
        Claim rejected = pending.reject("Synthetic rejection.", BASE_TIME.plusSeconds(10));
        Claim afterRejection = pending(3, "student-1", LOST_B, FOUND_A, 20);
        Claim approved = pending.approve(java.util.Optional.empty(), BASE_TIME.plusSeconds(10));
        Claim afterApproval = pending(4, "student-2", LOST_B, FOUND_A, 20);

        assertThrows(IllegalArgumentException.class,
                () -> ClaimLedger.from(List.of(pending, duplicateId)));
        assertThrows(IllegalArgumentException.class,
                () -> ClaimLedger.from(List.of(pending, sharedLost)));
        assertThrows(IllegalArgumentException.class,
                () -> ClaimLedger.from(List.of(rejected, afterRejection)));
        assertThrows(IllegalArgumentException.class,
                () -> ClaimLedger.from(List.of(approved, afterApproval)));
    }

    private static Claim pending(int id, String claimant, UUID lost, UUID found,
            long submittedSeconds) {
        return Claim.createPending(ClaimId.of(uuid(id)), claimant, lost, found,
                "Synthetic evidence.", BASE_TIME.plusSeconds(submittedSeconds));
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
