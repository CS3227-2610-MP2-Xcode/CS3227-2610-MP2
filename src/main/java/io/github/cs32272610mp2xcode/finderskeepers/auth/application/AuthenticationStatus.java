package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

/** Outcome categories for a local authentication attempt. */
public enum AuthenticationStatus {
    /** Credentials were verified. */
    SUCCESS,

    /** Credentials were blank or invalid. */
    INVALID_CREDENTIALS,

    /** The local account store could not be safely read. */
    STORAGE_ERROR
}
