package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Desk Officer appointment, custody, and collection use cases. */
public final class OfficerAppointmentService {
    private final AuthenticatedUser user;

    private final AppointmentRepository repository;

    private final Clock clock;

    private final Supplier<UUID> slotIds;

    /** Creates an authenticated Desk Officer appointment service.
     * @param authenticatedUser authenticated Desk Officer
     * @param appointments appointment repository
     * @param eventClock operation clock
     * @param ids slot ID source */
    public OfficerAppointmentService(AuthenticatedUser authenticatedUser,
            AppointmentRepository appointments, Clock eventClock, Supplier<UUID> ids) {
        user = requireOfficer(authenticatedUser);
        repository = Objects.requireNonNull(appointments, "appointments");
        clock = Objects.requireNonNull(eventClock, "clock");
        slotIds = Objects.requireNonNull(ids, "ids");
    }

    /** Loads all retained slots ordered by start time.
     * @return ordered slots
     * @throws AppointmentStoreException when storage is unavailable */
    public List<CollectionSlot> loadSlots() throws AppointmentStoreException {
        return repository.loadSlots().stream()
                .sorted(Comparator.comparing(CollectionSlot::startsAt)).toList();
    }

    /** Loads cases with currently booked appointments.
     * @return booked cases
     * @throws AppointmentStoreException when storage is unavailable */
    public List<CollectionCase> loadBookedCases() throws AppointmentStoreException {
        return repository.loadCases().stream()
                .filter(caseState -> caseState.activeAppointment().isPresent())
                .sorted(Comparator.comparing(caseState -> caseState.activeAppointment()
                        .orElseThrow().slotId().value()))
                .toList();
    }

    /** Loads all cases for custody and audit operations.
     * @return all cases
     * @throws AppointmentStoreException when storage is unavailable */
    public List<CollectionCase> loadCases() throws AppointmentStoreException {
        return repository.loadCases();
    }

    /** Creates one enabled 30-minute slot.
     * @param startsAt slot start instant
     * @return slot operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.SlotResult createSlot(Instant startsAt)
            throws AppointmentStoreException {
        Instant now = operationTime();
        return repository.createSlot(CollectionSlot.create(SlotId.of(slotIds.get()), startsAt,
                now, user.userId()));
    }

    /** Disables one future unbooked slot.
     * @param slotId slot to disable
     * @return slot operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.SlotResult disableSlot(SlotId slotId)
            throws AppointmentStoreException {
        return repository.disableSlot(slotId, user.userId(), operationTime());
    }

    /** Records that a booked appointment was missed.
     * @param appointmentId appointment to mark
     * @return appointment operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.AppointmentResult recordNoShow(AppointmentId appointmentId)
            throws AppointmentStoreException {
        return repository.recordNoShow(appointmentId, user.userId(), operationTime());
    }

    /** Records the officer-only storage location.
     * @param claimId Claim being stored
     * @param location storage location
     * @return case operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.CaseResult recordStorageLocation(ClaimId claimId,
            String location) throws AppointmentStoreException {
        return repository.recordStorageLocation(claimId, user.userId(), location,
                operationTime());
    }

    /** Marks stored custody ready for collection.
     * @param claimId Claim being prepared
     * @return case operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.CaseResult markReadyForCollection(ClaimId claimId)
            throws AppointmentStoreException {
        return repository.markReadyForCollection(claimId, user.userId(), operationTime());
    }

    /** Confirms attendance and records collection time.
     * @param appointmentId appointment being collected
     * @return appointment operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.AppointmentResult confirmCollection(
            AppointmentId appointmentId) throws AppointmentStoreException {
        return repository.confirmCollection(appointmentId, user.userId(), operationTime());
    }

    /** Records that custody was returned to the Student.
     * @param claimId Claim being returned
     * @return case operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.CaseResult markReturned(ClaimId claimId)
            throws AppointmentStoreException {
        return repository.markReturned(claimId, user.userId(), operationTime());
    }

    /** Closes a returned case.
     * @param claimId Claim to close
     * @return case operation result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.CaseResult closeCase(ClaimId claimId)
            throws AppointmentStoreException {
        return repository.closeCase(claimId, user.userId(), operationTime());
    }

    /** Returns the service clock time used for UI availability hints.
     * @return current operation time */
    public Instant currentTime() {
        return operationTime();
    }

    private Instant operationTime() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }

    private static AuthenticatedUser requireOfficer(AuthenticatedUser candidate) {
        AuthenticatedUser authenticatedUser = Objects.requireNonNull(candidate, "user");
        if (authenticatedUser.role() != UserRole.DESK_OFFICER) {
            throw new IllegalArgumentException("Officer appointment service requires a Desk Officer.");
        }
        return authenticatedUser;
    }
}
