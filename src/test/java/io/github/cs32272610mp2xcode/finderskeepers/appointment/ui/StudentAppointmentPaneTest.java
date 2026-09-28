package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.StudentActiveAppointmentSummary;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimSummary;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;

class StudentAppointmentPaneTest {
    private static final Instant SLOT_TIME = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void unrelatedActiveAppointmentDoesNotHideBookForSelectedClaim() {
        ApprovedClaimSummary selected = claim(2);

        assertTrue(StudentAppointmentPane.shouldShowBook(selected, true,
                List.of(activeAppointment(1))));
        assertFalse(StudentAppointmentPane.shouldShowBook(selected, true,
                List.of(activeAppointment(2))));
    }

    @Test
    void activeAppointmentLabelIncludesDistinguishingClaimReference() {
        assertEquals("CLM-00000001 — BOOKED — 01 Jan 2030, 08:00",
                StudentAppointmentPane.formatActiveAppointment(activeAppointment(1)));
    }

    private static ApprovedClaimSummary claim(long id) {
        ClaimId claimId = ClaimId.of(uuid(id));
        return new ApprovedClaimSummary(claimId, claimId.reference(), Optional.empty(),
                Optional.empty(), SLOT_TIME);
    }

    private static StudentActiveAppointmentSummary activeAppointment(long claimId) {
        return new StudentActiveAppointmentSummary(AppointmentId.of(uuid(100 + claimId)),
                ClaimId.of(uuid(claimId)), AppointmentStatus.BOOKED, SLOT_TIME);
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
