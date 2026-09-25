package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.JsonAppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentApprovedClaimService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppointmentClockPrecisionTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:02.123456789Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void officerCanCreateSlotAndStudentCanBookWithNanosecondClock() throws Exception {
        JsonAppointmentRepository appointments = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        JsonClaimRepository claims = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        AuthenticatedUser officer = new AuthenticatedUser("officer-1", "officer",
                UserRole.DESK_OFFICER);
        AuthenticatedUser student = new AuthenticatedUser("student-1", "student",
                UserRole.STUDENT);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        SlotId slotId = SlotId.of(UUID.randomUUID());
        OfficerAppointmentService officerService = new OfficerAppointmentService(officer,
                appointments, clock, () -> slotId.value());

        assertEquals(AppointmentRepository.SlotOutcome.CREATED,
                officerService.createSlot(Instant.parse("2030-01-01T02:00:00Z")).outcome());
        assertEquals(NOW.truncatedTo(ChronoUnit.MILLIS),
                appointments.loadSlots().getFirst().createdAt());

        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        claims.submit(Claim.createPending(claimId, student.userId(), UUID.randomUUID(),
                UUID.randomUUID(), "Synthetic identifying mark",
                Instant.parse("2030-01-01T00:00:00Z")));
        claims.approve(claimId, Optional.empty(), Instant.parse("2030-01-01T00:00:01Z"));
        StudentAppointmentService studentService = new StudentAppointmentService(student,
                appointments, new StudentApprovedClaimService(student, claims), clock,
                UUID::randomUUID);

        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                studentService.book(claimId, slotId).outcome());
        assertEquals(NOW.truncatedTo(ChronoUnit.MILLIS),
                appointments.loadCases().getFirst().activeAppointment().orElseThrow().bookedAt());
    }
}
