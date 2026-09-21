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

    /** Restricted public row projection; it is never accepted by matching or storage. */
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

    /** One row in a suggestion or linked relationship section. */
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

    /** Selected officer-only comparison; canonical reports remain immutable. */
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

        /** Returns a score only while the pair currently qualifies. */
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

    /** Returns the workspace load status. */
    public Availability availability() {
        return availability;
    }

    /** Returns ordered currently qualifying unlinked suggestions. */
    public List<PairRow> suggestions() {
        return suggestions;
    }

    /** Returns ordered durable possible-match relationships. */
    public List<PairRow> linkedPairs() {
        return linkedPairs;
    }

    /** Returns the exact successful suggestion-empty classification, when empty. */
    public Optional<SuggestionEmptyReason> suggestionEmptyReason() {
        return suggestionEmptyReason;
    }

    /** Returns the selected officer-only comparison. */
    public Optional<SelectedComparison> selectedComparison() {
        return selectedComparison;
    }

    /** Returns current typed operation feedback. */
    public Optional<Feedback> feedback() {
        return feedback;
    }

    /** Returns whether rows describe explicitly retained last-known state. */
    public boolean lastKnownState() {
        return lastKnownState;
    }

    /** Returns whether the selected pair can be explicitly linked. */
    public boolean linkEnabled() {
        return availability == Availability.READY
                && selectedComparison.map(SelectedComparison::section)
                        .filter(Section.SUGGESTIONS::equals).isPresent();
    }

    /** Returns whether the selected durable relationship can be explicitly unlinked. */
    public boolean unlinkEnabled() {
        return availability == Availability.READY
                && selectedComparison.map(SelectedComparison::section)
                        .filter(Section.LINKED::equals).isPresent();
    }

    /** Returns whether the whole unavailable workspace supports an explicit Retry. */
    public boolean retryVisible() {
        return availability == Availability.UNAVAILABLE;
    }

    /** Returns whether no durable possible-match relationships exist. */
    public boolean linkedEmpty() {
        return availability == Availability.READY && linkedPairs.isEmpty();
    }

    @Override
    public String toString() {
        return "MatchingWorkspaceState[redacted]";
    }
}
