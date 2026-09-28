package io.github.cs32272610mp2xcode.finderskeepers.auth.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.cs32272610mp2xcode.finderskeepers.auth.application.ApplicationRoute;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationCoordinator;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationResult;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.AuthenticationStatus;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.RegistrationService;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordAlgorithm;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordHasher;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.Pbkdf2PasswordHasher;

class JsonUserRepositoryTest {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final String VALID_SALT = encodedBytes(PasswordCredential.SALT_LENGTH_BYTES);

    private static final String VALID_HASH = encodedBytes(PasswordCredential.HASH_LENGTH_BYTES);

    @TempDir
    private Path temporaryDirectory;

    private Path store;

    private JsonUserRepository repository;

    @BeforeEach
    void setUp() {
        store = temporaryDirectory.resolve("accounts.json");
        repository = new JsonUserRepository(store);
    }

    @Test
    void missingStoreBehavesAsAnEmptyRepositoryWithoutCreatingAFile()
            throws UserStoreException {
        assertTrue(repository.findByUsername("nobody").isEmpty());
        assertFalse(Files.exists(store));
    }

    @Test
    void readsVersionOneCredentialsAtBothAcceptedIterationBoundaries()
            throws IOException, UserStoreException {
        assertEquals(210_000, PasswordCredential.MIN_ITERATIONS);
        assertEquals(1_000_000, PasswordCredential.MAX_ITERATIONS);
        String lowerBoundary = accountJson(
                "lower-id", "lower.user", "STUDENT", ALGORITHM,
                PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                VALID_SALT, VALID_HASH);
        String upperBoundary = accountJson(
                "upper-id", "upper.user", "DESK_OFFICER", ALGORITHM,
                PasswordCredential.MAX_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                VALID_SALT, VALID_HASH);
        Files.writeString(store, storeJson(lowerBoundary, upperBoundary), StandardCharsets.UTF_8);

        UserAccount lower = repository.findByUsername("LOWER.USER").orElseThrow();
        UserAccount upper = repository.findByUsername("upper.user").orElseThrow();

        assertEquals(PasswordCredential.MIN_ITERATIONS, lower.credential().iterations());
        assertEquals(UserRole.STUDENT, lower.role());
        assertEquals(PasswordCredential.MAX_ITERATIONS, upper.credential().iterations());
        assertEquals(UserRole.DESK_OFFICER, upper.role());
    }

    @Test
    void readsTopLevelFieldsIndependentOfJsonObjectMemberOrder()
            throws IOException, UserStoreException {
        String reversedDocument = """
                {
                  "accounts": [
                %s
                  ],
                  "version": 1
                }
                """.formatted(validAccountJson());
        Files.writeString(store, reversedDocument, StandardCharsets.UTF_8);

        assertTrue(repository.findByUsername("demo.user").isPresent());
    }

