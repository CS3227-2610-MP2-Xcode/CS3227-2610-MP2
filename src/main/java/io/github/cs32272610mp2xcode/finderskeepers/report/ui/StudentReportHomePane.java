package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.Objects;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/** Student report workspace containing submission and personal history. */
public final class StudentReportHomePane extends BorderPane {
    /**
     * Creates the Student report workspace.
     *
     * @param username signed-in username displayed by both views
     * @param formController authenticated report-submission controller
     * @param historyController authenticated personal-history controller
     */
    public StudentReportHomePane(String username,
            StudentReportFormController formController,
            StudentReportHistoryController historyController) {
        Objects.requireNonNull(username, "username");
        StudentReportForm reportForm = new StudentReportForm(
                username,
                Objects.requireNonNull(formController, "formController"));
        StudentReportHistoryPane reportHistory = new StudentReportHistoryPane(
                username,
                Objects.requireNonNull(historyController, "historyController"));

        Tab submitTab = fixedTab("Report an item", reportForm);
        Tab historyTab = fixedTab("My reports", reportHistory);
        TabPane navigation = new TabPane(submitTab, historyTab);
        navigation.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (selected == historyTab) {
                        reportHistory.refresh();
                    }
                });
        setCenter(navigation);
    }

    private static Tab fixedTab(String label, javafx.scene.Node content) {
        Tab tab = new Tab(label, content);
        tab.setClosable(false);
        return tab;
    }
}
