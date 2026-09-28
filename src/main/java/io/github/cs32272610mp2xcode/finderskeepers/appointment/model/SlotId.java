package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable identity for one collection slot.
 *
 * @param value wrapped UUID
 */
public record SlotId(UUID value) {
    /** Validates the wrapped identity. */
    public SlotId {
        value = Objects.requireNonNull(value, "value");
    }

    /**
     * Creates a slot identity.
     *
     * @param value UUID value
     * @return slot identity
     */
    public static SlotId of(UUID value) {
        return new SlotId(value);
    }

    /** Redacts the UUID in diagnostics. */
    @Override
    public String toString() {
        return "SlotId[redacted]";
    }
}
