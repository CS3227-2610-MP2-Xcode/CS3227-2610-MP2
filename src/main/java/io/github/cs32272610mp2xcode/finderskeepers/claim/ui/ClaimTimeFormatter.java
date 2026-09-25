package io.github.cs32272610mp2xcode.finderskeepers.claim.ui;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Formats every Claim event time in fixed, explicitly labelled UTC. */
public final class ClaimTimeFormatter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss.SSS 'UTC'")
            .withZone(ZoneOffset.UTC);

    private ClaimTimeFormatter() {
    }

    /**
     * Formats one event instant without consulting the machine default zone.
     *
     * @param instant event time
     * @return fixed UTC display value
     */
    public static String format(Instant instant) {
        return FORMATTER.format(Objects.requireNonNull(instant, "instant"));
    }
}
