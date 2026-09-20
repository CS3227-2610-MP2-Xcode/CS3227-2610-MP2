package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmissionException;

class StudentReportFormControllerTest {
    private static final UUID REPORT_ID =
            UUID.fromString("1cb6b428-85d4-4e2f-9994-79962d2e1001");

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-20T08:00:00.123Z"), ZoneOffset.UTC);

    private static final String REPORTER_ID = "student-001";

    @Test
    void validInputUsesAuthenticatedReporterAndReturnsSavedReportId() {
        AtomicReference<ReportCreationRequest> captured = new AtomicReference<>();
        StudentReportFormController controller = new StudentReportFormController(
                REPORTER_ID, request -> {
                    captured.set(request);
                    return ItemReport.create(REPORT_ID, request, CLOCK);
                });

        SubmissionViewState state = controller.submit(validInput(ReportType.LOST));

        assertAll(
                () -> assertTrue(state.successful()),
                () -> assertEquals(REPORT_ID, state.reportId()),
                () -> assertTrue(state.message().contains(REPORT_ID.toString())),
                () -> assertTrue(state.fieldErrors().isEmpty()),
                () -> assertEquals(REPORTER_ID, captured.get().reporterId()),
                () -> assertEquals(ReportType.LOST, captured.get().reportType()));
    }

    @Test
    void foundInputCanBeSubmittedWithSubmittedStatus() {
        AtomicReference<ItemReport> saved = new AtomicReference<>();
        StudentReportFormController controller = new StudentReportFormController(
                REPORTER_ID, request -> {
                    ItemReport report = ItemReport.create(REPORT_ID, request, CLOCK);
                    saved.set(report);
                    return report;
                });

        SubmissionViewState state = controller.submit(validInput(ReportType.FOUND));

        assertAll(
                () -> assertTrue(state.successful()),
                () -> assertEquals(ReportType.FOUND, saved.get().reportType()),
                () -> assertEquals(ReportStatus.SUBMITTED, saved.get().status()));
    }

    @Test
    void validationFailureReturnsEveryEditableFieldError() {
        AtomicInteger attempts = new AtomicInteger();
        StudentReportFormController controller = new StudentReportFormController(
                REPORTER_ID, request -> {
                    attempts.incrementAndGet();
                    return ItemReport.create(REPORT_ID, request, CLOCK);
                });
        ReportFormInput invalid = new ReportFormInput(
                null, "   ", null, "", LocalDate.of(2026, 9, 21), "", "");

        SubmissionViewState state = controller.submit(invalid);

        assertAll(
                () -> assertFalse(state.successful()),
                () -> assertNull(state.reportId()),
                () -> assertEquals(1, attempts.get()),
                () -> assertEquals(7, state.fieldErrors().size()),
                () -> assertEquals("Report type is required.",
                        state.fieldErrors().get(ReportFormField.REPORT_TYPE)),
                () -> assertEquals("Occurrence date cannot be in the future.",
                        state.fieldErrors().get(ReportFormField.OCCURRENCE_DATE)));
    }

    @Test
    void submissionFailureReturnsSafeMessageWithoutTechnicalDetails() {
        IOException cause = new IOException("/private/path/reports.json denied");
        StudentReportFormController controller = new StudentReportFormController(
                REPORTER_ID, request -> {
                    throw new ReportSubmissionException(cause);
                });

        SubmissionViewState state = controller.submit(validInput(ReportType.LOST));

        assertAll(
                () -> assertFalse(state.successful()),
                () -> assertNull(state.reportId()),
                () -> assertTrue(state.fieldErrors().isEmpty()),
                () -> assertFalse(state.message().contains("/private/path")),
                () -> assertFalse(state.message().contains("denied")));
    }

    @Test
    void confirmationDoesNotExposeEitherDescription() {
        ReportFormInput input = validInput(ReportType.LOST);
        StudentReportFormController controller = new StudentReportFormController(
                REPORTER_ID,
                request -> ItemReport.create(REPORT_ID, request, CLOCK));

        SubmissionViewState state = controller.submit(input);

        assertAll(
                () -> assertFalse(state.message().contains(input.publicDescription())),
                () -> assertFalse(state.message().contains(
                        input.privateIdentifyingDetail())));
    }

    @Test
    void formInputAddsReporterWithoutChangingEditableValues() {
        ReportFormInput input = validInput(ReportType.FOUND);

        ReportCreationRequest request = input.toCreationRequest(REPORTER_ID);

        assertAll(
                () -> assertEquals(REPORTER_ID, request.reporterId()),
                () -> assertSame(input.reportType(), request.reportType()),
                () -> assertEquals(input.itemName(), request.itemName()),
                () -> assertSame(input.category(), request.category()),
                () -> assertEquals(input.location(), request.location()),
                () -> assertEquals(input.occurrenceDate(), request.occurrenceDate()),
                () -> assertEquals(input.publicDescription(), request.publicDescription()),
                () -> assertEquals(input.privateIdentifyingDetail(),
                        request.privateIdentifyingDetail()));
    }

    @Test
    void controllerRejectsMissingAuthenticatedIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new StudentReportFormController("   ", request -> null));
    }

    private static ReportFormInput validInput(ReportType type) {
        return new ReportFormInput(
                type,
                "Blue pencil case",
                ItemCategory.STATIONERY,
                "Library shelf 2",
                LocalDate.of(2026, 9, 19),
                "Blue case with a white zipper.",
                "Contains a synthetic blue star sticker.");
    }
}
