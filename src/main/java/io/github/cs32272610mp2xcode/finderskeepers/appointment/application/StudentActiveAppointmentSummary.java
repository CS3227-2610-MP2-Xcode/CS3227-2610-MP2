package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import java.time.Instant;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;

/** Student-safe active appointment details, including its scheduled slot time.
 * @param appointmentId identity used for cancellation or rescheduling
 * @param status active appointment status
 * @param startsAt scheduled collection time */
public record StudentActiveAppointmentSummary(AppointmentId appointmentId,
        AppointmentStatus status, Instant startsAt) {
    /** Validates active appointment details.
     * @param appointmentId appointment identity
     * @param status active appointment status
     * @param startsAt scheduled collection time */
    public StudentActiveAppointmentSummary {
        appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
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
