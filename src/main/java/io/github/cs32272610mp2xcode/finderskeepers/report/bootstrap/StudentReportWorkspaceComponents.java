package io.github.cs32272610mp2xcode.finderskeepers.report.bootstrap;

import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmitter;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.StudentReportHistoryService;

record StudentReportWorkspaceComponents(ReportSubmitter submitter,
        StudentReportHistoryService historyService) {
}
