package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.AppointmentDiagnostics;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEvent;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEventId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEventType;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionAppointment;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CustodyStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Synchronized, atomic JSON repository for appointment state. */
public final class JsonAppointmentRepository implements AppointmentRepository {
    /** Maximum accepted/generated document size. */
    public static final int MAX_STORE_BYTES = 16_777_216;

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Singapore");

    private final Path storePath;

    private final AppointmentStoreFiles storeFiles;

    private final int maximumStoreBytes;

    private final Supplier<UUID> ids;

    private final AppointmentStoreJsonCodec codec;

    /** Creates a repository with production filesystem and UUID behavior.
     * @param path appointment JSON path */
    public JsonAppointmentRepository(Path path) {
        this(path, new NioAppointmentStoreFiles(), MAX_STORE_BYTES, UUID::randomUUID);
    }

    /** Creates a repository with an injected UUID supplier for deterministic tests.
     * @param path appointment JSON path
     *  @param idSupplier UUID source */
    public JsonAppointmentRepository(Path path, Supplier<UUID> idSupplier) {
        this(path, new NioAppointmentStoreFiles(), MAX_STORE_BYTES, idSupplier);
    }

    JsonAppointmentRepository(Path path, AppointmentStoreFiles files, int maximumBytes,
            Supplier<UUID> idSupplier) {
        storePath = Objects.requireNonNull(path, "storePath").toAbsolutePath().normalize();
        storeFiles = Objects.requireNonNull(files, "storeFiles");
        if (maximumBytes <= 0 || maximumBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Maximum store size must support a probe.");
        }
        maximumStoreBytes = maximumBytes;
        ids = Objects.requireNonNull(idSupplier, "idSupplier");
        codec = new AppointmentStoreJsonCodec();
    }

    @Override
    public synchronized List<CollectionSlot> loadSlots() throws AppointmentStoreException {
        return List.copyOf(readCurrent().slots());
    }

    @Override
    public synchronized List<CollectionCase> loadCases() throws AppointmentStoreException {
        return List.copyOf(readCurrent().cases());
    }

