package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

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

final class NioReportStoreFiles implements ReportStoreFiles {
    private static final int READ_BUFFER_BYTES = 8192;

    @Override
    public Optional<byte[]> readBounded(Path target, int maximumBytes) throws StoreFileFailure {
        if (Files.notExists(target, NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(target, NOFOLLOW_LINKS)) {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }

        try (InputStream input = Files.newInputStream(target, NOFOLLOW_LINKS);
                ByteArrayOutputStream output = new ByteArrayOutputStream(
                        Math.min(maximumBytes, READ_BUFFER_BYTES))) {
            byte[] buffer = new byte[READ_BUFFER_BYTES];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > maximumBytes - total) {
                    throw new StoreFileFailure(StoreFileFailure.Kind.OVER_LIMIT);
                }
                output.write(buffer, 0, count);
                total += count;
            }
            return Optional.of(output.toByteArray());
        } catch (StoreFileFailure failure) {
            throw failure;
        } catch (IOException | SecurityException failure) {
            throw new StoreFileFailure(StoreFileFailure.Kind.ACCESS);
        }
    }

    @Override
    public void replaceAtomically(Path target, byte[] completeDocument) throws StoreFileFailure {
        Path parent = target.getParent();
        Path temporary = null;
        boolean committed = false;
        try {
            Files.createDirectories(parent);
            if (Files.exists(target, NOFOLLOW_LINKS) && !Files.isRegularFile(target, NOFOLLOW_LINKS)) {
                throw new IOException("Target is not a regular file");
            }
            temporary = Files.createTempFile(parent, ".report-store-", ".tmp");
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
                    // The target remains authoritative; orphan cleanup is best effort.
                }
            }
        }
    }
}
