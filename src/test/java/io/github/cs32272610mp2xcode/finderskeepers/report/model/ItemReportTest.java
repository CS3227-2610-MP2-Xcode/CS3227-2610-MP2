package io.github.cs32272610mp2xcode.finderskeepers.report.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

class ItemReportTest {
    private static final UUID REPORT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final Instant CREATED_AT = Instant.parse("2026-09-20T01:02:03.456Z");

    @Test
    void canonicalReportRequiresEveryFieldAndPreservesAcceptedValuesExactly() {
        ItemReport report = report(UnaryOperator.identity());

        assertTrue(report.reporterId().equals(" reporter-01 "), "Reporter ID must be preserved exactly");
        assertTrue(report.itemName().equals(" Item name "), "Item name must be preserved exactly");
        assertTrue(report.location().equals(" Location "), "Location must be preserved exactly");
        assertTrue(report.publicDescription().equals(" Public description "),
                "Public description must be preserved exactly");
        assertTrue(report.privateIdentifyingDetail().equals(" Private detail "),
                "Private detail must be preserved exactly");

        for (int field = 0; field < 11; field++) {
            int nullField = field;
            assertThrows(NullPointerException.class, () -> reportWithNullField(nullField),
                    "Every canonical field must reject null");
        }

        assertTextBoundary(128, value -> reportWithReporterId(value));
        assertTextBoundary(100, value -> reportWithItemName(value));
        assertTextBoundary(120, value -> reportWithLocation(value));
        assertTextBoundary(500, value -> reportWithPublicDescription(value));
        assertTextBoundary(500, value -> reportWithPrivateDetail(value));
    }

    @Test
    void canonicalReportUsesApprovedIdentityEnumsAndMillisecondPrecision() {
        ItemReport first = report(UnaryOperator.identity());
        ItemReport equivalent = report(UnaryOperator.identity());

        assertTrue(first.equals(equivalent), "Equivalent canonical reports must compare equally");
        assertTrue(first.reportId().equals(REPORT_ID), "Report identity must use the supplied UUID");
        assertTrue(Arrays.equals(ReportType.values(), new ReportType[] {ReportType.LOST, ReportType.FOUND}),
                "Report types must match the approved storage tokens");
        assertTrue(Arrays.equals(ItemCategory.values(), new ItemCategory[] {ItemCategory.OTHER}),
                "Item categories must match the approved storage tokens");
        assertTrue(Arrays.equals(
                ReportStatus.values(), new ReportStatus[] {ReportStatus.SUBMITTED, ReportStatus.UNDER_REVIEW}),
                "Report statuses must match the approved storage tokens");
        assertNotNull(reportWithCreatedAt(Instant.parse("2026-09-20T01:02:03.000Z")),
                "Whole-millisecond precision must be accepted");
        assertThrows(IllegalArgumentException.class,
                () -> reportWithCreatedAt(Instant.parse("2026-09-20T01:02:03.000000001Z")),
                "Sub-millisecond precision must be rejected");
    }

    @Test
    void canonicalReportStringRepresentationDisclosesNoFieldValues() {
        ItemReport report = report(UnaryOperator.identity());

        assertTrue(report.toString().equals("ItemReport[redacted]"),
                "The report string representation must be fixed and redacted");
    }

    private static void assertTextBoundary(int maximumCodePoints, ReportFactory factory) {
        assertNotNull(factory.create("x"), "One code point must be accepted");
        assertNotNull(factory.create("😀".repeat(maximumCodePoints)),
                "The inclusive supplementary-code-point maximum must be accepted");
        assertThrows(IllegalArgumentException.class, () -> factory.create(""),
                "An empty canonical text field must be rejected");
        assertThrows(IllegalArgumentException.class, () -> factory.create("x".repeat(maximumCodePoints + 1)),
                "A value above the code-point maximum must be rejected");
    }

    private static ItemReport report(UnaryOperator<String> values) {
        return new ItemReport(
                REPORT_ID,
                values.apply(" reporter-01 "),
                ReportType.LOST,
                values.apply(" Item name "),
                ItemCategory.OTHER,
                values.apply(" Location "),
                LocalDate.of(2026, 9, 19),
                values.apply(" Public description "),
                values.apply(" Private detail "),
                ReportStatus.SUBMITTED,
                CREATED_AT);
    }

    private static ItemReport reportWithNullField(int field) {
        return new ItemReport(
                field == 0 ? null : REPORT_ID,
                field == 1 ? null : "reporter-01",
                field == 2 ? null : ReportType.LOST,
                field == 3 ? null : "Item name",
                field == 4 ? null : ItemCategory.OTHER,
                field == 5 ? null : "Location",
                field == 6 ? null : LocalDate.of(2026, 9, 19),
                field == 7 ? null : "Public description",
                field == 8 ? null : "Private detail",
                field == 9 ? null : ReportStatus.SUBMITTED,
                field == 10 ? null : CREATED_AT);
    }

    private static ItemReport reportWithReporterId(String value) {
        return report(values -> values.equals(" reporter-01 ") ? value : values);
    }

    private static ItemReport reportWithItemName(String value) {
        return report(values -> values.equals(" Item name ") ? value : values);
    }

    private static ItemReport reportWithLocation(String value) {
        return report(values -> values.equals(" Location ") ? value : values);
    }

    private static ItemReport reportWithPublicDescription(String value) {
        return report(values -> values.equals(" Public description ") ? value : values);
    }

    private static ItemReport reportWithPrivateDetail(String value) {
        return report(values -> values.equals(" Private detail ") ? value : values);
    }

    private static ItemReport reportWithCreatedAt(Instant createdAt) {
        return new ItemReport(
                REPORT_ID,
                "reporter-01",
                ReportType.FOUND,
                "Item name",
                ItemCategory.OTHER,
                "Location",
                LocalDate.of(2026, 9, 19),
                "Public description",
                "Private detail",
                ReportStatus.UNDER_REVIEW,
                createdAt);
    }

    @FunctionalInterface
    private interface ReportFactory {
        ItemReport create(String value);
    }
}
