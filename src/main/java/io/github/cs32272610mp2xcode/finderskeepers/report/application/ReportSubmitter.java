package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportCreationRequest;

/** Application boundary for creating and saving one report. */
@FunctionalInterface
public interface ReportSubmitter {
    /**
     * Creates and saves a report before returning it to the caller.
     *
     * @param request validated-by-domain creation input
     * @return saved report
     * @throws ReportSubmissionException if persistence fails
     */
    ItemReport submit(ReportCreationRequest request);
}
