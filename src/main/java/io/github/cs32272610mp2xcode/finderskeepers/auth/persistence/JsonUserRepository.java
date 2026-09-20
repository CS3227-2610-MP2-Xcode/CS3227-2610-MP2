package io.github.cs32272610mp2xcode.finderskeepers.auth.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;

/** JSON-backed local user repository whose storage path is supplied by its caller. */
public final class JsonUserRepository implements UserRepository {
    /** Maximum supported account-store size in bytes. */
    public static final int MAX_STORE_BYTES = 1_048_576;

    private static final int BOUNDED_READ_BYTES = MAX_STORE_BYTES + 1;

    private final Path storagePath;

    /**
     * Creates a repository at the supplied path.
     *
     * @param accountStorePath local JSON account store
     */
    public JsonUserRepository(Path accountStorePath) {
        storagePath = Objects.requireNonNull(accountStorePath, "accountStorePath").toAbsolutePath();
    }

    @Override
    public synchronized Optional<UserAccount> findByUsername(String username)
            throws UserStoreException {
        Objects.requireNonNull(username, "username");
        String lookup = username.toLowerCase(Locale.ROOT);
        return readAll().stream()
                .filter(account -> account.username().toLowerCase(Locale.ROOT).equals(lookup))
                .findFirst();
    }

    @Override
    public synchronized void add(UserAccount account) throws UserStoreException {
        Objects.requireNonNull(account, "account");
        List<UserAccount> accounts = new ArrayList<>(readAll());
        boolean duplicateId = accounts.stream()
                .anyMatch(existing -> existing.userId().equalsIgnoreCase(account.userId()));
        boolean duplicateUsername = accounts.stream()
                .anyMatch(existing -> existing.username().equalsIgnoreCase(account.username()));
        if (duplicateId || duplicateUsername) {
            throw new UserStoreException(
                    "A local account with that identifier or username already exists");
        }
        accounts.add(account);
        writeAtomically(accounts);
    }

    private List<UserAccount> readAll() throws UserStoreException {
        if (Files.notExists(storagePath)) {
            return List.of();
        }
        if (!Files.isRegularFile(storagePath) || !Files.isReadable(storagePath)) {
            throw new UserStoreException("Local account store is not readable");
        }
        try {
            String content = readBoundedUtf8();
            return List.copyOf(UserStoreJsonCodec.decode(content));
        } catch (IOException | IllegalArgumentException exception) {
            throw new UserStoreException("Local account store is corrupt or unreadable", exception);
        }
    }

    private String readBoundedUtf8() throws IOException {
        byte[] encoded;
        try (InputStream input = Files.newInputStream(storagePath)) {
            encoded = input.readNBytes(BOUNDED_READ_BYTES);
        }
        if (encoded.length > MAX_STORE_BYTES) {
            throw new IllegalArgumentException("Local account store exceeds the size limit");
        }
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(encoded))
                .toString();
    }

    private void writeAtomically(List<UserAccount> accounts) throws UserStoreException {
        Path parent = storagePath.getParent();
        if (parent == null) {
            throw new UserStoreException("Local account store has no parent directory");
        }
        byte[] encoded = encodeBounded(accounts);
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, ".users-", ".tmp");
            Files.write(temporary, encoded);
            try {
                Files.move(temporary, storagePath,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, storagePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new UserStoreException("Local account store could not be updated", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    // The account operation has already completed or reported its primary failure.
                }
            }
        }
    }

    private static byte[] encodeBounded(List<UserAccount> accounts) throws UserStoreException {
        long identityCharacters = 0;
        for (UserAccount account : accounts) {
            identityCharacters += account.userId().length();
            identityCharacters += account.username().length();
            if (identityCharacters > MAX_STORE_BYTES) {
                throw new UserStoreException("Local account store exceeds the size limit");
            }
        }
        String json = UserStoreJsonCodec.encode(accounts);
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(json));
            if (buffer.remaining() > MAX_STORE_BYTES) {
                throw new UserStoreException("Local account store exceeds the size limit");
            }
            byte[] encoded = new byte[buffer.remaining()];
            buffer.get(encoded);
            return encoded;
        } catch (CharacterCodingException exception) {
            throw new UserStoreException("Local account store contains invalid text", exception);
        }
    }
}
