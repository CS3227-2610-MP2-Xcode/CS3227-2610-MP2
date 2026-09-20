package io.github.cs32272610mp2xcode.finderskeepers.report;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ItemReportValidationTest {
    private static final UUID REPORT_ID = UUID.fromString(
            "123e4567-e89b-12d3-a456-426614174000");

    private static final Instant NOW = Instant.parse("2026-09-19T04:05:06.789Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Singapore"));

    @ParameterizedTest(name = "{index}: rejects missing {1}")
    @MethodSource("missingRequestFields")
    void rejectsEachMissingRequestField(ReportCreationRequest request, String field) {
        ReportValidationException exception = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.create(REPORT_ID, request, CLOCK));

        assertHasField(exception, field);
    }

    @ParameterizedTest(name = "{index}: rejects blank {1}")
    @MethodSource("blankTextFields")
    void rejectsEachBlankTextField(ReportCreationRequest request, String field) {
        ReportValidationException exception = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.create(REPORT_ID, request, CLOCK));

        assertHasField(exception, field);
    }

    @Test
    void rejectsMissingReportIdAndRequest() {
        ReportValidationException missingId = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.create(null, validRequest(), CLOCK));
        ReportValidationException missingRequest = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.create(REPORT_ID, null, CLOCK));

        assertHasField(missingId, "reportId");
        assertHasField(missingRequest, "request");
    }

    @Test
    void rejectsFutureOccurrenceDateButAcceptsToday() {
        ReportCreationRequest futureRequest = requestWith(
                "student-001", ReportType.LOST, "Blue pencil case",
                ItemCategory.STATIONERY, "School library",
                LocalDate.of(2026, 9, 20), "Blue fabric case",
                "Synthetic star-shaped label inside");
        ReportCreationRequest todayRequest = requestWith(
                "student-001", ReportType.LOST, "Blue pencil case",
                ItemCategory.STATIONERY, "School library",
                LocalDate.of(2026, 9, 19), "Blue fabric case",
                "Synthetic star-shaped label inside");

        ReportValidationException exception = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.create(REPORT_ID, futureRequest, CLOCK));

        assertHasField(exception, "occurrenceDate");
        assertDoesNotThrow(() -> ItemReport.create(REPORT_ID, todayRequest, CLOCK));
    }

    @Test
    void acceptsEveryTextLimitAndCountsUnicodeCodePoints() {
        ReportCreationRequest request = requestWith(
                "r".repeat(ReportConstraints.MAX_REPORTER_ID_LENGTH),
                ReportType.LOST,
                "🎒".repeat(ReportConstraints.MAX_ITEM_NAME_LENGTH),
                ItemCategory.BAGS,
                "l".repeat(ReportConstraints.MAX_LOCATION_LENGTH),
                LocalDate.of(2026, 9, 18),
                "p".repeat(ReportConstraints.MAX_PUBLIC_DESCRIPTION_LENGTH),
                "i".repeat(ReportConstraints.MAX_PRIVATE_IDENTIFYING_DETAIL_LENGTH));

        assertDoesNotThrow(() -> ItemReport.create(REPORT_ID, request, CLOCK));
    }

    @Test
    void reportsEveryTextFieldThatExceedsItsLimit() {
        ReportCreationRequest request = requestWith(
                "r".repeat(ReportConstraints.MAX_REPORTER_ID_LENGTH + 1),
                ReportType.LOST,
                "n".repeat(ReportConstraints.MAX_ITEM_NAME_LENGTH + 1),
                ItemCategory.BAGS,
                "l".repeat(ReportConstraints.MAX_LOCATION_LENGTH + 1),
                LocalDate.of(2026, 9, 18),
                "p".repeat(ReportConstraints.MAX_PUBLIC_DESCRIPTION_LENGTH + 1),
                "i".repeat(ReportConstraints.MAX_PRIVATE_IDENTIFYING_DETAIL_LENGTH + 1));

        ReportValidationException exception = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.create(REPORT_ID, request, CLOCK));

        assertEquals(List.of(
                "reporterId", "itemName", "location",
                "publicDescription", "privateIdentifyingDetail"),
                exception.errors().stream().map(ValidationError::field).toList());
    }

    @Test
    void rejectsMissingStoredFieldsAndSubMillisecondCreationTime() {
        ReportValidationException missingFields = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.restore(
                        REPORT_ID, "student-001", ReportType.LOST,
                        "Blue pencil case", ItemCategory.STATIONERY,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic star-shaped label inside",
                        null, null, CLOCK));
        ReportValidationException excessivePrecision = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.restore(
                        REPORT_ID, "student-001", ReportType.LOST,
                        "Blue pencil case", ItemCategory.STATIONERY,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic star-shaped label inside",
                        ReportStatus.SUBMITTED,
                        Instant.parse("2026-09-19T04:05:06.789123Z"), CLOCK));

        assertHasField(missingFields, "status");
        assertHasField(missingFields, "createdAt");
        assertHasField(excessivePrecision, "createdAt");
    }

    @Test
    void collectsReadableErrorsAndExposesAnImmutableList() {
        ReportCreationRequest request = requestWith(
                " ", null, "", null, "\t", null, "\n", " ");

        ReportValidationException exception = assertThrows(
                ReportValidationException.class,
                () -> ItemReport.create(REPORT_ID, request, CLOCK));

        assertEquals(8, exception.errors().size());
        assertTrue(exception.getMessage().contains("Reporter ID cannot be blank."));
        assertThrows(UnsupportedOperationException.class,
                () -> exception.errors().add(
                        new ValidationError("extra", "Synthetic extra error.")));
    }

    private static Stream<Arguments> missingRequestFields() {
        return Stream.of(
                Arguments.of(requestWith(
                        null, ReportType.LOST, "Blue pencil case", ItemCategory.STATIONERY,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "reporterId"),
                Arguments.of(requestWith(
                        "student-001", null, "Blue pencil case", ItemCategory.STATIONERY,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "reportType"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, null, ItemCategory.STATIONERY,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "itemName"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case", null,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "category"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, null, LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "location"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, "School library", null,
                        "Blue fabric case", "Synthetic detail"), "occurrenceDate"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, "School library",
                        LocalDate.of(2026, 9, 18), null, "Synthetic detail"),
                        "publicDescription"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, "School library",
                        LocalDate.of(2026, 9, 18), "Blue fabric case", null),
                        "privateIdentifyingDetail"));
    }

    private static Stream<Arguments> blankTextFields() {
        return Stream.of(
                Arguments.of(requestWith(
                        " \t", ReportType.LOST, "Blue pencil case", ItemCategory.STATIONERY,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "reporterId"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "\n", ItemCategory.STATIONERY,
                        "School library", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "itemName"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, " ", LocalDate.of(2026, 9, 18),
                        "Blue fabric case", "Synthetic detail"), "location"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, "School library",
                        LocalDate.of(2026, 9, 18), "\t", "Synthetic detail"),
                        "publicDescription"),
                Arguments.of(requestWith(
                        "student-001", ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, "School library",
                        LocalDate.of(2026, 9, 18), "Blue fabric case", "\n"),
                        "privateIdentifyingDetail"));
    }

    private static ReportCreationRequest validRequest() {
        return requestWith(
                "student-001", ReportType.LOST, "Blue pencil case",
                ItemCategory.STATIONERY, "School library",
                LocalDate.of(2026, 9, 18), "Blue fabric case",
                "Synthetic star-shaped label inside");
    }

    private static ReportCreationRequest requestWith(String reporterId,
            ReportType reportType, String itemName, ItemCategory category,
            String location, LocalDate occurrenceDate, String publicDescription,
            String privateIdentifyingDetail) {
        return new ReportCreationRequest(
                reporterId, reportType, itemName, category, location,
                occurrenceDate, publicDescription, privateIdentifyingDetail);
    }

    private static void assertHasField(
            ReportValidationException exception, String field) {
        assertTrue(exception.errors().stream()
                .anyMatch(error -> error.field().equals(field)));
    }
}
