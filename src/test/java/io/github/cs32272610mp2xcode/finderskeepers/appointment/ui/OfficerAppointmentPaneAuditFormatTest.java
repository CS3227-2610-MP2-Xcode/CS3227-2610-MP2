package io.github.cs32272610mp2xcode.finderskeepers.appointment.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEvent;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEventId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEventType;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;

class OfficerAppointmentPaneAuditFormatTest {
    private static final Instant EVENT_TIME = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void refreshPreservesDraftOnlyWhenSameCaseRemainsSelected() {
        ClaimId selectedClaim = ClaimId.of(uuid(1));

        assertEquals("Shelf A3", OfficerAppointmentPane.storageDraftAfterRefresh(selectedClaim,
                CollectionCase.open(selectedClaim, "student-1"), "Shelf A3"));
        assertEquals("", OfficerAppointmentPane.storageDraftAfterRefresh(selectedClaim,
                CollectionCase.open(ClaimId.of(uuid(2)), "student-1"), "Shelf A3"));
        assertEquals("", OfficerAppointmentPane.storageDraftAfterRefresh(selectedClaim,
                null, "Shelf A3"));
    }

    @Test
    void studentBookingIsLabelledAsStudent() {
        AuditEvent event = event(AuditEventType.APPOINTMENT_BOOKED, "student-1",
                UserRole.STUDENT);

        assertEquals("01 Jan 2030, 08:00 — APPOINTMENT_BOOKED — Student student-1",
                OfficerAppointmentPane.formatAuditEvent(event));
    }

    @Test
    void officerCustodyActionIsLabelledAsDeskOfficer() {
        AuditEvent event = event(AuditEventType.CUSTODY_READY, "officer-1",
                UserRole.DESK_OFFICER);

        assertEquals("01 Jan 2030, 08:00 — CUSTODY_READY — Desk Officer officer-1",
                OfficerAppointmentPane.formatAuditEvent(event));
    }

    private static AuditEvent event(AuditEventType type, String actor, UserRole role) {
        return new AuditEvent(AuditEventId.of(UUID.randomUUID()), type, EVENT_TIME,
                actor, role, Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
