package io.github.cs32272610mp2xcode.finderskeepers.report.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * The canonical immutable state of one lost-or-found report.
 *
 * @param reportId globally unique report identity
 * @param reporterId exact identity of the Student who owns the report
 * @param reportType whether the belonging was lost or found
 * @param itemName Student-provided item name
 * @param category broad item category
 * @param location occurrence location
 * @param occurrenceDate date on which the loss or find occurred
 * @param publicDescription description suitable for ordinary report views
 * @param privateIdentifyingDetail detail reserved for trusted workflows
 * @param status current review status
 * @param createdAt report creation instant at millisecond precision
 */
public record ItemReport(
        UUID reportId,
        String reporterId,
        ReportType reportType,
        String itemName,
        ItemCategory category,
        String location,
        LocalDate occurrenceDate,
        String publicDescription,
        String privateIdentifyingDetail,
        ReportStatus status,
        Instant createdAt) {
    private static final int MAX_REPORTER_ID_CODE_POINTS = 128;

    private static final int MAX_ITEM_NAME_CODE_POINTS = 100;

    private static final int MAX_LOCATION_CODE_POINTS = 120;

    private static final int MAX_DESCRIPTION_CODE_POINTS = 500;

    /**
     * Validates the minimum shared report contract without normalizing values.
     */
    public ItemReport {
        Objects.requireNonNull(reportId, "reportId");
        Objects.requireNonNull(reportType, "reportType");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(occurrenceDate, "occurrenceDate");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        if (createdAt.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException("createdAt must have millisecond precision");
        }
        reporterId = requireLength(reporterId, MAX_REPORTER_ID_CODE_POINTS, "reporterId");
        itemName = requireLength(itemName, MAX_ITEM_NAME_CODE_POINTS, "itemName");
        location = requireLength(location, MAX_LOCATION_CODE_POINTS, "location");
        publicDescription = requireLength(
                publicDescription, MAX_DESCRIPTION_CODE_POINTS, "publicDescription");
        privateIdentifyingDetail = requireLength(
                privateIdentifyingDetail, MAX_DESCRIPTION_CODE_POINTS, "privateIdentifyingDetail");
    }

    private static String requireLength(String value, int maximumCodePoints, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints < 1 || codePoints > maximumCodePoints) {
            throw new IllegalArgumentException(fieldName + " has an invalid code-point length");
        }
        return value;
    }

    /**
     * Returns a fixed privacy-safe representation.
     *
     * @return a redacted report label
     */
    @Override
    public String toString() {
        return "ItemReport[redacted]";
    }
}
