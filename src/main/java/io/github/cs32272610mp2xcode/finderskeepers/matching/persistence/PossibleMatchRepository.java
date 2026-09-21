package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import java.util.Set;

import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;

/** Durable storage boundary for officer-created possible-match relationships. */
public interface PossibleMatchRepository {
    /** Loads the complete current relationship set. */
    Set<PossibleMatchPair> loadAll() throws PossibleMatchStoreException;

    /** Adds an absent pair durably, returning whether storage changed. */
    boolean link(PossibleMatchPair pair) throws PossibleMatchStoreException;

    /** Removes a present pair durably, returning whether storage changed. */
    boolean unlink(PossibleMatchPair pair) throws PossibleMatchStoreException;
}