    @Test
    void addedEscapedUnicodeIdentitySurvivesRestartExactly()
            throws UserStoreException {
        String userId = "synthetic-\"\\/\bé";
        String username = "student-\"\\/\b\f\n\r\t\u0001é";
        UserAccount original = account(userId, username);

        repository.add(original);

        UserAccount restored = new JsonUserRepository(store)
                .findByUsername(username).orElseThrow();
        assertEquals(original, restored);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCredentialMetadata")
    void rejectsInvalidCredentialMetadataWithoutReplacingTheStore(
            String description, String accountJson) throws IOException {
        assertRejectedWithoutReplacement(storeJson(accountJson));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDocuments")
    void rejectsMalformedOrInvalidDocumentsWithoutReplacingTheStore(
            String description, String document) throws IOException {
        assertRejectedWithoutReplacement(document);
    }

    @Test
    void rejectsCaseInsensitiveDuplicateUserIdentifiersWithoutReplacingTheStore()
            throws IOException {
        String first = validAccountJson();
        String duplicateIdentifier = accountJson(
                "DEMO-ID", "second.user", "STUDENT", ALGORITHM,
                PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                VALID_SALT, VALID_HASH);

        assertRejectedWithoutReplacement(storeJson(first, duplicateIdentifier));
    }

    @Test
    void rejectsCaseInsensitiveDuplicateUsernamesWithoutReplacingTheStore()
            throws IOException {
        String first = validAccountJson();
        String duplicateUsername = accountJson(
                "second-id", "DEMO.USER", "STUDENT", ALGORITHM,
                PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                VALID_SALT, VALID_HASH);

        assertRejectedWithoutReplacement(storeJson(first, duplicateUsername));
    }

    @Test
    void rejectsInvalidUtf8WithoutReplacingTheStore() throws IOException {
        String validDocument = storeJson(validAccountJson());
        int replacementIndex = validDocument.indexOf("demo.user");
        byte[] prefix = validDocument.substring(0, replacementIndex)
                .getBytes(StandardCharsets.UTF_8);
        byte[] suffix = validDocument.substring(replacementIndex + 1)
                .getBytes(StandardCharsets.UTF_8);
        byte[] invalidUtf8 = new byte[prefix.length + 2 + suffix.length];
        System.arraycopy(prefix, 0, invalidUtf8, 0, prefix.length);
        invalidUtf8[prefix.length] = (byte) 0xc3;
        invalidUtf8[prefix.length + 1] = (byte) 0x28;
        System.arraycopy(suffix, 0, invalidUtf8, prefix.length + 2, suffix.length);
        Files.write(store, invalidUtf8);

        assertCorruptReadAndAddFail();
        assertAuthenticationFailsSafely();
        assertArrayEquals(invalidUtf8, Files.readAllBytes(store));
    }

    @Test
    void rejectsStoresLargerThanOneMebibyteWithoutReplacingTheStore() throws IOException {
        assertEquals(1_048_576, JsonUserRepository.MAX_STORE_BYTES);
        byte[] validDocument = storeJson(validAccountJson()).getBytes(StandardCharsets.UTF_8);
        byte[] oversized = Arrays.copyOf(
                validDocument, JsonUserRepository.MAX_STORE_BYTES + 1);
        Arrays.fill(oversized, validDocument.length, oversized.length, (byte) ' ');
        Files.write(store, oversized);

        assertCorruptReadAndAddFail();
        assertAuthenticationFailsSafely();
        assertArrayEquals(oversized, Files.readAllBytes(store));
    }

    @Test
    void refusesAnOversizedWriteWithoutReplacingExistingValidData()
            throws IOException, UserStoreException {
        Files.writeString(store, storeJson(validAccountJson()), StandardCharsets.UTF_8);
        byte[] original = Files.readAllBytes(store);
        String oversizedUsername = "u".repeat(JsonUserRepository.MAX_STORE_BYTES);

        assertThrows(UserStoreException.class, () -> repository.add(
                account("oversized-id", oversizedUsername)));
        assertArrayEquals(original, Files.readAllBytes(store));
        assertTrue(repository.findByUsername("demo.user").isPresent());
    }

    @Test
    void refusesInvalidUnicodeOnWriteWithoutReplacingExistingValidData()
            throws IOException, UserStoreException {
        Files.writeString(store, storeJson(validAccountJson()), StandardCharsets.UTF_8);
        byte[] original = Files.readAllBytes(store);

        assertThrows(UserStoreException.class, () -> repository.add(
                account("invalid-text-id", String.valueOf(Character.MIN_HIGH_SURROGATE))));

        assertArrayEquals(original, Files.readAllBytes(store));
        assertTrue(repository.findByUsername("demo.user").isPresent());
    }

    @Test
    void readsAndVerifiesTheBundledDemoStoreFromAnUnchangedTemporaryCopy()
            throws IOException, UserStoreException {
        byte[] original;
        try (var source = Objects.requireNonNull(
                BundledUserRepository.class.getResourceAsStream("demo-users.json"))) {
            original = source.readAllBytes();
        }
        Files.write(store, original);
        DemoAccount studentDemo = readDemoAccount("Student");
        DemoAccount officerDemo = readDemoAccount("Desk Officer");

        UserAccount student = repository.findByUsername(
                studentDemo.username().toUpperCase(java.util.Locale.ROOT)).orElseThrow();
        UserAccount officer = repository.findByUsername(officerDemo.username()).orElseThrow();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();

        try {
            assertEquals(UserRole.STUDENT, student.role());
            assertEquals(UserRole.DESK_OFFICER, officer.role());
            assertEquals(210_000, student.credential().iterations());
            assertEquals(210_000, officer.credential().iterations());
            assertTrue(hasher.verify(studentDemo.password(), student.credential()));
            assertTrue(hasher.verify(officerDemo.password(), officer.credential()));
            assertArrayEquals(original, Files.readAllBytes(store));
        } finally {
            Arrays.fill(studentDemo.password(), '\0');
            Arrays.fill(officerDemo.password(), '\0');
        }
    }

    private static Stream<Arguments> invalidCredentialMetadata() {
        return Stream.of(
                arguments("unknown algorithm", accountJson(
                        "demo-id", "demo.user", "STUDENT", "PBKDF2WithHmacSHA1",
                        PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                        VALID_SALT, VALID_HASH)),
                arguments("iterations below lower bound", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS - 1,
                        PasswordCredential.KEY_LENGTH_BITS, VALID_SALT, VALID_HASH)),
                arguments("iterations above upper bound", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MAX_ITERATIONS + 1,
                        PasswordCredential.KEY_LENGTH_BITS, VALID_SALT, VALID_HASH)),
                arguments("wrong key length", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS, 128, VALID_SALT, VALID_HASH)),
                arguments("wrong salt length", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                        encodedBytes(PasswordCredential.SALT_LENGTH_BYTES - 1), VALID_HASH)),
                arguments("oversized salt", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                        encodedBytes(PasswordCredential.SALT_LENGTH_BYTES + 1), VALID_HASH)),
                arguments("wrong hash length", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                        VALID_SALT, encodedBytes(PasswordCredential.HASH_LENGTH_BYTES - 1))),
                arguments("oversized hash", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                        VALID_SALT, encodedBytes(PasswordCredential.HASH_LENGTH_BYTES + 1))),
                arguments("malformed salt Base64", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                        "%%%", VALID_HASH)),
                arguments("malformed hash Base64", accountJson(
                        "demo-id", "demo.user", "STUDENT", ALGORITHM,
                        PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                        VALID_SALT, "%%%")));
    }

    private static Stream<Arguments> invalidDocuments() {
        String validAccount = validAccountJson();
        String quotedIterations = validAccount.replace(
                "\"iterations\": 210000", "\"iterations\": \"210000\"");
        String missingHash = validAccount.replace(
                ",\n  \"passwordHash\": \"" + VALID_HASH + "\"", "");
        String unknownField = validAccount.replace(
                "  \"passwordHash\":", "  \"pepper\": \"synthetic\",\n  \"passwordHash\":");
        String duplicateField = validAccount.replace(
                "  \"username\": \"demo.user\",",
                "  \"username\": \"demo.user\",\n  \"username\": \"duplicate.user\",");
        String leadingZeroIterations = validAccount.replace(
                "\"iterations\": 210000", "\"iterations\": 0210000");
        String validDocument = storeJson(validAccount);
        String unicodeWhitespace = validDocument.replace(
                "\n  \"version\"", "\n" + '\u2003' + "\"version\"");
        String unicodeVersionDigit = validDocument.replace(
                "\"version\": 1", "\"version\": " + '\u0661');
        return Stream.of(
                arguments("unsupported version", storeJson(2, validAccount)),
                arguments("malformed JSON", "{not valid JSON"),
                arguments("number has a leading zero", storeJson(leadingZeroIterations)),
                arguments("non-JSON whitespace is present", unicodeWhitespace),
                arguments("number uses a non-ASCII digit", unicodeVersionDigit),
                arguments("top-level field is duplicated", """
                        {"version": 1, "version": 1, "accounts": []}
                        """),
                arguments("top-level field is missing", """
                        {"version": 1}
                        """),
                arguments("top-level field is unknown", """
                        {"version": 1, "accounts": [], "extra": true}
                        """),
                arguments("field has wrong JSON type", storeJson(quotedIterations)),
                arguments("required field is missing", storeJson(missingHash)),
                arguments("unknown field is present", storeJson(unknownField)),
                arguments("field is duplicated", storeJson(duplicateField)),
                arguments("role value is invalid", storeJson(validAccount.replace(
                        "\"role\": \"STUDENT\"", "\"role\": \"COMMUNITY_MEMBER\""))),
                arguments("empty top-level object", "{}"),
                arguments("empty account object", storeJson("{}")),
                arguments("unterminated string", validDocument.replace(
                        "\"demo.user\"", "\"demo.user")),
                arguments("raw control character in string", validDocument.replace(
                        "demo.user", "demo\nuser")),
                arguments("escape is missing its value", validDocument.replace(
                        "demo.user", "demo.user\\")),
                arguments("escape is not defined by JSON", validDocument.replace(
                        "demo.user", "demo\\q.user")),
                arguments("unicode escape is incomplete", validDocument.replace(
                        "demo.user", "demo\\u123.user")),
                arguments("unicode escape has a non-hex digit", validDocument.replace(
                        "demo.user", "demo\\u12x4.user")),
                arguments("integer exceeds supported range", validDocument.replace(
                        "\"iterations\": 210000", "\"iterations\": 99999999999999999999")));
    }

    private void assertRejectedWithoutReplacement(String content) throws IOException {
        byte[] original = content.getBytes(StandardCharsets.UTF_8);
        Files.write(store, original);

        assertCorruptReadAndAddFail();
        assertAuthenticationFailsSafely();
        assertArrayEquals(original, Files.readAllBytes(store));
    }

    private void assertCorruptReadAndAddFail() {
        assertThrows(UserStoreException.class, () -> repository.findByUsername("demo.user"));
        assertThrows(UserStoreException.class, () -> repository.add(account("new-id", "new.user")));
    }

    private void assertAuthenticationFailsSafely() {
        CountingPasswordHasher hasher = new CountingPasswordHasher();
        AuthenticationCoordinator coordinator = new AuthenticationCoordinator(
                new AuthenticationService(repository, hasher),
                new RegistrationService(repository, hasher, UUID::randomUUID));
        char[] password = "synthetic password".toCharArray();

        AuthenticationResult result = coordinator.login("demo.user", password);

        assertEquals(AuthenticationStatus.STORAGE_ERROR, result.status());
        assertEquals(AuthenticationResult.STORAGE_ERROR_MESSAGE, coordinator.message());
        assertTrue(coordinator.currentUser().isEmpty());
        assertEquals(ApplicationRoute.LOGIN, coordinator.route());
        assertEquals(0, hasher.verificationCount());
        assertArrayEquals(new char[password.length], password);
    }

    private static UserAccount account(String userId, String username) {
        byte[] salt = new byte[PasswordCredential.SALT_LENGTH_BYTES];
        byte[] hash = new byte[PasswordCredential.HASH_LENGTH_BYTES];
        return new UserAccount(userId, username, UserRole.STUDENT, new PasswordCredential(
                PasswordAlgorithm.PBKDF2_HMAC_SHA256,
                PasswordCredential.MIN_ITERATIONS,
                PasswordCredential.KEY_LENGTH_BITS,
                salt,
                hash));
    }

    private static String validAccountJson() {
        return accountJson(
                "demo-id", "demo.user", "STUDENT", ALGORITHM,
                PasswordCredential.MIN_ITERATIONS, PasswordCredential.KEY_LENGTH_BITS,
                VALID_SALT, VALID_HASH);
    }

    private static String accountJson(String userId, String username, String role,
            String algorithm, int iterations, int keyLength, String salt, String hash) {
        return """
                {
                  "userId": "%s",
                  "username": "%s",
                  "role": "%s",
                  "algorithm": "%s",
                  "iterations": %d,
                  "keyLength": %d,
                  "salt": "%s",
                  "passwordHash": "%s"
                }""".formatted(
                        userId, username, role, algorithm,
                        iterations, keyLength, salt, hash);
    }

    private static String storeJson(String... accounts) {
        return storeJson(1, accounts);
    }

    private static String storeJson(int version, String... accounts) {
        return """
                {
                  "version": %d,
                  "accounts": [
                %s
                  ]
                }
                """.formatted(version, String.join(",\n", accounts));
    }

    private static String encodedBytes(int length) {
        return Base64.getEncoder().encodeToString(new byte[length]);
    }

    private static DemoAccount readDemoAccount(String roleLabel) throws IOException {
        List<String> guideLines = Files.readAllLines(
                Path.of("docs", "UserGuide.md"), StandardCharsets.UTF_8);
        String prefix = "| " + roleLabel + " |";
        String accountRow = guideLines.stream()
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .orElseThrow();
        String[] quotedValues = accountRow.split("`");
        if (quotedValues.length < 4) {
            throw new IllegalStateException("Synthetic demo account row is malformed");
        }
        return new DemoAccount(quotedValues[1], quotedValues[3].toCharArray());
    }

    private record DemoAccount(String username, char[] password) {
    }

    private static final class CountingPasswordHasher implements PasswordHasher {
        private int verificationCount;

        @Override
        public PasswordCredential hash(char[] password) {
            throw new UnsupportedOperationException("Corrupt-store tests do not create hashes");
        }

        @Override
        public boolean verify(char[] password, PasswordCredential credential) {
            verificationCount++;
            return false;
        }

        int verificationCount() {
            return verificationCount;
        }
    }
}