    @Override
    public synchronized SlotResult createSlot(CollectionSlot slot)
            throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            Objects.requireNonNull(slot, "slot");
            if (!isValidStart(slot.startsAt(), slot.createdAt())) {
                return new SlotResult(SlotOutcome.INVALID_TIME, Optional.empty());
            }
            StoreState state = readCurrent();
            if (state.slots().stream().anyMatch(existing ->
                    existing.slotId().equals(slot.slotId()))) {
                return new SlotResult(SlotOutcome.ID_COLLISION, Optional.empty());
            }
            if (state.slots().stream().anyMatch(existing -> existing.enabled()
                    && overlaps(existing, slot))) {
                return new SlotResult(SlotOutcome.OVERLAPPING, Optional.empty());
            }
            List<CollectionSlot> slots = new ArrayList<>(state.slots());
            slots.add(slot);
            writeCandidate(slots, state.cases());
            return new SlotResult(SlotOutcome.CREATED, Optional.of(slot));
        }
    }

    @Override
    public synchronized SlotResult disableSlot(SlotId slotId, String officerId, Instant time)
            throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            Objects.requireNonNull(slotId, "slotId");
            requireText(officerId, "officerId");
            StoreState state = readCurrent();
            int index = findSlotIndex(state.slots(), slotId);
            if (index < 0) {
                return new SlotResult(SlotOutcome.NOT_FOUND, Optional.empty());
            }
            CollectionSlot current = state.slots().get(index);
            if (!current.enabled()) {
                return new SlotResult(SlotOutcome.ALREADY_DISABLED, Optional.of(current));
            }
            if (state.cases().stream().flatMap(caseState -> caseState.appointments().stream())
                    .anyMatch(appointment -> appointment.status() == AppointmentStatus.BOOKED
                            && appointment.slotId().equals(slotId))) {
                return new SlotResult(SlotOutcome.BOOKED, Optional.of(current));
            }
            if (!current.startsAt().isAfter(time)) {
                return new SlotResult(SlotOutcome.INVALID_TIME, Optional.of(current));
            }
            List<CollectionSlot> slots = new ArrayList<>(state.slots());
            CollectionSlot replacement = current.disable(time, officerId);
            slots.set(index, replacement);
            writeCandidate(slots, state.cases());
            return new SlotResult(SlotOutcome.CREATED, Optional.of(replacement));
        }
    }

    @Override
    public synchronized BookingResult book(ClaimId claimId, String studentUserId,
            SlotId slotId, AppointmentId appointmentId, String actorUserId,
            UserRole actorRole, Instant time) throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireRole(actorRole, UserRole.STUDENT);
            requireText(studentUserId, "studentUserId");
            requireText(actorUserId, "actorUserId");
            StoreState state = readCurrent();
            if (containsAppointment(state.cases(), appointmentId)) {
                return new BookingResult(BookingOutcome.ID_COLLISION, Optional.empty());
            }
            Optional<CollectionSlot> target = findSlot(state.slots(), slotId);
            if (target.isEmpty()) {
                return new BookingResult(BookingOutcome.SLOT_NOT_FOUND, Optional.empty());
            }
            if (!target.orElseThrow().enabled()) {
                return new BookingResult(BookingOutcome.SLOT_DISABLED, Optional.empty());
            }
            if (!target.orElseThrow().startsAt().isAfter(time)) {
                return new BookingResult(BookingOutcome.INVALID_TIME, Optional.empty());
            }
            if (hasActiveSlot(state.cases(), slotId)) {
                return new BookingResult(BookingOutcome.SLOT_TAKEN, Optional.empty());
            }
            Optional<CollectionCase> existing = findCase(state.cases(), claimId);
            if (existing.isPresent()) {
                CollectionCase caseState = existing.orElseThrow();
                if (!caseState.studentUserId().equals(studentUserId)) {
                    return new BookingResult(BookingOutcome.NOT_AUTHORIZED, Optional.empty());
                }
                if (caseState.status() == CaseStatus.CLOSED) {
                    return new BookingResult(BookingOutcome.CASE_CLOSED, Optional.of(caseState));
                }
                if (!caseState.mayBookAgain()) {
                    return new BookingResult(BookingOutcome.CLAIM_ALREADY_ACTIVE,
                            Optional.of(caseState));
                }
            }
            CollectionAppointment appointment = CollectionAppointment.book(appointmentId, claimId,
                    studentUserId, slotId, time);
            CollectionCase updated = existing.orElseGet(() -> CollectionCase.open(claimId,
                    studentUserId)).withAppointment(appointment).withAuditEvent(event(
                            AuditEventType.APPOINTMENT_BOOKED, actorUserId, actorRole, time,
                            Optional.of(appointmentId), Optional.empty(), Optional.of(slotId)));
            List<CollectionCase> cases = replaceCase(state.cases(), existing, updated);
            writeCandidate(state.slots(), cases);
            return new BookingResult(BookingOutcome.BOOKED, Optional.of(updated));
        }
    }

    @Override
    public synchronized BookingResult reschedule(AppointmentId appointmentId,
            String studentUserId, SlotId replacementSlotId, String actorUserId,
            UserRole actorRole, Instant time) throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireRole(actorRole, UserRole.STUDENT);
            requireText(studentUserId, "studentUserId");
            requireText(actorUserId, "actorUserId");
            StoreState state = readCurrent();
            CaseAndAppointment found = findAppointment(state.cases(), appointmentId);
            if (found == null) {
                return new BookingResult(BookingOutcome.NOT_FOUND, Optional.empty());
            }
            if (!found.appointment().studentUserId().equals(studentUserId)) {
                return new BookingResult(BookingOutcome.NOT_AUTHORIZED, Optional.empty());
            }
            if (found.appointment().status() != AppointmentStatus.BOOKED) {
                return new BookingResult(BookingOutcome.NOT_BOOKED, Optional.of(found.caseState()));
            }
            CollectionSlot oldSlot = findSlot(state.slots(), found.appointment().slotId())
                    .orElseThrow();
            if (!oldSlot.startsAt().isAfter(time)) {
                return new BookingResult(BookingOutcome.TOO_LATE, Optional.of(found.caseState()));
            }
            Optional<CollectionSlot> replacement = findSlot(state.slots(), replacementSlotId);
            if (replacement.isEmpty()) {
                return new BookingResult(BookingOutcome.SLOT_NOT_FOUND, Optional.empty());
            }
            if (!replacement.orElseThrow().enabled()) {
                return new BookingResult(BookingOutcome.SLOT_DISABLED, Optional.empty());
            }
            if (!replacement.orElseThrow().startsAt().isAfter(time)) {
                return new BookingResult(BookingOutcome.INVALID_TIME, Optional.empty());
            }
            if (hasActiveSlotExcept(state.cases(), replacementSlotId, appointmentId)) {
                return new BookingResult(BookingOutcome.SLOT_TAKEN, Optional.empty());
            }
            CollectionAppointment updatedAppointment = found.appointment().reschedule(replacementSlotId);
            CollectionCase updatedCase = found.caseState().replaceAppointment(updatedAppointment)
                    .withAuditEvent(event(AuditEventType.APPOINTMENT_RESCHEDULED, actorUserId,
                            actorRole, time, Optional.of(appointmentId), Optional.of(oldSlot.slotId()),
                            Optional.of(replacementSlotId)));
            writeCandidate(state.slots(), replaceCase(state.cases(), Optional.of(found.caseState()),
                    updatedCase));
            return new BookingResult(BookingOutcome.BOOKED, Optional.of(updatedCase));
        }
    }

    @Override
    public synchronized AppointmentResult cancel(AppointmentId appointmentId,
            String studentUserId, String actorUserId, UserRole actorRole, Instant time)
            throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireRole(actorRole, UserRole.STUDENT);
            requireText(studentUserId, "studentUserId");
            requireText(actorUserId, "actorUserId");
            StoreState state = readCurrent();
            CaseAndAppointment found = findAppointment(state.cases(), appointmentId);
            if (found == null) {
                return new AppointmentResult(AppointmentOutcome.NOT_FOUND, Optional.empty());
            }
            if (!found.appointment().studentUserId().equals(studentUserId)) {
                return new AppointmentResult(AppointmentOutcome.NOT_AUTHORIZED, Optional.empty());
            }
            if (found.appointment().status() != AppointmentStatus.BOOKED) {
                return new AppointmentResult(AppointmentOutcome.ALREADY_TERMINAL,
                        Optional.of(found.caseState()));
            }
            CollectionSlot slot = findSlot(state.slots(), found.appointment().slotId()).orElseThrow();
            if (!slot.startsAt().isAfter(time)) {
                return new AppointmentResult(AppointmentOutcome.TOO_LATE,
                        Optional.of(found.caseState()));
            }
            CollectionAppointment replacement = found.appointment().cancel(time);
            CollectionCase updated = found.caseState().replaceAppointment(replacement)
                    .withAuditEvent(event(AuditEventType.APPOINTMENT_CANCELLED, actorUserId,
                            actorRole, time, Optional.of(appointmentId), Optional.empty(),
                            Optional.empty()));
            writeCandidate(state.slots(), replaceCase(state.cases(), Optional.of(found.caseState()),
                    updated));
            return new AppointmentResult(AppointmentOutcome.CHANGED, Optional.of(updated));
        }
    }

    @Override
    public synchronized AppointmentResult recordNoShow(AppointmentId appointmentId,
            String officerId, Instant time) throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireText(officerId, "officerId");
            StoreState state = readCurrent();
            CaseAndAppointment found = findAppointment(state.cases(), appointmentId);
            if (found == null) {
                return new AppointmentResult(AppointmentOutcome.NOT_FOUND, Optional.empty());
            }
            if (found.appointment().status() != AppointmentStatus.BOOKED) {
                return new AppointmentResult(AppointmentOutcome.ALREADY_TERMINAL,
                        Optional.of(found.caseState()));
            }
            CollectionSlot slot = findSlot(state.slots(), found.appointment().slotId()).orElseThrow();
            if (!slot.endsAt().isBefore(time) && !slot.endsAt().equals(time)) {
                return new AppointmentResult(AppointmentOutcome.TOO_EARLY,
                        Optional.of(found.caseState()));
            }
            CollectionAppointment replacement = found.appointment().markNoShow(time);
            CollectionCase updated = found.caseState().replaceAppointment(replacement)
                    .withAuditEvent(event(AuditEventType.NO_SHOW_RECORDED, officerId,
                            UserRole.DESK_OFFICER, time, Optional.of(appointmentId), Optional.empty(),
                            Optional.empty()));
            writeCandidate(state.slots(), replaceCase(state.cases(), Optional.of(found.caseState()),
                    updated));
            return new AppointmentResult(AppointmentOutcome.CHANGED, Optional.of(updated));
        }
    }

    @Override
    public synchronized CaseResult recordStorageLocation(ClaimId claimId, String officerId,
            String location, Instant time) throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireText(officerId, "officerId");
            StoreState state = readCurrent();
            Optional<CollectionCase> found = findCase(state.cases(), claimId);
            if (found.isEmpty()) {
                return new CaseResult(CaseOutcome.NOT_FOUND, Optional.empty());
            }
            CollectionCase current = found.orElseThrow();
            if (current.status() == CaseStatus.CLOSED || current.custodyStatus() == CustodyStatus.RETURNED) {
                return new CaseResult(CaseOutcome.CASE_CLOSED, Optional.of(current));
            }
            try {
                // A location correction must not undo readiness or confirmed collection.
                CustodyStatus updatedCustody = current.custodyStatus()
                        == CustodyStatus.READY_FOR_COLLECTION
                                ? CustodyStatus.READY_FOR_COLLECTION : CustodyStatus.STORED;
                CollectionCase updated = current.withCustody(updatedCustody,
                        Optional.of(location)).withAuditEvent(event(
                                AuditEventType.STORAGE_LOCATION_RECORDED, officerId,
                                UserRole.DESK_OFFICER, time, Optional.empty(), Optional.empty(),
                                Optional.empty()));
                writeCandidate(state.slots(), replaceCase(state.cases(), found, updated));
                return new CaseResult(CaseOutcome.CHANGED, Optional.of(updated));
            } catch (IllegalArgumentException failure) {
                return new CaseResult(CaseOutcome.INVALID_LOCATION, Optional.of(current));
            }
        }
    }

    @Override
    public synchronized CaseResult markReadyForCollection(ClaimId claimId, String officerId,
            Instant time) throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireText(officerId, "officerId");
            StoreState state = readCurrent();
            Optional<CollectionCase> found = findCase(state.cases(), claimId);
            if (found.isEmpty()) {
                return new CaseResult(CaseOutcome.NOT_FOUND, Optional.empty());
            }
            CollectionCase current = found.orElseThrow();
            if (current.status() == CaseStatus.CLOSED) {
                return new CaseResult(CaseOutcome.CASE_CLOSED, Optional.of(current));
            }
            if (current.custodyStatus() != CustodyStatus.STORED) {
                return new CaseResult(CaseOutcome.INVALID_CUSTODY, Optional.of(current));
            }
            CollectionCase updated = current.withCustody(CustodyStatus.READY_FOR_COLLECTION,
                    current.storageLocation()).withAuditEvent(event(AuditEventType.CUSTODY_READY,
                            officerId, UserRole.DESK_OFFICER, time, Optional.empty(), Optional.empty(),
                            Optional.empty()));
            writeCandidate(state.slots(), replaceCase(state.cases(), found, updated));
            return new CaseResult(CaseOutcome.CHANGED, Optional.of(updated));
        }
    }

    @Override
    public synchronized AppointmentResult confirmCollection(AppointmentId appointmentId,
            String officerId, Instant time) throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireText(officerId, "officerId");
            StoreState state = readCurrent();
            CaseAndAppointment found = findAppointment(state.cases(), appointmentId);
            if (found == null) {
                return new AppointmentResult(AppointmentOutcome.NOT_FOUND, Optional.empty());
            }
            if (found.appointment().status() != AppointmentStatus.BOOKED) {
                return new AppointmentResult(AppointmentOutcome.ALREADY_TERMINAL,
                        Optional.of(found.caseState()));
            }
            CollectionSlot slot = findSlot(state.slots(), found.appointment().slotId()).orElseThrow();
            if (slot.startsAt().isAfter(time)) {
                return new AppointmentResult(AppointmentOutcome.TOO_EARLY,
                        Optional.of(found.caseState()));
            }
            if (found.caseState().custodyStatus() != CustodyStatus.READY_FOR_COLLECTION) {
                return new AppointmentResult(AppointmentOutcome.INVALID_CUSTODY,
                        Optional.of(found.caseState()));
            }
            CollectionAppointment replacement = found.appointment().confirmCollection(time);
            CollectionCase updated = found.caseState().replaceAppointment(replacement)
                    .withAuditEvent(event(AuditEventType.COLLECTION_CONFIRMED, officerId,
                            UserRole.DESK_OFFICER, time, Optional.of(appointmentId), Optional.empty(),
                            Optional.empty()));
            writeCandidate(state.slots(), replaceCase(state.cases(), Optional.of(found.caseState()),
                    updated));
            return new AppointmentResult(AppointmentOutcome.CHANGED, Optional.of(updated));
        }
    }

    @Override
    public synchronized CaseResult markReturned(ClaimId claimId, String officerId, Instant time)
            throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireText(officerId, "officerId");
            StoreState state = readCurrent();
            Optional<CollectionCase> found = findCase(state.cases(), claimId);
            if (found.isEmpty()) {
                return new CaseResult(CaseOutcome.NOT_FOUND, Optional.empty());
            }
            CollectionCase current = found.orElseThrow();
            if (current.status() == CaseStatus.CLOSED) {
                return new CaseResult(CaseOutcome.CASE_CLOSED, Optional.of(current));
            }
            boolean confirmed = current.appointments().stream().anyMatch(appointment ->
                    appointment.status() == AppointmentStatus.COLLECTION_CONFIRMED);
            if (!confirmed || current.custodyStatus() != CustodyStatus.READY_FOR_COLLECTION) {
                return new CaseResult(CaseOutcome.INVALID_CUSTODY, Optional.of(current));
            }
            CollectionCase updated = current.withCustody(CustodyStatus.RETURNED,
                    current.storageLocation()).withAuditEvent(event(AuditEventType.ITEM_RETURNED,
                            officerId, UserRole.DESK_OFFICER, time, Optional.empty(), Optional.empty(),
                            Optional.empty()));
            writeCandidate(state.slots(), replaceCase(state.cases(), found, updated));
            return new CaseResult(CaseOutcome.CHANGED, Optional.of(updated));
        }
    }

    @Override
    public synchronized CaseResult closeCase(ClaimId claimId, String officerId, Instant time)
            throws AppointmentStoreException {
        try (AppointmentStoreLock.Guard storeLock = AppointmentStoreLock.acquire(storePath)) {
            storeLock.ensureHeld();
            requireText(officerId, "officerId");
            StoreState state = readCurrent();
            Optional<CollectionCase> found = findCase(state.cases(), claimId);
            if (found.isEmpty()) {
                return new CaseResult(CaseOutcome.NOT_FOUND, Optional.empty());
            }
            CollectionCase current = found.orElseThrow();
            if (current.status() == CaseStatus.CLOSED) {
                return new CaseResult(CaseOutcome.ALREADY_CLOSED, Optional.of(current));
            }
            try {
                CollectionCase updated = current.close(time).withAuditEvent(event(
                        AuditEventType.CASE_CLOSED, officerId, UserRole.DESK_OFFICER, time,
                        Optional.empty(), Optional.empty(), Optional.empty()));
                writeCandidate(state.slots(), replaceCase(state.cases(), found, updated));
                return new CaseResult(CaseOutcome.CHANGED, Optional.of(updated));
            } catch (IllegalStateException failure) {
                return new CaseResult(CaseOutcome.INVALID_CUSTODY, Optional.of(current));
            }
        }
    }

    private StoreState readCurrent() throws AppointmentStoreException {
        byte[] document;
        try {
            Optional<byte[]> stored = storeFiles.readBounded(storePath, maximumStoreBytes);
            if (stored.isEmpty()) {
                return new StoreState(List.of(), List.of());
            }
            document = stored.orElseThrow();
        } catch (StoreFileFailure failure) {
            AppointmentStoreException.Reason reason = failure.kind() == StoreFileFailure.Kind.OVER_LIMIT
                    ? AppointmentStoreException.Reason.CORRUPT_STORE
                    : AppointmentStoreException.Reason.READ_FAILURE;
            AppointmentDiagnostics.failure("read", reason);
            throw new AppointmentStoreException(reason);
        }
        try {
            AppointmentStoreJsonCodec.DecodedStore decoded = codec.decode(document);
            return new StoreState(decoded.slots(), decoded.cases());
        } catch (AppointmentStoreJsonCodec.InvalidStoreException failure) {
            AppointmentStoreException.Reason reason = failure.unsupportedVersion()
                    ? AppointmentStoreException.Reason.UNSUPPORTED_VERSION
                    : AppointmentStoreException.Reason.CORRUPT_STORE;
            AppointmentDiagnostics.failure("decode", reason);
            throw new AppointmentStoreException(reason, failure.getMessage());
        }
    }

    private void writeCandidate(List<CollectionSlot> slots, List<CollectionCase> cases)
            throws AppointmentStoreException {
        byte[] document;
        try {
            document = codec.encode(slots, cases, maximumStoreBytes);
        } catch (AppointmentStoreJsonCodec.InvalidStateException failure) {
            AppointmentStoreException.Reason reason =
                    AppointmentStoreException.Reason.RESULT_TOO_LARGE;
            AppointmentDiagnostics.failure("encode", reason);
            throw new AppointmentStoreException(reason);
        }
        try {
            storeFiles.replaceAtomically(storePath, document);
        } catch (StoreFileFailure failure) {
            AppointmentStoreException.Reason reason =
                    AppointmentStoreException.Reason.WRITE_FAILURE;
            AppointmentDiagnostics.failure("write", reason);
            throw new AppointmentStoreException(reason);
        }
    }

    private AuditEvent event(AuditEventType type, String actor, UserRole role, Instant time,
            Optional<AppointmentId> appointment, Optional<SlotId> source,
            Optional<SlotId> target) {
        return new AuditEvent(new AuditEventId(ids.get()), type, time, actor, role,
                appointment, source, target);
    }

    private static boolean isValidStart(Instant start, Instant now) {
        ZonedDateTime local = start.atZone(APPLICATION_ZONE);
        return start.isAfter(now) && (local.getMinute() == 0 || local.getMinute() == 30)
                && local.getSecond() == 0 && local.getNano() == 0;
    }

    private static boolean overlaps(CollectionSlot left, CollectionSlot right) {
        return left.startsAt().isBefore(right.endsAt())
                && right.startsAt().isBefore(left.endsAt());
    }

    private static Optional<CollectionSlot> findSlot(List<CollectionSlot> slots, SlotId id) {
        int index = findSlotIndex(slots, id);
        return index < 0 ? Optional.empty() : Optional.of(slots.get(index));
    }

    private static int findSlotIndex(List<CollectionSlot> slots, SlotId id) {
        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).slotId().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static Optional<CollectionCase> findCase(List<CollectionCase> cases, ClaimId id) {
        return cases.stream().filter(caseState -> caseState.claimId().equals(id)).findFirst();
    }

    private static CaseAndAppointment findAppointment(List<CollectionCase> cases,
            AppointmentId id) {
        for (CollectionCase caseState : cases) {
            for (CollectionAppointment appointment : caseState.appointments()) {
                if (appointment.appointmentId().equals(id)) {
                    return new CaseAndAppointment(caseState, appointment);
                }
            }
        }
        return null;
    }

    private static boolean containsAppointment(List<CollectionCase> cases, AppointmentId id) {
        return findAppointment(cases, id) != null;
    }

    private static boolean hasActiveSlot(List<CollectionCase> cases, SlotId id) {
        return cases.stream().flatMap(caseState -> caseState.appointments().stream())
                .anyMatch(appointment -> appointment.status() == AppointmentStatus.BOOKED
                        && appointment.slotId().equals(id));
    }

    private static boolean hasActiveSlotExcept(List<CollectionCase> cases, SlotId id,
            AppointmentId excluded) {
        return cases.stream().flatMap(caseState -> caseState.appointments().stream())
                .anyMatch(appointment -> appointment.status() == AppointmentStatus.BOOKED
                        && appointment.slotId().equals(id)
                        && !appointment.appointmentId().equals(excluded));
    }

    private static List<CollectionCase> replaceCase(List<CollectionCase> cases,
            Optional<CollectionCase> previous, CollectionCase replacement) {
        List<CollectionCase> updated = new ArrayList<>(cases);
        if (previous.isEmpty()) {
            updated.add(replacement);
            return updated;
        }
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).claimId().equals(previous.orElseThrow().claimId())) {
                updated.set(index, replacement);
                return updated;
            }
        }
        throw new IllegalStateException("Case disappeared during command.");
    }

    private static void requireRole(UserRole actual, UserRole expected) {
        if (actual != expected) {
            throw new IllegalArgumentException("The authenticated role is not permitted.");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }

    /** Immutable snapshot read from or written to the appointment store. */
    private record StoreState(List<CollectionSlot> slots, List<CollectionCase> cases) {
    }

    /** Case lookup result that keeps the selected appointment beside its parent case. */
    private record CaseAndAppointment(CollectionCase caseState,
            CollectionAppointment appointment) {
    }
}
