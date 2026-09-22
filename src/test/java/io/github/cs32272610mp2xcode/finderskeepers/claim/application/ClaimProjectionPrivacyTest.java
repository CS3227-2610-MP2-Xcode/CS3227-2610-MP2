package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.DecisionReview;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.HistoryClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimDetail;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.OfficerClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsState.PendingClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchCard;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.AvailableMatchHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.ExistingClaimNotice;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.MyClaimRow;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.SafeMatchSummary;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.StudentClaimDetail;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.StudentClaimHandle;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsState.SubmissionReview;
import org.junit.jupiter.api.Test;

class ClaimProjectionPrivacyTest {
    @Test
    void studentProjectionComponentsMatchApprovedAllowlists() {
        assertComponents(AvailableMatchCard.class, "handle", "lostItemName",
                "foundItemName", "foundCategory", "foundOccurrenceDate",
                "foundLocation");
        assertComponents(ExistingClaimNotice.class, "handle", "lostItemName");
        assertComponents(MyClaimRow.class, "handle", "lostItemName", "foundItemName",
                "foundCategory", "status", "submittedAt");
        assertComponents(SafeMatchSummary.class, "lostItemName", "foundItemName",
                "foundCategory", "foundOccurrenceDate", "foundLocation");
        assertComponents(StudentClaimDetail.class, "handle", "claimReference",
                "ownershipEvidence", "status", "matchSummary", "submittedAt",
                "terminalAt", "decisionReason");
        assertComponents(SubmissionReview.class, "handle", "matchSummary",
                "normalizedEvidence");
        assertNoPublicDomainAccessor(AvailableMatchHandle.class);
        assertNoPublicDomainAccessor(StudentClaimHandle.class);
    }

    @Test
    void officerRowsAndReviewsMatchApprovedAllowlists() {
        assertComponents(PendingClaimRow.class, "handle", "claimReference",
                "lostItemName", "foundItemName", "foundCategory", "submittedAt");
        assertComponents(HistoryClaimRow.class, "handle", "claimReference",
                "lostItemName", "foundItemName", "foundCategory", "status",
                "terminalAt");
        assertComponents(OfficerClaimDetail.class, "handle", "claimReference",
                "claimantUserId", "ownershipEvidence", "status", "submittedAt",
                "terminalAt", "decisionReason", "lostReport", "foundReport");
        assertComponents(DecisionReview.class, "handle", "claimReference", "kind",
                "normalizedReason");
        assertNoPublicDomainAccessor(OfficerClaimHandle.class);
    }

    private static void assertComponents(Class<?> type, String... expected) {
        Set<String> names = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName).collect(Collectors.toUnmodifiableSet());
        assertEquals(Set.of(expected), names);
    }

    private static void assertNoPublicDomainAccessor(Class<?> type) {
        assertTrue(Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .allMatch(method -> Set.of("equals", "hashCode", "toString")
                        .contains(method.getName())));
    }
}
