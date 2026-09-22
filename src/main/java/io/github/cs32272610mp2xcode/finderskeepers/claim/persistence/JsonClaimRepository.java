package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimLedger;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;

/** Strict version-one JSON implementation of the Claim repository. */
public final class JsonClaimRepository implements ClaimRepository {
    /** Maximum accepted or generated document size. */
    static final int MAX_STORE_BYTES = 16_777_216;

    private final Path claimStorePath;

    private final ClaimStoreFiles storeFiles;

    private final int maximumStoreBytes;

    private final ClaimStoreJsonCodec codec;

    /**
     * Creates a repository for one caller-supplied Claim-store path.
     *
     * @param path Claim-store location
     */
    public JsonClaimRepository(Path path) {
        this(path, new NioClaimStoreFiles(), MAX_STORE_BYTES);
    }

    JsonClaimRepository(Path path, ClaimStoreFiles files, int maximumBytes) {
        claimStorePath = Objects.requireNonNull(path, "claimStorePath")
                .toAbsolutePath().normalize();
        storeFiles = Objects.requireNonNull(files, "storeFiles");
        if (maximumBytes <= 0 || maximumBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Maximum store size must support a one-byte probe.");
        }
        maximumStoreBytes = maximumBytes;
        codec = new ClaimStoreJsonCodec();
    }

    @Override
    public synchronized List<Claim> loadAll() throws ClaimStoreException {
        return List.copyOf(readCurrent());
    }

    @Override
    public synchronized SubmissionResult submit(Claim pending) throws ClaimStoreException {
        Objects.requireNonNull(pending, "pending");
        if (pending.status() != ClaimStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Only a Pending review Claim may be submitted.");
        }
        List<Claim> claims = new ArrayList<>(readCurrent());
        if (claims.stream().anyMatch(existing -> existing.claimId().equals(pending.claimId()))) {
            return new SubmissionResult(SubmissionOutcome.ID_COLLISION, Optional.empty());
        }
        ClaimLedger.SubmissionEvaluation evaluation = ClaimLedger.from(claims).evaluate(
                pending.claimantUserId(), pending.lostReportId(), pending.foundReportId());
        if (evaluation.eligibility() == ClaimLedger.SubmissionEligibility.OWN_ACTIVE_CLAIM) {
            return new SubmissionResult(SubmissionOutcome.OWN_ACTIVE_CLAIM,
                    evaluation.ownActiveClaim());
        }
        if (evaluation.eligibility() == ClaimLedger.SubmissionEligibility.BLOCKED) {
            return new SubmissionResult(SubmissionOutcome.BLOCKED, Optional.empty());
        }
        claims.add(pending);
        ClaimLedger.from(claims);
        writeCandidate(claims);
        return new SubmissionResult(SubmissionOutcome.CREATED, Optional.of(pending));
    }

    @Override
    public synchronized TerminalResult withdraw(ClaimId id, String claimantUserId,
            Instant terminalAt) throws ClaimStoreException {
        Objects.requireNonNull(claimantUserId, "claimantUserId");
        return transition(id, claim -> {
            if (!claim.claimantUserId().equals(claimantUserId)) {
                return Transition.denied();
            }
            return Transition.changed(claim.withdraw(terminalAt));
        });
    }

    @Override
    public synchronized TerminalResult approve(ClaimId id,
            Optional<String> decisionReason, Instant terminalAt) throws ClaimStoreException {
        Objects.requireNonNull(decisionReason, "decisionReason");
        return transition(id, claim -> Transition.changed(
                claim.approve(decisionReason, terminalAt)));
    }

    @Override
    public synchronized TerminalResult reject(ClaimId id, String decisionReason,
            Instant terminalAt) throws ClaimStoreException {
        Objects.requireNonNull(decisionReason, "decisionReason");
        return transition(id, claim -> Transition.changed(
                claim.reject(decisionReason, terminalAt)));
    }

    private TerminalResult transition(ClaimId id, TransitionCommand command)
            throws ClaimStoreException {
        Objects.requireNonNull(id, "claimId");
        List<Claim> claims = new ArrayList<>(readCurrent());
        int index = findIndex(claims, id);
        if (index < 0) {
            return new TerminalResult(TerminalOutcome.NOT_FOUND, Optional.empty());
        }
        Claim current = claims.get(index);
        if (current.status().isTerminal()) {
            return new TerminalResult(TerminalOutcome.ALREADY_TERMINAL,
                    Optional.of(current));
        }
        Transition transition = command.apply(current);
        if (transition.unauthorized()) {
            return new TerminalResult(TerminalOutcome.NOT_AUTHORIZED, Optional.empty());
        }
        Claim replacement = transition.replacement().orElseThrow();
        claims.set(index, replacement);
        ClaimLedger.from(claims);
        writeCandidate(claims);
        return new TerminalResult(TerminalOutcome.CHANGED, Optional.of(replacement));
    }

    private static int findIndex(List<Claim> claims, ClaimId id) {
        for (int index = 0; index < claims.size(); index++) {
            if (claims.get(index).claimId().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private List<Claim> readCurrent() throws ClaimStoreException {
        byte[] document;
        try {
            Optional<byte[]> stored = storeFiles.readBounded(
                    claimStorePath, maximumStoreBytes);
            if (stored.isEmpty()) {
                return List.of();
            }
            document = stored.orElseThrow();
        } catch (StoreFileFailure failure) {
            ClaimStoreException.Reason reason = failure.kind() == StoreFileFailure.Kind.OVER_LIMIT
                    ? ClaimStoreException.Reason.CORRUPT_STORE
                    : ClaimStoreException.Reason.READ_FAILURE;
            throw new ClaimStoreException(reason);
        }
        try {
            return codec.decode(document);
        } catch (ClaimStoreJsonCodec.InvalidStoreException failure) {
            throw new ClaimStoreException(failure.unsupportedVersion()
                    ? ClaimStoreException.Reason.UNSUPPORTED_VERSION
                    : ClaimStoreException.Reason.CORRUPT_STORE);
        }
    }

    private void writeCandidate(List<Claim> claims) throws ClaimStoreException {
        byte[] document;
        try {
            document = codec.encode(claims, maximumStoreBytes);
        } catch (ClaimStoreJsonCodec.InvalidClaimStateException failure) {
            throw new ClaimStoreException(ClaimStoreException.Reason.RESULT_TOO_LARGE);
        }
        try {
            storeFiles.replaceAtomically(claimStorePath, document);
        } catch (StoreFileFailure failure) {
            throw new ClaimStoreException(ClaimStoreException.Reason.WRITE_FAILURE);
        }
    }

    @FunctionalInterface
    private interface TransitionCommand {
        Transition apply(Claim current);
    }

    private record Transition(boolean unauthorized, Optional<Claim> replacement) {
        private static Transition denied() {
            return new Transition(true, Optional.empty());
        }

        private static Transition changed(Claim replacement) {
            return new Transition(false, Optional.of(replacement));
        }
    }
}
