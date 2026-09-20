package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Presentation-ready outcome of one report-submission attempt.
 *
 * @param successful whether the report was saved
 * @param message safe user-facing summary
 * @param fieldErrors field-specific validation messages
 * @param reportId saved identifier, present only on success
 */
public record SubmissionViewState(
        boolean successful,
        String message,
        Map<ReportFormField, String> fieldErrors,
        UUID reportId) {
    /** Protects the returned state from later mutation. */
    public SubmissionViewState {
        message = Objects.requireNonNull(message, "message");
        fieldErrors = Map.copyOf(fieldErrors);
        if (successful && reportId == null) {
            throw new IllegalArgumentException("Successful submission requires a report ID");
        }
        if (!successful && reportId != null) {
            throw new IllegalArgumentException("Failed submission cannot contain a report ID");
        }
    }

    /**
     * Creates a successful confirmation state.
     *
     * @param savedReportId saved report identifier
     * @return success state
     */
    public static SubmissionViewState success(UUID savedReportId) {
        Objects.requireNonNull(savedReportId, "savedReportId");
        return new SubmissionViewState(true,
                "Report submitted. Your report ID is " + savedReportId + ".",
                Map.of(), savedReportId);
    }

    /**
     * Creates a field-validation state.
     *
     * @param errors field-specific readable messages
     * @return validation-failure state
     */
    public static SubmissionViewState invalid(Map<ReportFormField, String> errors) {
        return new SubmissionViewState(false,
                "Please fix the highlighted fields and try again.",
                errors, null);
    }

    /**
     * Creates a safe storage-failure state.
     *
     * @return submission-failure state
     */
    public static SubmissionViewState storageFailure() {
        return new SubmissionViewState(false,
                "We could not save your report. Please try again or ask the Desk Officer.",
                Map.of(), null);
    }
}
