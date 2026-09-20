package io.github.cs32272610mp2xcode.finderskeepers.report.application;

/** Indicates that personal report history could not be loaded. */
public final class ReportHistoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a history failure while retaining its technical cause.
     *
     * @param cause underlying storage failure
     */
    public ReportHistoryException(Throwable cause) {
        super("Report history could not be loaded", cause);
    }
}
