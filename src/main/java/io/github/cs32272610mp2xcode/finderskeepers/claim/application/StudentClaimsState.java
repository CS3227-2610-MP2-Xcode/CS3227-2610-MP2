package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;

/** Immutable role-narrow presentation state for one Student Claims workspace. */
public final class StudentClaimsState {
    /** Exact successful-empty copy for Available matches. */
    public static final String AVAILABLE_EMPTY_MESSAGE = "No available matches right now.";

    private final Availability availableAvailability;

    private final Availability myClaimsAvailability;

    private final View selectedView;

    private final List<AvailableMatchGroup> availableGroups;

    private final List<MyClaimRow> myClaimRows;

    private final Optional<StudentClaimDetail> selectedClaim;

    private final Optional<Feedback> feedback;

    StudentClaimsState(Availability availableState, Availability myClaimsState,
            View view, List<AvailableMatchGroup> groups, List<MyClaimRow> rows,
            Optional<StudentClaimDetail> selected, Optional<Feedback> operationFeedback) {
        availableAvailability = Objects.requireNonNull(availableState, "availableAvailability");
        myClaimsAvailability = Objects.requireNonNull(myClaimsState, "myClaimsAvailability");
        selectedView = Objects.requireNonNull(view, "selectedView");
        availableGroups = List.copyOf(groups);
        myClaimRows = List.copyOf(rows);
        selectedClaim = Objects.requireNonNull(selected, "selectedClaim");
        feedback = Objects.requireNonNull(operationFeedback, "feedback");
    }

    /**
     * Returns the current Available matches load state.
     *
     * @return Available matches load state
     */
    public Availability availableAvailability() {
        return availableAvailability;
    }

    /**
     * Returns the current My claims load state.
     *
     * @return My claims load state
     */
    public Availability myClaimsAvailability() {
        return myClaimsAvailability;
    }

    /**
     * Returns the selected fixed Claims subview.
     *
     * @return selected Claims subview
     */
    public View selectedView() {
        return selectedView;
    }

    /**
     * Returns the grouped privacy-safe Available matches snapshot.
     *
     * @return grouped privacy-safe available matches
     */
    public List<AvailableMatchGroup> availableGroups() {
        return availableGroups;
    }

    /**
     * Returns the current rows belonging to the authenticated Student.
     *
     * @return current Student-owned Claim rows
     */
    public List<MyClaimRow> myClaimRows() {
        return myClaimRows;
    }

    /**
     * Returns the selected Student-safe Claim detail.
     *
     * @return selected Student-safe Claim detail
     */
    public Optional<StudentClaimDetail> selectedClaim() {
        return selectedClaim;
    }

    /**
     * Returns current typed privacy-safe operation feedback.
     *
     * @return typed privacy-safe operation feedback
     */
    public Optional<Feedback> feedback() {
        return feedback;
    }

    /**
     * Returns whether Available matches loaded successfully with no groups.
     *
     * @return true when Available matches loaded successfully with no groups
     */
    public boolean availableEmpty() {
        return availableAvailability == Availability.READY && availableGroups.isEmpty();
    }

    /**
     * Returns whether Available matches supports explicit Retry.
     *
     * @return true when Available matches supports explicit Retry
     */
    public boolean availableRetryVisible() {
        return availableAvailability == Availability.UNAVAILABLE;
    }

    /**
     * Returns whether My claims supports explicit Retry.
     *
     * @return true when My claims supports explicit Retry
     */
    public boolean myClaimsRetryVisible() {
        return myClaimsAvailability == Availability.UNAVAILABLE;
    }

    @Override
    public String toString() {
        return "StudentClaimsState[redacted]";
    }

    /** Authoritative load state for one Student subview. */
    public enum Availability {
        /** Subview has not yet loaded. */
        NOT_LOADED,
        /** Subview contains a current successful snapshot. */
        READY,
        /** Subview cannot safely display rows and offers Retry. */
        UNAVAILABLE
    }

