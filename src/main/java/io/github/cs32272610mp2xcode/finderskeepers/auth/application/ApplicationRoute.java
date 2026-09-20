package io.github.cs32272610mp2xcode.finderskeepers.auth.application;

/** Authentication-controlled application destinations. */
public enum ApplicationRoute {
    /** Login interface shown without an authenticated session. */
    LOGIN,

    /** Student interface. */
    STUDENT,

    /** Desk Officer interface. */
    DESK_OFFICER
}
