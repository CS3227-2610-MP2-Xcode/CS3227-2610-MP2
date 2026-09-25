package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

final class StoreFileFailure extends Exception {
    private static final long serialVersionUID = 1L;

    private final Kind kind;

    StoreFileFailure(Kind failureKind) {
        kind = failureKind;
    }

    Kind kind() {
        return kind;
    }

    enum Kind {
        ACCESS,
        OVER_LIMIT
    }
}
