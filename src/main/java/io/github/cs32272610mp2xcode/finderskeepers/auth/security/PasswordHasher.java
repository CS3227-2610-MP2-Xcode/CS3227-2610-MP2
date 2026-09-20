package io.github.cs32272610mp2xcode.finderskeepers.auth.security;

/** Replaceable seam for password hashing and verification. */
public interface PasswordHasher {
    /**
     * Hashes a password with a new unique salt.
     *
     * @param password password characters
     * @return password-verification material
     */
    PasswordCredential hash(char[] password);

    /**
     * Verifies a password against stored material.
     *
     * @param password candidate password characters
     * @param credential stored material
     * @return whether the password matches
     */
    boolean verify(char[] password, PasswordCredential credential);
}
