package io.github.cs32272610mp2xcode.finderskeepers.matching.application;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.PairRow;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.ReportSummary;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.RowKind;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Section;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.SelectedComparison;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.SuggestionEmptyReason;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.DeterministicMatcher;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.MatchEvaluation;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

/** Stateful plain-Java workflow for one authenticated Desk Officer matching view. */
public final class OfficerMatchingService {
    private static final Comparator<PairRow> LINKED_ORDER =
            Comparator.comparingInt(OfficerMatchingService::linkedGroup)
                    .thenComparing(Comparator.comparingInt(
                            (PairRow row) -> row.rulePoints().orElse(0)).reversed())
                    .thenComparing(PairRow::pair, PossibleMatchPair.CANONICAL_ORDER);

    private final ReportRepository reportRepository;

    private final PossibleMatchRepository matchRepository;

    private final DeterministicMatcher matcher;

    private MatchingWorkspaceState state = emptyState(Availability.NOT_LOADED, null);

    private Map<UUID, ItemReport> reportsById = Map.of();

    /**
     * Creates the workflow over shared report and relationship repositories.
     *
     * @param reports shared canonical report repository
     * @param relationships possible-match relationship repository
     * @param deterministicMatcher fixed matching policy
     */
    public OfficerMatchingService(ReportRepository reports,
            PossibleMatchRepository relationships, DeterministicMatcher deterministicMatcher) {
        reportRepository = Objects.requireNonNull(reports, "reports");
        matchRepository = Objects.requireNonNull(relationships, "relationships");
        matcher = Objects.requireNonNull(deterministicMatcher, "matcher");
    }

    /** Starts a fresh authoritative workspace load.
     * @return current immutable state
     */
    public MatchingWorkspaceState enter() {
        clearInternal();
        return load(null, null);
    }

    /** Explicitly reloads both stores and recomputes both sections.
     * @return refreshed immutable state
     */
    public MatchingWorkspaceState refresh() {
        SelectionKey selection = selectedKey();
        return load(selection, null);
    }

    /** Retries an unavailable whole-workspace load.
     * @return current immutable state
     */
    public MatchingWorkspaceState retry() {
        if (state.availability() != Availability.UNAVAILABLE) {
            return state;
        }
        return load(null, null);
    }

    /**
     * Selects a row in one exact section without performing I/O.
     *
     * @param section section containing the row
     * @param first one report identifier
     * @param second the other report identifier
     * @return current immutable state
     */
    public MatchingWorkspaceState select(Section section, UUID first, UUID second) {
        Objects.requireNonNull(section, "section");
        if (state.availability() != Availability.READY) {
            return state;
        }
        PossibleMatchPair pair;
        try {
            pair = PossibleMatchPair.of(first, second);
        } catch (NullPointerException | IllegalArgumentException failure) {
            state = withSelectionAndFeedback(null, Feedback.INVALID_PAIR, false);
            return state;
        }
        boolean present = rows(section).stream().anyMatch(row -> row.pair().equals(pair));
        state = withSelectionAndFeedback(
                present ? new SelectionKey(section, pair) : null, null, false);
        return state;
    }

    /**
     * Authoritatively rechecks and durably links one possible-match pair.
     *
     * @param first one report identifier
     * @param second the other report identifier
     * @return resulting immutable state
     */
    public MatchingWorkspaceState link(UUID first, UUID second) {
        PossibleMatchPair pair = validCommandPair(first, second);
        if (pair == null || state.availability() != Availability.READY) {
            return state;
        }
        Snapshot snapshot = readSnapshotForAction();
        if (snapshot == null) {
            return state;
        }
        if (snapshot.links().contains(pair)) {
            state = publish(snapshot, new SelectionKey(Section.LINKED, pair),
                    Feedback.ALREADY_LINKED, false);
            return state;
        }
        MatchEvaluation evaluation = evaluation(snapshot.reportsById(), pair).orElse(null);
        if (evaluation == null || !evaluation.qualifies()) {
            state = publish(snapshot, null, Feedback.STALE_PAIR, false);
            return state;
        }

        Set<PossibleMatchPair> linked = new HashSet<>(snapshot.links());
        linked.add(pair);
        Snapshot linkedSnapshot = snapshot.withLinks(linked);
        try {
            boolean changed = matchRepository.link(pair);
            state = publish(linkedSnapshot, new SelectionKey(Section.LINKED, pair),
                    changed ? Feedback.LINKED : Feedback.ALREADY_LINKED, false);
        } catch (PossibleMatchStoreException failure) {
            state = copyState(state, Feedback.LINK_FAILED, true);
        }
        return state;
    }

