package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionAppointment;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimSummary;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentApprovedClaimService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Student-facing appointment use cases bound to one authenticated Student. */
public final class StudentAppointmentService {
    private final AuthenticatedUser user;

    private final AppointmentRepository repository;

    private final StudentApprovedClaimService approvedClaims;

    private final Clock clock;

    private final Supplier<UUID> appointmentIds;

    /** Creates an authenticated Student appointment service.
     * @param authenticatedUser authenticated Student
     * @param appointments appointment repository
     * @param claims approved Claim boundary
     * @param eventClock operation clock
     * @param ids appointment ID source */
    public StudentAppointmentService(AuthenticatedUser authenticatedUser,
            AppointmentRepository appointments, StudentApprovedClaimService claims,
            Clock eventClock, Supplier<UUID> ids) {
        user = requireStudent(authenticatedUser);
        repository = Objects.requireNonNull(appointments, "appointments");
        approvedClaims = Objects.requireNonNull(claims, "claims");
        clock = Objects.requireNonNull(eventClock, "clock");
        appointmentIds = Objects.requireNonNull(ids, "ids");
    }

    /** Loads the currently eligible approved Claims for booking.
     * @return approved Claim summaries
     * @throws ClaimStoreException when Claim storage is unavailable */
    public List<ApprovedClaimSummary> loadApprovedClaims()
            throws ClaimStoreException {
        return approvedClaims.loadApprovedClaims();
    }

    /** Loads future enabled slots that are not actively occupied.
     * @return available slots
     * @throws AppointmentStoreException when appointment storage is unavailable */
    public List<CollectionSlot> loadAvailableSlots() throws AppointmentStoreException {
        Instant now = clock.instant();
        List<CollectionCase> cases = repository.loadCases();
        return repository.loadSlots().stream()
                .filter(CollectionSlot::enabled)
                .filter(slot -> slot.startsAt().isAfter(now))
                .filter(slot -> cases.stream()
                        .flatMap(caseState -> caseState.appointments().stream())
                        .noneMatch(appointment -> appointment.status() == AppointmentStatus.BOOKED
                                && appointment.slotId().equals(slot.slotId())))
                .sorted(Comparator.comparing(CollectionSlot::startsAt))
                .toList();
    }

    /** Loads the Student's active appointments with their scheduled slot times.
     * @return Student-safe active appointment summaries
     * @throws AppointmentStoreException when storage is unavailable */
    public List<StudentActiveAppointmentSummary> loadActiveAppointments()
            throws AppointmentStoreException {
        Map<SlotId, Instant> startsBySlot = repository.loadSlots().stream()
                .collect(Collectors.toMap(CollectionSlot::slotId, CollectionSlot::startsAt));
        List<StudentActiveAppointmentSummary> active = new ArrayList<>();
        for (CollectionAppointment appointment : loadOwnedAppointments()) {
            if (appointment.status() == AppointmentStatus.BOOKED) {
                Instant startsAt = startsBySlot.get(appointment.slotId());
                if (startsAt == null) {
                    throw new AppointmentStoreException(
                            AppointmentStoreException.Reason.CORRUPT_STORE);
                }
                active.add(new StudentActiveAppointmentSummary(appointment.appointmentId(),
                        appointment.status(), startsAt));
            }
        }
        return List.copyOf(active);
    }

    /** Loads every appointment attempt for the Student's collection cases.
     * @return Student-safe attempt history, newest attempt first within each case
     * @throws AppointmentStoreException when storage is unavailable */
    public List<StudentAppointmentHistorySummary> loadHistory()
            throws AppointmentStoreException {
        Map<SlotId, Instant> startsBySlot = repository.loadSlots().stream()
                .collect(Collectors.toMap(CollectionSlot::slotId, CollectionSlot::startsAt));
        List<CollectionCase> ownedCases = repository.loadCases().stream()
                .filter(caseState -> caseState.studentUserId().equals(user.userId()))
                .sorted(Comparator.comparing(CollectionCase::claimId,
                        Comparator.comparing(ClaimId::value)).reversed())
                .toList();
        List<StudentAppointmentHistorySummary> history = new ArrayList<>();
        for (CollectionCase caseState : ownedCases) {
            if (caseState.appointments().isEmpty()) {
                history.add(new StudentAppointmentHistorySummary(caseState.claimId().reference(),
                        caseState.status(), 0, Optional.empty(), Optional.empty()));
            }
            for (int index = caseState.appointments().size() - 1; index >= 0; index--) {
                CollectionAppointment appointment = caseState.appointments().get(index);
                Instant startsAt = startsBySlot.get(appointment.slotId());
                if (startsAt == null) {
                    throw new AppointmentStoreException(
                            AppointmentStoreException.Reason.CORRUPT_STORE);
                }
                history.add(new StudentAppointmentHistorySummary(caseState.claimId().reference(),
                        caseState.status(), index + 1, Optional.of(appointment.status()),
                        Optional.of(startsAt)));
            }
        }
        return List.copyOf(history);
    }

