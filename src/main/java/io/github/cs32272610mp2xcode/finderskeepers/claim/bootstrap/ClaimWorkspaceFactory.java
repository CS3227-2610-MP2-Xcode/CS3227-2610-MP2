package io.github.cs32272610mp2xcode.finderskeepers.claim.bootstrap;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimReportService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentApprovedClaimService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.ui.DeskOfficerClaimsPane;
import io.github.cs32272610mp2xcode.finderskeepers.claim.ui.StudentClaimsPane;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.WorkspaceFeature;

/** Creates fresh per-login Claims features over shared application repositories. */
public final class ClaimWorkspaceFactory {
    private final ClaimRepository claimRepository;

    private final ReportRepository reportRepository;

    private final PossibleMatchRepository matchRepository;

    private final Clock clock;

    private final Supplier<UUID> claimIds;

    /**
     * Creates an application-lifetime Claims feature factory.
     *
     * @param claims one shared Claim repository instance
     * @param reports shared canonical report repository
     * @param matches shared durable possible-match repository
     * @param eventClock event clock
     * @param ids Claim UUID supplier
     */
    public ClaimWorkspaceFactory(ClaimRepository claims, ReportRepository reports,
            PossibleMatchRepository matches, Clock eventClock, Supplier<UUID> ids) {
        claimRepository = Objects.requireNonNull(claims, "claims");
        reportRepository = Objects.requireNonNull(reports, "reports");
        matchRepository = Objects.requireNonNull(matches, "matches");
        clock = Objects.requireNonNull(eventClock, "clock");
        claimIds = Objects.requireNonNull(ids, "ids");
    }

    /**
     * Creates one fresh authenticated Student Claims feature.
     *
     * @param user authenticated Student
     * @return neutral workspace feature
     */
    public WorkspaceFeature createStudentFeature(AuthenticatedUser user) {
        StudentClaimsPane pane = new StudentClaimsPane(new StudentClaimsService(user,
                claimRepository, reportRepository, matchRepository, clock, claimIds));
        return new WorkspaceFeature(pane, pane::enter, pane::hasUnsavedText,
                pane::clearSessionState);
    }

    /**
     * Creates the minimal approved-Claim read boundary for one Student.
     *
     * @param user authenticated Student
     * @return privacy-safe approved-Claim service
     */
    public StudentApprovedClaimService createApprovedClaimService(AuthenticatedUser user) {
        return new StudentApprovedClaimService(user, claimRepository, reportRepository);
    }

    /**
     * Creates one fresh authenticated Desk Officer Claims feature.
     *
     * @param user authenticated Desk Officer
     * @return Claims workspace and approved-report read contract
     */
    public DeskOfficerClaimFeature createDeskOfficerFeature(AuthenticatedUser user) {
        DeskOfficerClaimsPane pane = new DeskOfficerClaimsPane(
                new OfficerClaimsService(user, claimRepository, reportRepository, clock));
        WorkspaceFeature workspace = new WorkspaceFeature(pane, pane::enter,
                pane::hasUnsavedText, pane::clearSessionState);
        return new DeskOfficerClaimFeature(workspace,
                new ApprovedClaimReportService(claimRepository));
    }
}
