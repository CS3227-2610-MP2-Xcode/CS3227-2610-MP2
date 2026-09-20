package io.github.cs32272610mp2xcode.finderskeepers.auth.security;

/** Shared rules for accepting password character arrays without copying them to strings. */
public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    /**
     * Checks whether a password is absent, empty, or entirely whitespace.
     *
     * @param password password characters
     * @return whether the password is blank
     */
    public static boolean isBlank(char[] password) {
        if (password == null || password.length == 0) {
            return true;
        }
        for (char character : password) {
            if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requires a nonblank password without trimming it.
     *
     * @param password password characters
     * @throws IllegalArgumentException when the password is blank
     */
    public static void requireNonBlank(char[] password) {
        if (isBlank(password)) {
            throw new IllegalArgumentException("password must not be blank");
        }
    }
}
