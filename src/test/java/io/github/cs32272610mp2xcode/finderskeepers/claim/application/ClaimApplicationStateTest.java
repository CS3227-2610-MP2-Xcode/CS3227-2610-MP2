package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionKind;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionReview;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.HistoryFilter;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimDetail;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.PendingClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.View;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchCard;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchGroup;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.StudentClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import org.junit.jupiter.api.Test;

class ClaimApplicationStateTest {
    private static final Instant NOW = Instant.parse("2026-09-22T03:04:05.678Z");

    @Test
    void studentStateDistinguishesSuccessfulEmptyFromUnavailableAndCopiesGroups() {
        List<AvailableMatchCard> cards = new ArrayList<>();
        cards.add(new AvailableMatchCard(new AvailableMatchHandle(uuid(1), uuid(2)),
                "Synthetic lost", "Synthetic found", ItemCategory.ELECTRONICS,
                LocalDate.of(2026, 9, 21), "Synthetic location"));
        AvailableMatchGroup group = new AvailableMatchGroup(
                "Synthetic lost", cards, List.of());
        cards.clear();

        assertEquals(1, group.cards().size());
        StudentClaimsState readyEmpty = new StudentClaimsState(
                StudentClaimsState.Availability.READY,
                StudentClaimsState.Availability.NOT_LOADED,
                StudentClaimsState.View.AVAILABLE_MATCHES, List.of(), List.of(),
                Optional.empty(), Optional.empty());
        StudentClaimsState unavailable = new StudentClaimsState(
                StudentClaimsState.Availability.UNAVAILABLE,
                StudentClaimsState.Availability.UNAVAILABLE,
                StudentClaimsState.View.AVAILABLE_MATCHES, List.of(), List.of(),
                Optional.empty(), Optional.empty());

        assertTrue(readyEmpty.availableEmpty());
        assertFalse(unavailable.availableEmpty());
        assertTrue(unavailable.availableRetryVisible());
        assertTrue(unavailable.myClaimsRetryVisible());
    }

    @Test
    void officerStateEnablesDecisionsOnlyForSelectedPendingClaimWithBothReports() {
        ClaimId id = ClaimId.of(uuid(10));
        OfficerClaimHandle handle = new OfficerClaimHandle(id);
        ItemReport lost = report(uuid(11), ReportType.LOST);
        ItemReport found = report(uuid(12), ReportType.FOUND);
        OfficerClaimDetail complete = detail(handle, ClaimStatus.PENDING_REVIEW,
                Optional.of(lost), Optional.of(found));
        PendingClaimRow row = new PendingClaimRow(handle, id.reference(),
                Optional.of("Synthetic lost"), Optional.of("Synthetic found"),
                Optional.of(ItemCategory.BAGS), NOW);

        OfficerClaimsState enabled = officerState(View.PENDING_REVIEW,
                List.of(row), Optional.of(complete));
        OfficerClaimsState historyView = officerState(View.CLAIM_HISTORY,
                List.of(row), Optional.of(complete));
        OfficerClaimsState missingReport = officerState(View.PENDING_REVIEW,
                List.of(row), Optional.of(detail(handle, ClaimStatus.PENDING_REVIEW,
                        Optional.of(lost), Optional.empty())));
        OfficerClaimsState terminal = officerState(View.PENDING_REVIEW,
                List.of(row), Optional.of(detail(handle, ClaimStatus.APPROVED,
                        Optional.of(lost), Optional.of(found))));

        assertTrue(enabled.decisionsEnabled());
        assertFalse(historyView.decisionsEnabled());
        assertFalse(missingReport.decisionsEnabled());
        assertFalse(terminal.decisionsEnabled());
        assertFalse(enabled.pendingEmpty());
        assertTrue(officerState(View.PENDING_REVIEW, List.of(), Optional.empty())
                .pendingEmpty());
    }

    @Test
    void opaqueHandlesUseValueIdentityAndDecisionReviewEnforcesRejectionReason() {
        AvailableMatchHandle available = new AvailableMatchHandle(uuid(1), uuid(2));
        assertEquals(available, new AvailableMatchHandle(uuid(1), uuid(2)));
        assertNotEquals(available, new AvailableMatchHandle(uuid(1), uuid(3)));
        assertFalse(available.equals(null));
        assertEquals("AvailableMatchHandle[redacted]", available.toString());

        ClaimId claimId = ClaimId.of(uuid(4));
        StudentClaimHandle student = new StudentClaimHandle(claimId);
        OfficerClaimHandle officer = new OfficerClaimHandle(claimId);
        assertEquals(student, new StudentClaimHandle(claimId));
        assertEquals(officer, new OfficerClaimHandle(claimId));
        assertNotEquals(student, new Object());
        assertNotEquals(officer, new Object());
        assertThrows(IllegalArgumentException.class, () -> new DecisionReview(
                officer, claimId.reference(), DecisionKind.REJECT, Optional.empty()));
        assertTrue(new DecisionReview(officer, claimId.reference(),
                DecisionKind.APPROVE, Optional.empty()).normalizedReason().isEmpty());
    }

    private static OfficerClaimsState officerState(View view, List<PendingClaimRow> rows,
            Optional<OfficerClaimDetail> detail) {
        return new OfficerClaimsState(Availability.READY, Availability.NOT_LOADED,
                view, HistoryFilter.ALL, rows, List.of(), detail, Optional.empty());
    }

    private static OfficerClaimDetail detail(OfficerClaimHandle handle,
            ClaimStatus status, Optional<ItemReport> lost, Optional<ItemReport> found) {
        Optional<Instant> terminalAt = status.isTerminal() ? Optional.of(NOW) : Optional.empty();
        return new OfficerClaimDetail(handle, handle.claimId().reference(),
                "synthetic-student", "Synthetic evidence.", status,
                NOW.minusSeconds(10), terminalAt, Optional.empty(), lost, found);
    }

    private static ItemReport report(UUID id, ReportType type) {
        return ItemReport.restore(id, "synthetic-reporter", type,
                "Synthetic item", ItemCategory.BAGS, "Synthetic location",
                LocalDate.of(2026, 9, 21), "Synthetic public description",
                "Synthetic private detail", ReportStatus.SUBMITTED,
                NOW.minusSeconds(20));
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
