package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable identity for one appointment attempt.
 *
 * @param value wrapped UUID
 */
public record AppointmentId(UUID value) {
    /** Validates the wrapped identity. */
    public AppointmentId {
        value = Objects.requireNonNull(value, "value");
    }

    /**
     * Creates an appointment identity.
     *
     * @param value UUID value
     * @return appointment identity
     */
    public static AppointmentId of(UUID value) {
        return new AppointmentId(value);
    }

    /** Redacts the UUID in diagnostics. */
    @Override
    public String toString() {
        return "AppointmentId[redacted]";
    }
}
