package io.github.cs32272610mp2xcode.finderskeepers.matching.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PossibleMatchPairTest {
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private static final UUID C = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    void canonicalizesSymmetricIdentityAndSupportsSharedEndpoints() {
        PossibleMatchPair forward = PossibleMatchPair.of(A, B);
        PossibleMatchPair reverse = PossibleMatchPair.of(B, A);

        assertEquals(forward, reverse);
        assertEquals(forward.hashCode(), reverse.hashCode());
        assertEquals(A, forward.firstId());
        assertEquals(B, forward.secondId());
        assertNotEquals(forward, PossibleMatchPair.of(A, C));
    }

    @Test
    void rejectsAbsentAndSelfIdentifiers() {
        assertThrows(NullPointerException.class, () -> PossibleMatchPair.of(null, A));
        assertThrows(NullPointerException.class, () -> PossibleMatchPair.of(A, null));
        assertThrows(IllegalArgumentException.class, () -> PossibleMatchPair.of(A, A));
    }

    @Test
    void comparatorUsesCanonicalUuidStringTuple() {
        List<PossibleMatchPair> pairs = new java.util.ArrayList<>(List.of(
                PossibleMatchPair.of(B, C), PossibleMatchPair.of(A, C),
                PossibleMatchPair.of(A, B)));

        pairs.sort(PossibleMatchPair.CANONICAL_ORDER);

        assertEquals(List.of(PossibleMatchPair.of(A, B),
                PossibleMatchPair.of(A, C), PossibleMatchPair.of(B, C)), pairs);
    }
}
