package io.github.cs32272610mp2xcode.finderskeepers.matching.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.matching.model.MatchEvaluation;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;

/** Immutable presentation state for one authenticated officer matching view. */
public final class MatchingWorkspaceState {
    /** Workspace load status. */
    public enum Availability {
        /** No entry load has occurred or state has been cleared. */
        NOT_LOADED,
        /** Both stores and matching evidence are current. */
        READY,
        /** An authoritative load or evaluation failed. */
        UNAVAILABLE
    }

    /** Mutually exclusive workspace sections. */
    public enum Section {
        /** Currently qualifying pairs without durable links. */
        SUGGESTIONS,
        /** Durable possible-match relationships. */
        LINKED
    }

    /** Privacy-safe row presentation state. */
    public enum RowKind {
        /** Qualifying unlinked suggestion. */
        SUGGESTED,
        /** Linked pair that currently qualifies. */
        LINKED_QUALIFYING,
        /** Linked pair that no longer qualifies or is ineligible. */
        LINKED_NON_QUALIFYING,
        /** Linked pair with at least one unavailable report. */
        LINKED_REPORT_UNAVAILABLE
    }

    /** Successful empty classification for the suggestion section. */
    public enum SuggestionEmptyReason {
        /** At least one LOST and one FOUND eligible report do not coexist. */
        NO_ELIGIBLE_PAIR,
        /** Eligible pairs exist but none pass the approved rules. */
        NO_QUALIFYING_PAIR,
        /** Qualifying pairs exist, but each one is linked. */
        ALL_QUALIFYING_PAIRS_LINKED
    }

    /** Typed privacy-safe operation feedback. */
    public enum Feedback {
        /** Canonical reports could not be loaded. */
        REPORT_LOAD_FAILED,
        /** Possible-match relationships could not be loaded. */
        RELATIONSHIP_LOAD_FAILED,
        /** The snapshot could not be evaluated safely. */
        EVALUATION_FAILED,
        /** Durable Link failed. */
        LINK_FAILED,
        /** Durable Unlink failed. */
        UNLINK_FAILED,
        /** The requested pair was no longer linkable. */
        STALE_PAIR,
        /** The supplied pair identity was invalid. */
        INVALID_PAIR,
        /** A new possible-match link was committed. */
        LINKED,
        /** The selected relationship was durably removed. */
        UNLINKED,
        /** The pair was already linked. */
        ALREADY_LINKED,
        /** The pair was already unlinked. */
        ALREADY_UNLINKED
    }

    /**
     * Restricted public row projection; it is never accepted by matching or storage.
     *
     * @param reportType canonical LOST or FOUND type
     * @param itemName public row item name
     * @param category canonical category
     * @param occurrenceDate report occurrence date
     * @param location public row location
     */
    public record ReportSummary(ReportType reportType, String itemName,
            ItemCategory category, LocalDate occurrenceDate, String location) {
        /** Validates every approved row field. */
        public ReportSummary {
            Objects.requireNonNull(reportType, "reportType");
            Objects.requireNonNull(itemName, "itemName");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(occurrenceDate, "occurrenceDate");
            Objects.requireNonNull(location, "location");
        }

        @Override
        public String toString() {
            return "ReportSummary[redacted]";
        }
    }

    /**
     * One row in a suggestion or linked relationship section.
     *
     * @param pair internal canonical selection key
     * @param firstReport first available public summary
     * @param secondReport second available public summary
     * @param kind row lifecycle kind
     * @param rulePoints qualifying rule points, when applicable
     * @param reasonLabels brief positive reasons
     */
    public record PairRow(PossibleMatchPair pair, Optional<ReportSummary> firstReport,
            Optional<ReportSummary> secondReport, RowKind kind,
            Optional<Integer> rulePoints, List<String> reasonLabels) {
        /** Defensively copies row values and labels. */
        public PairRow {
            Objects.requireNonNull(pair, "pair");
            Objects.requireNonNull(firstReport, "firstReport");
            Objects.requireNonNull(secondReport, "secondReport");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(rulePoints, "rulePoints");
            reasonLabels = List.copyOf(reasonLabels);
        }

        @Override
        public String toString() {
            return "PairRow[redacted]";
        }
    }

