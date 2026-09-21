package io.github.cs32272610mp2xcode.finderskeepers.matching.model;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;

/** Pure deterministic implementation of the approved possible-match policy. */
public final class DeterministicMatcher {
    private static final Comparator<MatchEvaluation> SUGGESTION_ORDER =
            Comparator.comparingInt(MatchEvaluation::totalPoints).reversed()
                    .thenComparing(MatchEvaluation::pair, PossibleMatchPair.CANONICAL_ORDER);

    /** Immutable generated result before relationship-state partitioning. */
    public record Generation(boolean hasEligiblePairs,
            List<MatchEvaluation> qualifyingPairs) {
        /** Defensively copies generated evidence. */
        public Generation {
            qualifyingPairs = List.copyOf(qualifyingPairs);
        }
    }

    /**
     * Generates every qualifying LOST-to-FOUND pair in deterministic order.
     *
     * @param reports canonical report snapshot
     * @return generated qualifying evidence and eligible-pair fact
     */
    public Generation generate(List<ItemReport> reports) {
        Objects.requireNonNull(reports, "reports");
        List<ItemReport> lostReports = new ArrayList<>();
        List<ItemReport> foundReports = new ArrayList<>();
        Set<UUID> reportIds = new HashSet<>();
        Map<UUID, NormalizedText> normalized = new HashMap<>();

        for (ItemReport report : reports) {
            Objects.requireNonNull(report, "report member");
            if (!reportIds.add(report.reportId())) {
                throw new IllegalArgumentException("Report snapshot contains a duplicate report ID.");
            }
            if (!eligibleStatus(report.status())) {
                continue;
            }
            if (report.reportType() == ReportType.LOST) {
                lostReports.add(report);
            } else if (report.reportType() == ReportType.FOUND) {
                foundReports.add(report);
            }
            normalized.put(report.reportId(), normalize(report));
        }

        List<MatchEvaluation> qualifying = new ArrayList<>();
        for (ItemReport lost : lostReports) {
            for (ItemReport found : foundReports) {
                if (lost.reportId().equals(found.reportId())) {
                    continue;
                }
                MatchEvaluation evaluation = evaluateEligible(
                        lost, found, normalized.get(lost.reportId()), normalized.get(found.reportId()));
                if (evaluation.qualifies()) {
                    qualifying.add(evaluation);
                }
            }
        }
        qualifying.sort(SUGGESTION_ORDER);
        return new Generation(!lostReports.isEmpty() && !foundReports.isEmpty(), qualifying);
    }

    /**
     * Evaluates one pair in either input order.
     *
     * @return complete four-rule evidence, or empty when the pair is ineligible
     */
    public Optional<MatchEvaluation> evaluate(ItemReport first, ItemReport second) {
        Objects.requireNonNull(first, "first report");
        Objects.requireNonNull(second, "second report");
        if (first.reportId().equals(second.reportId())
                || first.reportType() == second.reportType()
                || !eligibleStatus(first.status())
                || !eligibleStatus(second.status())) {
            return Optional.empty();
        }
        ItemReport lost = first.reportType() == ReportType.LOST ? first : second;
        ItemReport found = first.reportType() == ReportType.FOUND ? first : second;
        return Optional.of(evaluateEligible(lost, found, normalize(lost), normalize(found)));
    }

    /** Returns the exact deterministic comparator used for suggestions. */
    public Comparator<MatchEvaluation> suggestionOrder() {
        return SUGGESTION_ORDER;
    }

    private MatchEvaluation evaluateEligible(ItemReport lost, ItemReport found,
            NormalizedText lostText, NormalizedText foundText) {
        Set<String> sharedTokens = new TreeSet<>(lostText.keywords());
        sharedTokens.retainAll(foundText.keywords());
        long dayGap = ChronoUnit.DAYS.between(
                lost.occurrenceDate(), found.occurrenceDate());
        return new MatchEvaluation(
                PossibleMatchPair.of(lost.reportId(), found.reportId()),
                lost.reportId(),
                found.reportId(),
                lost.category() == found.category(),
                sharedTokens,
                lostText.location().equals(foundText.location()),
                dayGap);
    }

    private static boolean eligibleStatus(ReportStatus status) {
        return status == ReportStatus.SUBMITTED || status == ReportStatus.UNDER_REVIEW;
    }

    private static NormalizedText normalize(ItemReport report) {
        List<String> nameRuns = letterOrDigitRuns(report.itemName());
        Set<String> keywords = new HashSet<>();
        for (String run : nameRuns) {
            if (run.codePointCount(0, run.length()) >= 2) {
                keywords.add(run);
            }
        }
        return new NormalizedText(Set.copyOf(keywords),
                String.join(" ", letterOrDigitRuns(report.location())));
    }

    private static List<String> letterOrDigitRuns(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        List<String> runs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        lower.codePoints().forEach(codePoint -> {
            if (Character.isLetterOrDigit(codePoint)) {
                current.appendCodePoint(codePoint);
            } else if (!current.isEmpty()) {
                runs.add(current.toString());
                current.setLength(0);
            }
        });
        if (!current.isEmpty()) {
            runs.add(current.toString());
        }
        return runs;
    }

    private record NormalizedText(Set<String> keywords, String location) {
    }
}
