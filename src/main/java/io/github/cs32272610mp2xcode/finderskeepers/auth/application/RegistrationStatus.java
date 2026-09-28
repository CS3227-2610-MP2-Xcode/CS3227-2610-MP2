package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

/** Outcome categories for local self-registration. */
public enum RegistrationStatus {
    /** A new account was stored and authenticated. */
    SUCCESS,

    /** The username was absent or blank. */
    INVALID_USERNAME,

    /** The password was absent or blank. */
    INVALID_PASSWORD,

    /** The confirmation did not match the password. */
    PASSWORD_MISMATCH,

    /** No supported role was selected. */
    INVALID_ROLE,

    /** The case-insensitive username already belongs to an account. */
    USERNAME_TAKEN,

    /** The local account store could not be read or updated safely. */
    STORAGE_ERROR
}
