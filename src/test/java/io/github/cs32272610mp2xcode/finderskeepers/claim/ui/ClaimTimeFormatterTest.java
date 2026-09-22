package io.github.cs32272610mp2xcode.finderskeepers.claim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class ClaimTimeFormatterTest {
    @Test
    void formatsInExplicitUtcIndependentlyOfDefaultTimeZone() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
            assertEquals("2026-09-22 03:04:05.678 UTC",
                    ClaimTimeFormatter.format(
                            Instant.parse("2026-09-22T03:04:05.678Z")));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void rejectsMissingInstant() {
        assertThrows(NullPointerException.class, () -> ClaimTimeFormatter.format(null));
    }
}
