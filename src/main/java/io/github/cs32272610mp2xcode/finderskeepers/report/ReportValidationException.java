package io.github.cs32272610mp2xcode.finderskeepers.report;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Indicates that one or more item-report fields are invalid. */
public final class ReportValidationException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Serializable defensive copy of the detected errors. */
    private final ValidationError[] errors;

    /**
     * Creates an exception containing every detected field error.
     *
     * @param detectedErrors non-empty validation-error list
     */
    public ReportValidationException(List<ValidationError> detectedErrors) {
        super(toMessage(detectedErrors));
        errors = detectedErrors.toArray(ValidationError[]::new);
    }

    /**
     * Returns an immutable list of field-specific errors.
     *
     * @return validation errors
     */
    public List<ValidationError> errors() {
        return List.of(errors.clone());
    }

    private static String toMessage(List<ValidationError> detectedErrors) {
        Objects.requireNonNull(detectedErrors, "detectedErrors");
        if (detectedErrors.isEmpty()) {
            throw new IllegalArgumentException("At least one validation error is required");
        }
        return detectedErrors.stream()
                .map(ValidationError::message)
                .collect(Collectors.joining(" "));
    }
}
