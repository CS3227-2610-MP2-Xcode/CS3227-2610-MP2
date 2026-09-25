package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.Stream;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimValidationException.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClaimTextPolicyTest {
    @Test
    void acceptsTrimsAndPreservesApprovedText() {
        assertEquals("Synthetic punctuation!\nSecond line.",
                ClaimTextPolicy.evidence("  Synthetic punctuation!\nSecond line.  "));
        assertEquals("First\r\nSecond", ClaimTextPolicy.requiredDecisionReason("First\r\nSecond"));
        assertEquals("🙂".repeat(500), ClaimTextPolicy.evidence("🙂".repeat(500)));
        assertEquals(Optional.empty(), ClaimTextPolicy.optionalDecisionReason(Optional.of(" \n ")));
        assertEquals(Optional.of("Synthetic reason."),
                ClaimTextPolicy.optionalDecisionReason(Optional.of(" Synthetic reason. ")));
    }

    @ParameterizedTest(name = "invalid partition {index}")
    @MethodSource("invalidText")
    void rejectsInvalidTextWithoutRepeatingIt(String invalid) {
        ClaimValidationException failure = assertThrows(
                ClaimValidationException.class, () -> ClaimTextPolicy.evidence(invalid));

        assertEquals(Field.EVIDENCE, failure.field());
        if (invalid != null && !invalid.isEmpty()) {
            assertTrue(!failure.getMessage().contains(invalid));
        }
    }

    @Test
    void rejectsOverLimitAndUsesDecisionReasonCategory() {
        ClaimValidationException failure = assertThrows(ClaimValidationException.class,
                () -> ClaimTextPolicy.requiredDecisionReason("x".repeat(501)));

        assertEquals(Field.DECISION_REASON, failure.field());
    }

    private static Stream<Arguments> invalidText() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of(" \n "),
                Arguments.of("x".repeat(501)),
                Arguments.of("line\rbreak"),
                Arguments.of("tab\tbreak"),
                Arguments.of("format\u200Bcharacter"),
                Arguments.of("unpaired\uD800"));
    }
}
