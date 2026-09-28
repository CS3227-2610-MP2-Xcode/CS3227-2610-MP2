package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Immutable aggregate of one Claim's appointment and custody history. */
public final class CollectionCase {
    private static final int MAX_LOCATION_CODE_POINTS = 120;

    private final ClaimId claimId;

    private final String studentUserId;

    private final CaseStatus status;

    private final CustodyStatus custodyStatus;

    private final Optional<String> storageLocation;

    private final List<CollectionAppointment> appointments;

    private final List<AuditEvent> auditEvents;

    private final Optional<Instant> closedAt;

    private CollectionCase(ClaimId claim, String student, CaseStatus caseStatus,
            CustodyStatus custody, Optional<String> location,
            List<CollectionAppointment> appointmentHistory,
            List<AuditEvent> eventHistory, Optional<Instant> closeTime) {
        claimId = Objects.requireNonNull(claim, "claimId");
        studentUserId = requireText(student, "studentUserId");
        status = Objects.requireNonNull(caseStatus, "status");
        custodyStatus = Objects.requireNonNull(custody, "custodyStatus");
        storageLocation = normalizeLocation(location);
        appointments = List.copyOf(Objects.requireNonNull(appointmentHistory, "appointments"));
        auditEvents = List.copyOf(Objects.requireNonNull(eventHistory, "auditEvents"));
        closedAt = requireOptionalMillis(closeTime, "closedAt");
        validate();
    }

    /** Creates a new open case awaiting item storage.
     * @param claim owning Claim
     *  @param student authenticated Student
     *  @return open collection case */
    public static CollectionCase open(ClaimId claim, String student) {
        return new CollectionCase(claim, student, CaseStatus.OPEN,
                CustodyStatus.AWAITING_STORAGE, Optional.empty(), List.of(), List.of(),
                Optional.empty());
    }

    /** Restores a complete persisted case.
     * @param claim owning Claim
     *  @param student Student identity
     *  @param caseStatus case status
     *  @param custody custody status
     *  @param location storage location
     *  @param appointmentHistory appointment history
     *  @param eventHistory audit history
     *  @param closeTime close timestamp
     *  @return restored case */
    public static CollectionCase restore(ClaimId claim, String student,
            CaseStatus caseStatus, CustodyStatus custody, Optional<String> location,
            List<CollectionAppointment> appointmentHistory,
            List<AuditEvent> eventHistory, Optional<Instant> closeTime) {
        return new CollectionCase(claim, student, caseStatus, custody, location,
                appointmentHistory, eventHistory, closeTime);
    }

    /** Returns the Claim identity.
     * @return Claim identity */
    public ClaimId claimId() { return claimId; }

    /** Returns the authenticated Student identity.
     * @return Student identity */
    public String studentUserId() { return studentUserId; }

    /** Returns the case lifecycle status.
     * @return case status */
    public CaseStatus status() { return status; }

    /** Returns the current custody status.
     * @return custody status */
    public CustodyStatus custodyStatus() { return custodyStatus; }

    /** Returns the officer-only storage location.
     * @return optional storage location */
    public Optional<String> storageLocation() { return storageLocation; }

    /** Returns immutable appointment history.
     * @return appointment history */
    public List<CollectionAppointment> appointments() { return appointments; }

    /** Returns immutable audit history.
     * @return audit history */
    public List<AuditEvent> auditEvents() { return auditEvents; }

    /** Returns the case close time, if closed.
     * @return optional close timestamp */
    public Optional<Instant> closedAt() { return closedAt; }

    /** Returns the current active booking, if one exists.
     * @return active booking, when present */
    public Optional<CollectionAppointment> activeAppointment() {
        return appointments.stream()
                .filter(appointment -> appointment.status() == AppointmentStatus.BOOKED)
                .findFirst();
    }

    /** Returns whether this case may receive another booking.
     * @return whether another booking is allowed */
    public boolean mayBookAgain() {
        return status == CaseStatus.OPEN && activeAppointment().isEmpty()
                && custodyStatus != CustodyStatus.RETURNED
                && appointments.stream().noneMatch(appointment ->
                        appointment.status() == AppointmentStatus.COLLECTION_CONFIRMED);
    }

    /** Returns a copy with a changed custody status and location.
     * @param replacement new custody status
     *  @param replacementLocation new storage location
     *  @return updated case */
    public CollectionCase withCustody(CustodyStatus replacement,
            Optional<String> replacementLocation) {
        return new CollectionCase(claimId, studentUserId, status, replacement,
                replacementLocation, appointments, auditEvents, closedAt);
    }

