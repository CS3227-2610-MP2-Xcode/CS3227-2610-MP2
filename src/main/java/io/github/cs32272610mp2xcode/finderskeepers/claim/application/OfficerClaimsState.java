package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;

/** Immutable role-narrow presentation state for one Desk Officer Claims workspace. */
public final class OfficerClaimsState {
    private final Availability pendingAvailability;

    private final Availability historyAvailability;

    private final View selectedView;

    private final HistoryFilter historyFilter;

    private final List<PendingClaimRow> pendingRows;

    private final List<HistoryClaimRow> historyRows;

    private final Optional<OfficerClaimDetail> selectedDetail;

    private final Optional<Feedback> feedback;

    OfficerClaimsState(Availability pendingState, Availability historyState,
            View view, HistoryFilter filter, List<PendingClaimRow> pending,
            List<HistoryClaimRow> history, Optional<OfficerClaimDetail> selected,
            Optional<Feedback> operationFeedback) {
        pendingAvailability = Objects.requireNonNull(pendingState, "pendingAvailability");
        historyAvailability = Objects.requireNonNull(historyState, "historyAvailability");
        selectedView = Objects.requireNonNull(view, "selectedView");
        historyFilter = Objects.requireNonNull(filter, "historyFilter");
        pendingRows = List.copyOf(pending);
        historyRows = List.copyOf(history);
        selectedDetail = Objects.requireNonNull(selected, "selectedDetail");
        feedback = Objects.requireNonNull(operationFeedback, "feedback");
    }

    /**
     * Returns the Pending review load state.
     *
     * @return pending load state
     */
    public Availability pendingAvailability() {
        return pendingAvailability;
    }

    /**
     * Returns the Claim history load state.
     *
     * @return history load state
     */
    public Availability historyAvailability() {
        return historyAvailability;
    }

    /**
     * Returns the selected fixed Claims subview.
     *
     * @return selected subview
     */
    public View selectedView() {
        return selectedView;
    }

    /**
     * Returns the selected history filter.
     *
     * @return current history filter
     */
    public HistoryFilter historyFilter() {
        return historyFilter;
    }

    /**
     * Returns the current pending queue rows.
     *
     * @return pending rows
     */
    public List<PendingClaimRow> pendingRows() {
        return pendingRows;
    }

    /**
     * Returns the filtered terminal history rows.
     *
     * @return history rows
     */
    public List<HistoryClaimRow> historyRows() {
        return historyRows;
    }

    /**
     * Returns selected-only verification detail.
     *
     * @return selected detail
     */
    public Optional<OfficerClaimDetail> selectedDetail() {
        return selectedDetail;
    }

    /**
     * Returns privacy-safe operation feedback.
     *
     * @return typed feedback
     */
    public Optional<Feedback> feedback() {
        return feedback;
    }

    /**
     * Returns whether no Claims currently await review.
     *
     * @return true only for a successful empty pending load
     */
    public boolean pendingEmpty() {
        return pendingAvailability == Availability.READY && pendingRows.isEmpty();
    }

    /**
     * Returns whether the selected pending Claim can be decided.
     *
     * @return true when both canonical reports are available
     */
    public boolean decisionsEnabled() {
        return selectedView == View.PENDING_REVIEW
                && selectedDetail.filter(detail -> detail.status() == ClaimStatus.PENDING_REVIEW)
                        .filter(detail -> detail.lostReport().isPresent()
                                && detail.foundReport().isPresent())
                        .isPresent();
    }

    @Override
    public String toString() {
        return "OfficerClaimsState[redacted]";
    }

    /** Authoritative load state for one officer subview. */
    public enum Availability {
        /** Subview has not loaded. */
        NOT_LOADED,
        /** Subview contains a current successful snapshot. */
        READY,
        /** Subview is unavailable and offers Retry. */
        UNAVAILABLE
    }

    /** Fixed officer Claims subviews. */
    public enum View {
        /** Pending review queue. */
        PENDING_REVIEW,
        /** Read-only terminal Claim history. */
        CLAIM_HISTORY
    }

    /** Exact supported terminal-history filters. */
    public enum HistoryFilter {
        /** All terminal statuses. */
        ALL,
        /** Approved Claims only. */
        APPROVED,
        /** Rejected Claims only. */
        REJECTED,
        /** Withdrawn Claims only. */
        WITHDRAWN
    }

    /** Explicit officer decision kinds. */
    public enum DecisionKind {
        /** Approve the selected Claim. */
        APPROVE,
        /** Reject the selected Claim. */
        REJECT
    }

    /** Privacy-safe officer operation feedback. */
    public enum Feedback {
        /** Approval committed. */
        APPROVED,
        /** Rejection committed. */
        REJECTED,
        /** A competing action already made the Claim terminal. */
        ALREADY_TERMINAL,
        /** A store load failed. */
        LOAD_FAILED,
        /** A terminal change failed before commit. */
        DECISION_FAILED,
        /** One or both current canonical reports are unavailable. */
        REPORT_UNAVAILABLE,
        /** A stale or fabricated selection was rejected. */
        INVALID_SELECTION
    }

