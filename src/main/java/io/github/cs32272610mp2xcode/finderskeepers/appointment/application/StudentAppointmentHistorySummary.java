package io.github.cs32272610mp2xcode.finderskeepers.appointment.application;

import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;

/**
 * Student-safe summary of a collection case for appointment history.
 *
 * @param claimReference user-visible Claim reference
 * @param status case lifecycle status
 * @param latestAppointmentStatus latest Student-visible appointment status
 */
public record StudentAppointmentHistorySummary(String claimReference, CaseStatus status,
        Optional<AppointmentStatus> latestAppointmentStatus) {
    /** Validates the values displayed in Student appointment history.
     * @param claimReference user-visible Claim reference
     * @param status case lifecycle status
     * @param latestAppointmentStatus latest appointment status, if available
     */
    public StudentAppointmentHistorySummary {
        claimReference = requireText(claimReference, "claimReference");
        status = Objects.requireNonNull(status, "status");
        latestAppointmentStatus = Objects.requireNonNull(latestAppointmentStatus,
                "latestAppointmentStatus");
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
