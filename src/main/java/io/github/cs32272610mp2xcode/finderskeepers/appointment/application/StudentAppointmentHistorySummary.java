package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;

/**
 * Student-safe history row for one appointment attempt or an empty case.
 *
 * @param claimReference user-visible Claim reference
 * @param status case lifecycle status
 * @param attemptNumber one-based attempt number, or zero when there is no appointment
 * @param appointmentStatus Student-visible appointment status, if an attempt exists
 * @param startsAt scheduled collection time, if an attempt exists
 */
public record StudentAppointmentHistorySummary(String claimReference, CaseStatus status,
        int attemptNumber, Optional<AppointmentStatus> appointmentStatus,
        Optional<Instant> startsAt) {
    /** Validates the values displayed in Student appointment history.
     * @param claimReference user-visible Claim reference
     * @param status case lifecycle status
     * @param attemptNumber one-based attempt number, or zero for an empty case
     * @param appointmentStatus appointment status, if an attempt exists
     * @param startsAt scheduled collection time, if an attempt exists
     */
    public StudentAppointmentHistorySummary {
        claimReference = requireText(claimReference, "claimReference");
        status = Objects.requireNonNull(status, "status");
        appointmentStatus = Objects.requireNonNull(appointmentStatus, "appointmentStatus");
        startsAt = Objects.requireNonNull(startsAt, "startsAt");
        boolean hasAttempt = attemptNumber > 0;
        if (attemptNumber < 0 || appointmentStatus.isPresent() != hasAttempt
                || startsAt.isPresent() != hasAttempt) {
            throw new IllegalArgumentException("History attempt details are inconsistent.");
        }
    }

    /** Returns a concise label without displaying the full Claim UUID.
     * @return short approved-item label */
    public String displayLabel() {
        String reference = claimReference.substring(Math.max(0, claimReference.length() - 8));
        return "Approved item · " + reference;
    }

    @Override
    public String toString() {
        return "StudentAppointmentHistorySummary[redacted]";
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
