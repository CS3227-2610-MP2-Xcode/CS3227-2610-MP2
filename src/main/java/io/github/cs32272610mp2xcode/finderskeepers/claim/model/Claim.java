package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable retained ownership claim. */
public final class Claim {
    private final ClaimId claimId;

    private final String claimantUserId;

    private final UUID lostReportId;

    private final UUID foundReportId;

    private final String ownershipEvidence;

    private final ClaimStatus status;

    private final Instant submittedAt;

    private final Optional<Instant> terminalAt;

    private final Optional<String> decisionReason;

    private Claim(ClaimId id, String claimant, UUID lostId, UUID foundId,
            String evidence, ClaimStatus claimStatus, Instant submissionTime,
            Optional<Instant> terminalTime, Optional<String> reason) {
        claimId = id;
        claimantUserId = claimant;
        lostReportId = lostId;
        foundReportId = foundId;
        ownershipEvidence = evidence;
        status = claimStatus;
        submittedAt = submissionTime;
        terminalAt = terminalTime;
        decisionReason = reason;
    }

    /**
     * Creates a validated Pending review claim.
     *
     * @param id claim identity
     * @param claimant stable authenticated claimant ID
     * @param lostId claimant's LOST report ID
     * @param foundId target FOUND report ID
     * @param evidence raw ownership evidence
     * @param submissionTime submission event time
     * @return validated pending claim
     */
    public static Claim createPending(ClaimId id, String claimant, UUID lostId,
            UUID foundId, String evidence, Instant submissionTime) {
        return build(id, claimant, lostId, foundId, ClaimTextPolicy.evidence(evidence),
                ClaimStatus.PENDING_REVIEW, submissionTime, Optional.empty(),
                Optional.empty(), false);
    }

    /**
     * Restores a complete persisted Claim and validates every invariant.
     *
     * @param id claim identity
     * @param claimant stable authenticated claimant ID
     * @param lostId LOST report ID
     * @param foundId FOUND report ID
     * @param evidence normalized immutable evidence
     * @param claimStatus stored lifecycle status
     * @param submissionTime stored submission time
     * @param terminalTime stored terminal time
     * @param reason stored decision reason
     * @return validated restored claim
     */
    public static Claim restore(ClaimId id, String claimant, UUID lostId,
            UUID foundId, String evidence, ClaimStatus claimStatus,
            Instant submissionTime, Optional<Instant> terminalTime,
            Optional<String> reason) {
        String normalizedEvidence = ClaimTextPolicy.evidence(evidence);
        if (!Objects.equals(evidence, normalizedEvidence)) {
            throw new IllegalArgumentException("Stored ownership evidence is not normalized.");
        }
        Optional<String> normalizedReason = normalizedReason(claimStatus, reason);
        if (!Objects.equals(reason, normalizedReason)) {
            throw new IllegalArgumentException("Stored decision reason is not normalized.");
        }
        return build(id, claimant, lostId, foundId, normalizedEvidence, claimStatus,
                submissionTime, terminalTime, normalizedReason, true);
    }

    /**
     * Returns an Approved copy of a Pending review Claim.
     *
     * @param rawReason optional approval reason
     * @param time decision time
     * @return approved immutable copy
     */
    public Claim approve(Optional<String> rawReason, Instant time) {
        requirePending();
        Optional<String> reason = ClaimTextPolicy.optionalDecisionReason(rawReason);
        return build(claimId, claimantUserId, lostReportId, foundReportId,
                ownershipEvidence, ClaimStatus.APPROVED, submittedAt,
                Optional.ofNullable(time), reason, true);
    }

    /**
     * Returns a Rejected copy of a Pending review Claim.
     *
     * @param rawReason mandatory rejection reason
     * @param time decision time
     * @return rejected immutable copy
     */
    public Claim reject(String rawReason, Instant time) {
        requirePending();
        String reason = ClaimTextPolicy.requiredDecisionReason(rawReason);
        return build(claimId, claimantUserId, lostReportId, foundReportId,
                ownershipEvidence, ClaimStatus.REJECTED, submittedAt,
                Optional.ofNullable(time), Optional.of(reason), true);
    }

    /**
     * Returns a Withdrawn copy of a Pending review Claim.
     *
     * @param time withdrawal time
     * @return withdrawn immutable copy
     */
    public Claim withdraw(Instant time) {
        requirePending();
        return build(claimId, claimantUserId, lostReportId, foundReportId,
                ownershipEvidence, ClaimStatus.WITHDRAWN, submittedAt,
                Optional.ofNullable(time), Optional.empty(), true);
    }

