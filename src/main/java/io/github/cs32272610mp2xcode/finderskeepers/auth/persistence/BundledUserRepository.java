package io.github.cs32272610mp2xcode.finderskeepers.auth.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;

/** Read-only account repository loaded from the application's packaged resources. */
public final class BundledUserRepository implements UserRepository {
    private static final String DEMO_ACCOUNT_RESOURCE = "demo-users.json";

    private final List<UserAccount> accounts;

    /**
     * Loads and validates the bundled demonstration accounts.
     *
     * @throws IllegalStateException when the packaged resource is missing or invalid
     */
    public BundledUserRepository() {
        accounts = loadAccounts();
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        Objects.requireNonNull(username, "username");
        String lookup = username.toLowerCase(Locale.ROOT);
        return accounts.stream()
                .filter(account -> account.username().toLowerCase(Locale.ROOT).equals(lookup))
                .findFirst();
    }

    @Override
    public void add(UserAccount account) throws UserStoreException {
        Objects.requireNonNull(account, "account");
        throw new UserStoreException("Bundled demonstration accounts are read-only");
    }

    private static List<UserAccount> loadAccounts() {
        try (InputStream input = BundledUserRepository.class.getResourceAsStream(
                DEMO_ACCOUNT_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Bundled demonstration accounts are missing");
            }
            byte[] encoded = input.readNBytes(JsonUserRepository.MAX_STORE_BYTES + 1);
            if (encoded.length > JsonUserRepository.MAX_STORE_BYTES) {
                throw new IllegalStateException("Bundled demonstration accounts exceed the size limit");
            }
            String json = decodeUtf8(encoded);
            return List.copyOf(UserStoreJsonCodec.decode(json));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Bundled demonstration accounts are invalid", exception);
        }
    }

    private static String decodeUtf8(byte[] encoded) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Bundled account data is not valid UTF-8", exception);
        }
    }
}
