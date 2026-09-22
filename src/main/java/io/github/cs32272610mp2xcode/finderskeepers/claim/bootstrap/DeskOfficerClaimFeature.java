package io.github.cs32272610mp2xcode.finderskeepers.claim.bootstrap;

import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimReportService;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.WorkspaceFeature;

/**
 * Desk Officer Claims UI together with its narrow report-review read contract.
 *
 * @param workspace neutral Claims workspace feature
 * @param approvedClaimReports approved-Claim endpoint read service
 */
public record DeskOfficerClaimFeature(
        WorkspaceFeature workspace,
        ApprovedClaimReportService approvedClaimReports) {
    /** Keeps both feature components non-null. */
    public DeskOfficerClaimFeature {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(approvedClaimReports, "approvedClaimReports");
    }
}
