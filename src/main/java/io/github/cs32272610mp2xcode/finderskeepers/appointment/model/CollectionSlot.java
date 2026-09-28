package io.github.cs32272610mp2xcode.finderskeepers.appointment.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/** Immutable half-hour collection slot managed by a Desk Officer. */
public final class CollectionSlot {
    private final SlotId slotId;

    private final Instant startsAt;

    private final boolean enabled;

    private final Instant createdAt;

    private final String createdByOfficerId;

    private final Optional<Instant> disabledAt;

    private final Optional<String> disabledByOfficerId;

    private CollectionSlot(SlotId id, Instant start, boolean isEnabled,
            Instant creationTime, String creator, Optional<Instant> disabledTime,
            Optional<String> disabler) {
        slotId = Objects.requireNonNull(id, "slotId");
        startsAt = requireMillis(start, "startsAt");
        createdAt = requireMillis(creationTime, "createdAt");
        createdByOfficerId = requireText(creator, "createdByOfficerId");
        disabledAt = Objects.requireNonNull(disabledTime, "disabledAt");
        disabledByOfficerId = Objects.requireNonNull(disabler, "disabledByOfficerId");
        enabled = isEnabled;
        if (enabled && (disabledAt.isPresent() || disabledByOfficerId.isPresent())) {
            throw new IllegalArgumentException("Enabled slots cannot have disable data.");
        }
        if (!enabled && (disabledAt.isEmpty() || disabledByOfficerId.isEmpty())) {
            throw new IllegalArgumentException("Disabled slots require disable data.");
        }
        disabledAt.ifPresent(time -> {
            requireMillis(time, "disabledAt");
            if (time.isBefore(createdAt)) {
                throw new IllegalArgumentException("A slot cannot be disabled before creation.");
            }
        });
    }

    /** Creates an enabled slot after validating the schedule timestamp.
     * @param id slot identity
     *  @param start slot start instant
     *  @param creationTime creation timestamp
     *  @param officerId creating officer
     *  @return enabled slot */
    public static CollectionSlot create(SlotId id, Instant start, Instant creationTime,
            String officerId) {
        return new CollectionSlot(id, start, true, creationTime, officerId,
                Optional.empty(), Optional.empty());
    }

    /** Restores a validated persisted slot.
     * @param id slot identity
     *  @param start slot start instant
     *  @param enabled whether the slot is enabled
     *  @param creationTime creation timestamp
     *  @param creator creating officer
     *  @param disabledTime disable timestamp
     *  @param disabler disabling officer
     *  @return restored slot */
    public static CollectionSlot restore(SlotId id, Instant start, boolean enabled,
            Instant creationTime, String creator, Optional<Instant> disabledTime,
            Optional<String> disabler) {
        return new CollectionSlot(id, start, enabled, creationTime, creator,
                disabledTime, disabler);
    }

    /** Returns a disabled copy of this slot.
     * @param time disable timestamp
     *  @param officerId disabling officer
     *  @return disabled slot */
    public CollectionSlot disable(Instant time, String officerId) {
        if (!enabled) {
            throw new IllegalStateException("The slot is already disabled.");
        }
        return new CollectionSlot(slotId, startsAt, false, createdAt, createdByOfficerId,
                Optional.of(requireMillis(time, "disabledAt")),
                Optional.of(requireText(officerId, "disabledByOfficerId")));
    }

    /** Returns the slot identity.
     * @return slot identity */
    public SlotId slotId() { return slotId; }

    /** Returns the local schedule start represented as an instant.
     * @return start instant */
    public Instant startsAt() { return startsAt; }

    /** Returns the derived exclusive end instant.
     * @return exclusive end instant */
    public Instant endsAt() { return startsAt.plus(30, ChronoUnit.MINUTES); }

    /** Returns whether the slot can accept a new booking.
     * @return whether enabled */
    public boolean enabled() { return enabled; }

    /** Returns when the slot was created.
     * @return creation timestamp */
    public Instant createdAt() { return createdAt; }

    /** Returns the creating officer identity.
     * @return officer identity */
    public String createdByOfficerId() { return createdByOfficerId; }

    /** Returns the disable time, if disabled.
     * @return optional disable timestamp */
    public Optional<Instant> disabledAt() { return disabledAt; }

    /** Returns the disabling officer, if disabled.
     * @return optional officer identity */
    public Optional<String> disabledByOfficerId() { return disabledByOfficerId; }

    @Override
    public String toString() {
        return "CollectionSlot[redacted]";
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
