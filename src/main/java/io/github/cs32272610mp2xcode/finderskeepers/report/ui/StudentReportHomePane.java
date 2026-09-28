package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.workspace.SessionView;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.WorkspaceFeature;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Student report workspace containing submission and personal history. */
public final class StudentReportHomePane extends BorderPane implements SessionView {
    private final WorkspaceFeature claimsFeature;

    private final WorkspaceFeature appointmentsFeature;

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
        this(username, formController, historyController, null);
    }

    /**
     * Creates the Student report workspace with an appended Claims feature.
     *
     * @param username signed-in username displayed by both report views
     * @param formController authenticated report-submission controller
     * @param historyController authenticated personal-history controller
     * @param claims neutral authenticated Claims feature
     */
    public StudentReportHomePane(String username,
            StudentReportFormController formController,
            StudentReportHistoryController historyController,
            WorkspaceFeature claims) {
        this(username, formController, historyController, claims, null);
    }

    /**
     * Creates the Student report workspace with Claims and appointments.
     *
     * @param username signed-in username displayed by both views
     * @param formController authenticated report-submission controller
     * @param historyController authenticated personal-history controller
     * @param claims neutral authenticated Claims feature
     * @param appointments neutral authenticated appointment feature
     */
    public StudentReportHomePane(String username,
            StudentReportFormController formController,
            StudentReportHistoryController historyController,
            WorkspaceFeature claims, WorkspaceFeature appointments) {
        Objects.requireNonNull(username, "username");
        getStyleClass().add("student-workspace");
        setTop(studentBanner());
        claimsFeature = claims;
        appointmentsFeature = appointments;
        StudentReportForm reportForm = new StudentReportForm(
                username,
                Objects.requireNonNull(formController, "formController"));
        StudentReportHistoryPane reportHistory = new StudentReportHistoryPane(
                username,
                Objects.requireNonNull(historyController, "historyController"));

        Tab submitTab = fixedTab("Report an item", reportForm);
        Tab historyTab = fixedTab("My reports", reportHistory);
        TabPane navigation = new TabPane(submitTab, historyTab);
        navigation.getStyleClass().add("workspace-tabs");
        Tab claimsTab = claimsFeature == null
                ? null : fixedTab("Claims", claimsFeature.content());
        Tab appointmentsTab = appointmentsFeature == null ? null
                : fixedTab("Appointments", appointmentsFeature.content());
        if (claimsTab != null) {
            navigation.getTabs().add(claimsTab);
        }
        if (appointmentsTab != null) {
            navigation.getTabs().add(appointmentsTab);
        }
        navigation.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    if (selected == historyTab) {
                        reportHistory.refresh();
                    } else if (claimsTab != null && selected == claimsTab) {
                        claimsFeature.onEnter().run();
                    } else if (appointmentsTab != null && selected == appointmentsTab) {
                        appointmentsFeature.onEnter().run();
                    }
                });
        setCenter(navigation);
    }

    @Override
    public boolean hasUnsavedText() {
        return claimsFeature != null && claimsFeature.hasUnsavedText().getAsBoolean();
    }

    @Override
    public void clearSessionState() {
        if (claimsFeature != null) {
            claimsFeature.clearSessionState().run();
        }
        if (appointmentsFeature != null) {
            appointmentsFeature.clearSessionState().run();
        }
    }

    private static Tab fixedTab(String label, javafx.scene.Node content) {
        Tab tab = new Tab(label, content);
        tab.setClosable(false);
        return tab;
    }

    private static HBox studentBanner() {
        Image mascot = new Image(Objects.requireNonNull(
                StudentReportHomePane.class.getResource("assets/student-bear.png"))
                .toExternalForm(), 88, 88, true, true);
        ImageView mascotView = new ImageView(mascot);
        mascotView.setAccessibleText("Friendly bear holding a magnifying glass");
        Label greeting = new Label("Let’s find it together!");
        greeting.getStyleClass().add("student-banner-title");
        Label guidance = new Label(
                "Report an item, check your Claims, or plan a collection.");
        guidance.getStyleClass().add("student-banner-copy");
        VBox message = new VBox(4, greeting, guidance);
        HBox banner = new HBox(16, mascotView, message);
        banner.getStyleClass().add("student-banner");
        banner.setAlignment(Pos.CENTER_LEFT);
        return banner;
    }
}