    /** Fixed Student Claims subviews. */
    public enum View {
        /** Claimable durable possible-match relationships. */
        AVAILABLE_MATCHES,
        /** Retained Claims belonging to the authenticated Student. */
        MY_CLAIMS
    }

    /** Privacy-safe operation feedback categories. */
    public enum Feedback {
        /** A new Claim was committed. */
        SUBMITTED,
        /** A Claim was durably withdrawn. */
        WITHDRAWN,
        /** The Student's own active Claim blocks a new submission. */
        OWN_ACTIVE_CLAIM,
        /** The selected match is no longer claimable. */
        MATCH_NO_LONGER_AVAILABLE,
        /** The Claim is already terminal. */
        ALREADY_TERMINAL,
        /** A dependency could not be loaded. */
        LOAD_FAILED,
        /** A submission could not be committed. */
        SUBMISSION_FAILED,
        /** A withdrawal could not be committed. */
        WITHDRAWAL_FAILED,
        /** A stale or fabricated selection was rejected. */
        INVALID_SELECTION
    }

    /**
     * Opaque actionable handle for one Available match.
     *
     * <p>No identifier accessor is public, so a JavaFX cell cannot render internal IDs.</p>
     */
    public static final class AvailableMatchHandle {
        private final UUID lostReportId;

        private final UUID foundReportId;

        AvailableMatchHandle(UUID lostId, UUID foundId) {
            lostReportId = lostId;
            foundReportId = foundId;
        }

        UUID lostReportId() {
            return lostReportId;
        }

        UUID foundReportId() {
            return foundReportId;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof AvailableMatchHandle that
                    && lostReportId.equals(that.lostReportId)
                    && foundReportId.equals(that.foundReportId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lostReportId, foundReportId);
        }

        @Override
        public String toString() {
            return "AvailableMatchHandle[redacted]";
        }
    }

    /** Opaque handle for one Student-owned Claim. */
    public static final class StudentClaimHandle {
        private final ClaimId claimId;

        StudentClaimHandle(ClaimId id) {
            claimId = id;
        }

        ClaimId claimId() {
            return claimId;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof StudentClaimHandle that
                    && claimId.equals(that.claimId);
        }

        @Override
        public int hashCode() {
            return claimId.hashCode();
        }

        @Override
        public String toString() {
            return "StudentClaimHandle[redacted]";
        }
    }

    /**
     * Group under one Student LOST report.
     *
     * @param lostItemName Student's LOST item name
     * @param cards currently claimable FOUND targets
     * @param existingClaims own active blockers directing to My claims
     */
    public record AvailableMatchGroup(String lostItemName,
            List<AvailableMatchCard> cards, List<ExistingClaimNotice> existingClaims) {
        /** Defensively copies group values. */
        public AvailableMatchGroup {
            lostItemName = Objects.requireNonNull(lostItemName, "lostItemName");
            cards = List.copyOf(cards);
            existingClaims = List.copyOf(existingClaims);
        }
    }

    /**
     * Privacy-safe Available-match card.
     *
     * @param handle opaque selection handle
     * @param lostItemName Student's LOST item name
     * @param foundItemName FOUND item name
     * @param foundCategory FOUND category
     * @param foundOccurrenceDate FOUND occurrence date
     * @param foundLocation FOUND location
     */
    public record AvailableMatchCard(AvailableMatchHandle handle, String lostItemName,
            String foundItemName, ItemCategory foundCategory,
            LocalDate foundOccurrenceDate, String foundLocation) {
        /** Validates required safe fields. */
        public AvailableMatchCard {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(lostItemName, "lostItemName");
            Objects.requireNonNull(foundItemName, "foundItemName");
            Objects.requireNonNull(foundCategory, "foundCategory");
            Objects.requireNonNull(foundOccurrenceDate, "foundOccurrenceDate");
            Objects.requireNonNull(foundLocation, "foundLocation");
        }
    }

    /**
     * Safe direction to the Student's own active Claim.
     *
     * @param handle opaque own-Claim handle
     * @param lostItemName applicable LOST item name
     */
    public record ExistingClaimNotice(StudentClaimHandle handle, String lostItemName) {
        /** Validates safe notice fields. */
        public ExistingClaimNotice {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(lostItemName, "lostItemName");
        }
    }

