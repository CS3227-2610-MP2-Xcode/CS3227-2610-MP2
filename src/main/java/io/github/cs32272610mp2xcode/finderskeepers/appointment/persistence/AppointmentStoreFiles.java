package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import java.nio.file.Path;
import java.util.Optional;

/** Filesystem seam used to test appointment-store safety without real I/O. */
interface AppointmentStoreFiles {
    Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure;

    void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure;
}
