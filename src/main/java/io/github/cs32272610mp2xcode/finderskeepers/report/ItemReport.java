package io.github.cs32272610mp2xcode.finderskeepers.report;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable lost-or-found item report. */
public final class ItemReport {
    private final UUID reportId;

    private final String reporterId;

    private final ReportType reportType;

    private final String itemName;

    private final ItemCategory category;

    private final String location;

    private final LocalDate occurrenceDate;

    private final String publicDescription;

    private final String privateIdentifyingDetail;

    private final ReportStatus status;

    private final Instant createdAt;

    private ItemReport(UUID id, String reporter, ReportType type, String name,
            ItemCategory itemCategory, String place, LocalDate date,
            String publicText, String privateText, ReportStatus reportStatus,
            Instant creationTime) {
        reportId = id;
        reporterId = reporter;
        reportType = type;
        itemName = name;
        category = itemCategory;
        location = place;
        occurrenceDate = date;
        publicDescription = publicText;
        privateIdentifyingDetail = privateText;
        status = reportStatus;
        createdAt = creationTime;
    }

    /**
     * Creates and validates a newly submitted report.
     *
     * @param reportId identifier generated for the new report
     * @param request user-supplied report information
     * @param clock clock used for date validation and creation time
     * @return validated report with {@link ReportStatus#SUBMITTED} status
     * @throws ReportValidationException if any report field is invalid
     */
    public static ItemReport create(UUID reportId, ReportCreationRequest request, Clock clock) {
        Objects.requireNonNull(clock, "clock");
        if (request == null) {
            throw validationError("request", "Report creation request is required.");
        }
        Instant creationTime = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        return validateAndBuild(reportId, request.reporterId(), request.reportType(),
                request.itemName(), request.category(), request.location(),
                request.occurrenceDate(), request.publicDescription(),
                request.privateIdentifyingDetail(), ReportStatus.SUBMITTED,
                creationTime, clock, true);
    }

    /**
     * Restores and validates a complete report read from storage.
     *
     * <p>Unlike {@link #create(UUID, ReportCreationRequest, Clock)}, this method
     * preserves valid text exactly so a save-load cycle does not rewrite stored
     * data. It validates only intrinsic record constraints; occurrence dates are
     * not compared with the current date because persisted data must remain
     * readable as time-zone rules and clocks change.</p>
     *
     * @param reportId stored report identifier
     * @param reporterId stored reporter identifier
     * @param reportType stored lost-or-found type
     * @param itemName stored item name
     * @param category stored item category
     * @param location stored occurrence location
     * @param occurrenceDate stored occurrence date
     * @param publicDescription stored public description
     * @param privateIdentifyingDetail stored private identifying detail
     * @param status stored report status
     * @param createdAt stored creation time
     * @return validated restored report
     * @throws ReportValidationException if any stored report field is invalid
     */
    public static ItemReport restore(UUID reportId, String reporterId,
            ReportType reportType, String itemName, ItemCategory category,
            String location, LocalDate occurrenceDate, String publicDescription,
            String privateIdentifyingDetail, ReportStatus status, Instant createdAt) {
        return validateAndBuild(reportId, reporterId, reportType, itemName, category,
                location, occurrenceDate, publicDescription, privateIdentifyingDetail,
                status, createdAt, null, false);
    }

    /**
     * Restores a complete report and additionally checks the occurrence date
     * against a caller-supplied clock.
     *
     * <p>This overload is useful when importing externally supplied records. A
     * repository loading the application's own previously validated data should
     * normally use the overload without a clock.</p>
     *
     * @param reportId stored report identifier
     * @param reporterId stored reporter identifier
     * @param reportType stored lost-or-found type
     * @param itemName stored item name
     * @param category stored item category
     * @param location stored occurrence location
     * @param occurrenceDate stored occurrence date
     * @param publicDescription stored public description
     * @param privateIdentifyingDetail stored private identifying detail
     * @param status stored report status
     * @param createdAt stored creation time
     * @param clock clock used to reject a future occurrence date
     * @return validated restored report
     * @throws ReportValidationException if any stored report field is invalid
     */
    public static ItemReport restore(UUID reportId, String reporterId,
            ReportType reportType, String itemName, ItemCategory category,
            String location, LocalDate occurrenceDate, String publicDescription,
            String privateIdentifyingDetail, ReportStatus status, Instant createdAt,
            Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return validateAndBuild(reportId, reporterId, reportType, itemName, category,
                location, occurrenceDate, publicDescription, privateIdentifyingDetail,
                status, createdAt, clock, false);
    }

