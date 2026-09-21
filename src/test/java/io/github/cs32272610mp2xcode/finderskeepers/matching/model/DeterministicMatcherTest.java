package io.github.cs32272610mp2xcode.finderskeepers.matching.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;

class DeterministicMatcherTest {
    private final DeterministicMatcher matcher = new DeterministicMatcher();

    @Test
    void evaluatesExactRulesAndThresholds() {
        ItemReport lost = report(1, ReportType.LOST, "Blue Pencil-Case",
                "Library, Level 2", LocalDate.of(2026, 9, 1));
        ItemReport found = report(2, ReportType.FOUND, "pencil case blue",
                "library - level 2", LocalDate.of(2026, 9, 8));

        MatchEvaluation strong = matcher.evaluate(lost, found).orElseThrow();
        MatchEvaluation reversed = matcher.evaluate(found, lost).orElseThrow();

        assertEquals(strong, reversed);
        assertEquals(100, strong.totalPoints());
        assertTrue(strong.qualifies());
        assertEquals(List.of("blue", "case", "pencil"), strong.sharedTokens());
        assertEquals(7, strong.dayGap());
        assertEquals(List.of(40, 20, 30, 10),
                strong.components().stream().map(MatchEvaluation.CriterionResult::points).toList());

        MatchEvaluation threshold = matcher.evaluate(lost,
                report(3, ReportType.FOUND, "pencil pouch", "Canteen",
                        LocalDate.of(2026, 9, 1))).orElseThrow();
        assertEquals(70, threshold.totalPoints());
        assertTrue(threshold.qualifies());

        MatchEvaluation belowThreshold = matcher.evaluate(lost,
                report(4, ReportType.FOUND, "different object", "Canteen",
                        LocalDate.of(2026, 9, 1))).orElseThrow();
        assertEquals(50, belowThreshold.totalPoints());
        assertFalse(belowThreshold.qualifies());
    }

    @Test
    void enforcesCategoryAndDirectionalDateGates() {
        ItemReport lost = report(1, ReportType.LOST, "Red jacket", "Canteen",
                LocalDate.of(2026, 9, 1));
        ItemReport dayEight = report(2, ReportType.FOUND, "jacket red", "canteen",
                LocalDate.of(2026, 9, 9));
        MatchEvaluation late = matcher.evaluate(lost, dayEight).orElseThrow();

        assertEquals(90, late.totalPoints());
        assertFalse(late.qualifies());
        assertFalse(late.components().get(MatchEvaluation.Criterion.DATE.ordinal()).passed());

        ItemReport before = report(3, ReportType.FOUND, "jacket red", "canteen",
                LocalDate.of(2026, 8, 31));
        assertFalse(matcher.evaluate(lost, before).orElseThrow().qualifies());

        ItemReport otherCategory = ItemReport.restore(
                id(4), "synthetic-reporter", ReportType.FOUND, "jacket red",
                ItemCategory.BAGS, "canteen", LocalDate.of(2026, 9, 1),
                "Synthetic public description", "Synthetic private detail",
                ReportStatus.SUBMITTED, Instant.parse("2026-09-20T01:02:03.456Z"));
        assertFalse(matcher.evaluate(lost, otherCategory).orElseThrow().qualifies());
    }

    @Test
    void appliesUnicodeCodePointTokenAndSeparatorRules() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            ItemReport lost = report(1, ReportType.LOST, "A PEN pen 𐐀𐐁",
                    "Hall★A", LocalDate.of(2026, 9, 1));
            ItemReport found = report(2, ReportType.FOUND, "pen 𐐀𐐁",
                    "hall-a", LocalDate.of(2026, 9, 1));

            MatchEvaluation evaluation = matcher.evaluate(lost, found).orElseThrow();

            assertEquals(List.of("pen", "𐐨𐐩"), evaluation.sharedTokens());
            assertEquals(100, evaluation.totalPoints());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void doesNotApplySubstringStemmingOrUnicodeNormalization() {
        ItemReport lost = report(1, ReportType.LOST, "notebook bottle café", "Library",
                LocalDate.of(2026, 9, 1));
        ItemReport found = report(2, ReportType.FOUND, "book bottles café", "Elsewhere",
                LocalDate.of(2026, 9, 1));

        MatchEvaluation evaluation = matcher.evaluate(lost, found).orElseThrow();

        assertEquals(List.of(), evaluation.sharedTokens());
        assertEquals(50, evaluation.totalPoints());
        assertFalse(evaluation.qualifies());
    }

    @Test
    void generationIsUniqueStableAndIndependentOfEncounterOrder() {
        ItemReport lost = report(3, ReportType.LOST, "Green bottle", "Library",
                LocalDate.of(2026, 9, 1));
        ItemReport lowerIdFound = report(1, ReportType.FOUND, "Green bottle", "Library",
                LocalDate.of(2026, 9, 2));
        ItemReport higherIdFound = report(2, ReportType.FOUND, "Green bottle", "Library",
                LocalDate.of(2026, 9, 2));
        List<ItemReport> original = List.of(lost, higherIdFound, lowerIdFound);
        List<ItemReport> shuffled = new ArrayList<>(original);
        Collections.reverse(shuffled);

        DeterministicMatcher.Generation first = matcher.generate(original);
        DeterministicMatcher.Generation second = matcher.generate(shuffled);

        assertTrue(first.hasEligiblePairs());
        assertEquals(first, second);
        assertEquals(List.of(PossibleMatchPair.of(lost.reportId(), lowerIdFound.reportId()),
                        PossibleMatchPair.of(lost.reportId(), higherIdFound.reportId())),
                first.qualifyingPairs().stream().map(MatchEvaluation::pair).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> first.qualifyingPairs().clear());
    }

    @Test
    void distinguishesNoEligiblePairFromNoQualifyingPair() {
        ItemReport lost = report(1, ReportType.LOST, "Notebook", "Library",
                LocalDate.of(2026, 9, 1));
        assertFalse(matcher.generate(List.of(lost)).hasEligiblePairs());

        ItemReport found = report(2, ReportType.FOUND, "Book", "Canteen",
                LocalDate.of(2026, 9, 1));
        DeterministicMatcher.Generation generation = matcher.generate(List.of(lost, found));
        assertTrue(generation.hasEligiblePairs());
        assertTrue(generation.qualifyingPairs().isEmpty());
    }

    @Test
    void rejectsDuplicateIdsAndIneligiblePairs() {
        ItemReport lost = report(1, ReportType.LOST, "Item", "Place",
                LocalDate.of(2026, 9, 1));
        ItemReport duplicate = report(1, ReportType.FOUND, "Item", "Place",
                LocalDate.of(2026, 9, 1));

        assertThrows(IllegalArgumentException.class,
                () -> matcher.generate(List.of(lost, duplicate)));
        assertEquals(Optional.empty(), matcher.evaluate(lost, lost));
        assertEquals(Optional.empty(), matcher.evaluate(lost,
                report(2, ReportType.LOST, "Item", "Place", LocalDate.of(2026, 9, 1))));
    }

    private static UUID id(int sequence) {
        return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", sequence));
    }

    private static ItemReport report(int sequence, ReportType type, String name,
            String location, LocalDate date) {
        return ItemReport.restore(
                id(sequence),
                "synthetic-reporter-" + sequence,
                type,
                name,
                ItemCategory.CLOTHING,
                location,
                date,
                "Synthetic public description " + sequence,
                "Synthetic private detail " + sequence,
                sequence % 2 == 0 ? ReportStatus.UNDER_REVIEW : ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }
}
