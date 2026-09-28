package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CustodyStatus;
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

class AppointmentLifecycleIntegrationTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    private static final Instant FIRST = NOW.plusSeconds(3600);

    private static final Instant SECOND = NOW.plusSeconds(5400);

    private static final Instant THIRD = NOW.plusSeconds(7200);

    private static final Instant FOURTH = NOW.plusSeconds(10800);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void sequentialStudentsCannotTakeOneSlotAndCancellationReleasesIt() throws Exception {
        Fixture fixture = new Fixture(temporaryDirectory);
        fixture.approveClaim(1, "student-a");
        fixture.approveClaim(2, "student-b");
        fixture.officer(NOW, 11).createSlot(FIRST);
        fixture.officer(NOW, 12).createSlot(SECOND);
        SlotId firstSlot = SlotId.of(uuid(11));
        SlotId secondSlot = SlotId.of(uuid(12));

        AppointmentRepository.BookingResult first = fixture.student("student-a", NOW, 21)
                .book(ClaimId.of(uuid(1)), firstSlot);
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED, first.outcome());
        AppointmentId appointmentId = first.caseState().orElseThrow()
                .activeAppointment().orElseThrow().appointmentId();
        byte[] beforeRejectedBooking = Files.readAllBytes(fixture.appointmentPath);
        assertEquals(AppointmentRepository.BookingOutcome.SLOT_TAKEN,
                fixture.student("student-b", NOW, 22)
                        .book(ClaimId.of(uuid(2)), firstSlot).outcome());
        assertTrue(java.util.Arrays.equals(beforeRejectedBooking,
                Files.readAllBytes(fixture.appointmentPath)));

        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                fixture.student("student-a", NOW, 23)
                        .reschedule(appointmentId, secondSlot).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                fixture.student("student-a", NOW, 24).cancel(appointmentId).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                fixture.student("student-b", NOW, 25)
                        .book(ClaimId.of(uuid(2)), firstSlot).outcome());

        List<StudentAppointmentHistorySummary> aHistory = fixture
                .student("student-a", NOW, 26).loadHistory();
        assertEquals(1, aHistory.size());
        assertEquals(Optional.of(AppointmentStatus.CANCELLED),
                aHistory.getFirst().appointmentStatus());
        assertEquals(Optional.of(SECOND), aHistory.getFirst().startsAt());
        assertEquals(1, fixture.student("student-b", NOW, 27)
                .loadActiveAppointments().size());
        assertTrue(fixture.student("student-a", NOW, 28)
                .loadActiveAppointments().isEmpty());
    }

    @Test
    void noShowBoundaryAndCompletedCustodySurviveReconstruction() throws Exception {
        Fixture fixture = new Fixture(temporaryDirectory);
        fixture.approveClaim(1, "student-a");
        fixture.officer(NOW, 11).createSlot(FIRST);
        fixture.officer(NOW, 12).createSlot(THIRD);
        fixture.officer(NOW, 13).createSlot(FOURTH);
        ClaimId claimId = ClaimId.of(uuid(1));
        AppointmentId firstId = fixture.student("student-a", NOW, 21)
                .book(claimId, SlotId.of(uuid(11))).caseState().orElseThrow()
                .activeAppointment().orElseThrow().appointmentId();

        assertEquals(AppointmentRepository.AppointmentOutcome.TOO_EARLY,
                fixture.officer(FIRST.plusSeconds(1799), 31)
                        .recordNoShow(firstId).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                fixture.officer(FIRST.plusSeconds(1800), 32)
                        .recordNoShow(firstId).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                fixture.student("student-a", SECOND, 22)
                        .book(claimId, SlotId.of(uuid(12))).outcome());
        AppointmentId secondId = fixture.appointments.loadCases().getFirst()
                .activeAppointment().orElseThrow().appointmentId();

        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                fixture.officer(SECOND, 33)
                        .recordStorageLocation(claimId, "Office shelf").outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                fixture.officer(SECOND, 34).markReadyForCollection(claimId).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.TOO_EARLY,
                fixture.officer(THIRD.minusMillis(1), 35)
                        .confirmCollection(secondId).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                fixture.officer(THIRD, 36).confirmCollection(secondId).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                fixture.officer(THIRD, 37).markReturned(claimId).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                fixture.officer(THIRD, 38).closeCase(claimId).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.CASE_CLOSED,
                fixture.student("student-a", THIRD, 23)
                        .book(claimId, SlotId.of(uuid(13))).outcome());

        var restored = new JsonAppointmentRepository(fixture.appointmentPath)
                .loadCases().getFirst();
        assertEquals(CaseStatus.CLOSED, restored.status());
        assertEquals(CustodyStatus.RETURNED, restored.custodyStatus());
        assertEquals(List.of(AppointmentStatus.NO_SHOW,
                AppointmentStatus.COLLECTION_CONFIRMED), restored.appointments().stream()
                .map(appointment -> appointment.status()).toList());
        assertFalse(restored.auditEvents().isEmpty());
        assertEquals(2, fixture.student("student-a", THIRD, 24).loadHistory().size());
    }

    private static UUID uuid(long value) {
        return new UUID(0, value);
    }

    private static final class Fixture {
        private final Path appointmentPath;

        private final JsonAppointmentRepository appointments;

        private final JsonClaimRepository claims;

        private Fixture(Path directory) {
            appointmentPath = directory.resolve("appointments.json");
            appointments = new JsonAppointmentRepository(appointmentPath);
            claims = new JsonClaimRepository(directory.resolve("claims.json"));
        }

        private void approveClaim(long id, String studentId) throws Exception {
            ClaimId claimId = ClaimId.of(uuid(id));
            claims.submit(Claim.createPending(claimId, studentId, uuid(id + 100),
                    uuid(id + 200), "Synthetic ownership detail",
                    NOW.minusSeconds(2)));
            claims.approve(claimId, Optional.empty(), NOW.minusSeconds(1));
        }

        private OfficerAppointmentService officer(Instant time, long slotId) {
            AuthenticatedUser user = new AuthenticatedUser("officer-a", "desk.a",
                    UserRole.DESK_OFFICER);
            return new OfficerAppointmentService(user, appointments,
                    Clock.fixed(time, ZoneOffset.UTC), () -> uuid(slotId));
        }

        private StudentAppointmentService student(String studentId, Instant time,
                long appointmentId) {
            AuthenticatedUser user = new AuthenticatedUser(studentId, studentId,
                    UserRole.STUDENT);
            return new StudentAppointmentService(user, appointments,
                    new StudentApprovedClaimService(user, claims),
                    Clock.fixed(time, ZoneOffset.UTC), () -> uuid(appointmentId));
        }
    }
}
