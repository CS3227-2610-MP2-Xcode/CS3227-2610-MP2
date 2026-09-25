package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Atomic persistence boundary for collection slots and appointment cases. */
public interface AppointmentRepository {
    /**
     * Loads all retained slots in persisted order.
     * @return immutable slot snapshot
     * @throws AppointmentStoreException when storage is unavailable or corrupt
     */
    List<CollectionSlot> loadSlots() throws AppointmentStoreException;

    /**
     * Loads all retained cases in persisted order.
     * @return immutable case snapshot
     * @throws AppointmentStoreException when storage is unavailable or corrupt
     */
    List<CollectionCase> loadCases() throws AppointmentStoreException;

    /**
     * Atomically creates a slot.
     * @param slot validated slot candidate
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    SlotResult createSlot(CollectionSlot slot) throws AppointmentStoreException;

    /**
     * Atomically disables an unbooked slot.
     * @param slotId target slot
     * @param officerId authenticated officer
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    SlotResult disableSlot(SlotId slotId, String officerId, Instant time)
            throws AppointmentStoreException;

    /**
     * Atomically books one available slot for one approved Claim.
     * @param claimId approved Claim
     * @param studentUserId authenticated Student
     * @param slotId target slot
     * @param appointmentId new appointment identity
     * @param actorUserId audit actor
     * @param actorRole audit actor role
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    BookingResult book(ClaimId claimId, String studentUserId, SlotId slotId,
            AppointmentId appointmentId, String actorUserId, UserRole actorRole,
            Instant time) throws AppointmentStoreException;

    /**
     * Atomically moves a booked appointment to another available slot.
     * @param appointmentId target appointment
     * @param studentUserId authenticated Student
     * @param replacementSlotId new slot
     * @param actorUserId audit actor
     * @param actorRole audit actor role
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    BookingResult reschedule(AppointmentId appointmentId, String studentUserId,
            SlotId replacementSlotId, String actorUserId, UserRole actorRole,
            Instant time) throws AppointmentStoreException;

    /**
     * Atomically cancels an owned booked appointment.
     * @param appointmentId target appointment
     * @param studentUserId authenticated Student
     * @param actorUserId audit actor
     * @param actorRole audit actor role
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    AppointmentResult cancel(AppointmentId appointmentId, String studentUserId,
            String actorUserId, UserRole actorRole, Instant time)
            throws AppointmentStoreException;

    /**
     * Atomically records an officer no-show after the slot has ended.
     * @param appointmentId target appointment
     * @param officerId authenticated officer
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    AppointmentResult recordNoShow(AppointmentId appointmentId, String officerId,
            Instant time) throws AppointmentStoreException;

    /**
     * Atomically records the officer-only storage location.
     * @param claimId target case
     * @param officerId authenticated officer
     * @param location storage location
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    CaseResult recordStorageLocation(ClaimId claimId, String officerId,
            String location, Instant time) throws AppointmentStoreException;

    /**
     * Atomically marks stored custody ready for collection.
     * @param claimId target case
     * @param officerId authenticated officer
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    CaseResult markReadyForCollection(ClaimId claimId, String officerId, Instant time)
            throws AppointmentStoreException;

    /**
     * Atomically confirms attendance at the collection desk.
     * @param appointmentId target appointment
     * @param officerId authenticated officer
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    AppointmentResult confirmCollection(AppointmentId appointmentId, String officerId,
            Instant time) throws AppointmentStoreException;

    /**
     * Atomically records that the item was returned to the Student.
     * @param claimId target case
     * @param officerId authenticated officer
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    CaseResult markReturned(ClaimId claimId, String officerId, Instant time)
            throws AppointmentStoreException;

    /**
     * Atomically closes a returned collection case.
     * @param claimId target case
     * @param officerId authenticated officer
     * @param time command time
     * @return typed command outcome
     * @throws AppointmentStoreException when storage cannot be committed
     */
    CaseResult closeCase(ClaimId claimId, String officerId, Instant time)
            throws AppointmentStoreException;

