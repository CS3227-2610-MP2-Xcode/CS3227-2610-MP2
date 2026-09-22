package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import java.nio.file.Path;
import java.util.Optional;

interface ClaimStoreFiles {
    Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure;

    void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure;
}
