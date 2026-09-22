package io.github.cs32272610mp2xcode.finderskeepers.report.bootstrap;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
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
import io.github.cs32272610mp2xcode.finderskeepers.workspace.WorkspaceFeature;
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
        return create(new JsonReportRepository(
                Objects.requireNonNull(reportStorePath, "reportStorePath")));
    }

    /**
     * Creates a Student-view factory backed by the supplied report repository.
     *
     * @param repository application-lifetime report repository
     * @return factory that binds each view to its authenticated identity
     */
    public static Function<AuthenticatedUser, Node> create(
            ReportRepository repository) {
        ReportRepository reportStore = Objects.requireNonNull(
                repository, "repository");
        return user -> createView(user, reportStore, null);
    }

    /**
     * Creates a Student-view factory with an authenticated workspace feature.
     *
     * @param repository application-lifetime report repository
     * @param featureFactory authenticated neutral feature factory
     * @return factory that binds each view to its authenticated identity
     */
    public static Function<AuthenticatedUser, Node> create(
            ReportRepository repository,
            Function<AuthenticatedUser, WorkspaceFeature> featureFactory) {
        ReportRepository reportStore = Objects.requireNonNull(
                repository, "repository");
        Function<AuthenticatedUser, WorkspaceFeature> features =
                Objects.requireNonNull(featureFactory, "featureFactory");
        return user -> createView(user, reportStore, features);
    }

    private static Node createView(AuthenticatedUser user,
            ReportRepository repository,
            Function<AuthenticatedUser, WorkspaceFeature> featureFactory) {
        AuthenticatedUser authenticatedUser = requireStudent(user);
        StudentReportWorkspaceComponents components = compose(
                authenticatedUser, repository);
        StudentReportFormController formController = new StudentReportFormController(
                authenticatedUser.userId(), components.submitter());
        StudentReportHistoryController historyController =
                new StudentReportHistoryController(
                        authenticatedUser.userId(),
                        components.historyService());
        if (featureFactory == null) {
            return new StudentReportHomePane(
                    authenticatedUser.username(), formController, historyController);
        }
        WorkspaceFeature feature = Objects.requireNonNull(
                featureFactory.apply(authenticatedUser), "featureFactory result");
        return new StudentReportHomePane(authenticatedUser.username(), formController,
                historyController, feature);
    }

    static StudentReportWorkspaceComponents compose(AuthenticatedUser user,
            ReportRepository repository) {
        requireStudent(user);
        ReportRepository reportStore = Objects.requireNonNull(repository, "repository");
        ReportSubmitter submissionService = new ReportSubmissionService(
                Clock.systemDefaultZone(), UUID::randomUUID,
                reportSaver(reportStore));
        return new StudentReportWorkspaceComponents(
                submissionService,
                new StudentReportHistoryService(reportStore));
    }

    private static AuthenticatedUser requireStudent(AuthenticatedUser user) {
        AuthenticatedUser authenticatedUser = Objects.requireNonNull(user, "user");
        if (authenticatedUser.role() != UserRole.STUDENT) {
            throw new IllegalArgumentException(
                    "Student report workspace requires a Student identity");
        }
        return authenticatedUser;
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
