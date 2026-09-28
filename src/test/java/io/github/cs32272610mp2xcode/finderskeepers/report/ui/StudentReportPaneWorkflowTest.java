package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.button;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.descendants;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.FxTestNodes.labels;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmissionException;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.ReportSubmissionService;
import io.github.cs32272610mp2xcode.finderskeepers.report.application.StudentReportHistoryService;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudentReportPaneWorkflowTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        startToolkit();
    }

    @Test
    void formValidatesBlankLengthAndFutureDateBeforeSavingThenClearsOnSuccess()
            throws Exception {
        JsonReportRepository repository = repository();
        runOnFxThread(() -> {
            StudentReportForm form = form("student-a", repository);
            button(form, "Submit report").fire();
            assertTrue(labels(form).contains("Report type is required."));
            assertTrue(labels(form).contains("Item name cannot be blank."));
            assertTrue(repository.loadAll().isEmpty());

            fill(form, ReportType.LOST, "P".repeat(101),
                    LocalDate.of(2030, 1, 2));
            button(form, "Submit report").fire();
            assertTrue(labels(form).contains("Item name must not exceed 100 characters."));
            assertTrue(labels(form).contains("Occurrence date cannot be in the future."));
            assertTrue(repository.loadAll().isEmpty());

            itemName(form).setText("Synthetic pencil case");
            date(form).setValue(LocalDate.of(2030, 1, 1));
            button(form, "Submit report").fire();
            assertTrue(labels(form).contains("Report submitted"));
            assertEquals("", itemName(form).getText());
            assertEquals(null, date(form).getValue());
            return null;
        });
        List<ItemReport> saved = new JsonReportRepository(
                temporaryDirectory.resolve("reports.json")).loadAll();
        assertEquals(1, saved.size());
        assertEquals("student-a", saved.getFirst().reporterId());
        assertEquals(ReportType.LOST, saved.getFirst().reportType());
    }

    @Test
    void storageFailureKeepsInputForRetryWithoutCorruptingExistingReports()
            throws Exception {
        JsonReportRepository repository = repository();
        Path store = temporaryDirectory.resolve("reports.json");
        repository.insert(report(1, "student-a", ReportType.FOUND));
        Files.writeString(store, "invalid JSON");
        byte[] invalidBytes = Files.readAllBytes(store);
        runOnFxThread(() -> {
            StudentReportForm form = form("student-a", repository);
            fill(form, ReportType.FOUND, "Synthetic umbrella",
                    LocalDate.of(2029, 12, 31));
            button(form, "Submit report").fire();
            assertTrue(labels(form).contains("could not save"));
            assertEquals("Synthetic umbrella", itemName(form).getText());
            assertTrue(java.util.Arrays.equals(invalidBytes, Files.readAllBytes(store)));
            button(form, "Clear").fire();
            assertEquals("", itemName(form).getText());
            assertFalse(labels(form).contains("could not save"));
            return null;
        });
    }

    @Test
    void historySearchIsPersonalAndRecoversAfterUnreadableStore() throws Exception {
        JsonReportRepository repository = repository();
        Path store = temporaryDirectory.resolve("reports.json");
        runOnFxThread(() -> {
            StudentReportHistoryPane empty = history("student-a", repository);
            assertTrue(labels(empty).contains("have not submitted"));
            return null;
        });
        repository.insert(report(1, "student-a", ReportType.LOST));
        repository.insert(report(2, "student-b", ReportType.FOUND));
        byte[] validBytes = Files.readAllBytes(store);
        runOnFxThread(() -> {
            StudentReportHistoryPane a = history("student-a", repository);
            StudentReportHistoryPane b = history("student-b", repository);
            assertTrue(labels(a).contains("Synthetic item 1"));
            assertFalse(labels(a).contains("Synthetic item 2"));
            assertTrue(labels(b).contains("Synthetic item 2"));
            assertFalse(labels(b).contains("Synthetic item 1"));

            search(a).setText("no matching item");
            button(a, "Search").fire();
            assertFalse(labels(a).contains("Synthetic item 1"));
            assertTrue(labels(a).contains("No reports match"));
            button(a, "Clear search").fire();
            assertTrue(labels(a).contains("Synthetic item 1"));

            Files.writeString(store, "invalid JSON");
            a.refresh();
            assertTrue(labels(a).contains("could not load"));
            assertFalse(labels(a).contains("Synthetic item 1"));
            Files.write(store, validBytes);
            a.refresh();
            assertTrue(labels(a).contains("Synthetic item 1"));
            return null;
        });
    }

    private JsonReportRepository repository() {
        return new JsonReportRepository(temporaryDirectory.resolve("reports.json"));
    }

    private static StudentReportForm form(String user, JsonReportRepository repository) {
        ReportSubmissionService submitter = new ReportSubmissionService(
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID,
                report -> {
                    try {
                        repository.insert(report);
                    } catch (ReportStoreException exception) {
                        throw new ReportSubmissionException(exception);
                    }
                });
        return new StudentReportForm(user,
                new StudentReportFormController(user, submitter));
    }

    private static StudentReportHistoryPane history(String user,
            JsonReportRepository repository) {
        return new StudentReportHistoryPane(user,
                new StudentReportHistoryController(user,
                        new StudentReportHistoryService(repository)));
    }

    @SuppressWarnings("unchecked")
    private static void fill(StudentReportForm form, ReportType type,
            String name, LocalDate occurrence) {
        List<Node> choices = descendants(form).filter(ComboBox.class::isInstance).toList();
        ((ComboBox<ReportType>) choices.getFirst()).setValue(type);
        itemName(form).setText(name);
        ((ComboBox<ItemCategory>) choices.get(1)).setValue(ItemCategory.STATIONERY);
        List<TextField> fields = descendants(form).filter(TextField.class::isInstance)
                .map(TextField.class::cast).toList();
        fields.get(1).setText("School library");
        date(form).setValue(occurrence);
        List<TextArea> descriptions = descendants(form).filter(TextArea.class::isInstance)
                .map(TextArea.class::cast).toList();
        descriptions.get(0).setText("Synthetic public description");
        descriptions.get(1).setText("Synthetic private mark");
    }

    private static TextField itemName(StudentReportForm form) {
        return descendants(form).filter(TextField.class::isInstance)
                .map(TextField.class::cast).findFirst().orElseThrow();
    }

    private static DatePicker date(StudentReportForm form) {
        return descendants(form).filter(DatePicker.class::isInstance)
                .map(DatePicker.class::cast).findFirst().orElseThrow();
    }

    private static TextField search(StudentReportHistoryPane pane) {
        return descendants(pane).filter(TextField.class::isInstance)
                .map(TextField.class::cast).findFirst().orElseThrow();
    }

    private static ItemReport report(long id, String reporter, ReportType type) {
        return ItemReport.restore(new UUID(0, id), reporter, type,
                "Synthetic item " + id, ItemCategory.STATIONERY, "Library",
                LocalDate.of(2029, 12, 31), "Synthetic public description",
                "Synthetic private mark", ReportStatus.SUBMITTED,
                NOW.minusSeconds(10));
    }
}
