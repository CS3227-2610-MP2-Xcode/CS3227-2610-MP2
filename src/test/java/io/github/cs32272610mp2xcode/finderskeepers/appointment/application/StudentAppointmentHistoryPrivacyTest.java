package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.JsonAppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentApprovedClaimService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudentAppointmentHistoryPrivacyTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void historyCanRepresentAnEmptyCaseButRejectsIncompleteAttemptDetails() {
        var empty = new StudentAppointmentHistorySummary("CLM-SYNTHETIC", CaseStatus.OPEN,
                0, Optional.empty(), Optional.empty());
        assertEquals(0, empty.attemptNumber());
        assertEquals("Approved item · YNTHETIC", empty.displayLabel());
        assertEquals(Optional.empty(), empty.appointmentStatus());
        assertThrows(IllegalArgumentException.class, () -> new StudentAppointmentHistorySummary(
                "CLM-SYNTHETIC", CaseStatus.OPEN, 1, Optional.empty(), Optional.empty()));
    }

    @Test
    void historyContainsOnlyStudentSafeFieldsAndOnlyOwnedCases() throws Exception {
        JsonAppointmentRepository appointments = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        ClaimId ownedClaim = ClaimId.of(UUID.randomUUID());
        ClaimId otherClaim = ClaimId.of(UUID.randomUUID());
        SlotId ownedSlot = SlotId.of(UUID.randomUUID());
        SlotId otherSlot = SlotId.of(UUID.randomUUID());
        AppointmentId ownedAppointment = AppointmentId.of(UUID.randomUUID());
        appointments.createSlot(CollectionSlot.create(ownedSlot, NOW.plusSeconds(7200), NOW,
                "officer-confidential-7"));
        appointments.createSlot(CollectionSlot.create(otherSlot, NOW.plusSeconds(10800), NOW,
                "officer-confidential-7"));
        appointments.book(ownedClaim, "student-1", ownedSlot,
                ownedAppointment,
                "student-1", UserRole.STUDENT, NOW);
        appointments.recordStorageLocation(ownedClaim, "officer-confidential-7",
                "SECRET-LOCKER-42", NOW.plusSeconds(1));
        appointments.markReadyForCollection(ownedClaim, "officer-confidential-7",
                NOW.plusSeconds(2));
        appointments.recordNoShow(ownedAppointment,
                "officer-confidential-7", NOW.plusSeconds(9000));
        appointments.book(otherClaim, "student-2", otherSlot,
                AppointmentId.of(UUID.randomUUID()),
                "student-2", UserRole.STUDENT, NOW);

        StudentAppointmentService service = new StudentAppointmentService(
                new AuthenticatedUser("student-1", "student", UserRole.STUDENT),
                appointments,
                new StudentApprovedClaimService(
                        new AuthenticatedUser("student-1", "student", UserRole.STUDENT),
                        new JsonClaimRepository(temporaryDirectory.resolve("claims.json"))),
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);

        var result = service.loadHistory();

        assertEquals(1, result.size());
        assertEquals(ownedClaim.reference(), result.getFirst().claimReference());
        assertEquals(CaseStatus.OPEN, result.getFirst().status());
        assertEquals(1, result.getFirst().attemptNumber());
        assertEquals(Optional.of(AppointmentStatus.NO_SHOW),
                result.getFirst().appointmentStatus());
        assertEquals(Optional.of(NOW.plusSeconds(7200)), result.getFirst().startsAt());
        assertFalse(result.getFirst().toString().contains("officer-confidential-7"));
        assertFalse(result.getFirst().toString().contains("SECRET-LOCKER-42"));
        assertEquals("StudentAppointmentHistorySummary[redacted]",
                result.getFirst().toString());
    }

    @Test
    void repeatedBookingsRetainEveryStudentVisibleAttemptAfterReopening() throws Exception {
        Path appointmentPath = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository appointments = new JsonAppointmentRepository(appointmentPath);
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        SlotId thirdSlot = SlotId.of(UUID.randomUUID());
        SlotId otherStudentSlot = SlotId.of(UUID.randomUUID());
        List<SlotId> slots = List.of(firstSlot, secondSlot, thirdSlot, otherStudentSlot);
        for (int index = 0; index < slots.size(); index++) {
            appointments.createSlot(CollectionSlot.create(slots.get(index),
                    NOW.plusSeconds((index + 2) * 3600L), NOW, "officer-confidential-7"));
        }
        AppointmentId first = AppointmentId.of(UUID.randomUUID());
        AppointmentId second = AppointmentId.of(UUID.randomUUID());
        AppointmentId third = AppointmentId.of(UUID.randomUUID());
        appointments.book(claimId, "student-1", firstSlot, first,
                "student-1", UserRole.STUDENT, NOW);
        appointments.recordNoShow(first, "officer-confidential-7", NOW.plusSeconds(9000));
        appointments.book(claimId, "student-1", secondSlot, second,
                "student-1", UserRole.STUDENT, NOW.plusSeconds(9001));
        appointments.cancel(second, "student-1", "student-1", UserRole.STUDENT,
                NOW.plusSeconds(9002));
        appointments.book(claimId, "student-1", thirdSlot, third,
                "student-1", UserRole.STUDENT, NOW.plusSeconds(9003));
        appointments.book(ClaimId.of(UUID.randomUUID()), "student-2", otherStudentSlot,
                AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT, NOW);

        AuthenticatedUser student = new AuthenticatedUser("student-1", "student",
                UserRole.STUDENT);
        StudentAppointmentService service = new StudentAppointmentService(student,
                new JsonAppointmentRepository(appointmentPath),
                new StudentApprovedClaimService(student, new JsonClaimRepository(
                        temporaryDirectory.resolve("claims.json"))),
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);

        var history = service.loadHistory();
        assertEquals(3, history.size());
        assertEquals(List.of(3, 2, 1), history.stream()
                .map(StudentAppointmentHistorySummary::attemptNumber).toList());
        assertEquals(List.of(AppointmentStatus.BOOKED, AppointmentStatus.CANCELLED,
                AppointmentStatus.NO_SHOW), history.stream()
                        .map(row -> row.appointmentStatus().orElseThrow()).toList());
        assertEquals(List.of(NOW.plusSeconds(14400), NOW.plusSeconds(10800),
                NOW.plusSeconds(7200)), history.stream()
                        .map(row -> row.startsAt().orElseThrow()).toList());
        for (StudentAppointmentHistorySummary row : history) {
            assertEquals(claimId.reference(), row.claimReference());
            assertEquals("StudentAppointmentHistorySummary[redacted]", row.toString());
        }
    }
}
