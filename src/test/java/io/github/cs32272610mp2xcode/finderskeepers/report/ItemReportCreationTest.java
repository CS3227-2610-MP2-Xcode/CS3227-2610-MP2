package io.github.cs32272610mp2xcode.finderskeepers.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ItemReportCreationTest {
    private static final UUID REPORT_ID = UUID.fromString(
            "123e4567-e89b-12d3-a456-426614174000");

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-19T04:05:06.789456Z"),
            ZoneId.of("Asia/Singapore"));

    @Test
    void createsValidLostReportWithGeneratedFieldsAndNormalizedText() {
        ReportCreationRequest request = new ReportCreationRequest(
                "  student-001  ", ReportType.LOST, "  Blue pencil case  ",
                ItemCategory.STATIONERY, "  School library  ",
                LocalDate.of(2026, 9, 18), "  Blue fabric case  ",
                "  Synthetic star-shaped label inside  ");

        ItemReport report = ItemReport.create(REPORT_ID, request, CLOCK);

        assertEquals(REPORT_ID, report.reportId());
        assertEquals("student-001", report.reporterId());
        assertEquals(ReportType.LOST, report.reportType());
        assertEquals("Blue pencil case", report.itemName());
        assertEquals(ItemCategory.STATIONERY, report.category());
        assertEquals("School library", report.location());
        assertEquals(LocalDate.of(2026, 9, 18), report.occurrenceDate());
        assertEquals("Blue fabric case", report.publicDescription());
        assertEquals("Synthetic star-shaped label inside",
                report.privateIdentifyingDetail());
        assertEquals(ReportStatus.SUBMITTED, report.status());
        assertEquals(Instant.parse("2026-09-19T04:05:06.789Z"), report.createdAt());
        assertEquals("2026-09-19T04:05:06.789Z",
                ReportConstraints.formatCreationTime(report.createdAt()));
    }

    @Test
    void createsValidFoundReportOccurringToday() {
        ReportCreationRequest request = validRequest(
                ReportType.FOUND, LocalDate.of(2026, 9, 19));

        ItemReport report = ItemReport.create(REPORT_ID, request, CLOCK);

        assertEquals(ReportType.FOUND, report.reportType());
        assertEquals(ReportStatus.SUBMITTED, report.status());
    }

    @Test
    void keepsPublicAndPrivateDescriptionsSeparateAndPrivateDetailOutOfToString() {
        ReportCreationRequest request = validRequest(
                ReportType.LOST, LocalDate.of(2026, 9, 18));
        ItemReport report = ItemReport.create(REPORT_ID, request, CLOCK);

        assertEquals("Blue fabric case", report.publicDescription());
        assertEquals("Synthetic star-shaped label inside",
                report.privateIdentifyingDetail());
        assertFalse(report.toString().contains(report.privateIdentifyingDetail()));
        assertFalse(request.toString().contains(request.publicDescription()));
        assertFalse(request.toString().contains(request.privateIdentifyingDetail()));
    }

    @Test
    void creationTimeStorageFormatRequiresExactlyMillisecondPrecision() {
        Instant creationTime = Instant.parse("2026-09-19T04:05:06.789Z");

        String storedValue = ReportConstraints.formatCreationTime(creationTime);

        assertEquals("2026-09-19T04:05:06.789Z", storedValue);
        assertEquals(creationTime, ReportConstraints.parseCreationTime(storedValue));
        assertThrows(DateTimeParseException.class,
                () -> ReportConstraints.parseCreationTime("2026-09-19T04:05:06Z"));
        assertThrows(DateTimeParseException.class,
                () -> ReportConstraints.parseCreationTime(
                        "2026-09-19T04:05:06.789123Z"));
    }

    private static ReportCreationRequest validRequest(ReportType type, LocalDate date) {
        return new ReportCreationRequest(
                "student-001", type, "Blue pencil case", ItemCategory.STATIONERY,
                "School library", date, "Blue fabric case",
                "Synthetic star-shaped label inside");
    }
}
