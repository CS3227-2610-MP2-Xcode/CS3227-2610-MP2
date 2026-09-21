package io.github.cs32272610mp2xcode.finderskeepers.matching.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Availability;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Feedback;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.RowKind;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.Section;
import io.github.cs32272610mp2xcode.finderskeepers.matching.application.MatchingWorkspaceState.SuggestionEmptyReason;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.DeterministicMatcher;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

class OfficerMatchingServiceTest {
    @Test
    void loadsOrderedSectionsAndRestrictsRowsUntilSelection() {
        ItemReport lost = report(1, ReportType.LOST, "Blue pencil case", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Pencil case", "Library");
        Repositories repositories = repositories(List.of(lost, found), Set.of());
        OfficerMatchingService service = repositories.service();

        MatchingWorkspaceState state = service.enter();

        assertEquals(Availability.READY, state.availability());
        assertEquals(1, state.suggestions().size());
        assertTrue(state.linkedEmpty());
        assertFalse(state.toString().contains("synthetic-private"));
        assertFalse(state.suggestions().getFirst().toString().contains("synthetic-reporter"));

        state = service.select(Section.SUGGESTIONS, lost.reportId(), found.reportId());
        assertTrue(state.linkEnabled());
        assertFalse(state.unlinkEnabled());
        assertSame(lost, state.selectedComparison().orElseThrow().firstReport().orElseThrow());
    }

    @Test
    void distinguishesSuccessfulEmptyStates() {
        Repositories noEligible = repositories(
                List.of(report(1, ReportType.LOST, "Item", "Place")), Set.of());
        assertEquals(SuggestionEmptyReason.NO_ELIGIBLE_PAIR,
                noEligible.service().enter().suggestionEmptyReason().orElseThrow());

        ItemReport lost = report(1, ReportType.LOST, "Notebook", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Book", "Canteen");
        Repositories noQualifier = repositories(List.of(lost, found), Set.of());
        assertEquals(SuggestionEmptyReason.NO_QUALIFYING_PAIR,
                noQualifier.service().enter().suggestionEmptyReason().orElseThrow());

        ItemReport matchingFound = report(3, ReportType.FOUND, "Notebook", "Library");
        PossibleMatchPair pair = PossibleMatchPair.of(lost.reportId(), matchingFound.reportId());
        Repositories allLinked = repositories(List.of(lost, matchingFound), Set.of(pair));
        assertEquals(SuggestionEmptyReason.ALL_QUALIFYING_PAIRS_LINKED,
                allLinked.service().enter().suggestionEmptyReason().orElseThrow());
    }

    @Test
    void linkRechecksMovesSelectionAndDoesNotChangeReports() {
        ItemReport lost = report(1, ReportType.LOST, "Bottle green", "Hall A");
        ItemReport found = report(2, ReportType.FOUND, "Green bottle", "Hall-A");
        List<ItemReport> original = List.of(lost, found);
        Repositories repositories = repositories(original, Set.of());
        OfficerMatchingService service = repositories.service();
        service.enter();
        service.select(Section.SUGGESTIONS, lost.reportId(), found.reportId());

        MatchingWorkspaceState linked = service.link(lost.reportId(), found.reportId());

        assertEquals(Feedback.LINKED, linked.feedback().orElseThrow());
        assertTrue(linked.suggestions().isEmpty());
        assertEquals(RowKind.LINKED_QUALIFYING, linked.linkedPairs().getFirst().kind());
        assertTrue(linked.unlinkEnabled());
        assertEquals(original, repositories.reports.reports);

        MatchingWorkspaceState repeated = service.link(found.reportId(), lost.reportId());
        assertEquals(Feedback.ALREADY_LINKED, repeated.feedback().orElseThrow());
        assertEquals(1, repositories.links.links.size());
    }

    @Test
    void staleLinkReconcilesWithoutMutationOrPrivateSelection() {
        ItemReport lost = report(1, ReportType.LOST, "Bottle", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Bottle", "Library");
        Repositories repositories = repositories(List.of(lost, found), Set.of());
        OfficerMatchingService service = repositories.service();
        service.enter();
        service.select(Section.SUGGESTIONS, lost.reportId(), found.reportId());
        repositories.reports.reports = List.of(lost);

        MatchingWorkspaceState stale = service.link(lost.reportId(), found.reportId());

        assertEquals(Feedback.STALE_PAIR, stale.feedback().orElseThrow());
        assertTrue(stale.selectedComparison().isEmpty());
        assertTrue(repositories.links.links.isEmpty());
    }

    @Test
    void unlinkRemovesOnlyTargetAndReturnsQualifierToSuggestions() {
        ItemReport lost = report(1, ReportType.LOST, "Bottle", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Bottle", "Library");
        ItemReport secondFound = report(3, ReportType.FOUND, "Bottle", "Library");
        PossibleMatchPair target = PossibleMatchPair.of(lost.reportId(), found.reportId());
        PossibleMatchPair other = PossibleMatchPair.of(lost.reportId(), secondFound.reportId());
        Repositories repositories = repositories(
                List.of(lost, found, secondFound), Set.of(target, other));
        OfficerMatchingService service = repositories.service();
        service.enter();
        service.select(Section.LINKED, target.firstId(), target.secondId());

        MatchingWorkspaceState unlinked = service.unlink(found.reportId(), lost.reportId());

        assertEquals(Feedback.UNLINKED, unlinked.feedback().orElseThrow());
        assertEquals(Set.of(other), repositories.links.links);
        assertEquals(target, unlinked.suggestions().getFirst().pair());
        assertTrue(unlinked.linkEnabled());

        MatchingWorkspaceState repeated = service.unlink(target.firstId(), target.secondId());
        assertEquals(Feedback.ALREADY_UNLINKED, repeated.feedback().orElseThrow());
    }

    @Test
    void keepsMissingAndNonQualifyingLinksReachableForUnlink() {
        ItemReport lost = report(1, ReportType.LOST, "Bottle", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Different", "Elsewhere");
        PossibleMatchPair nonQualifying = PossibleMatchPair.of(lost.reportId(), found.reportId());
        PossibleMatchPair missing = PossibleMatchPair.of(lost.reportId(), id(9));
        Repositories repositories = repositories(
                List.of(lost, found), Set.of(nonQualifying, missing));

        MatchingWorkspaceState state = repositories.service().enter();

        assertEquals(List.of(RowKind.LINKED_NON_QUALIFYING,
                        RowKind.LINKED_REPORT_UNAVAILABLE),
                state.linkedPairs().stream().map(MatchingWorkspaceState.PairRow::kind).toList());
        assertTrue(state.linkedPairs().stream().allMatch(row -> row.rulePoints().isEmpty()));
    }

    @Test
    void loadAndActionFailuresRemainTruthfulAndRetryable() {
        ItemReport lost = report(1, ReportType.LOST, "Bottle", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Bottle", "Library");
        Repositories repositories = repositories(List.of(lost, found), Set.of());
        repositories.reports.failLoad = true;
        OfficerMatchingService service = repositories.service();

        MatchingWorkspaceState unavailable = service.enter();
        assertEquals(Availability.UNAVAILABLE, unavailable.availability());
        assertEquals(Feedback.REPORT_LOAD_FAILED, unavailable.feedback().orElseThrow());
        assertTrue(unavailable.retryVisible());
        assertTrue(unavailable.suggestions().isEmpty());

        repositories.reports.failLoad = false;
        MatchingWorkspaceState recovered = service.retry();
        assertEquals(Availability.READY, recovered.availability());
        service.select(Section.SUGGESTIONS, lost.reportId(), found.reportId());
        repositories.links.failWrite = true;

        MatchingWorkspaceState failed = service.link(lost.reportId(), found.reportId());
        assertEquals(Feedback.LINK_FAILED, failed.feedback().orElseThrow());
        assertTrue(failed.lastKnownState());
        assertTrue(failed.linkEnabled());
        assertTrue(repositories.links.links.isEmpty());
    }

    @Test
    void relationshipLoadFailureIsUnavailableAndActionPrechecksAreTyped() {
        ItemReport lost = report(1, ReportType.LOST, "Bottle", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Bottle", "Library");
        Repositories repositories = repositories(List.of(lost, found), Set.of());
        OfficerMatchingService service = repositories.service();
        repositories.links.failLoad = true;

        MatchingWorkspaceState unavailable = service.enter();
        assertEquals(Availability.UNAVAILABLE, unavailable.availability());
        assertEquals(Feedback.RELATIONSHIP_LOAD_FAILED,
                unavailable.feedback().orElseThrow());

        repositories.links.failLoad = false;
        service.retry();
        service.select(Section.SUGGESTIONS, lost.reportId(), found.reportId());
        repositories.reports.failLoad = true;
        MatchingWorkspaceState retained = service.link(lost.reportId(), found.reportId());
        assertEquals(Feedback.REPORT_LOAD_FAILED, retained.feedback().orElseThrow());
        assertTrue(retained.lastKnownState());
        assertTrue(retained.linkEnabled());
    }

    @Test
    void refreshPreservesSelectionOnlyInSameSectionAndClearDropsEverything() {
        ItemReport lost = report(1, ReportType.LOST, "Bottle", "Library");
        ItemReport found = report(2, ReportType.FOUND, "Bottle", "Library");
        PossibleMatchPair pair = PossibleMatchPair.of(lost.reportId(), found.reportId());
        Repositories repositories = repositories(List.of(lost, found), Set.of());
        OfficerMatchingService service = repositories.service();
        service.enter();
        service.select(Section.SUGGESTIONS, pair.firstId(), pair.secondId());
        assertTrue(service.refresh().selectedComparison().isPresent());

        repositories.links.links.add(pair);
        assertTrue(service.refresh().selectedComparison().isEmpty());

        MatchingWorkspaceState cleared = service.clear();
        assertEquals(Availability.NOT_LOADED, cleared.availability());
        assertTrue(cleared.suggestions().isEmpty());
        assertTrue(cleared.linkedPairs().isEmpty());
        assertTrue(cleared.selectedComparison().isEmpty());
    }

    private static Repositories repositories(List<ItemReport> reports,
            Set<PossibleMatchPair> links) {
        return new Repositories(new ScriptedReportRepository(reports),
                new ScriptedMatchRepository(links));
    }

    private static ItemReport report(int sequence, ReportType type, String name,
            String location) {
        return ItemReport.restore(id(sequence), "synthetic-reporter-" + sequence,
                type, name, ItemCategory.WATER_BOTTLES, location,
                LocalDate.of(2026, 9, 1 + sequence),
                "synthetic-public-" + sequence, "synthetic-private-" + sequence,
                sequence % 2 == 0 ? ReportStatus.UNDER_REVIEW : ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }

    private static UUID id(int sequence) {
        return new UUID(0L, sequence);
    }

    private record Repositories(ScriptedReportRepository reports,
            ScriptedMatchRepository links) {
        private OfficerMatchingService service() {
            return new OfficerMatchingService(reports, links, new DeterministicMatcher());
        }
    }

    private static final class ScriptedReportRepository implements ReportRepository {
        private List<ItemReport> reports;

        private boolean failLoad;

        private ScriptedReportRepository(List<ItemReport> initialReports) {
            reports = List.copyOf(initialReports);
        }

        @Override
        public List<ItemReport> loadAll() throws ReportStoreException {
            if (failLoad) {
                throw new ReportStoreException(
                        ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE);
            }
            return reports;
        }

        @Override
        public void insert(ItemReport report) {
            throw new AssertionError("Matching must not insert reports.");
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement) {
            throw new AssertionError("Matching must not replace reports.");
        }
    }

    private static final class ScriptedMatchRepository implements PossibleMatchRepository {
        private final Set<PossibleMatchPair> links;

        private boolean failWrite;

        private boolean failLoad;

        private ScriptedMatchRepository(Set<PossibleMatchPair> initialLinks) {
            links = new HashSet<>(initialLinks);
        }

        @Override
        public Set<PossibleMatchPair> loadAll() throws PossibleMatchStoreException {
            if (failLoad) {
                throw new PossibleMatchStoreException(
                        PossibleMatchStoreException.Reason.READ_FAILURE);
            }
            return Set.copyOf(links);
        }

        @Override
        public boolean link(PossibleMatchPair pair) throws PossibleMatchStoreException {
            if (failWrite) {
                throw new PossibleMatchStoreException(
                        PossibleMatchStoreException.Reason.WRITE_FAILURE);
            }
            return links.add(pair);
        }

        @Override
        public boolean unlink(PossibleMatchPair pair) throws PossibleMatchStoreException {
            if (failWrite) {
                throw new PossibleMatchStoreException(
                        PossibleMatchStoreException.Reason.WRITE_FAILURE);
            }
            return links.remove(pair);
        }
    }
}
