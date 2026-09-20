package io.github.cs32272610mp2xcode.finderskeepers.auth.persistence;

/** Indicates that local account storage could not be read or written safely. */
public final class UserStoreException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a storage exception without credential details.
     *
     * @param message safe diagnostic message
     * @param cause underlying failure
     */
    public UserStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a storage exception without credential details.
     *
     * @param message safe diagnostic message
     */
    public UserStoreException(String message) {
        super(message);
    }
}
