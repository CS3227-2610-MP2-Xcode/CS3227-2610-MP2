package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;

/**
 * Immutable privacy-safe event in a collection-case audit trail.
 *
 * @param eventId event identity
 * @param eventType typed event kind
 * @param occurredAt event time
 * @param actorUserId authenticated actor identity
 * @param actorRole authenticated actor role
 * @param appointmentId optional appointment reference
 * @param sourceSlotId optional original slot reference
 * @param targetSlotId optional replacement slot reference
 */
public record AuditEvent(AuditEventId eventId, AuditEventType eventType,
        Instant occurredAt, String actorUserId, UserRole actorRole,
        Optional<AppointmentId> appointmentId, Optional<SlotId> sourceSlotId,
        Optional<SlotId> targetSlotId) {
    /** Validates the event and its optional references. */
    public AuditEvent {
        eventId = Objects.requireNonNull(eventId, "eventId");
        eventType = Objects.requireNonNull(eventType, "eventType");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        if (!occurredAt.equals(occurredAt.truncatedTo(ChronoUnit.MILLIS))) {
            throw new IllegalArgumentException("occurredAt must use millisecond precision.");
        }
        actorUserId = requireText(actorUserId, "actorUserId");
        actorRole = Objects.requireNonNull(actorRole, "actorRole");
        appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        sourceSlotId = Objects.requireNonNull(sourceSlotId, "sourceSlotId");
        targetSlotId = Objects.requireNonNull(targetSlotId, "targetSlotId");
    }

    /** Redacts actor and record values in diagnostics. */
    @Override
    public String toString() {
        return "AuditEvent[redacted]";
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
