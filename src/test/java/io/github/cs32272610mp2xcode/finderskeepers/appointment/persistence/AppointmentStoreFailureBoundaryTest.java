package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppointmentStoreFailureBoundaryTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void readAccessAndSizeFailuresAreClassifiedWithoutAttemptingAWrite() {
        FailingFiles inaccessible = new FailingFiles(StoreFileFailure.Kind.ACCESS, false);
        JsonAppointmentRepository repository = repository(inaccessible,
                JsonAppointmentRepository.MAX_STORE_BYTES);
        AppointmentStoreException readFailure = assertThrows(
                AppointmentStoreException.class, repository::loadSlots);
        assertEquals(AppointmentStoreException.Reason.READ_FAILURE,
                readFailure.reason());
        assertEquals(0, inaccessible.writes);

        FailingFiles oversized = new FailingFiles(StoreFileFailure.Kind.OVER_LIMIT, false);
        JsonAppointmentRepository tooLarge = repository(oversized,
                JsonAppointmentRepository.MAX_STORE_BYTES);
        AppointmentStoreException sizeFailure = assertThrows(
                AppointmentStoreException.class, tooLarge::loadCases);
        assertEquals(AppointmentStoreException.Reason.CORRUPT_STORE,
                sizeFailure.reason());
        assertEquals(0, oversized.writes);
    }

    @Test
    void failedAtomicReplacementAndOversizedCandidateDoNotReportCreatedSlot()
            throws Exception {
        CollectionSlot slot = CollectionSlot.create(SlotId.of(new UUID(0, 1)),
                NOW.plusSeconds(3600), NOW, "officer-a");
        FailingFiles unwritable = new FailingFiles(null, true);
        JsonAppointmentRepository repository = repository(unwritable,
                JsonAppointmentRepository.MAX_STORE_BYTES);
        AppointmentStoreException writeFailure = assertThrows(
                AppointmentStoreException.class, () -> repository.createSlot(slot));
        assertEquals(AppointmentStoreException.Reason.WRITE_FAILURE,
                writeFailure.reason());
        assertEquals(1, unwritable.writes);
        assertEquals(0, repository.loadSlots().size());

        FailingFiles smallFiles = new FailingFiles(null, false);
        JsonAppointmentRepository bounded = repository(smallFiles, 40);
        AppointmentStoreException sizeFailure = assertThrows(
                AppointmentStoreException.class, () -> bounded.createSlot(slot));
        assertEquals(AppointmentStoreException.Reason.RESULT_TOO_LARGE,
                sizeFailure.reason());
        assertEquals(0, smallFiles.writes);
        assertEquals(0, bounded.loadSlots().size());
    }

    private JsonAppointmentRepository repository(AppointmentStoreFiles files,
            int maximumBytes) {
        return new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"), files,
                maximumBytes, UUID::randomUUID);
    }

    private static final class FailingFiles implements AppointmentStoreFiles {
        private final StoreFileFailure.Kind readFailure;

        private final boolean writeFailure;

        private int writes;

        FailingFiles(StoreFileFailure.Kind requestedReadFailure, boolean failWrites) {
            readFailure = requestedReadFailure;
            writeFailure = failWrites;
        }

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes)
                throws StoreFileFailure {
            if (readFailure != null) {
                throw new StoreFileFailure(readFailure);
            }
            return Optional.empty();
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument)
                throws StoreFileFailure {
            writes++;
            if (writeFailure) {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
        }
    }
}
