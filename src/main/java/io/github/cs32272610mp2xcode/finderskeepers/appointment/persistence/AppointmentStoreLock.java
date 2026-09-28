package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.application.AppointmentDiagnostics;

/** Cross-thread and cross-process lock for one appointment store. */
final class AppointmentStoreLock {
    private static final ConcurrentHashMap<Path, ReentrantLock> JVM_LOCKS =
            new ConcurrentHashMap<>();

    private AppointmentStoreLock() {
    }

    static Guard acquire(Path storePath) throws AppointmentStoreException {
        ReentrantLock jvmLock;
        Path lockPath;
        try {
            Path parent = storePath.getParent();
            Files.createDirectories(parent);
            Path realParent = parent.toRealPath();
            String fileName = storePath.getFileName().toString() + ".lock";
            Path requestedLockPath = realParent.resolve(fileName);
            try {
                Files.createFile(requestedLockPath);
            } catch (FileAlreadyExistsException alreadyExists) {
                // Persistent sidecar already coordinates this store.
            }
            if (!Files.isRegularFile(requestedLockPath, NOFOLLOW_LINKS)) {
                throw lockFailure();
            }
            lockPath = requestedLockPath.toRealPath(NOFOLLOW_LINKS);
            jvmLock = JVM_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        } catch (IOException | SecurityException failure) {
            throw lockFailure();
        }

        jvmLock.lock();
        FileChannel channel = null;
        try {
            if (!Files.isRegularFile(lockPath, NOFOLLOW_LINKS)) {
                throw lockFailure();
            }
            channel = FileChannel.open(lockPath, WRITE, NOFOLLOW_LINKS);
            FileLock fileLock = channel.lock();
            return new Guard(jvmLock, channel, fileLock);
        } catch (AppointmentStoreException failure) {
            closeQuietly(channel);
            jvmLock.unlock();
            throw failure;
        } catch (IOException | SecurityException | UnsupportedOperationException
                | OverlappingFileLockException failure) {
            closeQuietly(channel);
            jvmLock.unlock();
            throw lockFailure();
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // The acquisition failure remains authoritative.
            }
        }
    }

    private static AppointmentStoreException lockFailure() {
        AppointmentDiagnostics.failure("lock", AppointmentStoreException.Reason.LOCK_FAILURE);
        return new AppointmentStoreException(AppointmentStoreException.Reason.LOCK_FAILURE);
    }

    /** Holds both the process-local and filesystem locks until the command completes. */
    static final class Guard implements AutoCloseable {
        private final ReentrantLock jvmLock;

        private final FileChannel channel;

        private final FileLock fileLock;

        private boolean closed;

        Guard(ReentrantLock processLock, FileChannel lockChannel, FileLock acquiredFileLock) {
            jvmLock = processLock;
            channel = lockChannel;
            fileLock = acquiredFileLock;
        }

        void ensureHeld() throws AppointmentStoreException {
            if (!fileLock.isValid()) {
                throw lockFailure();
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            IOException failure = null;
            try {
                fileLock.release();
            } catch (IOException releaseFailure) {
                failure = releaseFailure;
            }
            try {
                channel.close();
            } catch (IOException closeFailure) {
                if (failure == null) {
                    failure = closeFailure;
                }
            } finally {
                jvmLock.unlock();
            }
            if (failure != null) {
                AppointmentDiagnostics.failure("unlock",
                        AppointmentStoreException.Reason.LOCK_FAILURE);
            }
        }
    }
}
