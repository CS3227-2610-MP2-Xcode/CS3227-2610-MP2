package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Atomic persistence boundary for the complete retained Claim ledger. */
public interface ClaimRepository {
    /**
     * Loads every retained Claim in stable submission order.
     *
     * @return immutable complete Claim snapshot
     * @throws ClaimStoreException when storage cannot be read safely
     */
    List<Claim> loadAll() throws ClaimStoreException;

    /**
     * Atomically submits one validated Pending review Claim.
     *
     * @param pending proposed Pending review Claim
     * @return typed durable or non-mutating outcome
     * @throws ClaimStoreException when storage cannot be read or changed safely
     */
    SubmissionResult submit(Claim pending) throws ClaimStoreException;

    /**
     * Atomically withdraws an owned Pending review Claim.
     *
     * @param id target Claim identity
     * @param claimantUserId authenticated claimant identity
     * @param terminalAt withdrawal time
     * @return typed durable or non-mutating outcome
     * @throws ClaimStoreException when storage cannot be read or changed safely
     */
    TerminalResult withdraw(ClaimId id, String claimantUserId, Instant terminalAt)
            throws ClaimStoreException;

    /**
     * Atomically approves a Pending review Claim.
     *
     * @param id target Claim identity
     * @param decisionReason optional approval reason
     * @param terminalAt decision time
     * @return typed durable or non-mutating outcome
     * @throws ClaimStoreException when storage cannot be read or changed safely
     */
    TerminalResult approve(ClaimId id, Optional<String> decisionReason,
            Instant terminalAt) throws ClaimStoreException;

    /**
     * Atomically rejects a Pending review Claim.
     *
     * @param id target Claim identity
     * @param decisionReason mandatory rejection reason
     * @param terminalAt decision time
     * @return typed durable or non-mutating outcome
     * @throws ClaimStoreException when storage cannot be read or changed safely
     */
    TerminalResult reject(ClaimId id, String decisionReason, Instant terminalAt)
            throws ClaimStoreException;

    /** Outcomes of an atomic submission command. */
    enum SubmissionOutcome {
        /** Candidate was committed. */
        CREATED,
        /** Candidate Claim ID already exists. */
        ID_COLLISION,
        /** The same claimant has a blocking active Claim. */
        OWN_ACTIVE_CLAIM,
        /** A non-disclosable lock or closure blocked submission. */
        BLOCKED
    }

    /** Outcomes of an atomic terminal command. */
    enum TerminalOutcome {
        /** Pending Claim changed and committed. */
        CHANGED,
        /** Claim was already terminal and no write occurred. */
        ALREADY_TERMINAL,
        /** Target Claim does not exist. */
        NOT_FOUND,
        /** Authenticated claimant does not own the target. */
        NOT_AUTHORIZED
    }

    /**
     * Result of one submission command.
     *
     * @param outcome typed outcome
     * @param claim committed Claim or same claimant's active blocker
     */
    record SubmissionResult(SubmissionOutcome outcome, Optional<Claim> claim) {
        /** Validates the result without revealing another claimant. */
        public SubmissionResult {
            Objects.requireNonNull(outcome, "outcome");
            claim = Objects.requireNonNull(claim, "claim");
            boolean requiresClaim = outcome == SubmissionOutcome.CREATED
                    || outcome == SubmissionOutcome.OWN_ACTIVE_CLAIM;
            if (requiresClaim != claim.isPresent()) {
                throw new IllegalArgumentException("Submission result is inconsistent.");
            }
        }

        @Override
        public String toString() {
            return "SubmissionResult[redacted]";
        }
    }

    /**
     * Result of one terminal command.
     *
     * @param outcome typed outcome
     * @param claim committed or authoritative already-terminal Claim
     */
    record TerminalResult(TerminalOutcome outcome, Optional<Claim> claim) {
        /** Validates the result and suppresses unauthorized data. */
        public TerminalResult {
            Objects.requireNonNull(outcome, "outcome");
            claim = Objects.requireNonNull(claim, "claim");
            boolean requiresClaim = outcome == TerminalOutcome.CHANGED
                    || outcome == TerminalOutcome.ALREADY_TERMINAL;
            if (requiresClaim != claim.isPresent()) {
                throw new IllegalArgumentException("Terminal result is inconsistent.");
            }
        }

        @Override
        public String toString() {
            return "TerminalResult[redacted]";
        }
    }
}
