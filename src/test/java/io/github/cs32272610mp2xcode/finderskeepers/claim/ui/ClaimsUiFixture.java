package io.github.cs32272610mp2xcode.finderskeepers.claim.ui;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.OfficerClaimsService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.application.StudentClaimsService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.FilePossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportType;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;

/** Synthetic durable Claims UI fixture isolated under a JUnit temporary directory. */
final class ClaimsUiFixture {
    static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    static final UUID LOST_ID = uuid(1);

    static final UUID FOUND_ID = uuid(2);

    static final ClaimId CLAIM_ID = ClaimId.of(uuid(3));

    private final JsonReportRepository reports;

    private final FilePossibleMatchRepository matches;

    private final JsonClaimRepository claims;

    ClaimsUiFixture(Path directory) {
        reports = new JsonReportRepository(directory.resolve("reports.json"));
        matches = new FilePossibleMatchRepository(directory.resolve("matches.txt"));
        claims = new JsonClaimRepository(directory.resolve("claims.json"));
    }

    JsonClaimRepository claims() {
        return claims;
    }

    void addLinkedReports() throws Exception {
        reports.insert(report(LOST_ID, "student-a", ReportType.LOST,
                "Blue bag", "Private lost mark"));
        reports.insert(report(FOUND_ID, "student-b", ReportType.FOUND,
                "Found blue bag", "Private found mark"));
        matches.link(PossibleMatchPair.of(LOST_ID, FOUND_ID));
    }

    void addPendingClaim() throws Exception {
        claims.submit(Claim.createPending(CLAIM_ID, "student-a", LOST_ID, FOUND_ID,
                "Synthetic ownership evidence", NOW.minusSeconds(1)));
    }

    StudentClaimsService student() {
        return new StudentClaimsService(new AuthenticatedUser("student-a", "student.a",
                UserRole.STUDENT), claims, reports, matches,
                Clock.fixed(NOW, ZoneOffset.UTC), () -> CLAIM_ID.value());
    }

    OfficerClaimsService officer() {
        return new OfficerClaimsService(new AuthenticatedUser("officer-a", "desk.a",
                UserRole.DESK_OFFICER), claims, reports, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ItemReport report(UUID id, String reporter, ReportType type,
            String name, String privateDetail) {
        return ItemReport.restore(id, reporter, type, name, ItemCategory.BAGS,
                "Library", LocalDate.of(2029, 12, 31), "Synthetic public description",
                privateDetail, ReportStatus.SUBMITTED, NOW.minusSeconds(30));
    }

    private static UUID uuid(long value) {
        return new UUID(0, value);
    }
}
