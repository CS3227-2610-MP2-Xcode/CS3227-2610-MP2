package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;

/** Authenticated, minimal read boundary for a Student's approved Claims. */
public final class StudentApprovedClaimService {
    private final AuthenticatedUser user;

    private final ClaimRepository repository;

    /**
     * Creates the read boundary for one authenticated Student.
     *
     * @param authenticatedUser authenticated Student
     * @param claims authoritative Claim repository
     */
    public StudentApprovedClaimService(AuthenticatedUser authenticatedUser,
            ClaimRepository claims) {
        user = requireStudent(authenticatedUser);
        repository = Objects.requireNonNull(claims, "claims");
    }

    /**
     * Loads the user's currently approved Claims without exposing Claim data.
     *
     * @return immutable newest-approved-first projections
     * @throws ClaimStoreException when authoritative data is unavailable
     */
    public List<ApprovedClaimSummary> loadApprovedClaims() throws ClaimStoreException {
        return repository.loadAll().stream()
                .filter(claim -> claim.claimantUserId().equals(user.userId()))
                .filter(claim -> claim.status() == ClaimStatus.APPROVED)
                .map(claim -> new ApprovedClaimSummary(claim.claimId(),
                        claim.claimId().reference(), claim.terminalAt().orElseThrow()))
                .sorted(Comparator.comparing(ApprovedClaimSummary::approvedAt).reversed()
                        .thenComparing(summary -> summary.claimId().value()))
                .toList();
    }

    private static AuthenticatedUser requireStudent(AuthenticatedUser candidate) {
        AuthenticatedUser authenticatedUser = Objects.requireNonNull(candidate, "user");
        if (authenticatedUser.role() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Approved Claim lookup requires a Student identity");
        }
        return authenticatedUser;
    }
}
