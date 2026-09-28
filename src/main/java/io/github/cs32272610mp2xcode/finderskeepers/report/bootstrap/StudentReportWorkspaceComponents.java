package io.github.cs32272610mp2xcode.finderskeepers.report.bootstrap;

import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmitter;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.StudentReportHistoryService;

/**
 * Application services composed for one Student report workspace.
 *
 * @param submitter service used to create reports
 * @param historyService service used to browse the signed-in Student's reports
 */
record StudentReportWorkspaceComponents(ReportSubmitter submitter,
        StudentReportHistoryService historyService) {
}
