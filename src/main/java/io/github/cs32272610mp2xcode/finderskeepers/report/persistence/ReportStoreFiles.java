package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import java.nio.file.Path;
import java.util.Optional;

interface ReportStoreFiles {
    Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure;

    void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure;
}
