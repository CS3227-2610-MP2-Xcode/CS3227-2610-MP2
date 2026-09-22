package io.github.cs32272610mp2xcode.finderskeepers.claim.bootstrap;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.FilePossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.JsonReportRepository;

class ClaimWorkspaceFactoryTest {
    private static final AuthenticatedUser STUDENT = new AuthenticatedUser(
            "student-synthetic-001", "synthetic.student", UserRole.STUDENT);

    private static final AuthenticatedUser DESK_OFFICER = new AuthenticatedUser(
            "officer-synthetic-001", "synthetic.officer", UserRole.DESK_OFFICER);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void roleSpecificFeaturesRejectTheWrongAuthenticatedRoleBeforeCreatingUi() {
        ClaimWorkspaceFactory factory = new ClaimWorkspaceFactory(
                new JsonClaimRepository(temporaryDirectory.resolve("claims.json")),
                new JsonReportRepository(temporaryDirectory.resolve("reports.json")),
                new FilePossibleMatchRepository(
                        temporaryDirectory.resolve("possible-match-links.txt")),
                Clock.systemUTC(), UUID::randomUUID);

        assertThrows(IllegalArgumentException.class,
                () -> factory.createStudentFeature(DESK_OFFICER));
        assertThrows(IllegalArgumentException.class,
                () -> factory.createDeskOfficerFeature(STUDENT));
    }
}
