package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppointmentTransitionRejectionTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void slotAndBookingRejectionsPreserveStoredSchedule() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId booked = slot(1);
        SlotId disabled = slot(2);
        SlotId future = slot(3);
        repository.createSlot(CollectionSlot.create(booked, NOW.plusSeconds(3600),
                NOW, "officer-a"));
        repository.createSlot(CollectionSlot.create(disabled, NOW.plusSeconds(5400),
                NOW, "officer-a"));
        repository.createSlot(CollectionSlot.create(future, NOW.plusSeconds(7200),
                NOW, "officer-a"));
        repository.disableSlot(disabled, "officer-a", NOW);
        ClaimId claim = claim(4);
        AppointmentId appointment = appointment(5);
        repository.book(claim, "student-a", booked, appointment, "student-a",
                UserRole.STUDENT, NOW);
        byte[] before = Files.readAllBytes(store);

        assertEquals(AppointmentRepository.SlotOutcome.ID_COLLISION,
                repository.createSlot(CollectionSlot.create(booked,
                        NOW.plusSeconds(9000), NOW, "officer-a")).outcome());
        assertEquals(AppointmentRepository.SlotOutcome.NOT_FOUND,
                repository.disableSlot(slot(99), "officer-a", NOW).outcome());
        assertEquals(AppointmentRepository.SlotOutcome.BOOKED,
                repository.disableSlot(booked, "officer-a", NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.SLOT_DISABLED,
                repository.book(claim(6), "student-b", disabled, appointment(7),
                        "student-b", UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.INVALID_TIME,
                repository.book(claim(6), "student-b", future, appointment(7),
                        "student-b", UserRole.STUDENT, NOW.plusSeconds(7200)).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.CLAIM_ALREADY_ACTIVE,
                repository.book(claim, "student-a", future, appointment(7),
                        "student-a", UserRole.STUDENT, NOW).outcome());
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)));
    }

    @Test
    void rejectedRescheduleAndCustodyTransitionsDoNotAddAuditEvents()
            throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId original = slot(1);
        SlotId disabled = slot(2);
        repository.createSlot(CollectionSlot.create(original, NOW.plusSeconds(3600),
                NOW, "officer-a"));
        repository.createSlot(CollectionSlot.create(disabled, NOW.plusSeconds(5400),
                NOW, "officer-a"));
        repository.disableSlot(disabled, "officer-a", NOW);
        ClaimId claim = claim(3);
        AppointmentId appointment = appointment(4);
        repository.book(claim, "student-a", original, appointment, "student-a",
                UserRole.STUDENT, NOW);
        byte[] before = Files.readAllBytes(store);

        assertEquals(AppointmentRepository.BookingOutcome.SLOT_NOT_FOUND,
                repository.reschedule(appointment, "student-a", slot(99),
                        "student-a", UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.SLOT_DISABLED,
                repository.reschedule(appointment, "student-a", disabled,
                        "student-a", UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.TOO_LATE,
                repository.reschedule(appointment, "student-a", disabled,
                        "student-a", UserRole.STUDENT, NOW.plusSeconds(3600)).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.TOO_LATE,
                repository.cancel(appointment, "student-a", "student-a",
                        UserRole.STUDENT, NOW.plusSeconds(3600)).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.TOO_EARLY,
                repository.confirmCollection(appointment, "officer-a", NOW).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.INVALID_CUSTODY,
                repository.confirmCollection(appointment, "officer-a",
                        NOW.plusSeconds(3600)).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.INVALID_CUSTODY,
                repository.markReadyForCollection(claim, "officer-a", NOW).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.INVALID_CUSTODY,
                repository.markReturned(claim, "officer-a", NOW).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.INVALID_LOCATION,
                repository.recordStorageLocation(claim, "officer-a", "X".repeat(121),
                        NOW).outcome());
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)));
        assertEquals(1, repository.loadCases().getFirst().auditEvents().size());
    }

    private static SlotId slot(long id) {
        return SlotId.of(new UUID(0, id));
    }

    private static ClaimId claim(long id) {
        return ClaimId.of(new UUID(0, id));
    }

    private static AppointmentId appointment(long id) {
        return AppointmentId.of(new UUID(0, id));
    }
}
