package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CustodyStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonAppointmentRepositoryTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void createsBooksCancelsAndReloadsAppointment() throws Exception {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ClaimId claimId = ClaimId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        AppointmentId appointmentId = AppointmentId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000003"));
        CollectionSlot slot = CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1");

        assertEquals(AppointmentRepository.SlotOutcome.CREATED,
                repository.createSlot(slot).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.book(claimId, "student-1", slotId, appointmentId, "student-1",
                        UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentStatus.BOOKED,
                repository.loadCases().get(0).activeAppointment().orElseThrow().status());

        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.cancel(appointmentId, "student-1", "student-1", UserRole.STUDENT,
                        NOW.plusSeconds(1)).outcome());
        JsonAppointmentRepository reloaded = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        assertEquals(AppointmentStatus.CANCELLED,
                reloaded.loadCases().get(0).appointments().get(0).status());
        assertTrue(reloaded.loadCases().get(0).mayBookAgain());
    }

    @Test
    void competingBookingsForOneSlotAllowOnlyOne() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));

        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.book(ClaimId.of(UUID.randomUUID()), "student-1", slotId,
                        AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT,
                        NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.SLOT_TAKEN,
                repository.book(ClaimId.of(UUID.randomUUID()), "student-2", slotId,
                        AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT,
                        NOW).outcome());
    }

    @Test
    void custodyCollectionAndClosureFollowTheRequiredSequence() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        repository.book(claimId, "student-1", slotId, appointmentId, "student-1",
                UserRole.STUDENT, NOW);

        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.recordStorageLocation(claimId, "officer-1", "Locker A1",
                        NOW.plusSeconds(1)).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.markReadyForCollection(claimId, "officer-1", NOW.plusSeconds(2))
                        .outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.TOO_EARLY,
                repository.confirmCollection(appointmentId, "officer-1", NOW.plusSeconds(3))
                        .outcome());
        Instant duringSlot = Instant.parse("2030-01-01T02:05:00Z");
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.confirmCollection(appointmentId, "officer-1", duringSlot).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.markReturned(claimId, "officer-1", duringSlot.plusSeconds(1))
                        .outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.closeCase(claimId, "officer-1", duringSlot.plusSeconds(2)).outcome());

        var result = repository.loadCases().get(0);
        assertEquals(CaseStatus.CLOSED, result.status());
        assertEquals(CustodyStatus.RETURNED, result.custodyStatus());
        assertEquals(6, result.auditEvents().size());
    }

    @Test
    void noShowReleasesClaimForAnotherBooking() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId firstAppointment = AppointmentId.of(UUID.randomUUID());
        AppointmentId secondAppointment = AppointmentId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(firstSlot,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot,
                Instant.parse("2030-01-01T03:00:00Z"), NOW, "officer-1"));
        repository.book(claimId, "student-1", firstSlot, firstAppointment, "student-1",
                UserRole.STUDENT, NOW);
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.recordNoShow(firstAppointment, "officer-1",
                        Instant.parse("2030-01-01T02:30:00Z")).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.book(claimId, "student-1", secondSlot, secondAppointment, "student-1",
                        UserRole.STUDENT, NOW).outcome());
    }

    @Test
    void collectionConfirmationPreventsAnotherBookingForTheClaim()
            throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        Instant firstStart = Instant.parse("2030-01-01T02:00:00Z");
        repository.createSlot(CollectionSlot.create(firstSlot, firstStart, NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot,
                Instant.parse("2030-01-01T03:00:00Z"), NOW, "officer-1"));
        repository.book(claimId, "student-1", firstSlot, appointmentId, "student-1",
                UserRole.STUDENT, NOW);
        repository.recordStorageLocation(claimId, "officer-1", "Locker A1", NOW.plusSeconds(1));
        repository.markReadyForCollection(claimId, "officer-1", NOW.plusSeconds(2));

        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.confirmCollection(appointmentId, "officer-1",
                        firstStart.plusSeconds(1)).outcome());
        var caseState = repository.loadCases().get(0);
        assertFalse(caseState.mayBookAgain());
        assertEquals(AppointmentRepository.BookingOutcome.CLAIM_ALREADY_ACTIVE,
                repository.book(claimId, "student-1", secondSlot,
                        AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT,
                        NOW).outcome());
    }

    @Test
    void rescheduleAndDisableRejectConflictsAndPastChanges() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        Instant firstStart = Instant.parse("2030-01-01T02:00:00Z");
        Instant secondStart = Instant.parse("2030-01-01T03:00:00Z");
        repository.createSlot(CollectionSlot.create(firstSlot, firstStart, NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot, secondStart, NOW, "officer-1"));
        repository.book(claimId, "student-1", firstSlot, appointmentId, "student-1",
                UserRole.STUDENT, NOW);

        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.reschedule(appointmentId, "student-1", secondSlot, "student-1",
                        UserRole.STUDENT, NOW.plusSeconds(1)).outcome());
        assertEquals(AppointmentRepository.SlotOutcome.BOOKED,
                repository.disableSlot(secondSlot, "officer-1", NOW.plusSeconds(2)).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.TOO_LATE,
                repository.reschedule(appointmentId, "student-1", firstSlot, "student-1",
                        UserRole.STUDENT, secondStart.plusSeconds(1)).outcome());
    }

    @Test
    void repeatedCollectionAndInvalidClosureAreRejected() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        Instant start = Instant.parse("2030-01-01T02:00:00Z");
        repository.createSlot(CollectionSlot.create(slotId, start, NOW, "officer-1"));
        repository.book(claimId, "student-1", slotId, appointmentId, "student-1",
                UserRole.STUDENT, NOW);

        assertEquals(AppointmentRepository.CaseOutcome.INVALID_CUSTODY,
                repository.closeCase(claimId, "officer-1", NOW.plusSeconds(1)).outcome());
        repository.recordStorageLocation(claimId, "officer-1", "Locker A1", NOW.plusSeconds(2));
        repository.markReadyForCollection(claimId, "officer-1", NOW.plusSeconds(3));
        Instant duringSlot = start.plusSeconds(60);
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.confirmCollection(appointmentId, "officer-1", duringSlot).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.ALREADY_TERMINAL,
                repository.confirmCollection(appointmentId, "officer-1", duringSlot)
                        .outcome());
        repository.markReturned(claimId, "officer-1", duringSlot.plusSeconds(1));
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.closeCase(claimId, "officer-1", duringSlot.plusSeconds(2)).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.ALREADY_CLOSED,
                repository.closeCase(claimId, "officer-1", duringSlot.plusSeconds(3)).outcome());
    }

    @Test
    void corruptStoreFailsClosedAndPreservesBytes() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        byte[] corrupt = "{not-json".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(store, corrupt);
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);

        AppointmentStoreException failure = org.junit.jupiter.api.Assertions.assertThrows(
                AppointmentStoreException.class, repository::loadCases);
        assertEquals(AppointmentStoreException.Reason.CORRUPT_STORE, failure.reason());
        assertTrue(java.util.Arrays.equals(corrupt, Files.readAllBytes(store)));
    }

    @Test
    void duplicateActiveSlotOccupancyFailsClosedAndPreservesBytes() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(firstSlot,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot,
                Instant.parse("2030-01-01T03:00:00Z"), NOW, "officer-1"));
        repository.book(ClaimId.of(UUID.randomUUID()), "student-1", firstSlot,
                AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT, NOW);
        repository.book(ClaimId.of(UUID.randomUUID()), "student-2", secondSlot,
                AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT, NOW);

        String validDocument = Files.readString(store, java.nio.charset.StandardCharsets.UTF_8);
        String secondSlotField = "\"slotId\": \"" + secondSlot.value() + "\"";
        int appointmentSlotReference = validDocument.lastIndexOf(secondSlotField);
        assertTrue(appointmentSlotReference >= 0);
        String inconsistentDocument = validDocument.substring(0, appointmentSlotReference)
                + "\"slotId\": \"" + firstSlot.value() + "\""
                + validDocument.substring(appointmentSlotReference + secondSlotField.length());
        byte[] corrupt = inconsistentDocument.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(store, corrupt);

        JsonAppointmentRepository reloaded = new JsonAppointmentRepository(store);
        AppointmentStoreException failure = org.junit.jupiter.api.Assertions.assertThrows(
                AppointmentStoreException.class, reloaded::loadCases);
        assertEquals(AppointmentStoreException.Reason.CORRUPT_STORE, failure.reason());
        assertTrue(java.util.Arrays.equals(corrupt, Files.readAllBytes(store)));
    }
}
