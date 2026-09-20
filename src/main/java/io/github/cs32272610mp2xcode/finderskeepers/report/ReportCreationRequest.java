package io.github.cs32272610mp2xcode.finderskeepers.report;

import java.time.LocalDate;

/**
 * User-supplied information needed to create an item report.
 *
 * @param reporterId stable identifier of the authenticated reporter
 * @param reportType whether the item was lost or found
 * @param itemName short name of the item
 * @param category category used to group the item
 * @param location place where the item was lost or found
 * @param occurrenceDate date when the item was lost or found
 * @param publicDescription description safe to show during matching
 * @param privateIdentifyingDetail detail reserved for identity verification
 */
public record ReportCreationRequest(
        String reporterId,
        ReportType reportType,
        String itemName,
        ItemCategory category,
        String location,
        LocalDate occurrenceDate,
        String publicDescription,
        String privateIdentifyingDetail) {
    /**
     * Returns a privacy-safe summary that excludes free-text descriptions.
     *
     * @return request summary without identifying details
     */
    @Override
    public String toString() {
        return "ReportCreationRequest{"
                + "reportType=" + reportType
                + ", category=" + category
                + ", occurrenceDate=" + occurrenceDate
                + '}';
    }
}
