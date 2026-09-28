package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Immutable appointment attempt belonging to one approved Claim. */
public final class CollectionAppointment {
    private final AppointmentId appointmentId;

    private final ClaimId claimId;

    private final String studentUserId;

    private final SlotId slotId;

    private final AppointmentStatus status;

    private final Instant bookedAt;

    private final Optional<Instant> cancelledAt;

    private final Optional<Instant> noShowAt;

    private final Optional<Instant> collectionConfirmedAt;

    private CollectionAppointment(AppointmentId id, ClaimId claim, String student,
            SlotId slot, AppointmentStatus appointmentStatus, Instant bookingTime,
            Optional<Instant> cancellationTime, Optional<Instant> noShowTime,
            Optional<Instant> confirmationTime) {
        appointmentId = Objects.requireNonNull(id, "appointmentId");
        claimId = Objects.requireNonNull(claim, "claimId");
        studentUserId = requireText(student, "studentUserId");
        slotId = Objects.requireNonNull(slot, "slotId");
        status = Objects.requireNonNull(appointmentStatus, "status");
        bookedAt = requireMillis(bookingTime, "bookedAt");
        cancelledAt = requireOptionalMillis(cancellationTime, "cancelledAt");
        noShowAt = requireOptionalMillis(noShowTime, "noShowAt");
        collectionConfirmedAt = requireOptionalMillis(confirmationTime,
                "collectionConfirmedAt");
        validateStatusData();
    }

    /** Creates a new booked appointment.
     * @param id appointment identity
     *  @param claim owning Claim
     *  @param student authenticated Student
     *  @param slot reserved slot
     *  @param bookedTime booking timestamp
     *  @return booked appointment */
    public static CollectionAppointment book(AppointmentId id, ClaimId claim,
            String student, SlotId slot, Instant bookedTime) {
        return new CollectionAppointment(id, claim, student, slot,
                AppointmentStatus.BOOKED, bookedTime, Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    /** Restores a validated persisted appointment.
     * @param id appointment identity
     *  @param claim owning Claim
     *  @param student Student identity
     *  @param slot reserved slot
     *  @param appointmentStatus persisted status
     *  @param bookedTime booking timestamp
     *  @param cancellationTime cancellation timestamp
     *  @param noShowTime no-show timestamp
     *  @param confirmationTime collection timestamp
     *  @return restored appointment */
    public static CollectionAppointment restore(AppointmentId id, ClaimId claim,
            String student, SlotId slot, AppointmentStatus appointmentStatus,
            Instant bookedTime, Optional<Instant> cancellationTime,
            Optional<Instant> noShowTime, Optional<Instant> confirmationTime) {
        return new CollectionAppointment(id, claim, student, slot, appointmentStatus,
                bookedTime, cancellationTime, noShowTime, confirmationTime);
    }

    /** Returns a booked appointment assigned to another slot.
     * @param replacement replacement slot
     *  @return rescheduled appointment */
    public CollectionAppointment reschedule(SlotId replacement) {
        requireBooked();
        return copy(replacement, status, cancelledAt, noShowAt, collectionConfirmedAt);
    }

    /** Returns a cancelled copy.
     * @param time cancellation timestamp
     *  @return cancelled appointment */
    public CollectionAppointment cancel(Instant time) {
        requireBooked();
        return copy(slotId, AppointmentStatus.CANCELLED, Optional.of(requireMillis(time,
                "cancelledAt")), Optional.empty(), Optional.empty());
    }

    /** Returns a no-show copy.
     * @param time no-show timestamp
     *  @return no-show appointment */
    public CollectionAppointment markNoShow(Instant time) {
        requireBooked();
        return copy(slotId, AppointmentStatus.NO_SHOW, Optional.empty(),
                Optional.of(requireMillis(time, "noShowAt")), Optional.empty());
    }

    /** Returns a collection-confirmed copy.
     * @param time collection timestamp
     *  @return confirmed appointment */
    public CollectionAppointment confirmCollection(Instant time) {
        requireBooked();
        return copy(slotId, AppointmentStatus.COLLECTION_CONFIRMED, Optional.empty(),
                Optional.empty(), Optional.of(requireMillis(time, "collectionConfirmedAt")));
    }

    /** Returns the appointment identity.
     * @return appointment identity */
    public AppointmentId appointmentId() { return appointmentId; }

    /** Returns the owning Claim identity.
     * @return Claim identity */
    public ClaimId claimId() { return claimId; }

    /** Returns the authenticated Student identity.
     * @return Student identity */
    public String studentUserId() { return studentUserId; }

    /** Returns the reserved slot identity.
     * @return slot identity */
    public SlotId slotId() { return slotId; }

    /** Returns the lifecycle status.
     * @return lifecycle status */
    public AppointmentStatus status() { return status; }

    /** Returns the initial booking time.
     * @return booking timestamp */
    public Instant bookedAt() { return bookedAt; }

    /** Returns the cancellation time, if cancelled.
     * @return optional cancellation timestamp */
    public Optional<Instant> cancelledAt() { return cancelledAt; }

    /** Returns the no-show time, if recorded.
     * @return optional no-show timestamp */
    public Optional<Instant> noShowAt() { return noShowAt; }

    /** Returns the collection confirmation time, if confirmed.
     * @return optional collection timestamp */
    public Optional<Instant> collectionConfirmedAt() { return collectionConfirmedAt; }

    @Override
    public String toString() {
        return "CollectionAppointment[redacted]";
    }

    private CollectionAppointment copy(SlotId slot, AppointmentStatus appointmentStatus,
            Optional<Instant> cancellationTime, Optional<Instant> noShowTime,
            Optional<Instant> confirmationTime) {
        return new CollectionAppointment(appointmentId, claimId, studentUserId, slot,
                appointmentStatus, bookedAt, cancellationTime, noShowTime,
                confirmationTime);
    }

    private void requireBooked() {
        if (status != AppointmentStatus.BOOKED) {
            throw new IllegalStateException("Only a booked appointment can be changed.");
        }
    }

    private void validateStatusData() {
        int present = (cancelledAt.isPresent() ? 1 : 0) + (noShowAt.isPresent() ? 1 : 0)
                + (collectionConfirmedAt.isPresent() ? 1 : 0);
        switch (status) {
            case BOOKED -> {
                if (present != 0) {
                    throw new IllegalArgumentException("A booked appointment has terminal data.");
                }
            }
            case CANCELLED -> requireExactly(cancelledAt, present, "cancelledAt");
            case NO_SHOW -> requireExactly(noShowAt, present, "noShowAt");
            case COLLECTION_CONFIRMED -> requireExactly(collectionConfirmedAt, present,
                    "collectionConfirmedAt");
            default -> throw new IllegalStateException("Unsupported appointment status.");
        }
    }

    private static void requireExactly(Optional<Instant> expected, int present, String name) {
        if (present != 1 || expected.isEmpty()) {
            throw new IllegalArgumentException("Appointment status has invalid " + name + ".");
        }
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
