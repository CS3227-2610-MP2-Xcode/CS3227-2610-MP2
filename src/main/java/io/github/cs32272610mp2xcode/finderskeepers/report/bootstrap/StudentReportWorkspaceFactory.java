package io.github.cs32272610mp2xcode.finderskeepers.report.bootstrap;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmissionException;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmissionService;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmitter;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.StudentReportHistoryService;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ui.StudentReportFormController;
import io.github.cs32272610mp2xcode.finderskeepers.report.ui.StudentReportHistoryController;
import io.github.cs32272610mp2xcode.finderskeepers.report.ui.StudentReportHomePane;
import javafx.scene.Node;

/** Production composition for the authenticated Student report workspace. */
public final class StudentReportWorkspaceFactory {
    private StudentReportWorkspaceFactory() {
    }

    /**
     * Creates a Student-view factory backed by one shared report repository.
     *
     * @param reportStorePath canonical JSON report-store path
     * @return factory that binds each view to its authenticated identity
     */
    public static Function<AuthenticatedUser, Node> create(Path reportStorePath) {
        ReportRepository repository = new JsonReportRepository(
                Objects.requireNonNull(reportStorePath, "reportStorePath"));
        return user -> createView(user, repository);
    }

    private static Node createView(AuthenticatedUser user,
            ReportRepository repository) {
        AuthenticatedUser authenticatedUser = Objects.requireNonNull(user, "user");
        StudentReportWorkspaceComponents components = compose(
                authenticatedUser, repository);
        StudentReportFormController formController = new StudentReportFormController(
                authenticatedUser.userId(), components.submitter());
        StudentReportHistoryController historyController =
                new StudentReportHistoryController(
                        authenticatedUser.userId(),
                        components.historyService());
        return new StudentReportHomePane(
                authenticatedUser.username(), formController, historyController);
    }

    static StudentReportWorkspaceComponents compose(AuthenticatedUser user,
            ReportRepository repository) {
        Objects.requireNonNull(user, "user");
        ReportRepository reportStore = Objects.requireNonNull(repository, "repository");
        ReportSubmitter submissionService = new ReportSubmissionService(
                Clock.systemDefaultZone(), UUID::randomUUID,
                reportSaver(reportStore));
        return new StudentReportWorkspaceComponents(
                submissionService,
                new StudentReportHistoryService(reportStore));
    }

    private static Consumer<ItemReport> reportSaver(ReportRepository repository) {
        return report -> {
            try {
                repository.insert(report);
            } catch (ReportStoreException exception) {
                throw new ReportSubmissionException(exception);
            }
        };
    }
}