    /** Outcomes for slot commands. */
    enum SlotOutcome {
        /** Slot was committed. */
        CREATED,
        /** Slot identity already exists. */
        ID_COLLISION,
        /** Slot identity was not found. */
        NOT_FOUND,
        /** Slot was already disabled. */
        ALREADY_DISABLED,
        /** A booked appointment prevents disabling. */
        BOOKED,
        /** Enabled slots overlap. */
        OVERLAPPING,
        /** Slot time violates the schedule policy. */
        INVALID_TIME
    }

    /**
     * Result for a slot command.
     * @param outcome typed outcome
     * @param slot authoritative slot, when available
     */
    record SlotResult(SlotOutcome outcome, Optional<CollectionSlot> slot) {
        /** Validates result consistency. */
        public SlotResult {
            outcome = Objects.requireNonNull(outcome, "outcome");
            slot = Objects.requireNonNull(slot, "slot");
        }
    }

    /** Outcomes for booking and rescheduling commands. */
    enum BookingOutcome {
        /** Booking or rescheduling committed. */
        BOOKED,
        /** Appointment identity already exists. */
        ID_COLLISION,
        /** Claim already has an active appointment. */
        CLAIM_ALREADY_ACTIVE,
        /** Case is closed. */
        CASE_CLOSED,
        /** Target slot was not found. */
        SLOT_NOT_FOUND,
        /** Target slot is disabled. */
        SLOT_DISABLED,
        /** Target slot is occupied. */
        SLOT_TAKEN,
        /** Appointment was not found. */
        NOT_FOUND,
        /** Authenticated owner does not match. */
        NOT_AUTHORIZED,
        /** Appointment is not currently booked. */
        NOT_BOOKED,
        /** Change was attempted too late. */
        TOO_LATE,
        /** Command time violates scheduling policy. */
        INVALID_TIME
    }

    /**
     * Result for booking and rescheduling commands.
     * @param outcome typed outcome
     * @param caseState authoritative case, when available
     */
    record BookingResult(BookingOutcome outcome, Optional<CollectionCase> caseState) {
        /** Validates result consistency. */
        public BookingResult {
            outcome = Objects.requireNonNull(outcome, "outcome");
            caseState = Objects.requireNonNull(caseState, "caseState");
        }
    }

    /** Outcomes for appointment commands. */
    enum AppointmentOutcome {
        /** Appointment command committed. */
        CHANGED,
        /** Appointment was not found. */
        NOT_FOUND,
        /** Authenticated actor is not authorized. */
        NOT_AUTHORIZED,
        /** Appointment is not currently booked. */
        NOT_BOOKED,
        /** Appointment already has a terminal status. */
        ALREADY_TERMINAL,
        /** Command happened before the allowed time. */
        TOO_EARLY,
        /** Command happened after the allowed time. */
        TOO_LATE,
        /** Custody state blocks the operation. */
        INVALID_CUSTODY,
        /** Case is closed. */
        CASE_CLOSED
    }

    /**
     * Result for appointment commands.
     * @param outcome typed outcome
     * @param caseState authoritative case, when available
     */
    record AppointmentResult(AppointmentOutcome outcome,
            Optional<CollectionCase> caseState) {
        /** Validates result consistency. */
        public AppointmentResult {
            outcome = Objects.requireNonNull(outcome, "outcome");
            caseState = Objects.requireNonNull(caseState, "caseState");
        }
    }

    /** Outcomes for custody and case commands. */
    enum CaseOutcome {
        /** Case command committed. */
        CHANGED,
        /** Case was not found. */
        NOT_FOUND,
        /** Authenticated actor is not authorized. */
        NOT_AUTHORIZED,
        /** Custody state blocks the operation. */
        INVALID_CUSTODY,
        /** Case is closed. */
        CASE_CLOSED,
        /** Case was already closed. */
        ALREADY_CLOSED,
        /** Storage location is invalid. */
        INVALID_LOCATION
    }

    /**
     * Result for custody and case commands.
     * @param outcome typed outcome
     * @param caseState authoritative case, when available
     */
    record CaseResult(CaseOutcome outcome, Optional<CollectionCase> caseState) {
        /** Validates result consistency. */
        public CaseResult {
            outcome = Objects.requireNonNull(outcome, "outcome");
            caseState = Objects.requireNonNull(caseState, "caseState");
        }
    }
}
