package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppointmentHelpDialogTest {
    @Test
    void studentHelpExplainsApprovalBookingAttendanceAndHistory() {
        assertEquals(4, AppointmentHelpDialog.studentStepTitles().size());
        assertTrue(AppointmentHelpDialog.studentStepTitles().getFirst().contains("approval"));
        assertTrue(AppointmentHelpDialog.studentStepTitles().getLast().contains("result"));
    }

    @Test
    void officerHelpExplainsFullCustodyOrder() {
        assertEquals(6, AppointmentHelpDialog.officerStepTitles().size());
        assertTrue(AppointmentHelpDialog.officerStepTitles().get(2).contains("Prepare"));
        assertTrue(AppointmentHelpDialog.officerStepTitles().get(4).contains("handover"));
    }
}
