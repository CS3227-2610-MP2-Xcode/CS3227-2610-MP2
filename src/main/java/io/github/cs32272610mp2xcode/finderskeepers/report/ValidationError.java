package io.github.cs32272610mp2xcode.finderskeepers.report;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * One readable validation problem associated with a report field.
 *
 * @param field stable field name for presentation mapping
 * @param message user-readable problem description
 */
public record ValidationError(String field, String message) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Validates an error before it is exposed to callers. */
    public ValidationError {
        field = requireText(field, "field");
        message = requireText(message, "message");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return stripped;
    }
}
