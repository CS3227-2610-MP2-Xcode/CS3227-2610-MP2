package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CustodyStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonAppointmentRepositoryTest {
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void createsBooksCancelsAndReloadsAppointment() throws Exception {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ClaimId claimId = ClaimId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        AppointmentId appointmentId = AppointmentId.of(
                UUID.fromString("00000000-0000-0000-0000-000000000003"));
        CollectionSlot slot = CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1");

        assertEquals(AppointmentRepository.SlotOutcome.CREATED,
                repository.createSlot(slot).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.book(claimId, "student-1", slotId, appointmentId, "student-1",
                        UserRole.STUDENT, NOW).outcome());
        assertEquals(AppointmentStatus.BOOKED,
                repository.loadCases().get(0).activeAppointment().orElseThrow().status());

        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.cancel(appointmentId, "student-1", "student-1", UserRole.STUDENT,
                        NOW.plusSeconds(1)).outcome());
        JsonAppointmentRepository reloaded = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        assertEquals(AppointmentStatus.CANCELLED,
                reloaded.loadCases().get(0).appointments().get(0).status());
        assertTrue(reloaded.loadCases().get(0).mayBookAgain());
    }

    @Test
    void competingBookingsForOneSlotAllowOnlyOne() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));

        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.book(ClaimId.of(UUID.randomUUID()), "student-1", slotId,
                        AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT,
                        NOW).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.SLOT_TAKEN,
                repository.book(ClaimId.of(UUID.randomUUID()), "student-2", slotId,
                        AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT,
                        NOW).outcome());
    }

    @Test
    void separateRepositoryInstancesSerializeCompetingBookings() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository first = new JsonAppointmentRepository(store);
        JsonAppointmentRepository second = new JsonAppointmentRepository(store);
        SlotId slotId = SlotId.of(UUID.randomUUID());
        first.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));

        var outcomes = runTogether(
                () -> first.book(ClaimId.of(UUID.randomUUID()), "student-1", slotId,
                        AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT,
                        NOW).outcome(),
                () -> second.book(ClaimId.of(UUID.randomUUID()), "student-2", slotId,
                        AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT,
                        NOW).outcome());

        assertEquals(1, outcomes.stream()
                .filter(outcome -> outcome == AppointmentRepository.BookingOutcome.BOOKED)
                .count());
        assertEquals(1, outcomes.stream()
                .filter(outcome -> outcome == AppointmentRepository.BookingOutcome.SLOT_TAKEN)
                .count());
        assertEquals(1, first.loadCases().size());
    }

    @Test
    void secondBookingCannotReadTheStoreWhileAnotherCommandIsWriting() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        SlotId slotId = SlotId.of(UUID.randomUUID());
        JsonAppointmentRepository setup = new JsonAppointmentRepository(store);
        setup.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        PausingAppointmentStoreFiles files = new PausingAppointmentStoreFiles();
        JsonAppointmentRepository first = new JsonAppointmentRepository(store, files,
                JsonAppointmentRepository.MAX_STORE_BYTES, UUID::randomUUID);
        JsonAppointmentRepository second = new JsonAppointmentRepository(store, files,
                JsonAppointmentRepository.MAX_STORE_BYTES, UUID::randomUUID);
        ClaimId firstClaim = ClaimId.of(UUID.randomUUID());
        ClaimId secondClaim = ClaimId.of(UUID.randomUUID());
        AppointmentId firstAppointment = AppointmentId.of(UUID.randomUUID());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var firstResult = executor.submit(() -> first.book(firstClaim, "student-1", slotId,
                    firstAppointment, "student-1", UserRole.STUDENT, NOW).outcome());
            assertTrue(files.firstWritePaused.await(5, TimeUnit.SECONDS));
            var secondResult = executor.submit(() -> second.book(secondClaim, "student-2", slotId,
                    AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT, NOW)
                    .outcome());

            boolean secondReadBeforeFirstWrite = files.secondRead.await(500, TimeUnit.MILLISECONDS);
            files.releaseFirstWrite.countDown();

            assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                    firstResult.get(5, TimeUnit.SECONDS));
            assertEquals(AppointmentRepository.BookingOutcome.SLOT_TAKEN,
                    secondResult.get(5, TimeUnit.SECONDS));
            assertFalse(secondReadBeforeFirstWrite);
            var durableAppointment = first.loadCases().stream()
                    .flatMap(caseState -> caseState.appointments().stream())
                    .filter(appointment -> appointment.status() == AppointmentStatus.BOOKED)
                    .findFirst().orElseThrow();
            assertEquals(firstAppointment, durableAppointment.appointmentId());
        } finally {
            files.releaseFirstWrite.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void separateProcessesSerializeCompetingBookings() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        SlotId slotId = SlotId.of(UUID.randomUUID());
        JsonAppointmentRepository setup = new JsonAppointmentRepository(store);
        setup.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));

        Path firstMarker = temporaryDirectory.resolve("first-ready");
        Path secondMarker = temporaryDirectory.resolve("second-ready");
        Path firstStarted = temporaryDirectory.resolve("first-started");
        Path secondStarted = temporaryDirectory.resolve("second-started");
        Path startSignal = temporaryDirectory.resolve("start-booking-workers");
        ClaimId firstClaim = ClaimId.of(UUID.randomUUID());
        ClaimId secondClaim = ClaimId.of(UUID.randomUUID());
        AppointmentId firstAppointment = AppointmentId.of(UUID.randomUUID());
        AppointmentId secondAppointment = AppointmentId.of(UUID.randomUUID());
        String classpath = codeLocation(JsonAppointmentRepository.class) + File.pathSeparator
                + codeLocation(JsonAppointmentRepositoryTest.class);

        Process first = startBookingWorker(classpath, store, slotId, firstClaim,
                firstAppointment, "student-1", firstMarker, secondMarker, firstStarted,
                startSignal);
        Process second = startBookingWorker(classpath, store, slotId, secondClaim,
                secondAppointment, "student-2", secondMarker, firstMarker, secondStarted,
                startSignal);
        try {
            assertTrue(waitForFile(firstStarted, 10, TimeUnit.SECONDS),
                    "first worker did not start");
            assertTrue(waitForFile(secondStarted, 10, TimeUnit.SECONDS),
                    "second worker did not start");
            Files.createFile(startSignal);
            assertTrue(first.waitFor(10, TimeUnit.SECONDS), "first worker timed out");
            assertTrue(second.waitFor(10, TimeUnit.SECONDS), "second worker timed out");
            String firstOutput = new String(first.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8).trim();
            String secondOutput = new String(second.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8).trim();

            assertEquals(0, first.exitValue(), firstOutput);
            assertEquals(0, second.exitValue(), secondOutput);
            assertEquals(1, java.util.List.of(firstOutput, secondOutput).stream()
                    .filter(AppointmentRepository.BookingOutcome.BOOKED.name()::equals)
                    .count(), "workers returned: " + firstOutput + ", " + secondOutput);
            assertEquals(1, java.util.List.of(firstOutput, secondOutput).stream()
                    .filter(AppointmentRepository.BookingOutcome.SLOT_TAKEN.name()::equals)
                    .count(), "workers returned: " + firstOutput + ", " + secondOutput);

            var durableAppointment = setup.loadCases().stream()
                    .flatMap(caseState -> caseState.appointments().stream())
                    .filter(appointment -> appointment.status() == AppointmentStatus.BOOKED)
                    .findFirst().orElseThrow();
            AppointmentId winningId = firstOutput.equals(
                    AppointmentRepository.BookingOutcome.BOOKED.name())
                    ? firstAppointment : secondAppointment;
            assertEquals(winningId, durableAppointment.appointmentId());
        } finally {
            first.destroyForcibly();
            second.destroyForcibly();
        }
    }

    @Test
    void separateRepositoryInstancesPreserveUnrelatedConcurrentUpdates() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository first = new JsonAppointmentRepository(store);
        JsonAppointmentRepository second = new JsonAppointmentRepository(store);
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        first.createSlot(CollectionSlot.create(firstSlot,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        first.createSlot(CollectionSlot.create(secondSlot,
                Instant.parse("2030-01-01T03:00:00Z"), NOW, "officer-1"));

        var outcomes = runTogether(
                () -> first.book(ClaimId.of(UUID.randomUUID()), "student-1", firstSlot,
                        AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT,
                        NOW).outcome(),
                () -> second.book(ClaimId.of(UUID.randomUUID()), "student-2", secondSlot,
                        AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT,
                        NOW).outcome());

        assertTrue(outcomes.stream()
                .allMatch(outcome -> outcome == AppointmentRepository.BookingOutcome.BOOKED));
        assertEquals(2, first.loadCases().size());
    }

    @Test
    void unsafeLockPathFailsClosedAndPreservesStoreBytes() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId slotId = SlotId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        byte[] before = Files.readAllBytes(store);
        Path lockFile = temporaryDirectory.resolve("appointments.json.lock");
        Files.delete(lockFile);
        Files.createDirectory(lockFile);

        AppointmentStoreException failure = org.junit.jupiter.api.Assertions.assertThrows(
                AppointmentStoreException.class, () -> repository.book(
                        ClaimId.of(UUID.randomUUID()), "student-1", slotId,
                        AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT,
                        NOW));

        assertEquals(AppointmentStoreException.Reason.LOCK_FAILURE, failure.reason());
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(store)));
    }

    @Test
    void custodyCollectionAndClosureFollowTheRequiredSequence() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(slotId,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        repository.book(claimId, "student-1", slotId, appointmentId, "student-1",
                UserRole.STUDENT, NOW);

        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.recordStorageLocation(claimId, "officer-1", "Locker A1",
                        NOW.plusSeconds(1)).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.markReadyForCollection(claimId, "officer-1", NOW.plusSeconds(2))
                        .outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.TOO_EARLY,
                repository.confirmCollection(appointmentId, "officer-1", NOW.plusSeconds(3))
                        .outcome());
        Instant duringSlot = Instant.parse("2030-01-01T02:05:00Z");
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.confirmCollection(appointmentId, "officer-1", duringSlot).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.markReturned(claimId, "officer-1", duringSlot.plusSeconds(1))
                        .outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.closeCase(claimId, "officer-1", duringSlot.plusSeconds(2)).outcome());

        var result = repository.loadCases().get(0);
        assertEquals(CaseStatus.CLOSED, result.status());
        assertEquals(CustodyStatus.RETURNED, result.custodyStatus());
        assertEquals(6, result.auditEvents().size());
    }

    @Test
    void correctingStorageAfterCollectionPreservesReadyCustodyAndAllowsReturn()
            throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        Instant start = Instant.parse("2030-01-01T02:00:00Z");
        repository.createSlot(CollectionSlot.create(slotId, start, NOW, "officer-1"));
        repository.book(claimId, "student-1", slotId, appointmentId, "student-1",
                UserRole.STUDENT, NOW);
        repository.recordStorageLocation(claimId, "officer-1", "Locker A1", NOW.plusSeconds(1));
        repository.markReadyForCollection(claimId, "officer-1", NOW.plusSeconds(2));
        repository.confirmCollection(appointmentId, "officer-1", start.plusSeconds(60));

        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.recordStorageLocation(claimId, "officer-1", "Locker B2",
                        start.plusSeconds(61)).outcome());
        var corrected = repository.loadCases().getFirst();
        assertEquals(CustodyStatus.READY_FOR_COLLECTION, corrected.custodyStatus());
        assertEquals(java.util.Optional.of("Locker B2"), corrected.storageLocation());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.markReturned(claimId, "officer-1", start.plusSeconds(62)).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.closeCase(claimId, "officer-1", start.plusSeconds(63)).outcome());
    }

    @Test
    void noShowReleasesClaimForAnotherBooking() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId firstAppointment = AppointmentId.of(UUID.randomUUID());
        AppointmentId secondAppointment = AppointmentId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(firstSlot,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot,
                Instant.parse("2030-01-01T03:00:00Z"), NOW, "officer-1"));
        repository.book(claimId, "student-1", firstSlot, firstAppointment, "student-1",
                UserRole.STUDENT, NOW);
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.recordNoShow(firstAppointment, "officer-1",
                        Instant.parse("2030-01-01T02:30:00Z")).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.book(claimId, "student-1", secondSlot, secondAppointment, "student-1",
                        UserRole.STUDENT, NOW).outcome());
    }

    @Test
    void collectionConfirmationPreventsAnotherBookingForTheClaim()
            throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        Instant firstStart = Instant.parse("2030-01-01T02:00:00Z");
        repository.createSlot(CollectionSlot.create(firstSlot, firstStart, NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot,
                Instant.parse("2030-01-01T03:00:00Z"), NOW, "officer-1"));
        repository.book(claimId, "student-1", firstSlot, appointmentId, "student-1",
                UserRole.STUDENT, NOW);
        repository.recordStorageLocation(claimId, "officer-1", "Locker A1", NOW.plusSeconds(1));
        repository.markReadyForCollection(claimId, "officer-1", NOW.plusSeconds(2));

        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.confirmCollection(appointmentId, "officer-1",
                        firstStart.plusSeconds(1)).outcome());
        var caseState = repository.loadCases().get(0);
        assertFalse(caseState.mayBookAgain());
        assertEquals(AppointmentRepository.BookingOutcome.CLAIM_ALREADY_ACTIVE,
                repository.book(claimId, "student-1", secondSlot,
                        AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT,
                        NOW).outcome());
    }

    @Test
    void rescheduleAndDisableRejectConflictsAndPastChanges() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        Instant firstStart = Instant.parse("2030-01-01T02:00:00Z");
        Instant secondStart = Instant.parse("2030-01-01T03:00:00Z");
        repository.createSlot(CollectionSlot.create(firstSlot, firstStart, NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot, secondStart, NOW, "officer-1"));
        repository.book(claimId, "student-1", firstSlot, appointmentId, "student-1",
                UserRole.STUDENT, NOW);

        assertEquals(AppointmentRepository.BookingOutcome.BOOKED,
                repository.reschedule(appointmentId, "student-1", secondSlot, "student-1",
                        UserRole.STUDENT, NOW.plusSeconds(1)).outcome());
        assertEquals(AppointmentRepository.SlotOutcome.BOOKED,
                repository.disableSlot(secondSlot, "officer-1", NOW.plusSeconds(2)).outcome());
        assertEquals(AppointmentRepository.BookingOutcome.TOO_LATE,
                repository.reschedule(appointmentId, "student-1", firstSlot, "student-1",
                        UserRole.STUDENT, secondStart.plusSeconds(1)).outcome());
    }

    @Test
    void repeatedCollectionAndInvalidClosureAreRejected() throws AppointmentStoreException {
        JsonAppointmentRepository repository = new JsonAppointmentRepository(
                temporaryDirectory.resolve("appointments.json"));
        SlotId slotId = SlotId.of(UUID.randomUUID());
        ClaimId claimId = ClaimId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        Instant start = Instant.parse("2030-01-01T02:00:00Z");
        repository.createSlot(CollectionSlot.create(slotId, start, NOW, "officer-1"));
        repository.book(claimId, "student-1", slotId, appointmentId, "student-1",
                UserRole.STUDENT, NOW);

        assertEquals(AppointmentRepository.CaseOutcome.INVALID_CUSTODY,
                repository.closeCase(claimId, "officer-1", NOW.plusSeconds(1)).outcome());
        repository.recordStorageLocation(claimId, "officer-1", "Locker A1", NOW.plusSeconds(2));
        repository.markReadyForCollection(claimId, "officer-1", NOW.plusSeconds(3));
        Instant duringSlot = start.plusSeconds(60);
        assertEquals(AppointmentRepository.AppointmentOutcome.CHANGED,
                repository.confirmCollection(appointmentId, "officer-1", duringSlot).outcome());
        assertEquals(AppointmentRepository.AppointmentOutcome.ALREADY_TERMINAL,
                repository.confirmCollection(appointmentId, "officer-1", duringSlot)
                        .outcome());
        repository.markReturned(claimId, "officer-1", duringSlot.plusSeconds(1));
        assertEquals(AppointmentRepository.CaseOutcome.CHANGED,
                repository.closeCase(claimId, "officer-1", duringSlot.plusSeconds(2)).outcome());
        assertEquals(AppointmentRepository.CaseOutcome.ALREADY_CLOSED,
                repository.closeCase(claimId, "officer-1", duringSlot.plusSeconds(3)).outcome());
    }

    private static <T> java.util.List<T> runTogether(Callable<T> first, Callable<T> second)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var firstResult = executor.submit(() -> {
                ready.countDown();
                start.await();
                return first.call();
            });
            var secondResult = executor.submit(() -> {
                ready.countDown();
                start.await();
                return second.call();
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return java.util.List.of(firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static Process startBookingWorker(String classpath, Path store, SlotId slotId,
            ClaimId claimId, AppointmentId appointmentId, String student, Path marker,
            Path peerMarker, Path startedMarker, Path startSignal) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows")
                ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        return new ProcessBuilder(java.toString(), "-cp", classpath,
                AppointmentBookingProcessWorker.class.getName(), store.toString(),
                slotId.value().toString(), claimId.value().toString(),
                appointmentId.value().toString(), student, marker.toString(),
                peerMarker.toString(), startedMarker.toString(), startSignal.toString())
                .redirectErrorStream(true)
                .start();
    }

    private static boolean waitForFile(Path target, long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (!Files.exists(target) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return Files.exists(target);
    }

    private static String codeLocation(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toString();
    }

    private static final class PausingAppointmentStoreFiles implements AppointmentStoreFiles {
        private final NioAppointmentStoreFiles delegate = new NioAppointmentStoreFiles();

        private final AtomicBoolean pauseFirstWrite = new AtomicBoolean(true);

        private final AtomicReference<Thread> firstWriter = new AtomicReference<>();

        private final CountDownLatch firstWritePaused = new CountDownLatch(1);

        private final CountDownLatch releaseFirstWrite = new CountDownLatch(1);

        private final CountDownLatch secondRead = new CountDownLatch(1);

        @Override
        public java.util.Optional<byte[]> readBounded(Path target, int maximumBytes)
                throws StoreFileFailure {
            Thread writer = firstWriter.get();
            if (writer != null && Thread.currentThread() != writer) {
                secondRead.countDown();
            }
            return delegate.readBounded(target, maximumBytes);
        }

        @Override
        public void replaceAtomically(Path target, byte[] completeDocument)
                throws StoreFileFailure {
            if (pauseFirstWrite.compareAndSet(true, false)) {
                firstWriter.set(Thread.currentThread());
                firstWritePaused.countDown();
                try {
                    if (!releaseFirstWrite.await(5, TimeUnit.SECONDS)) {
                        throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
                }
            }
            delegate.replaceAtomically(target, completeDocument);
        }
    }

    @Test
    void corruptStoreFailsClosedAndPreservesBytes() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        byte[] corrupt = "{not-json".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(store, corrupt);
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);

        AppointmentStoreException failure = org.junit.jupiter.api.Assertions.assertThrows(
                AppointmentStoreException.class, repository::loadCases);
        assertEquals(AppointmentStoreException.Reason.CORRUPT_STORE, failure.reason());
        assertTrue(java.util.Arrays.equals(corrupt, Files.readAllBytes(store)));
    }

    @Test
    void duplicateActiveSlotOccupancyFailsClosedAndPreservesBytes() throws Exception {
        Path store = temporaryDirectory.resolve("appointments.json");
        JsonAppointmentRepository repository = new JsonAppointmentRepository(store);
        SlotId firstSlot = SlotId.of(UUID.randomUUID());
        SlotId secondSlot = SlotId.of(UUID.randomUUID());
        repository.createSlot(CollectionSlot.create(firstSlot,
                Instant.parse("2030-01-01T02:00:00Z"), NOW, "officer-1"));
        repository.createSlot(CollectionSlot.create(secondSlot,
                Instant.parse("2030-01-01T03:00:00Z"), NOW, "officer-1"));
        repository.book(ClaimId.of(UUID.randomUUID()), "student-1", firstSlot,
                AppointmentId.of(UUID.randomUUID()), "student-1", UserRole.STUDENT, NOW);
        repository.book(ClaimId.of(UUID.randomUUID()), "student-2", secondSlot,
                AppointmentId.of(UUID.randomUUID()), "student-2", UserRole.STUDENT, NOW);

        String validDocument = Files.readString(store, java.nio.charset.StandardCharsets.UTF_8);
        String secondSlotField = "\"slotId\": \"" + secondSlot.value() + "\"";
        int appointmentSlotReference = validDocument.lastIndexOf(secondSlotField);
        assertTrue(appointmentSlotReference >= 0);
        String inconsistentDocument = validDocument.substring(0, appointmentSlotReference)
                + "\"slotId\": \"" + firstSlot.value() + "\""
                + validDocument.substring(appointmentSlotReference + secondSlotField.length());
        byte[] corrupt = inconsistentDocument.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(store, corrupt);

        JsonAppointmentRepository reloaded = new JsonAppointmentRepository(store);
        AppointmentStoreException failure = org.junit.jupiter.api.Assertions.assertThrows(
                AppointmentStoreException.class, reloaded::loadCases);
        assertEquals(AppointmentStoreException.Reason.CORRUPT_STORE, failure.reason());
        assertTrue(java.util.Arrays.equals(corrupt, Files.readAllBytes(store)));
    }
}
