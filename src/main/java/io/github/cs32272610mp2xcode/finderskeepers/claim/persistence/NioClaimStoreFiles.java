package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class NioClaimStoreFiles implements ClaimStoreFiles {
    private static final int READ_BUFFER_BYTES = 8192;

    @Override
    public Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure {
        try {
            if (Files.notExists(target, NOFOLLOW_LINKS)) {
                verifyMissingPathAncestors(target.getParent());
                return Optional.empty();
            }
            if (!Files.exists(target, NOFOLLOW_LINKS)
                    || !Files.isRegularFile(target, NOFOLLOW_LINKS)) {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
        } catch (StoreFileFailure failure) {
            throw failure;
        } catch (SecurityException failure) {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }

        try (InputStream input = Files.newInputStream(target, NOFOLLOW_LINKS);
                ByteArrayOutputStream output = new ByteArrayOutputStream(
                        Math.min(maximumBytes, READ_BUFFER_BYTES))) {
            byte[] buffer = new byte[READ_BUFFER_BYTES];
            int total = 0;
            int count;
            while ((count = input.read(
                    buffer, 0, Math.min(buffer.length, maximumBytes - total + 1))) != -1) {
                total += count;
                if (total > maximumBytes) {
                    throw new StoreFileFailure(StoreFileFailure.Kind.OVER_LIMIT);
                }
                output.write(buffer, 0, count);
            }
            return Optional.of(output.toByteArray());
        } catch (StoreFileFailure failure) {
            throw failure;
        } catch (IOException | SecurityException failure) {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }
    }

    private static void verifyMissingPathAncestors(Path parent) throws StoreFileFailure {
        Path current = parent;
        while (current != null) {
            if (Files.exists(current, NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(current, NOFOLLOW_LINKS)) {
                    throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
                }
                return;
            }
            if (!Files.notExists(current, NOFOLLOW_LINKS)) {
                throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
            }
            current = current.getParent();
        }
    }

    @Override
    public void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure {
        Path parent = target.getParent();
        Path temporary = null;
        boolean committed = false;
        try {
            Files.createDirectories(parent);
            if (Files.exists(target, NOFOLLOW_LINKS)
                    && !Files.isRegularFile(target, NOFOLLOW_LINKS)) {
                throw new IOException("Target is not a regular file");
            }
            temporary = Files.createTempFile(parent, ".claim-store-", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, WRITE, TRUNCATE_EXISTING)) {
                ByteBuffer remaining = ByteBuffer.wrap(completeDocument);
                while (remaining.hasRemaining()) {
                    channel.write(remaining);
                }
                channel.force(true);
            }
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING);
            committed = true;
        } catch (IOException | SecurityException failure) {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        } finally {
            if (!committed && temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException ignored) {
                    // The target remains authoritative; cleanup is best effort.
                }
            }
        }
    }
}
