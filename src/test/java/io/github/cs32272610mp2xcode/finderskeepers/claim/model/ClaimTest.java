package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ClaimTest {
    private static final ClaimId CLAIM_ID = ClaimId.of(
            UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private static final UUID LOST_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001");

    private static final UUID FOUND_ID = UUID.fromString(
            "20000000-0000-0000-0000-000000000001");

    private static final Instant SUBMITTED = Instant.parse("2026-09-22T01:02:03.456Z");

    private static final Instant TERMINAL = Instant.parse("2026-09-22T02:03:04.567Z");

    @Test
    void constructsExactlyFourValidLifecycleShapes() {
        Claim pending = pending();
        Claim approved = pending.approve(Optional.of("  Synthetic approval.  "), TERMINAL);
        Claim rejected = pending.reject("  Synthetic rejection.  ", TERMINAL);
        Claim withdrawn = pending.withdraw(TERMINAL);

        assertEquals(ClaimStatus.PENDING_REVIEW, pending.status());
        assertTrue(pending.terminalAt().isEmpty());
        assertEquals(Optional.of("Synthetic approval."), approved.decisionReason());
        assertEquals(Optional.of("Synthetic rejection."), rejected.decisionReason());
        assertTrue(withdrawn.decisionReason().isEmpty());
        assertTrue(approved.status().isTerminal());
        assertTrue(rejected.status().isTerminal());
        assertTrue(withdrawn.status().isTerminal());
        assertFalse(pending.status().isTerminal());
    }

    @Test
    void rejectsInvalidIdentityTimeAndLifecycleCombinations() {
        assertThrows(NullPointerException.class, () -> Claim.createPending(
                null, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.", SUBMITTED));
        assertThrows(IllegalArgumentException.class, () -> Claim.createPending(
                CLAIM_ID, " ", LOST_ID, FOUND_ID, "Synthetic evidence.", SUBMITTED));
        assertThrows(IllegalArgumentException.class, () -> Claim.createPending(
                CLAIM_ID, "student-1", LOST_ID, LOST_ID, "Synthetic evidence.", SUBMITTED));
        assertThrows(IllegalArgumentException.class, () -> Claim.createPending(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                Instant.parse("2026-09-22T01:02:03.456789Z")));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.PENDING_REVIEW, SUBMITTED, Optional.of(TERMINAL), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.REJECTED, SUBMITTED, Optional.of(TERMINAL), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.WITHDRAWN, SUBMITTED, Optional.of(TERMINAL),
                Optional.of("Synthetic reason.")));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, " Synthetic evidence. ",
                ClaimStatus.PENDING_REVIEW, SUBMITTED, Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> pending().withdraw(SUBMITTED.minusMillis(1)));
    }

    @Test
    void restorationRejectsNoncanonicalReasonsAndIncompleteTerminalEvents() {
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.APPROVED, SUBMITTED, Optional.of(TERMINAL),
                Optional.of(" Synthetic reason. ")));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.APPROVED, SUBMITTED, Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.REJECTED, SUBMITTED, Optional.empty(),
                Optional.of("Synthetic reason.")));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.WITHDRAWN, SUBMITTED, Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> Claim.restore(
                CLAIM_ID, "student-1", LOST_ID, FOUND_ID, "Synthetic evidence.",
                ClaimStatus.APPROVED, SUBMITTED,
                Optional.of(Instant.parse("2026-09-22T02:03:04.567890Z")),
                Optional.empty()));
    }

    @Test
    void transitionsRetainImmutableIdentityTargetEvidenceAndSubmission() {
        Claim original = pending();
        Claim changed = original.approve(Optional.empty(), TERMINAL);

        assertEquals(original.claimId(), changed.claimId());
        assertEquals(original.claimantUserId(), changed.claimantUserId());
        assertEquals(original.lostReportId(), changed.lostReportId());
        assertEquals(original.foundReportId(), changed.foundReportId());
        assertEquals(original.ownershipEvidence(), changed.ownershipEvidence());
        assertEquals(original.submittedAt(), changed.submittedAt());
        assertEquals(ClaimStatus.PENDING_REVIEW, original.status());
        assertThrows(IllegalStateException.class,
                () -> changed.reject("Synthetic conflict.", TERMINAL.plusMillis(1)));
    }

    @Test
    void identityReferenceValueBehaviorAndStringsAreSafe() {
        Claim first = pending();
        Claim equal = pending();
        Claim different = pending().withdraw(TERMINAL);

        assertEquals("CLM-00000000000000000000000000000001", CLAIM_ID.reference());
        assertEquals("CLM-00000001", CLAIM_ID.shortReference());
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
        assertEquals("ClaimId[redacted]", CLAIM_ID.toString());
        assertEquals("Claim[redacted]", first.toString());
    }

    private static Claim pending() {
        return Claim.createPending(CLAIM_ID, "student-1", LOST_ID, FOUND_ID,
                "  Synthetic ownership evidence.  ", SUBMITTED);
    }
}
