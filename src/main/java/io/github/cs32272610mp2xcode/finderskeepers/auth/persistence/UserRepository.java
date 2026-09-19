package io.github.cs32272610mp2xcode.finderskeepers.auth.persistence;

import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;

/** Storage seam for local user accounts. */
public interface UserRepository {
    /**
     * Finds an account using case-insensitive username matching.
     *
     * @param username normalized username to find
     * @return matching account, when configured
     * @throws UserStoreException when storage is unreadable or corrupt
     */
    Optional<UserAccount> findByUsername(String username) throws UserStoreException;

    /**
     * Adds an account without replacing existing data.
     *
     * @param account account to add
     * @throws UserStoreException when storage cannot be safely updated
     */
    void add(UserAccount account) throws UserStoreException;
}
