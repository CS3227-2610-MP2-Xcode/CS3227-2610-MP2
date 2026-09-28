package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

/** Authenticated, minimal read boundary for a Student's approved Claims. */
public final class StudentApprovedClaimService {
    private final AuthenticatedUser user;

    private final ClaimRepository repository;

    private final Optional<ReportRepository> reports;

    /**
     * Creates the read boundary for one authenticated Student.
     *
     * @param authenticatedUser authenticated Student
     * @param claims authoritative Claim repository
     */
    public StudentApprovedClaimService(AuthenticatedUser authenticatedUser,
            ClaimRepository claims) {
        this(authenticatedUser, claims, null);
    }

    /**
     * Creates the read boundary with safe item labels for appointment UI.
     *
     * @param authenticatedUser authenticated Student
     * @param claims authoritative Claim repository
     * @param reportRepository shared report repository used only for safe labels
     */
    public StudentApprovedClaimService(AuthenticatedUser authenticatedUser,
            ClaimRepository claims, ReportRepository reportRepository) {
        user = requireStudent(authenticatedUser);
        repository = Objects.requireNonNull(claims, "claims");
        reports = Optional.ofNullable(reportRepository);
    }

    /**
     * Loads the user's currently approved Claims without exposing Claim data.
     *
     * @return immutable newest-approved-first projections
     * @throws ClaimStoreException when authoritative data is unavailable
     */
    public List<ApprovedClaimSummary> loadApprovedClaims() throws ClaimStoreException {
        Map<UUID, ItemReport> reportsById = loadReports();
        return repository.loadAll().stream()
                .filter(claim -> claim.claimantUserId().equals(user.userId()))
                .filter(claim -> claim.status() == ClaimStatus.APPROVED)
                .map(claim -> {
                    ItemReport found = reportsById.get(claim.foundReportId());
                    return new ApprovedClaimSummary(claim.claimId(), claim.claimId().reference(),
                            Optional.ofNullable(found).map(ItemReport::itemName),
                            Optional.ofNullable(found).map(ItemReport::category),
                            claim.terminalAt().orElseThrow());
                })
                .sorted(Comparator.comparing(ApprovedClaimSummary::approvedAt).reversed()
                        .thenComparing(summary -> summary.claimId().value()))
                .toList();
    }

    private Map<UUID, ItemReport> loadReports() {
        if (reports.isEmpty()) {
            return Map.of();
        }
        try {
            Map<UUID, ItemReport> indexed = new HashMap<>();
            for (ItemReport report : reports.orElseThrow().loadAll()) {
                indexed.put(report.reportId(), report);
            }
            return Map.copyOf(indexed);
        } catch (ReportStoreException failure) {
            // Claim eligibility remains authoritative; only the optional UI label is unavailable.
            return Map.of();
        }
    }

    private static AuthenticatedUser requireStudent(AuthenticatedUser candidate) {
        AuthenticatedUser authenticatedUser = Objects.requireNonNull(candidate, "user");
        if (authenticatedUser.role() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Approved Claim lookup requires a Student identity");
        }
        return authenticatedUser;
    }
}