    /**
     * Durably removes only one possible-match relationship.
     *
     * @param first one report identifier
     * @param second the other report identifier
     * @return resulting immutable state
     */
    public MatchingWorkspaceState unlink(UUID first, UUID second) {
        PossibleMatchPair pair = validCommandPair(first, second);
        if (pair == null || state.availability() != Availability.READY) {
            return state;
        }
        Snapshot snapshot = readSnapshotForAction();
        if (snapshot == null) {
            return state;
        }
        if (!snapshot.links().contains(pair)) {
            SelectionKey selection = qualifying(snapshot.reportsById(), pair)
                    ? new SelectionKey(Section.SUGGESTIONS, pair) : null;
            state = publish(snapshot, selection, Feedback.ALREADY_UNLINKED, false);
            return state;
        }

        Set<PossibleMatchPair> remaining = new HashSet<>(snapshot.links());
        remaining.remove(pair);
        Snapshot unlinkedSnapshot = snapshot.withLinks(remaining);
        SelectionKey nextSelection = qualifying(snapshot.reportsById(), pair)
                ? new SelectionKey(Section.SUGGESTIONS, pair) : null;
        try {
            boolean changed = matchRepository.unlink(pair);
            state = publish(unlinkedSnapshot, nextSelection,
                    changed ? Feedback.UNLINKED : Feedback.ALREADY_UNLINKED, false);
        } catch (PossibleMatchStoreException failure) {
            state = copyState(state, Feedback.UNLINK_FAILED, true);
        }
        return state;
    }

    /** Clears all in-memory report and presentation state without writing storage.
     * @return cleared immutable state
     */
    public MatchingWorkspaceState clear() {
        clearInternal();
        return state;
    }

    private MatchingWorkspaceState load(SelectionKey selection, Feedback requestedFeedback) {
        List<ItemReport> reports;
        try {
            reports = reportRepository.loadAll();
        } catch (ReportStoreException failure) {
            clearForUnavailable(Feedback.REPORT_LOAD_FAILED);
            return state;
        }
        Set<PossibleMatchPair> links;
        try {
            links = matchRepository.loadAll();
        } catch (PossibleMatchStoreException failure) {
            clearForUnavailable(Feedback.RELATIONSHIP_LOAD_FAILED);
            return state;
        }
        try {
            Snapshot snapshot = createSnapshot(reports, links);
            state = publish(snapshot, selection, requestedFeedback, false);
        } catch (IllegalArgumentException | NullPointerException failure) {
            clearForUnavailable(Feedback.EVALUATION_FAILED);
        }
        return state;
    }

    private Snapshot readSnapshotForAction() {
        try {
            List<ItemReport> reports = reportRepository.loadAll();
            try {
                Set<PossibleMatchPair> links = matchRepository.loadAll();
                return createSnapshot(reports, links);
            } catch (PossibleMatchStoreException failure) {
                state = copyState(state, Feedback.RELATIONSHIP_LOAD_FAILED, true);
            }
        } catch (ReportStoreException failure) {
            state = copyState(state, Feedback.REPORT_LOAD_FAILED, true);
        } catch (IllegalArgumentException | NullPointerException failure) {
            state = copyState(state, Feedback.EVALUATION_FAILED, true);
        }
        return null;
    }

