package io.github.cs32272610mp2xcode.finderskeepers.report;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/** Stable size and serialization constraints for item reports. */
public final class ReportConstraints {
    /** Maximum reporter-identifier length in Unicode code points. */
    public static final int MAX_REPORTER_ID_LENGTH = 128;

    /** Maximum item-name length in Unicode code points. */
    public static final int MAX_ITEM_NAME_LENGTH = 100;

    /** Maximum location length in Unicode code points. */
    public static final int MAX_LOCATION_LENGTH = 120;

    /** Maximum public-description length in Unicode code points. */
    public static final int MAX_PUBLIC_DESCRIPTION_LENGTH = 500;

    /** Maximum private-identifying-detail length in Unicode code points. */
    public static final int MAX_PRIVATE_IDENTIFYING_DETAIL_LENGTH = 500;

    /** Formatter for occurrence dates stored as ISO local dates. */
    public static final DateTimeFormatter OCCURRENCE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Formatter for UTC creation times with exactly millisecond precision. */
    public static final DateTimeFormatter CREATION_TIME_FORMAT =
            new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    private ReportConstraints() {
    }

    /**
     * Formats a creation time using the report storage contract.
     *
     * @param creationTime creation time to format
     * @return UTC timestamp with exactly three fractional digits
     */
    public static String formatCreationTime(Instant creationTime) {
        return CREATION_TIME_FORMAT.format(creationTime);
    }

    /**
     * Parses a creation time stored using the report storage contract.
     *
     * @param storedValue stored UTC timestamp
     * @return parsed creation time
     */
    public static Instant parseCreationTime(String storedValue) {
        return Instant.from(CREATION_TIME_FORMAT.parse(storedValue));
    }
}
