package io.github.cs32272610mp2xcode.finderskeepers.matching.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;

class MatchEvaluationTest {
    private final DeterministicMatcher matcher = new DeterministicMatcher();

    @Test
    void explainsEveryRuleOutcomeWithDeterministicPointsAndReasons() {
        ItemReport lost = report(1, ReportType.LOST, "Blue bottle", ItemCategory.WATER_BOTTLES,
                "Library", LocalDate.of(2026, 9, 10));
        ItemReport found = report(2, ReportType.FOUND, "Green flask", ItemCategory.BAGS,
                "Canteen", LocalDate.of(2026, 9, 9));

        MatchEvaluation evaluation = matcher.evaluate(lost, found).orElseThrow();

        assertEquals(List.of(0, 0, 0, 0), evaluation.components().stream()
                .map(MatchEvaluation.CriterionResult::points).toList());
        assertFalse(evaluation.qualifies());
        assertEquals(List.of(), evaluation.positiveReasonLabels());
        assertEquals(List.of(
                "Categories differ (0 rule points; mandatory gate failed).",
                "No shared normalized item-name keyword (0 rule points).",
                "Normalized locations differ (0 rule points).",
                "Dates are outside the directional 0-to-7-day window "
                        + "(0 rule points; mandatory gate failed)."),
                evaluation.componentReasons());
    }

    @Test
    void positiveReasonsPreserveFixedCriterionOrderAndSortedKeywords() {
        ItemReport lost = report(1, ReportType.LOST, "Zebra blue bag", ItemCategory.BAGS,
                "Hall A", LocalDate.of(2026, 9, 10));
        ItemReport found = report(2, ReportType.FOUND, "bag zebra blue", ItemCategory.BAGS,
                "hall-a", LocalDate.of(2026, 9, 17));

        MatchEvaluation evaluation = matcher.evaluate(lost, found).orElseThrow();

        assertTrue(evaluation.qualifies());
        assertEquals(100, evaluation.totalPoints());
        assertEquals(List.of("bag", "blue", "zebra"), evaluation.sharedTokens());
        assertEquals(List.of("Same canonical category", "Shared item-name keyword(s)",
                "Same normalized location", "Dates within the allowed directional window"),
                evaluation.positiveReasonLabels());
        assertEquals("Shared normalized item-name keyword(s): bag, blue, zebra "
                        + "(+20 rule points).", evaluation.componentReasons().get(1));
        assertEquals("FOUND date is 7 day(s) after LOST date (+10 rule points).",
                evaluation.componentReasons().get(3));
    }

    private static ItemReport report(long id, ReportType type, String itemName,
            ItemCategory category, String location, LocalDate occurrenceDate) {
        return ItemReport.restore(new UUID(0L, id), "synthetic-user-" + id, type, itemName,
                category, location, occurrenceDate, "Synthetic public description",
                "Synthetic private detail", ReportStatus.SUBMITTED,
                Instant.parse("2026-09-20T01:02:03.456Z"));
    }
}
