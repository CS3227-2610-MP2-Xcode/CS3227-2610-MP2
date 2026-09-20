package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportValidationException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ValidationError;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmissionException;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmitter;

/** Converts Student form input into one report-submission use case. */
public final class StudentReportFormController {
    private final String reporterId;

    private final ReportSubmitter submitter;

    /**
     * Creates a controller bound to the signed-in reporter.
     *
     * @param authenticatedReporterId stable authenticated-user identifier
     * @param reportSubmitter application operation that creates and saves reports
     */
    public StudentReportFormController(String authenticatedReporterId,
            ReportSubmitter reportSubmitter) {
        Objects.requireNonNull(authenticatedReporterId, "authenticatedReporterId");
        reporterId = authenticatedReporterId.strip();
        if (reporterId.isEmpty()) {
            throw new IllegalArgumentException(
                    "authenticatedReporterId must not be blank");
        }
        submitter = Objects.requireNonNull(reportSubmitter, "reportSubmitter");
    }

    /**
     * Attempts to create and save one report.
     *
     * @param input current form values
     * @return presentation-ready success or error state
     */
    public SubmissionViewState submit(ReportFormInput input) {
        Objects.requireNonNull(input, "input");
        try {
            ItemReport report = submitter.submit(input.toCreationRequest(reporterId));
            return SubmissionViewState.success(report.reportId());
        } catch (ReportValidationException exception) {
            return validationState(exception);
        } catch (ReportSubmissionException exception) {
            return SubmissionViewState.storageFailure();
        }
    }

    private static SubmissionViewState validationState(
            ReportValidationException exception) {
        Map<ReportFormField, String> errors =
                new EnumMap<>(ReportFormField.class);
        for (ValidationError error : exception.errors()) {
            ReportFormField.fromDomainName(error.field())
                    .ifPresent(field -> errors.putIfAbsent(field, error.message()));
        }
        if (errors.isEmpty()) {
            return SubmissionViewState.storageFailure();
        }
        return SubmissionViewState.invalid(errors);
    }
}