    /** Opaque handle for one Claim in an officer snapshot. */
    public static final class OfficerClaimHandle {
        private final ClaimId claimId;

        OfficerClaimHandle(ClaimId id) {
            claimId = id;
        }

        ClaimId claimId() {
            return claimId;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof OfficerClaimHandle that
                    && claimId.equals(that.claimId);
        }

        @Override
        public int hashCode() {
            return claimId.hashCode();
        }

        @Override
        public String toString() {
            return "OfficerClaimHandle[redacted]";
        }
    }

    /**
     * Restricted pending queue row.
     *
     * @param handle opaque selection handle
     * @param claimReference stable visible Claim reference
     * @param lostItemName current LOST item name, when available
     * @param foundItemName current FOUND item name, when available
     * @param foundCategory current FOUND category, when available
     * @param submittedAt submission time
     */
    public record PendingClaimRow(OfficerClaimHandle handle, String claimReference,
            Optional<String> lostItemName, Optional<String> foundItemName,
            Optional<ItemCategory> foundCategory, Instant submittedAt) {
        /** Validates and retains restricted row values. */
        public PendingClaimRow {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(claimReference, "claimReference");
            lostItemName = Objects.requireNonNull(lostItemName, "lostItemName");
            foundItemName = Objects.requireNonNull(foundItemName, "foundItemName");
            foundCategory = Objects.requireNonNull(foundCategory, "foundCategory");
            Objects.requireNonNull(submittedAt, "submittedAt");
        }
    }

    /**
     * Restricted terminal history row.
     *
     * @param handle opaque selection handle
     * @param claimReference stable visible Claim reference
     * @param lostItemName current LOST item name, when available
     * @param foundItemName current FOUND item name, when available
     * @param foundCategory current FOUND category, when available
     * @param status terminal status
     * @param terminalAt terminal event time
     */
    public record HistoryClaimRow(OfficerClaimHandle handle, String claimReference,
            Optional<String> lostItemName, Optional<String> foundItemName,
            Optional<ItemCategory> foundCategory, ClaimStatus status,
            Instant terminalAt) {
        /** Validates and retains restricted row values. */
        public HistoryClaimRow {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(claimReference, "claimReference");
            lostItemName = Objects.requireNonNull(lostItemName, "lostItemName");
            foundItemName = Objects.requireNonNull(foundItemName, "foundItemName");
            foundCategory = Objects.requireNonNull(foundCategory, "foundCategory");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(terminalAt, "terminalAt");
        }
    }

    /**
     * Selected-only authorized officer detail.
     *
     * @param handle opaque selected Claim handle
     * @param claimReference stable visible reference
     * @param claimantUserId stable claimant identity
     * @param ownershipEvidence immutable evidence
     * @param status current Claim status
     * @param submittedAt submission time
     * @param terminalAt terminal time, when applicable
     * @param decisionReason decision reason, when present
     * @param lostReport complete current LOST report, when available
     * @param foundReport complete current FOUND report, when available
     */
    public record OfficerClaimDetail(OfficerClaimHandle handle, String claimReference,
            String claimantUserId, String ownershipEvidence, ClaimStatus status,
            Instant submittedAt, Optional<Instant> terminalAt,
            Optional<String> decisionReason, Optional<ItemReport> lostReport,
            Optional<ItemReport> foundReport) {
        /** Validates selected-only detail values. */
        public OfficerClaimDetail {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(claimReference, "claimReference");
            Objects.requireNonNull(claimantUserId, "claimantUserId");
            Objects.requireNonNull(ownershipEvidence, "ownershipEvidence");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(submittedAt, "submittedAt");
            terminalAt = Objects.requireNonNull(terminalAt, "terminalAt");
            decisionReason = Objects.requireNonNull(decisionReason, "decisionReason");
            lostReport = Objects.requireNonNull(lostReport, "lostReport");
            foundReport = Objects.requireNonNull(foundReport, "foundReport");
        }

        @Override
        public String toString() {
            return "OfficerClaimDetail[redacted]";
        }
    }

    /**
     * Validated read-only decision confirmation value.
     *
     * @param handle opaque selected Claim handle
     * @param claimReference visible selected reference
     * @param kind explicit decision kind
     * @param normalizedReason normalized optional or required reason
     */
    public record DecisionReview(OfficerClaimHandle handle, String claimReference,
            DecisionKind kind, Optional<String> normalizedReason) {
        /** Validates decision-review values. */
        public DecisionReview {
            Objects.requireNonNull(handle, "handle");
            Objects.requireNonNull(claimReference, "claimReference");
            Objects.requireNonNull(kind, "kind");
            normalizedReason = Objects.requireNonNull(normalizedReason, "normalizedReason");
            if (kind == DecisionKind.REJECT && normalizedReason.isEmpty()) {
                throw new IllegalArgumentException("A rejection review requires a reason.");
            }
        }

        @Override
        public String toString() {
            return "DecisionReview[redacted]";
        }
    }
}