    private Snapshot createSnapshot(List<ItemReport> reports, Set<PossibleMatchPair> links) {
        DeterministicMatcher.Generation generation = matcher.generate(reports);
        Map<UUID, ItemReport> indexed = new HashMap<>();
        for (ItemReport report : reports) {
            indexed.put(report.reportId(), report);
        }
        return new Snapshot(Map.copyOf(indexed), Set.copyOf(links), generation);
    }

    private MatchingWorkspaceState publish(Snapshot snapshot, SelectionKey selection,
            Feedback feedback, boolean lastKnown) {
        List<PairRow> suggestions = snapshot.generation().qualifyingPairs().stream()
                .filter(evaluation -> !snapshot.links().contains(evaluation.pair()))
                .map(evaluation -> qualifyingRow(
                        evaluation, snapshot.reportsById(), RowKind.SUGGESTED))
                .toList();
        List<PairRow> linked = snapshot.links().stream()
                .map(pair -> linkedRow(pair, snapshot.reportsById()))
                .sorted(LINKED_ORDER)
                .toList();
        reportsById = snapshot.reportsById();
        Optional<SuggestionEmptyReason> emptyReason = suggestions.isEmpty()
                ? Optional.of(emptyReason(snapshot.generation())) : Optional.empty();
        Optional<SelectedComparison> selected = selectedComparison(selection, suggestions, linked);
        state = new MatchingWorkspaceState(Availability.READY, suggestions, linked,
                emptyReason, selected, Optional.ofNullable(feedback), lastKnown);
        return state;
    }

    private SuggestionEmptyReason emptyReason(DeterministicMatcher.Generation generation) {
        if (!generation.hasEligiblePairs()) {
            return SuggestionEmptyReason.NO_ELIGIBLE_PAIR;
        }
        if (generation.qualifyingPairs().isEmpty()) {
            return SuggestionEmptyReason.NO_QUALIFYING_PAIR;
        }
        return SuggestionEmptyReason.ALL_QUALIFYING_PAIRS_LINKED;
    }

    private PairRow qualifyingRow(MatchEvaluation evaluation,
            Map<UUID, ItemReport> reports, RowKind kind) {
        return new PairRow(evaluation.pair(),
                summary(reports.get(evaluation.pair().firstId())),
                summary(reports.get(evaluation.pair().secondId())),
                kind, Optional.of(evaluation.totalPoints()),
                evaluation.positiveReasonLabels());
    }

    private PairRow linkedRow(PossibleMatchPair pair, Map<UUID, ItemReport> reports) {
        ItemReport first = reports.get(pair.firstId());
        ItemReport second = reports.get(pair.secondId());
        if (first == null || second == null) {
            return new PairRow(pair, summary(first), summary(second),
                    RowKind.LINKED_REPORT_UNAVAILABLE, Optional.empty(), List.of());
        }
        Optional<MatchEvaluation> evaluation = matcher.evaluate(first, second);
        if (evaluation.isPresent() && evaluation.orElseThrow().qualifies()) {
            return qualifyingRow(evaluation.orElseThrow(), reports, RowKind.LINKED_QUALIFYING);
        }
        return new PairRow(pair, summary(first), summary(second),
                RowKind.LINKED_NON_QUALIFYING, Optional.empty(), List.of());
    }

    private Optional<SelectedComparison> selectedComparison(SelectionKey selection,
            List<PairRow> suggestions, List<PairRow> linked) {
        if (selection == null || rows(selection.section(), suggestions, linked).stream()
                .noneMatch(row -> row.pair().equals(selection.pair()))) {
            return Optional.empty();
        }
        ItemReport first = reportsById.get(selection.pair().firstId());
        ItemReport second = reportsById.get(selection.pair().secondId());
        Optional<MatchEvaluation> evaluation = first == null || second == null
                ? Optional.empty() : matcher.evaluate(first, second);
        return Optional.of(new SelectedComparison(selection.pair(), selection.section(),
                Optional.ofNullable(first), Optional.ofNullable(second), evaluation));
    }