    /**
     * Student My-claims row.
     *
     * @param handle opaque own-Claim handle
     * @param lostItemName current LOST item name, when available
     * @param foundItemName current FOUND item name, when available
     * @param foundCategory current FOUND category, when available
     * @param status truthful Claim status
     * @param submittedAt submission event time
     */
    public record MyClaimRow(StudentClaimHandle handle, Optional<String> lostItemName,
            Optional<String> foundItemName, Optional<ItemCategory> foundCategory,
            ClaimStatus status, Instant submittedAt) {
        /** Defensively validates row values. */
        public MyClaimRow {
            Objects.requireNonNull(handle, "handle");
            lostItemName = Objects.requireNonNull(lostItemName, "lostItemName");
            foundItemName = Objects.requireNonNull(foundItemName, "foundItemName");
            foundCategory = Objects.requireNonNull(foundCategory, "foundCategory");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(submittedAt, "submittedAt");
        }
    }

    /**
     * Student-safe directional report summary.
     *
     * @param lostItemName current LOST item name, when available
     * @param foundItemName current FOUND item name, when available
     * @param foundCategory current FOUND category, when available
     * @param foundOccurrenceDate current FOUND occurrence date, when available
     * @param foundLocation current FOUND location, when available
     */
    public record SafeMatchSummary(Optional<String> lostItemName,
            Optional<String> foundItemName, Optional<ItemCategory> foundCategory,
            Optional<LocalDate> foundOccurrenceDate, Optional<String> foundLocation) {
        /** Defensively validates optional safe fields. */
        public SafeMatchSummary {
            lostItemName = Objects.requireNonNull(lostItemName, "lostItemName");
            foundItemName = Objects.requireNonNull(foundItemName, "foundItemName");
            foundCategory = Objects.requireNonNull(foundCategory, "foundCategory");
            foundOccurrenceDate = Objects.requireNonNull(
                    foundOccurrenceDate, "foundOccurrenceDate");
            foundLocation = Objects.requireNonNull(foundLocation, "foundLocation");
        }
    }

    /**
     * Student-safe selected Claim detail.
     *
     * @param handle opaque own-Claim handle
     * @param claimReference stable visible reference
     * @param ownershipEvidence own immutable evidence
     * @param status truthful lifecycle state
     * @param matchSummary current safe report projection
     * @param submittedAt submission time
     * @param terminalAt terminal time, when applicable
     * @param decisionReason Student-visible decision reason, when present
     */
    public record StudentClaimDetail(StudentClaimHandle handle, String claimReference,
            String ownershipEvidence, ClaimStatus status, SafeMatchSummary matchSummary,
            Instant submittedAt, Optional<Instant> terminalAt,
            Optional<String> decisionReason) {
        /** Validates selected-detail values. */
        public StudentClaimDetail {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(claimReference, "claimReference");
            Objects.requireNonNull(ownershipEvidence, "ownershipEvidence");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(matchSummary, "matchSummary");
            Objects.requireNonNull(submittedAt, "submittedAt");
            terminalAt = Objects.requireNonNull(terminalAt, "terminalAt");
            decisionReason = Objects.requireNonNull(decisionReason, "decisionReason");
        }

        @Override
        public String toString() {
            return "StudentClaimDetail[redacted]";
        }
    }

    /**
     * Validated review value shown before explicit submission confirmation.
     *
     * @param handle opaque selected match
     * @param matchSummary privacy-safe directional summary
     * @param normalizedEvidence validated immutable evidence candidate
     */
    public record SubmissionReview(AvailableMatchHandle handle,
            SafeMatchSummary matchSummary, String normalizedEvidence) {
        /** Validates review values. */
        public SubmissionReview {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(matchSummary, "matchSummary");
            Objects.requireNonNull(normalizedEvidence, "normalizedEvidence");
        }

        @Override
        public String toString() {
            return "SubmissionReview[redacted]";
        }
    }
}
