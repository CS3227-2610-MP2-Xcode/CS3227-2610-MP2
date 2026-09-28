package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

class AppointmentAuthorizationBoundaryTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void wrongRoleAndOtherStudentCannotMutateAnExistingBooking() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId first = slot(1);
        SlotId second = slot(2);
        ClaimId claim = claim(3);
        AppointmentId appointment = appointment(4);
        repository.createSlot(CollectionSlot.create(first, NOW.plusSeconds(3600),
                NOW, "officer-a"));
        repository.createSlot(CollectionSlot.create(second, NOW.plusSeconds(5400),
                NOW, "officer-a"));
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.book(claim, "student-a", first, appointment, "student-a",
                        UserRole.STUDENT, NOW).outcome());
        byte[] before = Files.readAllBytes(store);

        assertThrows(IllegalArgumentException.class,
                () -> repository.book(claim(5), "student-b", second,
                        appointment(6), "officer-a", UserRole.DESK_OFFICER, NOW));
        assertEquals(AppointmentRepository.BookingOutcome.NOT_AUTHORIZED,
                repository.reschedule(appointment, "student-b", second, "student-b",
                        UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.NOT_AUTHORIZED,
                repository.cancel(appointment, "student-b", "student-b",
                        UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.NOT_AUTHORIZED,
                repository.book(claim, "student-b", second, appointment(7), "student-b",
                        UserRole.STUDENT, NOW).outcome());
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)));
        assertEquals(first, new JsonAppointmentRepository(store).loadCases().getFirst()
                .appointments().getFirst().slotId());
    }

    @Test
    void duplicateAppointmentIdAndUnknownTargetsFailWithoutPartialWrite()
            throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId first = slot(1);
        SlotId second = slot(2);
        ClaimId claim = claim(3);
        AppointmentId appointment = appointment(4);
        repository.createSlot(CollectionSlot.create(first, NOW.plusSeconds(3600),
                NOW, "officer-a"));
        repository.createSlot(CollectionSlot.create(second, NOW.plusSeconds(5400),
                NOW, "officer-a"));
        repository.book(claim, "student-a", first, appointment, "student-a",
                UserRole.STUDENT, NOW);
        byte[] before = Files.readAllBytes(store);

        assertEquals(AppointmentRepository.BookingOutcome.ID_COLLISION,
                repository.book(claim(5), "student-b", second, appointment,
                        "student-b", UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.SLOT_NOT_FOUND,
                repository.book(claim(5), "student-b", slot(99), appointment(6),
                        "student-b", UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.NOT_FOUND,
                repository.reschedule(appointment(99), "student-a", second,
                        "student-a", UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.NOT_FOUND,
                repository.cancel(appointment(99), "student-a", "student-a",
                        UserRole.STUDENT, NOW).outcome());
        assertTrue(Arrays.equals(before, Files.readAllBytes(store)));
        assertEquals(1, new JsonAppointmentRepository(store).loadCases().size());
    }

    private static SlotId slot(long number) {
        return SlotId.of(new UUID(0, number));
    }

    private static ClaimId claim(long number) {
        return ClaimId.of(new UUID(0, number));
    }

    private static AppointmentId appointment(long number) {
        return AppointmentId.of(new UUID(0, number));
    }
}
