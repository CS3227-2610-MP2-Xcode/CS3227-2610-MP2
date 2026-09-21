package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;

/**
 * Privacy-safe Student history projection of one item report.
 *
 * @param reportId immutable report identifier
 * @param reportTypeLabel readable lost-or-found type
 * @param itemName Student-provided item name
 * @param categoryLabel readable broad item category
 * @param occurrenceDate date on which the loss or find occurred
 * @param publicDescription description suitable for the Student history
 * @param statusLabel readable current review status
 * @param createdAt report creation instant
 */
public record ReportHistoryEntry(
        UUID reportId,
        String reportTypeLabel,
        String itemName,
        String categoryLabel,
        LocalDate occurrenceDate,
        String publicDescription,
        String statusLabel,
        Instant createdAt) {
    /** Validates the presentation projection. */
    public ReportHistoryEntry {
        reportId = Objects.requireNonNull(reportId, "reportId");
        reportTypeLabel = Objects.requireNonNull(reportTypeLabel, "reportTypeLabel");
        itemName = Objects.requireNonNull(itemName, "itemName");
        categoryLabel = Objects.requireNonNull(categoryLabel, "categoryLabel");
        occurrenceDate = Objects.requireNonNull(occurrenceDate, "occurrenceDate");
        publicDescription = Objects.requireNonNull(publicDescription, "publicDescription");
        statusLabel = Objects.requireNonNull(statusLabel, "statusLabel");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * Creates a Student-safe projection without private identifying detail.
     *
     * @param report canonical report to project
     * @return privacy-safe history entry
     */
    public static ReportHistoryEntry from(ItemReport report) {
        Objects.requireNonNull(report, "report");
        return new ReportHistoryEntry(
                report.reportId(),
                report.reportType().displayName(),
                report.itemName(),
                report.category().displayName(),
                report.occurrenceDate(),
                report.publicDescription(),
                report.status().displayName(),
                report.createdAt());
    }
}
