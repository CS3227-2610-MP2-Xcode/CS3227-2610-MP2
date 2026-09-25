package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        assertEquals(AppointmentStatus.NO_SHOW,
                result.getFirst().latestAppointmentStatus().orElseThrow());
        assertFalse(result.getFirst().toString().contains("officer-confidential-7"));
        assertFalse(result.getFirst().toString().contains("SECRET-LOCKER-42"));
        assertEquals("StudentAppointmentHistorySummary[redacted]",
                result.getFirst().toString());
    }
}