    private static ItemReport validateAndBuild(UUID id, String reporter,
            ReportType type, String name, ItemCategory itemCategory, String place,
            LocalDate date, String publicText, String privateText,
            ReportStatus reportStatus, Instant creationTime, Clock clock,
            boolean normalizeText) {
        List<ValidationError> errors = new ArrayList<>();
        if (id == null) {
            errors.add(new ValidationError("reportId", "Report ID is required."));
        }
        String validReporter = validateText(errors, "reporterId", "Reporter ID",
                reporter, ReportConstraints.MAX_REPORTER_ID_LENGTH, normalizeText);
        if (type == null) {
            errors.add(new ValidationError("reportType", "Report type is required."));
        }
        String validName = validateText(errors, "itemName", "Item name",
                name, ReportConstraints.MAX_ITEM_NAME_LENGTH, normalizeText);
        if (itemCategory == null) {
            errors.add(new ValidationError("category", "Item category is required."));
        }
        String validPlace = validateText(errors, "location", "Location",
                place, ReportConstraints.MAX_LOCATION_LENGTH, normalizeText);
        if (date == null) {
            errors.add(new ValidationError("occurrenceDate", "Occurrence date is required."));
        } else if (clock != null && date.isAfter(LocalDate.now(clock))) {
            errors.add(new ValidationError(
                    "occurrenceDate", "Occurrence date cannot be in the future."));
        }
        String validPublicText = validateText(errors, "publicDescription",
                "Public description", publicText,
                ReportConstraints.MAX_PUBLIC_DESCRIPTION_LENGTH, normalizeText);
        String validPrivateText = validateText(errors, "privateIdentifyingDetail",
                "Private identifying detail", privateText,
                ReportConstraints.MAX_PRIVATE_IDENTIFYING_DETAIL_LENGTH, normalizeText);
        if (reportStatus == null) {
            errors.add(new ValidationError("status", "Report status is required."));
        }
        if (creationTime == null) {
            errors.add(new ValidationError("createdAt", "Creation time is required."));
        } else if (!creationTime.equals(creationTime.truncatedTo(ChronoUnit.MILLIS))) {
            errors.add(new ValidationError(
                    "createdAt", "Creation time must use millisecond precision."));
        }
        if (!errors.isEmpty()) {
            throw new ReportValidationException(errors);
        }
        return new ItemReport(id, validReporter, type, validName, itemCategory,
                validPlace, date, validPublicText, validPrivateText, reportStatus,
                creationTime);
    }

    private static String validateText(List<ValidationError> errors,
            String field, String label, String value, int maximumLength,
            boolean normalize) {
        if (value == null) {
            errors.add(new ValidationError(field, label + " is required."));
            return null;
        }
        String validatedValue = normalize ? value.strip() : value;
        if (validatedValue.isBlank()) {
            errors.add(new ValidationError(field, label + " cannot be blank."));
        } else if (validatedValue.codePointCount(0, validatedValue.length()) > maximumLength) {
            errors.add(new ValidationError(
                    field, label + " must not exceed " + maximumLength + " characters."));
        }
        return validatedValue;
    }

    private static ReportValidationException validationError(String field, String message) {
        return new ReportValidationException(List.of(new ValidationError(field, message)));
    }

    /**
     * Returns a complete immutable copy with a different status.
     *
     * <p>This method does not decide whether a workflow transition is legal.</p>
     *
     * @param newStatus status for the returned copy
     * @return complete report copy with the requested status
     * @throws ReportValidationException if the status is absent
     */
    public ItemReport withStatus(ReportStatus newStatus) {
        if (newStatus == null) {
            throw validationError("status", "Report status is required.");
        }
        return new ItemReport(reportId, reporterId, reportType, itemName, category,
                location, occurrenceDate, publicDescription, privateIdentifyingDetail,
                newStatus, createdAt);
    }

    /**
     * Returns the immutable report identifier.
     *
     * @return immutable report identifier
     */
    public UUID reportId() {
        return reportId;
    }

    /**
     * Returns the stable reporter identifier.
     *
     * @return stable reporter identifier
     */
    public String reporterId() {
        return reporterId;
    }

    /**
     * Returns whether the item was lost or found.
     *
     * @return lost-or-found report type
     */
    public ReportType reportType() {
        return reportType;
    }

    /**
     * Returns the short item name.
     *
     * @return short item name
     */
    public String itemName() {
        return itemName;
    }

    /**
     * Returns the item category.
     *
     * @return item category
     */
    public ItemCategory category() {
        return category;
    }

    /**
     * Returns where the item was lost or found.
     *
     * @return place where the item was lost or found
     */
    public String location() {
        return location;
    }

    /**
     * Returns when the item was lost or found.
     *
     * @return date when the item was lost or found
     */
    public LocalDate occurrenceDate() {
        return occurrenceDate;
    }

    /**
     * Returns the description safe to show during matching.
     *
     * @return description safe to show during matching
     */
    public String publicDescription() {
        return publicDescription;
    }

    /**
     * Returns the identifying detail reserved for verification.
     *
     * @return identifying detail reserved for verification
     */
    public String privateIdentifyingDetail() {
        return privateIdentifyingDetail;
    }

    /**
     * Returns the current processing status.
     *
     * @return current processing status
     */
    public ReportStatus status() {
        return status;
    }

    /**
     * Returns the UTC creation time.
     *
     * @return UTC creation time
     */
    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemReport that)) {
            return false;
        }
        return reportId.equals(that.reportId)
                && reporterId.equals(that.reporterId)
                && reportType == that.reportType
                && itemName.equals(that.itemName)
                && category == that.category
                && location.equals(that.location)
                && occurrenceDate.equals(that.occurrenceDate)
                && publicDescription.equals(that.publicDescription)
                && privateIdentifyingDetail.equals(that.privateIdentifyingDetail)
                && status == that.status
                && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, reporterId, reportType, itemName, category,
                location, occurrenceDate, publicDescription,
                privateIdentifyingDetail, status, createdAt);
    }

    @Override
    public String toString() {
        return "ItemReport[redacted]";
    }
}
