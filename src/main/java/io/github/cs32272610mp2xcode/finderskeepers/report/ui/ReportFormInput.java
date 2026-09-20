package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.time.LocalDate;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportCreationRequest;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;

/**
 * Snapshot of editable values from the Student report form.
 *
 * @param reportType whether the item was lost or found
 * @param itemName short name of the item
 * @param category item category
 * @param location place where the item was lost or found
 * @param occurrenceDate date when the item was lost or found
 * @param publicDescription description safe to show during matching
 * @param privateIdentifyingDetail detail reserved for staff verification
 */
public record ReportFormInput(
        ReportType reportType,
        String itemName,
        ItemCategory category,
        String location,
        LocalDate occurrenceDate,
        String publicDescription,
        String privateIdentifyingDetail) {
    /**
     * Adds the authenticated reporter identity to this form snapshot.
     *
     * @param reporterId stable authenticated-user identifier
     * @return domain creation request
     */
    public ReportCreationRequest toCreationRequest(String reporterId) {
        return new ReportCreationRequest(reporterId, reportType, itemName, category,
                location, occurrenceDate, publicDescription,
                privateIdentifyingDetail);
    }
}
