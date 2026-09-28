package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.runOnFxThread;
import static io.github.cs32272610mp2xcode.finderskeepers.testsupport.JavaFxTestSupport.startToolkit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

class AppointmentHelpDialogTest {
    @BeforeAll
    static void startJavaFxToolkit() throws InterruptedException {
        startToolkit();
    }

    @Test
    void studentHelpButtonOpensRenderedWorkflowExplanations() throws Exception {
        runOnFxThread(() -> {
            Button button = AppointmentHelpDialog.studentHelpButton();
            List<String> explanations = explanations(
                    AppointmentHelpDialog.studentPage().getContent());
            String renderedCopy = String.join(" ", explanations);

            assertAll(
                    () -> assertEquals("student-appointment-help", button.getId()),
                    () -> assertNotNull(button.getOnAction()),
                    () -> assertEquals(4, explanations.size()),
                    () -> assertFalse(explanations.stream().anyMatch(String::isBlank)),
                    () -> assertTrue(renderedCopy.contains("approves your Claim")),
                    () -> assertTrue(renderedCopy.contains("Book selected slot")),
                    () -> assertTrue(renderedCopy.contains("after the slot starts")),
                    () -> assertTrue(renderedCopy.contains("Appointment history")));
            return null;
        });
    }

    @Test
    void officerHelpButtonOpensRenderedCustodyExplanations() throws Exception {
        runOnFxThread(() -> {
            Button button = AppointmentHelpDialog.officerHelpButton();
            List<String> explanations = explanations(
                    AppointmentHelpDialog.officerPage().getContent());
            String renderedCopy = String.join(" ", explanations);

            assertAll(
                    () -> assertEquals("officer-appointment-help", button.getId()),
                    () -> assertNotNull(button.getOnAction()),
                    () -> assertEquals(6, explanations.size()),
                    () -> assertFalse(explanations.stream().anyMatch(String::isBlank)),
                    () -> assertTrue(renderedCopy.contains("future Singapore date")),
                    () -> assertTrue(renderedCopy.contains("after a Student books")),
                    () -> assertTrue(renderedCopy.contains("Record storage location")),
                    () -> assertTrue(renderedCopy.contains("too early")),
                    () -> assertTrue(renderedCopy.contains("Mark item returned")),
                    () -> assertTrue(renderedCopy.contains("who performed")));
            return null;
        });
    }

    private static List<String> explanations(Node root) {
        return descendants(root)
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> label.getStyleClass().contains("help-step-explanation"))
                .map(Label::getText)
                .toList();
    }

    private static Stream<Node> descendants(Node root) {
        Stream<Node> current = Stream.of(root);
        if (root instanceof Parent parent) {
            return Stream.concat(current, parent.getChildrenUnmodifiable().stream()
                    .flatMap(AppointmentHelpDialogTest::descendants));
        }
        return current;
    }
}