    /**
     * Selected officer-only comparison; canonical reports remain immutable.
     *
     * @param pair canonical selected pair
     * @param section selected section
     * @param firstReport first current canonical endpoint
     * @param secondReport second current canonical endpoint
     * @param evaluation current evidence when the pair remains eligible
     */
    public record SelectedComparison(PossibleMatchPair pair, Section section,
            Optional<ItemReport> firstReport, Optional<ItemReport> secondReport,
            Optional<MatchEvaluation> evaluation) {
        /** Validates optional selected values. */
        public SelectedComparison {
            Objects.requireNonNull(pair, "pair");
            Objects.requireNonNull(section, "section");
            Objects.requireNonNull(firstReport, "firstReport");
            Objects.requireNonNull(secondReport, "secondReport");
            Objects.requireNonNull(evaluation, "evaluation");
        }

        /** Returns a score only while the pair currently qualifies.
         * @return qualifying rule points, when applicable
         */
        public Optional<Integer> qualifyingRulePoints() {
            return evaluation.filter(MatchEvaluation::qualifies)
                    .map(MatchEvaluation::totalPoints);
        }

        @Override
        public String toString() {
            return "SelectedComparison[redacted]";
        }
    }

    private final Availability availability;

    private final List<PairRow> suggestions;

    private final List<PairRow> linkedPairs;

    private final Optional<SuggestionEmptyReason> suggestionEmptyReason;

    private final Optional<SelectedComparison> selectedComparison;

    private final Optional<Feedback> feedback;

    private final boolean lastKnownState;

    MatchingWorkspaceState(Availability workspaceAvailability,
            List<PairRow> suggestionRows, List<PairRow> linkedRows,
            Optional<SuggestionEmptyReason> emptyReason,
            Optional<SelectedComparison> selection,
            Optional<Feedback> operationFeedback, boolean retainedLastKnownState) {
        availability = Objects.requireNonNull(workspaceAvailability, "availability");
        suggestions = List.copyOf(suggestionRows);
        linkedPairs = List.copyOf(linkedRows);
        suggestionEmptyReason = Objects.requireNonNull(emptyReason, "emptyReason");
        selectedComparison = Objects.requireNonNull(selection, "selection");
        feedback = Objects.requireNonNull(operationFeedback, "feedback");
        lastKnownState = retainedLastKnownState;
    }

    /** Returns the workspace load status.
     * @return workspace availability
     */
    public Availability availability() {
        return availability;
    }

    /** Returns ordered currently qualifying unlinked suggestions.
     * @return immutable suggestion rows
     */
    public List<PairRow> suggestions() {
        return suggestions;
    }

    /** Returns ordered durable possible-match relationships.
     * @return immutable linked rows
     */
    public List<PairRow> linkedPairs() {
        return linkedPairs;
    }

    /** Returns the exact successful suggestion-empty classification, when empty.
     * @return successful empty reason, when applicable
     */
    public Optional<SuggestionEmptyReason> suggestionEmptyReason() {
        return suggestionEmptyReason;
    }

    /** Returns the selected officer-only comparison.
     * @return selected comparison, when present
     */
    public Optional<SelectedComparison> selectedComparison() {
        return selectedComparison;
    }

    /** Returns current typed operation feedback.
     * @return typed feedback, when present
     */
    public Optional<Feedback> feedback() {
        return feedback;
    }

    /** Returns whether rows describe explicitly retained last-known state.
     * @return true when current rows are explicitly last-known
     */
    public boolean lastKnownState() {
        return lastKnownState;
    }

    /** Returns whether the selected pair can be explicitly linked.
     * @return true when Link is enabled
     */
    public boolean linkEnabled() {
        return availability == Availability.READY
                && selectedComparison.map(SelectedComparison::section)
                        .filter(Section.SUGGESTIONS::equals).isPresent();
    }

    /** Returns whether the selected durable relationship can be explicitly unlinked.
     * @return true when Unlink is enabled
     */
    public boolean unlinkEnabled() {
        return availability == Availability.READY
                && selectedComparison.map(SelectedComparison::section)
                        .filter(Section.LINKED::equals).isPresent();
    }

    /** Returns whether the whole unavailable workspace supports an explicit Retry.
     * @return true when Retry is visible
     */
    public boolean retryVisible() {
        return availability == Availability.UNAVAILABLE;
    }

    /** Returns whether no durable possible-match relationships exist.
     * @return true for a ready empty linked section
     */
    public boolean linkedEmpty() {
        return availability == Availability.READY && linkedPairs.isEmpty();
    }

    @Override
    public String toString() {
        return "MatchingWorkspaceState[redacted]";
    }
}