    /** Attempts to book an approved Claim into one available slot.
     * @param claimId approved Claim
     * @param slotId requested slot
     * @return booking result
     * @throws ClaimStoreException when Claim storage is unavailable
     * @throws AppointmentStoreException when appointment storage is unavailable */
    public AppointmentRepository.BookingResult book(ClaimId claimId,
            io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId slotId)
            throws ClaimStoreException, AppointmentStoreException {
        ApprovedClaimSummary summary = findApprovedClaim(claimId);
        if (summary == null) {
            return new AppointmentRepository.BookingResult(
                    AppointmentRepository.BookingOutcome.NOT_AUTHORIZED,
                    java.util.Optional.empty());
        }
        Instant now = operationTime();
        return repository.book(claimId, user.userId(), slotId,
                AppointmentId.of(appointmentIds.get()), user.userId(), UserRole.STUDENT, now);
    }

    /** Attempts to reschedule one owned active appointment.
     * @param appointmentId appointment to change
     * @param slotId replacement slot
     * @return booking result
     * @throws ClaimStoreException when Claim storage is unavailable
     * @throws AppointmentStoreException when appointment storage is unavailable */
    public AppointmentRepository.BookingResult reschedule(AppointmentId appointmentId,
            io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId slotId)
            throws ClaimStoreException, AppointmentStoreException {
        CollectionAppointment appointment = findOwnedAppointment(appointmentId);
        if (appointment == null || findApprovedClaim(appointment.claimId()) == null) {
            return new AppointmentRepository.BookingResult(
                    AppointmentRepository.BookingOutcome.NOT_AUTHORIZED,
                    java.util.Optional.empty());
        }
        return repository.reschedule(appointmentId, user.userId(), slotId, user.userId(),
                UserRole.STUDENT, operationTime());
    }

    /** Attempts to cancel one owned active appointment.
     * @param appointmentId appointment to cancel
     * @return appointment result
     * @throws AppointmentStoreException when storage is unavailable */
    public AppointmentRepository.AppointmentResult cancel(AppointmentId appointmentId)
            throws AppointmentStoreException {
        return repository.cancel(appointmentId, user.userId(), user.userId(), UserRole.STUDENT,
                operationTime());
    }

    private Instant operationTime() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }

    private ApprovedClaimSummary findApprovedClaim(ClaimId claimId)
            throws ClaimStoreException {
        return approvedClaims.loadApprovedClaims().stream()
                .filter(summary -> summary.claimId().equals(claimId))
                .findFirst().orElse(null);
    }

    private List<CollectionAppointment> loadOwnedAppointments()
            throws AppointmentStoreException {
        return repository.loadCases().stream()
                .filter(caseState -> caseState.studentUserId().equals(user.userId()))
                .flatMap(caseState -> caseState.appointments().stream())
                .sorted(Comparator.comparing(CollectionAppointment::bookedAt).reversed())
                .toList();
    }

    private CollectionAppointment findOwnedAppointment(AppointmentId id)
            throws AppointmentStoreException {
        return loadOwnedAppointments().stream()
                .filter(appointment -> appointment.appointmentId().equals(id))
                .findFirst().orElse(null);
    }

    private static AuthenticatedUser requireStudent(AuthenticatedUser candidate) {
        AuthenticatedUser authenticatedUser = Objects.requireNonNull(candidate, "user");
        if (authenticatedUser.role() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Student appointment service requires a Student.");
        }
        return authenticatedUser;
    }
}