    private static Claim build(ClaimId id, String claimant, UUID lostId,
            UUID foundId, String evidence, ClaimStatus claimStatus,
            Instant submissionTime, Optional<Instant> terminalTime,
            Optional<String> reason, boolean normalizedText) {
        Objects.requireNonNull(id, "claimId");
        Objects.requireNonNull(claimant, "claimantUserId");
        Objects.requireNonNull(lostId, "lostReportId");
        Objects.requireNonNull(foundId, "foundReportId");
        Objects.requireNonNull(evidence, "ownershipEvidence");
        Objects.requireNonNull(claimStatus, "status");
        Objects.requireNonNull(submissionTime, "submittedAt");
        Objects.requireNonNull(terminalTime, "terminalAt");
        Objects.requireNonNull(reason, "decisionReason");
        if (claimant.isBlank()) {
            throw new IllegalArgumentException("Claimant user ID must not be blank.");
        }
        if (lostId.equals(foundId)) {
            throw new IllegalArgumentException("A claim requires distinct report endpoints.");
        }
        requireMilliseconds(submissionTime, "Submission time");
        terminalTime.ifPresent(time -> {
            requireMilliseconds(time, "Terminal time");
            if (time.isBefore(submissionTime)) {
                throw new IllegalArgumentException("Terminal time cannot precede submission time.");
            }
        });
        validateLifecycle(claimStatus, terminalTime, reason);
        String validEvidence = normalizedText ? evidence : ClaimTextPolicy.evidence(evidence);
        return new Claim(id, claimant, lostId, foundId, validEvidence, claimStatus,
                submissionTime, terminalTime, reason);
    }

    private static Optional<String> normalizedReason(ClaimStatus claimStatus,
            Optional<String> reason) {
        Objects.requireNonNull(reason, "decisionReason");
        return switch (claimStatus) {
            case APPROVED -> ClaimTextPolicy.optionalDecisionReason(reason);
            case REJECTED -> Optional.of(ClaimTextPolicy.requiredDecisionReason(
                    reason.orElse(null)));
            case PENDING_REVIEW, WITHDRAWN -> reason;
        };
    }

    private static void validateLifecycle(ClaimStatus claimStatus,
            Optional<Instant> terminalTime, Optional<String> reason) {
        switch (claimStatus) {
            case PENDING_REVIEW -> {
                if (terminalTime.isPresent() || reason.isPresent()) {
                    throw new IllegalArgumentException("A pending Claim cannot have terminal data.");
                }
            }
            case APPROVED -> {
                if (terminalTime.isEmpty()) {
                    throw new IllegalArgumentException("An approved Claim requires a terminal time.");
                }
            }
            case REJECTED -> {
                if (terminalTime.isEmpty() || reason.isEmpty()) {
                    throw new IllegalArgumentException("A rejected Claim requires terminal data.");
                }
            }
            case WITHDRAWN -> {
                if (terminalTime.isEmpty() || reason.isPresent()) {
                    throw new IllegalArgumentException("A withdrawn Claim has invalid terminal data.");
                }
            }
            default -> throw new IllegalStateException("Unsupported Claim status.");
        }
    }

    private static void requireMilliseconds(Instant time, String label) {
        if (!time.equals(time.truncatedTo(ChronoUnit.MILLIS))) {
            throw new IllegalArgumentException(label + " must use millisecond precision.");
        }
    }

    private void requirePending() {
        if (status != ClaimStatus.PENDING_REVIEW) {
            throw new IllegalStateException("A terminal Claim cannot transition again.");
        }
    }

    /**
     * Returns this Claim's immutable identity.
     *
     * @return immutable claim ID
     */
    public ClaimId claimId() {
        return claimId;
    }

    /**
     * Returns the stable authenticated claimant identity.
     *
     * @return stable claimant user ID
     */
    public String claimantUserId() {
        return claimantUserId;
    }

    /**
     * Returns the directional LOST endpoint.
     *
     * @return LOST report ID
     */
    public UUID lostReportId() {
        return lostReportId;
    }

    /**
     * Returns the directional FOUND endpoint.
     *
     * @return FOUND report ID
     */
    public UUID foundReportId() {
        return foundReportId;
    }

    /**
     * Returns the submitted immutable evidence.
     *
     * @return immutable ownership evidence
     */
    public String ownershipEvidence() {
        return ownershipEvidence;
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return current claim status
     */
    public ClaimStatus status() {
        return status;
    }

    /**
     * Returns the durable submission event time.
     *
     * @return submission time
     */
    public Instant submittedAt() {
        return submittedAt;
    }

    /**
     * Returns the terminal event time when the Claim is terminal.
     *
     * @return terminal event time, when terminal
     */
    public Optional<Instant> terminalAt() {
        return terminalAt;
    }

    /**
     * Returns the recorded approval or rejection reason, when present.
     *
     * @return optional decision reason
     */
    public Optional<String> decisionReason() {
        return decisionReason;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Claim that)) {
            return false;
        }
        return claimId.equals(that.claimId)
                && claimantUserId.equals(that.claimantUserId)
                && lostReportId.equals(that.lostReportId)
                && foundReportId.equals(that.foundReportId)
                && ownershipEvidence.equals(that.ownershipEvidence)
                && status == that.status
                && submittedAt.equals(that.submittedAt)
                && terminalAt.equals(that.terminalAt)
                && decisionReason.equals(that.decisionReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(claimId, claimantUserId, lostReportId, foundReportId,
                ownershipEvidence, status, submittedAt, terminalAt, decisionReason);
    }

    @Override
    public String toString() {
        return "Claim[redacted]";
    }
}
