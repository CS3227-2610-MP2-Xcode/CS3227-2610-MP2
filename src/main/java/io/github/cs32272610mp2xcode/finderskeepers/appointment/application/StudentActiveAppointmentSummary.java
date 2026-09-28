package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import java.time.Instant;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Student-safe active appointment details, including its scheduled slot time.
 * @param appointmentId identity used for cancellation or rescheduling
 * @param claimId approved Claim owning this appointment
 * @param status active appointment status
 * @param startsAt scheduled collection time */
public record StudentActiveAppointmentSummary(AppointmentId appointmentId, ClaimId claimId,
        AppointmentStatus status, Instant startsAt) {
    /** Validates active appointment details.
     * @param appointmentId appointment identity
     * @param claimId owning approved Claim
     * @param status active appointment status
     * @param startsAt scheduled collection time */
    public StudentActiveAppointmentSummary {
        appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        claimId = Objects.requireNonNull(claimId, "claimId");
        status = Objects.requireNonNull(status, "status");
        startsAt = Objects.requireNonNull(startsAt, "startsAt");
        if (status != AppointmentStatus.BOOKED) {
            throw new IllegalArgumentException("An active appointment must be booked.");
        }
    }

    @Override
    public String toString() {
        return "StudentActiveAppointmentSummary[redacted]";
    }
}
