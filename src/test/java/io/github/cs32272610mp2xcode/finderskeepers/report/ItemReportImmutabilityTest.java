package io.github.cs32272610mp2xcode.finderskeepers.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ItemReportImmutabilityTest {
    private static final UUID REPORT_ID = UUID.fromString(
            "123e4567-e89b-12d3-a456-426614174000");

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-19T04:05:06.789Z"),
            ZoneId.of("Asia/Singapore"));

    @Test
    void returnsCompleteCopyWithNewStatusWithoutMutatingOriginal() {
        ItemReport submitted = createReport();

        ItemReport underReview = submitted.withStatus(ReportStatus.UNDER_REVIEW);

        assertNotSame(submitted, underReview);
        assertEquals(ReportStatus.SUBMITTED, submitted.status());
        assertEquals(ReportStatus.UNDER_REVIEW, underReview.status());
        assertEquals(submitted.reportId(), underReview.reportId());
        assertEquals(submitted.reporterId(), underReview.reporterId());
        assertEquals(submitted.reportType(), underReview.reportType());
        assertEquals(submitted.itemName(), underReview.itemName());
        assertEquals(submitted.category(), underReview.category());
        assertEquals(submitted.location(), underReview.location());
        assertEquals(submitted.occurrenceDate(), underReview.occurrenceDate());
        assertEquals(submitted.publicDescription(), underReview.publicDescription());
        assertEquals(submitted.privateIdentifyingDetail(),
                underReview.privateIdentifyingDetail());
        assertEquals(submitted.createdAt(), underReview.createdAt());
        assertNotEquals(submitted, underReview);
    }

    @Test
    void restoresAnExactlyEqualReport() {
        ItemReport original = createReport();

        ItemReport restored = ItemReport.restore(
                original.reportId(), original.reporterId(), original.reportType(),
                original.itemName(), original.category(), original.location(),
                original.occurrenceDate(), original.publicDescription(),
                original.privateIdentifyingDetail(), original.status(),
                original.createdAt(), CLOCK);

        assertNotSame(original, restored);
        assertEquals(original, restored);
        assertEquals(original.hashCode(), restored.hashCode());
    }

    @Test
    void rejectsMissingStatusForCopy() {
        ItemReport report = createReport();

        ReportValidationException exception = assertThrows(
                ReportValidationException.class,
                () -> report.withStatus(null));

        assertEquals("status", exception.errors().getFirst().field());
    }

    private static ItemReport createReport() {
        ReportCreationRequest request = new ReportCreationRequest(
                "student-001", ReportType.LOST, "Blue pencil case",
                ItemCategory.STATIONERY, "School library",
                LocalDate.of(2026, 9, 18), "Blue fabric case",
                "Synthetic star-shaped label inside");
        return ItemReport.create(REPORT_ID, request, CLOCK);
    }
}
