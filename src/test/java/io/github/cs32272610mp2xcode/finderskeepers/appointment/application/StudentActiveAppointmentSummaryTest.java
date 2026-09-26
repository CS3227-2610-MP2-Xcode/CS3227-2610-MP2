package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.JsonAppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentApprovedClaimService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudentActiveAppointmentSummaryTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void reloadedAndRescheduledAppointmentShowsOwnedSlotTime() throws Exception {
        Path appointmentPath = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(appointmentPath);
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId replacementSlot = SlotId.of(UUID.randomUUID());
        SlotId otherSlot = SlotId.of(UUID.randomUUID());
        Instant firstStart = NOW.plusSeconds(7200);
        Instant replacementStart = NOW.plusSeconds(10800);
        repository.createSlot(CollectionSlot.create(firstSlot, firstStart, NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(replacementSlot, replacementStart, NOW,
                "officer-1"));
        repository.createSlot(CollectionSlot.create(otherSlot, NOW.plusSeconds(14400), NOW,
                "officer-1"));
        AppointmentId ownedAppointment = AppointmentId.of(UUID.randomUUID());
        repository.book(ClaimId.of(UUID.randomUUID()), "student-1", firstSlot,
                ownedAppointment, "student-1", UserRole.STUDENT, NOW);
        repository.book(ClaimId.of(UUID.randomUUID()), "student-2", otherSlot,
                AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT, NOW);

        JsonAppointmentRepository reopened = new JsonAppointmentRepository(appointmentPath);
        AuthenticatedUser student = new AuthenticatedUser("student-1", "student",
                UserRole.STUDENT);
        StudentAppointmentService service = new StudentAppointmentService(student, reopened,
                new StudentApprovedClaimService(student, new JsonClaimRepository(
                        temporaryDirectory.resolve("claims.json"))),
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);

        var active = service.loadActiveAppointments();
        assertEquals(1, active.size());
        assertEquals(ownedAppointment, active.getFirst().appointmentId());
        assertEquals(AppointmentStatus.BOOKED, active.getFirst().status());
        assertEquals(firstStart, active.getFirst().startsAt());
        assertEquals("StudentActiveAppointmentSummary[redacted]",
                active.getFirst().toString());

        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                reopened.reschedule(ownedAppointment, "student-1", replacementSlot,
                        "student-1", UserRole.STUDENT, NOW.plusSeconds(1)).outcome());
        assertEquals(replacementStart, service.loadActiveAppointments().getFirst().startsAt());
    }
}
