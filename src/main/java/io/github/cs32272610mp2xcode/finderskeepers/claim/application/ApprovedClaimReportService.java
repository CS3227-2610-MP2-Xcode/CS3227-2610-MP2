package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;

/** Supplies report endpoints closed by approved Claims without exposing persistence details. */
public final class ApprovedClaimReportService {
    private final ClaimRepository repository;

    /**
     * Creates the read service over the shared Claim repository.
     *
     * @param claims canonical Claim persistence boundary
     */
    public ApprovedClaimReportService(ClaimRepository claims) {
        repository = Objects.requireNonNull(claims, "claims");
    }

    /**
     * Returns both report endpoints of every approved Claim.
     *
     * @return immutable set of approved Claim endpoint identifiers
     * @throws ClaimStoreException when authoritative Claim data is unavailable
     */
    public Set<UUID> loadApprovedReportIds() throws ClaimStoreException {
        return repository.loadAll().stream()
                .filter(claim -> claim.status() == ClaimStatus.APPROVED)
                .flatMap(claim -> Stream.of(claim.lostReportId(), claim.foundReportId()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
