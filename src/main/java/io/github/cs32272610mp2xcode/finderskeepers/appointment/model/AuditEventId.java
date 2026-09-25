package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable identity for one audit event.
 *
 * @param value wrapped UUID
 */
public record AuditEventId(UUID value) {
    /** Validates the wrapped identity. */
    public AuditEventId {
        value = Objects.requireNonNull(value, "value");
    }

    /**
     * Creates an audit-event identity.
     *
     * @param value UUID value
     * @return audit-event identity
     */
    public static AuditEventId of(UUID value) {
        return new AuditEventId(value);
    }

    /** Redacts the UUID in diagnostics. */
    @Override
    public String toString() {
        return "AuditEventId[redacted]";
    }
}
