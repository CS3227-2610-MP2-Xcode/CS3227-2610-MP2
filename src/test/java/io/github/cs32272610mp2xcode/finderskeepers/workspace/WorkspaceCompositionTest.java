package io.github.cs32272610mp2xcode.finderskeepers.workspace;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.labels;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.descendants;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.FilePossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.bootstrap.StudentReportWorkspaceFactory;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ui.StudentReportHomePane;
import io.github.cs32272610mp2xcode.finderskeepers.review.ui.DeskOfficerWorkspacePane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceCompositionTest {
    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void studentTabSwitchRefreshesPersonalHistoryAndInvokesFeatureLifecycle()
            throws Exception {
        JsonReportRepository reports = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json"));
        AtomicInteger claimsEntered = new AtomicInteger();
        AtomicInteger appointmentsEntered = new AtomicInteger();
        AtomicInteger cleared = new AtomicInteger();
        AtomicBoolean unsaved = new AtomicBoolean(true);
        AuthenticatedUser student = new AuthenticatedUser("student-a", "Student A",
                UserRole.STUDENT);
        runOnFxThread(() -> {
            StudentReportHomePane pane = (StudentReportHomePane)
                    StudentReportWorkspaceFactory.create(reports,
                            user -> feature("Claims content", claimsEntered, unsaved, cleared),
                            user -> feature("Appointment content", appointmentsEntered,
                                    new AtomicBoolean(false), cleared)).apply(student);
            TabPane tabs = (TabPane) pane.getCenter();
            assertEquals(4, tabs.getTabs().size());
            assertEquals("Report an item", tabs.getTabs().getFirst().getText());
            assertTrue(pane.hasUnsavedText());
            assertTrue(labels(pane).contains("Signed in as Student A"));

            reports.insert(report(1, "student-a"));
            reports.insert(report(2, "student-b"));
            tabs.getSelectionModel().select(1);
            assertTrue(labels(pane).contains("Synthetic item 1"));
            assertFalse(labels(pane).contains("Synthetic item 2"));
            tabs.getSelectionModel().select(2);
            tabs.getSelectionModel().select(3);
            assertEquals(1, claimsEntered.get());
            assertEquals(1, appointmentsEntered.get());
            pane.clearSessionState();
            assertEquals(2, cleared.get());
            return null;
        });
    }

    @Test
    void officerSwitchBackToReviewReloadsReportsAndClaimsStateClears()
            throws Exception {
        JsonReportRepository reports = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json"));
        FilePossibleMatchRepository matches = new FilePossibleMatchRepository(
                temporaryDirectory.resolve("matches.txt"));
        AtomicInteger entered = new AtomicInteger();
        AtomicInteger cleared = new AtomicInteger();
        AtomicBoolean unsaved = new AtomicBoolean(true);
        runOnFxThread(() -> {
            DeskOfficerWorkspacePane pane = new DeskOfficerWorkspacePane(reports,
                    matches, feature("Claims content", entered, unsaved, cleared));
            assertEquals(3, pane.getTabs().size());
            assertTrue(pane.hasUnsavedText());
            reports.insert(report(1, "student-a"));
            pane.getSelectionModel().select(2);
            assertEquals(1, entered.get());
            pane.getSelectionModel().select(0);
            ListView<?> queue = descendants(pane).filter(ListView.class::isInstance)
                    .map(ListView.class::cast).findFirst().orElseThrow();
            assertEquals(1, queue.getItems().size());
            queue.getSelectionModel().selectFirst();
            assertTrue(labels(pane).contains("Synthetic item 1"));
            pane.clearSessionState();
            assertEquals(1, cleared.get());
            unsaved.set(false);
            assertFalse(pane.hasUnsavedText());
            return null;
        });
    }

    private static WorkspaceFeature feature(String title, AtomicInteger entered,
            AtomicBoolean unsaved, AtomicInteger cleared) {
        return new WorkspaceFeature(new Label(title), entered::incrementAndGet,
                unsaved::get, cleared::incrementAndGet);
    }

    private static ItemReport report(long id, String reporter) {
        return ItemReport.restore(new UUID(0, id), reporter, ReportType.LOST,
                "Synthetic item " + id, ItemCategory.STATIONERY, "Library",
                LocalDate.of(2029, 12, 31), "Synthetic public description",
                "Synthetic private mark", ReportStatus.SUBMITTED,
                Instant.parse("2030-01-01T00:00:00Z"));
    }
}
