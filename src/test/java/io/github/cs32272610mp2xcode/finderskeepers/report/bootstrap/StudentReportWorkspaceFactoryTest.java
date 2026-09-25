package io.github.cs32272610mp2xcode.finderskeepers.report.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportCreationRequest;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportHistorySearchResult;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmissionException;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

class StudentReportWorkspaceFactoryTest {
    private static final AuthenticatedUser STUDENT = new AuthenticatedUser(
            "student-synthetic-001", "synthetic.student", UserRole.STUDENT);

    private static final AuthenticatedUser DESK_OFFICER = new AuthenticatedUser(
            "officer-synthetic-001", "synthetic.officer", UserRole.DESK_OFFICER);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void composedSubmissionAndHistoryShareTheCanonicalRepository() {
        JsonReportRepository repository = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json"));
        StudentReportWorkspaceComponents components =
                StudentReportWorkspaceFactory.compose(STUDENT, repository);

        components.submitter().submit(new ReportCreationRequest(
                STUDENT.userId(), ReportType.LOST, "Blue pencil case",
                ItemCategory.STATIONERY, "Synthetic library shelf",
                LocalDate.of(2026, 9, 19), "Blue case with a white zipper.",
                "Synthetic star sticker inside."));

        ReportHistorySearchResult history = components.historyService().search(
                STUDENT.userId(), "pencil");

        assertEquals(1, history.matches().size());
        assertEquals("Blue pencil case", history.matches().getFirst().itemName());
    }

    @Test
    void compositionRejectsMissingAuthenticatedIdentityAndRepository() {
        JsonReportRepository repository = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json"));

        assertThrows(NullPointerException.class,
                () -> StudentReportWorkspaceFactory.compose(null, repository));
        assertThrows(NullPointerException.class,
                () -> StudentReportWorkspaceFactory.compose(STUDENT, null));
        assertThrows(NullPointerException.class,
                () -> StudentReportWorkspaceFactory.create((Path) null));
        assertThrows(NullPointerException.class,
                () -> StudentReportWorkspaceFactory.create((ReportRepository) null));
    }

    @Test
    void publicWorkspaceFactoryRejectsDeskOfficerIdentity() {
        Path reportStore = temporaryDirectory.resolve("reports.json");

        assertThrows(IllegalArgumentException.class,
                () -> StudentReportWorkspaceFactory.create(reportStore).apply(DESK_OFFICER));
        assertThrows(IllegalArgumentException.class,
                () -> StudentReportWorkspaceFactory.compose(
                        DESK_OFFICER, new JsonReportRepository(reportStore)));
    }

    @Test
    void claimsFeatureIsCreatedOnlyAfterStudentIdentityValidation() {
        JsonReportRepository repository = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json"));
        AtomicBoolean featureFactoryCalled = new AtomicBoolean();

        var workspaceFactory = StudentReportWorkspaceFactory.create(
                repository,
                user -> {
                    featureFactoryCalled.set(true);
                    throw new AssertionError("Feature factory must not be called");
                });

        assertThrows(IllegalArgumentException.class,
                () -> workspaceFactory.apply(DESK_OFFICER));
        assertFalse(featureFactoryCalled.get());
    }

    @Test
    void compositionTranslatesRepositoryWriteFailure() {
        ReportStoreException storeFailure = new ReportStoreException(
                ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE);
        ReportRepository repository = new FailingInsertRepository(storeFailure);
        StudentReportWorkspaceComponents components =
                StudentReportWorkspaceFactory.compose(STUDENT, repository);

        ReportSubmissionException exception = assertThrows(
                ReportSubmissionException.class,
                () -> components.submitter().submit(new ReportCreationRequest(
                        STUDENT.userId(), ReportType.LOST, "Blue pencil case",
                        ItemCategory.STATIONERY, "Synthetic library shelf",
                        LocalDate.of(2026, 9, 19), "Blue case with a white zipper.",
                        "Synthetic star sticker inside.")));

        assertSame(storeFailure, exception.getCause());
    }

    private static final class FailingInsertRepository implements ReportRepository {
        private final ReportStoreException insertFailure;

        private FailingInsertRepository(ReportStoreException failure) {
            insertFailure = failure;
        }

        @Override
        public List<ItemReport> loadAll() {
            return List.of();
        }

        @Override
        public void insert(ItemReport report)
                throws ReportStoreException {
            throw insertFailure;
        }

        @Override
        public void replace(UUID targetId, ItemReport replacement) {
            throw new UnsupportedOperationException("Not needed by composition test");
        }
    }
}