    /** Returns a copy with an appended appointment.
     * @param appointment appointment to append
     *  @return updated case */
    public CollectionCase withAppointment(CollectionAppointment appointment) {
        List<CollectionAppointment> updated = new java.util.ArrayList<>(appointments);
        updated.add(Objects.requireNonNull(appointment, "appointment"));
        return new CollectionCase(claimId, studentUserId, status, custodyStatus,
                storageLocation, updated, auditEvents, closedAt);
    }

    /** Returns a copy replacing an appointment with the same identity.
     * @param replacement replacement appointment
     *  @return updated case */
    public CollectionCase replaceAppointment(CollectionAppointment replacement) {
        Objects.requireNonNull(replacement, "replacement");
        List<CollectionAppointment> updated = new java.util.ArrayList<>(appointments);
        boolean replaced = false;
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).appointmentId().equals(replacement.appointmentId())) {
                updated.set(index, replacement);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            throw new IllegalArgumentException("Appointment does not belong to this case.");
        }
        return new CollectionCase(claimId, studentUserId, status, custodyStatus,
                storageLocation, updated, auditEvents, closedAt);
    }

    /** Returns a copy with an appended audit event.
     * @param event event to append
     *  @return updated case */
    public CollectionCase withAuditEvent(AuditEvent event) {
        List<AuditEvent> updated = new java.util.ArrayList<>(auditEvents);
        updated.add(Objects.requireNonNull(event, "event"));
        return new CollectionCase(claimId, studentUserId, status, custodyStatus,
                storageLocation, appointments, updated, closedAt);
    }

    /** Returns a closed copy.
     * @param time close timestamp
     *  @return closed case */
    public CollectionCase close(Instant time) {
        if (status != CaseStatus.OPEN || custodyStatus != CustodyStatus.RETURNED
                || appointments.stream().noneMatch(appointment ->
                        appointment.status() == AppointmentStatus.COLLECTION_CONFIRMED)) {
            throw new IllegalStateException("The case is not ready to close.");
        }
        return new CollectionCase(claimId, studentUserId, CaseStatus.CLOSED, custodyStatus,
                storageLocation, appointments, auditEvents,
                Optional.of(requireMillis(time, "closedAt")));
    }

    @Override
    public String toString() {
        return "CollectionCase[redacted]";
    }

    private void validate() {
        if (custodyStatus != CustodyStatus.AWAITING_STORAGE && storageLocation.isEmpty()) {
            throw new IllegalArgumentException("Stored custody requires a storage location.");
        }
        if (status == CaseStatus.CLOSED) {
            if (closedAt.isEmpty() || custodyStatus != CustodyStatus.RETURNED) {
                throw new IllegalArgumentException("A closed case has invalid terminal data.");
            }
        } else if (closedAt.isPresent()) {
            throw new IllegalArgumentException("An open case cannot have a close time.");
        }
        long active = appointments.stream().filter(appointment ->
                appointment.status() == AppointmentStatus.BOOKED).count();
        if (active > 1) {
            throw new IllegalArgumentException("A case cannot have multiple active appointments.");
        }
        appointments.forEach(appointment -> {
            if (!appointment.claimId().equals(claimId)
                    || !appointment.studentUserId().equals(studentUserId)) {
                throw new IllegalArgumentException("Appointment ownership is inconsistent.");
            }
        });
    }

    private static Optional<String> normalizeLocation(Optional<String> location) {
        Objects.requireNonNull(location, "storageLocation");
        if (location.isEmpty()) {
            return Optional.empty();
        }
        String trimmed = requireText(location.orElseThrow(), "storageLocation");
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LOCATION_CODE_POINTS) {
            throw new IllegalArgumentException("Storage location is too long.");
        }
        return Optional.of(trimmed);
    }

    private static Optional<Instant> requireOptionalMillis(Optional<Instant> value, String name) {
        Objects.requireNonNull(value, name);
        value.ifPresent(time -> requireMillis(time, name));
        return value;
    }

    private static Instant requireMillis(Instant value, String name) {
        Objects.requireNonNull(value, name);
        if (!value.equals(value.truncatedTo(ChronoUnit.MILLIS))) {
            throw new IllegalArgumentException(name + " must use millisecond precision.");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
