package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimValidationException.Field;

/** Shared normalization and validation policy for Claim text. */
public final class ClaimTextPolicy {
    /** Maximum accepted length after trimming, in Unicode code points. */
    public static final int MAXIMUM_CODE_POINTS = 500;

    private static final String EVIDENCE_MESSAGE =
            "Ownership evidence must contain 1 to 500 visible characters.";

    private static final String REASON_MESSAGE =
            "The decision reason must contain 1 to 500 visible characters.";

    private ClaimTextPolicy() {
    }

    /**
     * Normalizes required ownership evidence.
     *
     * @param rawValue raw evidence
     * @return normalized evidence
     */
    public static String evidence(String rawValue) {
        return required(rawValue, Field.EVIDENCE, EVIDENCE_MESSAGE);
    }

    /**
     * Normalizes a mandatory rejection reason.
     *
     * @param rawValue raw rejection reason
     * @return normalized reason
     */
    public static String requiredDecisionReason(String rawValue) {
        return required(rawValue, Field.DECISION_REASON, REASON_MESSAGE);
    }

    /**
     * Normalizes optional approval text, treating blank text as absent.
     *
     * @param rawValue raw optional reason, possibly absent
     * @return normalized optional reason
     */
    public static Optional<String> optionalDecisionReason(Optional<String> rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return Optional.empty();
        }
        String stripped = rawValue.orElseThrow().strip();
        if (stripped.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(required(stripped, Field.DECISION_REASON, REASON_MESSAGE));
    }

    private static String required(String rawValue, Field field, String message) {
        if (rawValue == null) {
            throw new ClaimValidationException(field, message);
        }
        String value = rawValue.strip();
        if (value.isEmpty()
                || value.codePointCount(0, value.length()) > MAXIMUM_CODE_POINTS
                || containsForbiddenCodePoint(value)
                || !containsVisibleCodePoint(value)) {
            throw new ClaimValidationException(field, message);
        }
        return value;
    }

    private static boolean containsForbiddenCodePoint(String value) {
        for (int offset = 0; offset < value.length();) {
            char current = value.charAt(offset);
            if (Character.isSurrogate(current)) {
                if (!Character.isHighSurrogate(current)
                        || offset + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(offset + 1))) {
                    return true;
                }
            }
            int codePoint = value.codePointAt(offset);
            if (codePoint == '\r') {
                if (offset + 1 >= value.length() || value.charAt(offset + 1) != '\n') {
                    return true;
                }
            } else if (codePoint != '\n' && Character.isISOControl(codePoint)) {
                return true;
            }
            if (Character.getType(codePoint) == Character.FORMAT) {
                return true;
            }
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    private static boolean containsVisibleCodePoint(String value) {
        return value.codePoints().anyMatch(codePoint -> !Character.isWhitespace(codePoint)
                && !Character.isISOControl(codePoint)
                && Character.getType(codePoint) != Character.FORMAT);
    }
}
