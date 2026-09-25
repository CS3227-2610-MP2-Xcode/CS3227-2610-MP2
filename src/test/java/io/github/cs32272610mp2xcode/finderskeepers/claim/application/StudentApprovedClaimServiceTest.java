package io.github.cs32272610mp2xcode.finderskeepers.claim.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.AuthenticatedUser;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimRepository;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.JsonClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudentApprovedClaimServiceTest {
    private static final Instant BASE = Instant.parse("2030-01-01T00:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void returnsOnlyOwnedApprovedClaimsAsSafeSummaries() throws ClaimStoreException {
        ClaimRepository repository = new JsonClaimRepository(
                temporaryDirectory.resolve("claims.json"));
        Claim owned = pending(1, "student-1");
        Claim other = pending(2, "student-2");
        repository.submit(owned);
        repository.submit(other);
        repository.approve(owned.claimId(), Optional.empty(), BASE.plusSeconds(10));
        repository.approve(other.claimId(), Optional.empty(), BASE.plusSeconds(20));

        List<ApprovedClaimSummary> result = new StudentApprovedClaimService(
                new AuthenticatedUser("student-1", "student", UserRole.STUDENT), repository)
                .loadApprovedClaims();

        assertEquals(List.of(owned.claimId()), result.stream().map(ApprovedClaimSummary::claimId)
                .toList());
        assertEquals(owned.claimId().reference(), result.get(0).claimReference());
        assertEquals("ApprovedClaimSummary[redacted]", result.get(0).toString());
    }

    @Test
    void rejectsDeskOfficerIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new StudentApprovedClaimService(
                new AuthenticatedUser("officer-1", "officer", UserRole.DESK_OFFICER),
                new JsonClaimRepository(temporaryDirectory.resolve("claims.json"))));
    }

    private static Claim pending(long id, String student) {
        return Claim.createPending(ClaimId.of(uuid(id)), student, uuid(100 + id),
                uuid(200 + id), "Synthetic evidence.", BASE);
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
