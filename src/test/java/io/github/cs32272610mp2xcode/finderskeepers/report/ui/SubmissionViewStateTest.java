package io.github.cs32272610mp2xcode.finderskeepers.report.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SubmissionViewStateTest {
    @Test
    void fieldErrorsAreDefensivelyCopied() {
        Map<ReportFormField, String> mutable =
                new EnumMap<>(ReportFormField.class);
        mutable.put(ReportFormField.ITEM_NAME, "Item name is required.");

        SubmissionViewState state = SubmissionViewState.invalid(mutable);
        mutable.clear();

        assertEquals(Map.of(ReportFormField.ITEM_NAME, "Item name is required."),
                state.fieldErrors());
        assertThrows(UnsupportedOperationException.class,
                () -> state.fieldErrors().clear());
    }

    @Test
    void successAndFailureKeepReportIdInvariant() {
        UUID reportId = UUID.fromString("a0b9d0e7-2d20-4407-b182-ee7ebd86f96a");

        assertThrows(IllegalArgumentException.class,
                () -> new SubmissionViewState(true, "Saved", Map.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new SubmissionViewState(false, "Failed", Map.of(), reportId));
    }
}
