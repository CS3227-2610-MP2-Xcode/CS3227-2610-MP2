package io.github.cs32272610mp2xcode.finderskeepers.report.application;

/** Indicates that a valid report could not be saved. */
public final class ReportSubmissionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a submission failure while retaining its technical cause.
     *
     * @param cause underlying storage or integration failure
     */
    public ReportSubmissionException(Throwable cause) {
        super("Report submission failed", cause);
    }
}
