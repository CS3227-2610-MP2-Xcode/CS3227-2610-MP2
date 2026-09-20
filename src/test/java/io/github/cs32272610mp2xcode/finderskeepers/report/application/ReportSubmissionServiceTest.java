package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.FileSystemException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportCreationRequest;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportValidationException;

class ReportSubmissionServiceTest {
    private static final UUID REPORT_ID =
            UUID.fromString("6df3ad50-0969-4e57-a3fd-77ec15f1e073");

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-20T11:15:20.123Z"), ZoneOffset.UTC);

    @Test
    void validReportIsSavedExactlyOnceBeforeSuccessIsReturned() {
        AtomicInteger saves = new AtomicInteger();
        AtomicReference<ItemReport> saved = new AtomicReference<>();
        ReportSubmissionService service = new ReportSubmissionService(
                CLOCK, () -> REPORT_ID, report -> {
                    saves.incrementAndGet();
                    saved.set(report);
                });

        ItemReport result = service.submit(validRequest());

        assertEquals(1, saves.get());
        assertSame(saved.get(), result);
        assertEquals(REPORT_ID, result.reportId());
        assertEquals(ReportStatus.SUBMITTED, result.status());
    }

    @Test
    void invalidReportNeverReachesStorage() {
        AtomicInteger saves = new AtomicInteger();
        ReportSubmissionService service = new ReportSubmissionService(
                CLOCK, () -> REPORT_ID, report -> saves.incrementAndGet());
        ReportCreationRequest invalid = new ReportCreationRequest(
                "student-001", ReportType.LOST, " ", ItemCategory.STATIONERY,
                "Library", LocalDate.of(2026, 9, 19),
                "Blue pencil case", "Synthetic star sticker");

        assertThrows(ReportValidationException.class,
                () -> service.submit(invalid));
        assertEquals(0, saves.get());
    }

    @Test
    void storageFailureIsPropagatedForSafePresentation() {
        ReportSubmissionException storageFailure = new ReportSubmissionException(
                new FileSystemException("synthetic-report-store"));
        ReportSubmissionService service = new ReportSubmissionService(
                CLOCK, () -> REPORT_ID, report -> {
                    throw storageFailure;
                });

        ReportSubmissionException exception = assertThrows(
                ReportSubmissionException.class,
                () -> service.submit(validRequest()));

        assertSame(storageFailure, exception);
    }

    private static ReportCreationRequest validRequest() {
        return new ReportCreationRequest(
                "student-001",
                ReportType.FOUND,
                "Blue pencil case",
                ItemCategory.STATIONERY,
                "Library shelf 2",
                LocalDate.of(2026, 9, 19),
                "Blue case with a white zipper.",
                "Contains a synthetic blue star sticker.");
    }
}
