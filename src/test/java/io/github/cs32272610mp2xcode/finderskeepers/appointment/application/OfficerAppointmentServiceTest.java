package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;

class OfficerAppointmentServiceTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void rejectsStudentIdentity() {
        AuthenticatedUser student = new AuthenticatedUser("student-1", "student",
                UserRole.STUDENT);

        assertThrows(IllegalArgumentException.class, () -> new OfficerAppointmentService(
                student, new RecordingRepository(), Clock.fixed(NOW, ZoneOffset.UTC),
                UUID::randomUUID));
    }

    @Test
    void disableSlotDelegatesAuthenticatedOfficerAndFixedTime() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        AuthenticatedUser officer = new AuthenticatedUser("officer-7", "desk.officer",
                UserRole.DESK_OFFICER);
        OfficerAppointmentService service = new OfficerAppointmentService(officer, repository,
                Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID);
        SlotId slotId = SlotId.of(UUID.fromString("00000000-0000-0000-0000-000000000007"));

        service.disableSlot(slotId);

        assertEquals(slotId, repository.disabledSlotId);
        assertEquals("officer-7", repository.officerUserId);
        assertEquals(NOW, repository.commandTime);
    }

    private static final class RecordingRepository implements AppointmentRepository {
        private SlotId disabledSlotId;

        private String officerUserId;

        private Instant commandTime;

        @Override
        public List<CollectionSlot> loadSlots() {
            return List.of();
        }

        @Override
        public List<CollectionCase> loadCases() {
            return List.of();
        }

        @Override
        public SlotResult createSlot(CollectionSlot slot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SlotResult disableSlot(SlotId slotId, String actorId, Instant time) {
            disabledSlotId = slotId;
            officerUserId = actorId;
            commandTime = time;
            return new SlotResult(SlotOutcome.NOT_FOUND, Optional.empty());
        }

        @Override
        public BookingResult book(ClaimId claimId, String studentUserId, SlotId slotId,
                AppointmentId appointmentId, String actorUserId, UserRole actorRole,
                Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BookingResult reschedule(AppointmentId appointmentId, String studentUserId,
                SlotId replacementSlotId, String actorUserId, UserRole actorRole, Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AppointmentResult cancel(AppointmentId appointmentId, String studentUserId,
                String actorUserId, UserRole actorRole, Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AppointmentResult recordNoShow(AppointmentId appointmentId, String officerId,
                Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CaseResult recordStorageLocation(ClaimId claimId, String officerId,
                String location, Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CaseResult markReadyForCollection(ClaimId claimId, String officerId,
                Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AppointmentResult confirmCollection(AppointmentId appointmentId,
                String officerId, Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CaseResult markReturned(ClaimId claimId, String officerId, Instant time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CaseResult closeCase(ClaimId claimId, String officerId, Instant time) {
            throw new UnsupportedOperationException();
        }
    }
}
