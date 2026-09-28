package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence.AppointmentRepository.SlotOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppointmentSlotBoundaryTest {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private static final Instant NOW = local(2030, 1, 1, 8, 0);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void futureMidnightAndLastHalfHourPersistAtTheirExactSingaporeTimes() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        Instant midnight = local(2030, 1, 2, 0, 0);
        Instant lastHalfHour = local(2030, 1, 2, 23, 30);

        assertEquals(SlotOutcome.CREATED,
                repository.createSlot(slot(1, midnight)).outcome());
        assertEquals(SlotOutcome.CREATED,
                repository.createSlot(slot(2, lastHalfHour)).outcome());

        List<CollectionSlot> restored = new JsonAppointmentRepository(store).loadSlots();
        assertEquals(List.of(midnight, lastHalfHour), restored.stream()
                .map(CollectionSlot::startsAt).toList());
        assertTrue(restored.stream().allMatch(CollectionSlot::enabled));
        assertTrue(restored.stream().allMatch(value ->
                value.createdByOfficerId().equals("officer-a")));
    }

    @Test
    void currentPastAndOffGridTimesNeverReplaceValidStorage() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        assertEquals(SlotOutcome.CREATED,
                repository.createSlot(slot(1, local(2030, 1, 1, 9, 0))).outcome());
        byte[] original = Files.readAllBytes(store);
        List<Instant> invalid = List.of(
                NOW,
                NOW.minusMillis(1),
                local(2030, 1, 1, 8, 1),
                local(2030, 1, 1, 8, 29),
                local(2030, 1, 1, 8, 31),
                local(2030, 1, 1, 8, 30).plusSeconds(1),
                local(2030, 1, 1, 8, 30).plusMillis(1));

        for (int index = 0; index < invalid.size(); index++) {
            assertEquals(SlotOutcome.INVALID_TIME,
                    repository.createSlot(slot(index + 2, invalid.get(index))).outcome());
            assertTrue(java.util.Arrays.equals(original, Files.readAllBytes(store)));
        }
        assertThrows(IllegalArgumentException.class, () -> slot(20,
                local(2030, 1, 1, 8, 30).plusNanos(1)));
        assertEquals(1, new JsonAppointmentRepository(store).loadSlots().size());
    }

    @Test
    void exactDuplicateIsRejectedAdjacentSlotIsAllowedAndDisabledHistoryIsRetained()
            throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        Instant start = local(2030, 1, 1, 9, 0);
        SlotId firstId = SlotId.of(uuid(1));
        assertEquals(SlotOutcome.CREATED, repository.createSlot(slot(1, start)).outcome());
        byte[] original = Files.readAllBytes(store);

        assertEquals(SlotOutcome.OVERLAPPING,
                repository.createSlot(slot(2, start)).outcome());
        assertTrue(java.util.Arrays.equals(original, Files.readAllBytes(store)));
        assertEquals(SlotOutcome.CREATED, repository.createSlot(
                slot(3, start.plusSeconds(30 * 60))).outcome());
        assertEquals(SlotOutcome.CREATED,
                repository.disableSlot(firstId, "officer-b", NOW).outcome());
        assertEquals(SlotOutcome.CREATED,
                repository.createSlot(slot(4, start)).outcome());

        List<CollectionSlot> restored = new JsonAppointmentRepository(store).loadSlots();
        assertEquals(3, restored.size());
        assertFalse(restored.stream().filter(value -> value.slotId().equals(firstId))
                .findFirst().orElseThrow().enabled());
        assertEquals(1, restored.stream().filter(value -> value.startsAt().equals(start)
                && value.enabled()).count());
    }

    private static CollectionSlot slot(long id, Instant start) {
        return CollectionSlot.create(SlotId.of(uuid(id)), start, NOW, "officer-a");
    }

    private static UUID uuid(long value) {
        return new UUID(0, value);
    }

    private static Instant local(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(SINGAPORE).toInstant();
    }
}
