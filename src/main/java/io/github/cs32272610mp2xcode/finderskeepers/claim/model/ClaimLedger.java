package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable derived consistency view over all retained Claims. */
public final class ClaimLedger {
    private final List<Claim> claims;

    private ClaimLedger(List<Claim> retainedClaims) {
        claims = retainedClaims;
    }

    /**
     * Validates and indexes one complete Claim snapshot.
     *
     * @param retainedClaims Claims in stable submission order
     * @return immutable validated ledger
     */
    public static ClaimLedger from(List<Claim> retainedClaims) {
        Objects.requireNonNull(retainedClaims, "claims");
        List<Claim> copy = List.copyOf(retainedClaims);
        validate(copy);
        return new ClaimLedger(copy);
    }

    /**
     * Returns the complete immutable retained snapshot.
     *
     * @return Claims in submission order
     */
    public List<Claim> claims() {
        return claims;
    }

    /**
     * Evaluates only Claim-derived locks and closures for a proposed pair.
     *
     * @param claimant stable claimant user ID
     * @param lostReportId proposed LOST endpoint
     * @param foundReportId proposed FOUND endpoint
     * @return nonleaking eligibility result
     */
    public SubmissionEvaluation evaluate(String claimant, UUID lostReportId,
            UUID foundReportId) {
        Objects.requireNonNull(claimant, "claimantUserId");
        Objects.requireNonNull(lostReportId, "lostReportId");
        Objects.requireNonNull(foundReportId, "foundReportId");
        if (claimant.isBlank() || lostReportId.equals(foundReportId)) {
            throw new IllegalArgumentException("Invalid claim candidate.");
        }
        Optional<Claim> ownBlocker = claims.stream()
                .filter(claim -> claim.status() == ClaimStatus.PENDING_REVIEW)
                .filter(claim -> claim.claimantUserId().equals(claimant))
                .filter(claim -> claim.lostReportId().equals(lostReportId)
                        || claim.foundReportId().equals(foundReportId))
                .findFirst();
        if (ownBlocker.isPresent()) {
            return new SubmissionEvaluation(SubmissionEligibility.OWN_ACTIVE_CLAIM,
                    ownBlocker);
        }
        boolean blocked = claims.stream().anyMatch(claim -> blocks(
                claim, claimant, lostReportId, foundReportId));
        return new SubmissionEvaluation(blocked
                ? SubmissionEligibility.BLOCKED : SubmissionEligibility.ELIGIBLE,
                Optional.empty());
    }

    private static boolean blocks(Claim claim, String claimant,
            UUID lostReportId, UUID foundReportId) {
        return switch (claim.status()) {
            case PENDING_REVIEW -> claim.lostReportId().equals(lostReportId)
                    || claim.foundReportId().equals(foundReportId);
            case APPROVED -> claim.lostReportId().equals(lostReportId)
                    || claim.foundReportId().equals(foundReportId);
            case REJECTED -> claim.claimantUserId().equals(claimant)
                    && claim.foundReportId().equals(foundReportId);
            case WITHDRAWN -> false;
        };
    }

    private static void validate(List<Claim> retainedClaims) {
        Set<ClaimId> ids = new HashSet<>();
        Set<UUID> pendingLost = new HashSet<>();
        Set<UUID> pendingFound = new HashSet<>();
        Set<UUID> approvedEndpoints = new HashSet<>();
        Set<ClaimantFound> rejectedTargets = new HashSet<>();
        List<Claim> previous = new ArrayList<>();
        for (Claim claim : retainedClaims) {
            Objects.requireNonNull(claim, "claim");
            if (!ids.add(claim.claimId())) {
                contradiction();
            }
            if (approvedEndpoints.contains(claim.lostReportId())
                    || approvedEndpoints.contains(claim.foundReportId())) {
                contradiction();
            }
            if (rejectedTargets.contains(new ClaimantFound(
                    claim.claimantUserId(), claim.foundReportId()))) {
                contradiction();
            }
            Optional<Claim> samePair = previous.stream()
                    .filter(existing -> samePair(existing, claim))
                    .reduce((first, second) -> second);
            if (samePair.isPresent() && samePair.orElseThrow().status() != ClaimStatus.WITHDRAWN) {
                contradiction();
            }
            if (claim.status() == ClaimStatus.PENDING_REVIEW
                    && (!pendingLost.add(claim.lostReportId())
                            || !pendingFound.add(claim.foundReportId()))) {
                contradiction();
            }
            if (claim.status() == ClaimStatus.APPROVED) {
                if (!approvedEndpoints.add(claim.lostReportId())
                        || !approvedEndpoints.add(claim.foundReportId())) {
                    contradiction();
                }
            } else if (claim.status() == ClaimStatus.REJECTED) {
                rejectedTargets.add(new ClaimantFound(
                        claim.claimantUserId(), claim.foundReportId()));
            }
            previous.add(claim);
        }
        for (Claim claim : retainedClaims) {
            if (claim.status() == ClaimStatus.PENDING_REVIEW
                    && (approvedEndpoints.contains(claim.lostReportId())
                            || approvedEndpoints.contains(claim.foundReportId()))) {
                contradiction();
            }
        }
    }

    private static boolean samePair(Claim first, Claim second) {
        return first.claimantUserId().equals(second.claimantUserId())
                && first.lostReportId().equals(second.lostReportId())
                && first.foundReportId().equals(second.foundReportId());
    }

    private static void contradiction() {
        throw new IllegalArgumentException("The Claim snapshot is contradictory.");
    }

    /** Claim-state eligibility outcome for a proposed submission. */
    public enum SubmissionEligibility {
        /** No current Claim-derived restriction applies. */
        ELIGIBLE,

        /** The same claimant has a blocking active Claim. */
        OWN_ACTIVE_CLAIM,

        /** Another lock or a closure blocks submission. */
        BLOCKED
    }

    /**
     * Nonleaking Claim-state evaluation.
     *
     * @param eligibility outcome category
     * @param ownActiveClaim same claimant's blocker only
     */
    public record SubmissionEvaluation(SubmissionEligibility eligibility,
            Optional<Claim> ownActiveClaim) {
        /** Validates outcome consistency and defensively retains the optional. */
        public SubmissionEvaluation {
            Objects.requireNonNull(eligibility, "eligibility");
            ownActiveClaim = Objects.requireNonNull(ownActiveClaim, "ownActiveClaim");
            if ((eligibility == SubmissionEligibility.OWN_ACTIVE_CLAIM)
                    != ownActiveClaim.isPresent()) {
                throw new IllegalArgumentException("Claim evaluation is inconsistent.");
            }
        }

        @Override
        public String toString() {
            return "SubmissionEvaluation[redacted]";
        }
    }

    /** Composite key enforcing one active Claim per Student and found report. */
    private record ClaimantFound(String claimantUserId, UUID foundReportId) {
    }

    @Override
    public String toString() {
        return "ClaimLedger[redacted]";
    }
}
