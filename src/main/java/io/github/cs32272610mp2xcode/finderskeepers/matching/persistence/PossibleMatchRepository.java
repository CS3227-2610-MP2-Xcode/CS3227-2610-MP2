package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import java.util.Set;

import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;

/** Durable storage boundary for officer-created possible-match relationships. */
public interface PossibleMatchRepository {
    /**
     * Loads the complete current relationship set.
     *
     * @return immutable current pairs
     * @throws PossibleMatchStoreException when storage cannot be read safely
     */
    Set<PossibleMatchPair> loadAll() throws PossibleMatchStoreException;

    /**
     * Adds an absent pair durably, returning whether storage changed.
     *
     * @param pair canonical pair to add
     * @return true only after adding and committing an absent pair
     * @throws PossibleMatchStoreException when storage cannot be changed safely
     */
    boolean link(PossibleMatchPair pair) throws PossibleMatchStoreException;

    /**
     * Removes a present pair durably, returning whether storage changed.
     *
     * @param pair canonical pair to remove
     * @return true only after removing and committing a present pair
     * @throws PossibleMatchStoreException when storage cannot be changed safely
     */
    boolean unlink(PossibleMatchPair pair) throws PossibleMatchStoreException;
}
