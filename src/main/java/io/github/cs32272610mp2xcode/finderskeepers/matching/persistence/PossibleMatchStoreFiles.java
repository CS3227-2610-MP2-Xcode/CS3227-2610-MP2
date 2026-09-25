package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import java.nio.file.Path;
import java.util.Optional;

interface PossibleMatchStoreFiles {
    Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure;

    void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure;
}
