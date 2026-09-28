package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

/** Internal filesystem failure classified before becoming a public store error. */
final class StoreFileFailure extends Exception {
    private static final long serialVersionUID = 1L;

    enum Kind { ACCESS, OVER_LIMIT }

    private final Kind failureKind;

    StoreFileFailure(Kind kind) {
        failureKind = kind;
    }

    Kind kind() {
        return failureKind;
    }
}