    private MatchingWorkspaceState withSelectionAndFeedback(SelectionKey selection,
            Feedback feedback, boolean lastKnown) {
        Optional<SelectedComparison> selected = selectedComparison(
                selection, state.suggestions(), state.linkedPairs());
        return new MatchingWorkspaceState(state.availability(), state.suggestions(),
                state.linkedPairs(), state.suggestionEmptyReason(), selected,
                Optional.ofNullable(feedback), lastKnown);
    }

    private static MatchingWorkspaceState copyState(MatchingWorkspaceState source,
            Feedback feedback, boolean lastKnown) {
        return new MatchingWorkspaceState(source.availability(), source.suggestions(),
                source.linkedPairs(), source.suggestionEmptyReason(),
                source.selectedComparison(), Optional.of(feedback), lastKnown);
    }

    private PossibleMatchPair validCommandPair(UUID first, UUID second) {
        try {
            return PossibleMatchPair.of(first, second);
        } catch (NullPointerException | IllegalArgumentException failure) {
            state = withSelectionAndFeedback(null, Feedback.INVALID_PAIR, false);
            return null;
        }
    }

    private Optional<MatchEvaluation> evaluation(Map<UUID, ItemReport> reports,
            PossibleMatchPair pair) {
        ItemReport first = reports.get(pair.firstId());
        ItemReport second = reports.get(pair.secondId());
        if (first == null || second == null) {
            return Optional.empty();
        }
        return matcher.evaluate(first, second);
    }

    private boolean qualifying(Map<UUID, ItemReport> reports, PossibleMatchPair pair) {
        return evaluation(reports, pair).filter(MatchEvaluation::qualifies).isPresent();
    }

    private List<PairRow> rows(Section section) {
        return rows(section, state.suggestions(), state.linkedPairs());
    }

    private static List<PairRow> rows(Section section,
            List<PairRow> suggestions, List<PairRow> linked) {
        return section == Section.SUGGESTIONS ? suggestions : linked;
    }

    private SelectionKey selectedKey() {
        return state.selectedComparison()
                .map(selected -> new SelectionKey(selected.section(), selected.pair()))
                .orElse(null);
    }

    private static Optional<ReportSummary> summary(ItemReport report) {
        if (report == null) {
            return Optional.empty();
        }
        return Optional.of(new ReportSummary(report.reportType(), report.itemName(),
                report.category(), report.occurrenceDate(), report.location()));
    }

    private static int linkedGroup(PairRow row) {
        return row.kind() == RowKind.LINKED_QUALIFYING ? 0 : 1;
    }

    private void clearInternal() {
        reportsById = Map.of();
        state = emptyState(Availability.NOT_LOADED, null);
    }

    private void clearForUnavailable(Feedback feedback) {
        reportsById = Map.of();
        state = emptyState(Availability.UNAVAILABLE, feedback);
    }

    private static MatchingWorkspaceState emptyState(Availability availability,
            Feedback feedback) {
        return new MatchingWorkspaceState(availability, List.of(), List.of(),
                Optional.empty(), Optional.empty(), Optional.ofNullable(feedback), false);
    }

    /** Identity of the row selected in one matching-workspace section. */
    private record SelectionKey(Section section, PossibleMatchPair pair) {
    }

    /** Reports, links, and generated suggestions loaded for one workspace refresh. */
    private record Snapshot(Map<UUID, ItemReport> reportsById,
            Set<PossibleMatchPair> links, DeterministicMatcher.Generation generation) {
        private Snapshot withLinks(Set<PossibleMatchPair> replacementLinks) {
            return new Snapshot(reportsById, Set.copyOf(replacementLinks), generation);
        }
    }
}
