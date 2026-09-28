package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import java.nio.file.Path;
import java.util.Optional;

/** Filesystem seam used to test possible-match storage without real I/O. */
interface PossibleMatchStoreFiles {
    /**
     * Reads a store only when it is a regular file within the configured size limit.
     *
     * @param target store path
     * @param maximumBytes largest accepted document size
     * @return the document, or an empty value when the store does not exist
     * @throws StoreFileFailure if the path is unsafe, inaccessible, or too large
     */
    Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure;

    /**
     * Replaces a store with a complete document using an atomic filesystem move.
     *
     * @param target store path
     * @param completeDocument complete serialized store
     * @throws StoreFileFailure if the document cannot be committed safely
     */
    void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure;
}
