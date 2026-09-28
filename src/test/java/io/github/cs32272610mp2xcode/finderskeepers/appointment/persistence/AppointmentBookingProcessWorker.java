package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Child-JVM entry point for the cross-process appointment booking regression. */
public final class AppointmentBookingProcessWorker {
    private static final Instant BOOKING_TIME = Instant.parse("2030-01-01T00:00:00Z");

    private AppointmentBookingProcessWorker() {
    }

    /** Runs one coordinated booking attempt and prints its outcome to standard output.
     * @param args store, slot, claim, appointment, student, read marker, peer marker,
     *  startup marker, and shared start signal */
    public static void main(String[] args) {
        try {
            Path store = Path.of(args[0]);
            SlotId slotId = SlotId.of(UUID.fromString(args[1]));
            ClaimId claimId = ClaimId.of(UUID.fromString(args[2]));
            AppointmentId appointmentId = AppointmentId.of(UUID.fromString(args[3]));
            String student = args[4];
            Path readMarker = Path.of(args[5]);
            Path peerReadMarker = Path.of(args[6]);
            Path startedMarker = Path.of(args[7]);
            Path startSignal = Path.of(args[8]);

            Files.writeString(startedMarker, "started", CREATE_NEW, WRITE);
            waitFor(startSignal, 10, TimeUnit.SECONDS);
            AppointmentStoreFiles files = new CoordinatedStoreFiles(readMarker,
                    peerReadMarker);
            JsonAppointmentRepository repository = new JsonAppointmentRepository(store, files,
                    JsonAppointmentRepository.MAX_STORE_BYTES, UUID::randomUUID);
            AppointmentRepository.BookingResult result = repository.book(claimId, student,
                    slotId, appointmentId, student, UserRole.STUDENT, BOOKING_TIME);
            System.out.println(result.outcome().name());
        } catch (IOException | AppointmentStoreException | IllegalArgumentException
                | InterruptedException failure) {
            failure.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static void waitFor(Path target, long timeout, TimeUnit unit)
            throws InterruptedException, IOException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (!Files.exists(target) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        if (!Files.exists(target)) {
            throw new IOException("Timed out waiting for test start signal.");
        }
    }

    private static final class CoordinatedStoreFiles implements AppointmentStoreFiles {
        private static final long PEER_READ_WAIT_MILLIS = 2_000;

        private final NioAppointmentStoreFiles delegate = new NioAppointmentStoreFiles();

        private final Path ownReadMarker;

        private final Path peerReadMarker;

        CoordinatedStoreFiles(Path ownMarker, Path peerMarker) {
            ownReadMarker = ownMarker;
            peerReadMarker = peerMarker;
        }

        @Override
        public Optional<byte[]> readBounded(Path target, int maximumBytes)
                throws StoreFileFailure {
            Optional<byte[]> snapshot = delegate.readBounded(target, maximumBytes);
            try {
                Files.writeString(ownReadMarker, "read", CREATE_NEW, WRITE);
                long deadline = System.nanoTime()
                        + TimeUnit.MILLISECONDS.toNanos(PEER_READ_WAIT_MILLIS);
                while (!Files.exists(peerReadMarker) && System.nanoTime() < deadline) {
                    Thread.sleep(10);
                }
            } catch (IOException failure) {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
            return snapshot;
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument)
                throws StoreFileFailure {
            delegate.replaceAtomically(target, completeDocument);
        }
    }
}
