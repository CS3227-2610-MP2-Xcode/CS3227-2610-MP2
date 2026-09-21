package io.github.cs32272610mp2xcode.finderskeepers.matching.persistence;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.cs32272610mp2xcode.finderskeepers.matching.model.PossibleMatchPair;

/** Strict version-one file repository for possible-match relationships. */
public final class FilePossibleMatchRepository implements PossibleMatchRepository {
    /** Maximum accepted or generated document size. */
    static final int MAX_STORE_BYTES = 16_777_216;

    private static final String MAGIC = "FINDERS_KEEPERS_POSSIBLE_MATCH_LINKS";

    private static final String HEADER = MAGIC + " 1";

    private static final Pattern VERSION_HEADER = Pattern.compile(
            Pattern.quote(MAGIC) + " ([0-9]+)");

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final Path storePath;

    private final PossibleMatchStoreFiles files;

    private final int maximumBytes;

    /** Creates a repository for one relationship-store path without performing I/O.
     * @param path relationship-store path
     */
    public FilePossibleMatchRepository(Path path) {
        this(path, new NioPossibleMatchStoreFiles(), MAX_STORE_BYTES);
    }

    FilePossibleMatchRepository(Path path, PossibleMatchStoreFiles storeFiles,
            int maximumStoreBytes) {
        storePath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        files = Objects.requireNonNull(storeFiles, "store files");
        if (maximumStoreBytes < HEADER.length() + 1) {
            throw new IllegalArgumentException("Maximum store size is too small.");
        }
        maximumBytes = maximumStoreBytes;
    }

    @Override
    public synchronized Set<PossibleMatchPair> loadAll() throws PossibleMatchStoreException {
        return Set.copyOf(readCurrent());
    }

    @Override
    public synchronized boolean link(PossibleMatchPair pair)
            throws PossibleMatchStoreException {
        Objects.requireNonNull(pair, "pair");
        Set<PossibleMatchPair> current = readCurrent();
        if (!current.add(pair)) {
            return false;
        }
        write(current);
        return true;
    }

    @Override
    public synchronized boolean unlink(PossibleMatchPair pair)
            throws PossibleMatchStoreException {
        Objects.requireNonNull(pair, "pair");
        Set<PossibleMatchPair> current = readCurrent();
        if (!current.remove(pair)) {
            return false;
        }
        write(current);
        return true;
    }

    private Set<PossibleMatchPair> readCurrent() throws PossibleMatchStoreException {
        byte[] document;
        try {
            var stored = files.readBounded(storePath, maximumBytes);
            if (stored.isEmpty()) {
                return new HashSet<>();
            }
            document = stored.orElseThrow();
        } catch (StoreFileFailure failure) {
            PossibleMatchStoreException.Reason reason = failure.kind() == StoreFileFailure.Kind.OVER_LIMIT
                    ? PossibleMatchStoreException.Reason.CORRUPT_STORE
                    : PossibleMatchStoreException.Reason.READ_FAILURE;
            throw new PossibleMatchStoreException(reason);
        }
        return decode(document);
    }

    private void write(Set<PossibleMatchPair> pairs) throws PossibleMatchStoreException {
        byte[] document = encode(pairs);
        if (document.length > maximumBytes) {
            throw new PossibleMatchStoreException(
                    PossibleMatchStoreException.Reason.RESULT_TOO_LARGE);
        }
        try {
            files.replaceAtomically(storePath, document);
        } catch (StoreFileFailure failure) {
            throw new PossibleMatchStoreException(
                    PossibleMatchStoreException.Reason.WRITE_FAILURE);
        }
    }

    private static Set<PossibleMatchPair> decode(byte[] document)
            throws PossibleMatchStoreException {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(document)).toString();
        } catch (CharacterCodingException failure) {
            throw corrupt();
        }
        if (text.isEmpty() || containsInvalidCarriageReturn(text)) {
            throw corrupt();
        }
        String normalized = text.replace("\r\n", "\n");
        String[] rawLines = normalized.split("\n", -1);
        int lineCount = rawLines.length;
        if (rawLines[lineCount - 1].isEmpty()) {
            lineCount--;
        }
        if (lineCount == 0) {
            throw corrupt();
        }
        validateHeader(rawLines[0]);

        Set<PossibleMatchPair> pairs = new LinkedHashSet<>();
        for (int index = 1; index < lineCount; index++) {
            String line = rawLines[index];
            if (line.length() != 73 || line.charAt(36) != ' ') {
                throw corrupt();
            }
            String firstText = line.substring(0, 36);
            String secondText = line.substring(37);
            if (!UUID_PATTERN.matcher(firstText).matches()
                    || !UUID_PATTERN.matcher(secondText).matches()) {
                throw corrupt();
            }
            PossibleMatchPair pair;
            try {
                pair = PossibleMatchPair.of(
                        UUID.fromString(firstText), UUID.fromString(secondText));
            } catch (IllegalArgumentException failure) {
                throw corrupt();
            }
            if (!pairs.add(pair)) {
                throw corrupt();
            }
        }
        return new HashSet<>(pairs);
    }

    private static boolean containsInvalidCarriageReturn(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\r'
                    && (index + 1 >= text.length() || text.charAt(index + 1) != '\n')) {
                return true;
            }
        }
        return false;
    }

    private static void validateHeader(String header) throws PossibleMatchStoreException {
        if (HEADER.equals(header)) {
            return;
        }
        Matcher matcher = VERSION_HEADER.matcher(header);
        if (matcher.matches()) {
            throw new PossibleMatchStoreException(
                    PossibleMatchStoreException.Reason.UNSUPPORTED_VERSION);
        }
        throw corrupt();
    }

    private static byte[] encode(Set<PossibleMatchPair> pairs) {
        List<PossibleMatchPair> ordered = new ArrayList<>(pairs);
        ordered.sort(PossibleMatchPair.CANONICAL_ORDER);
        StringBuilder document = new StringBuilder(HEADER).append('\n');
        for (PossibleMatchPair pair : ordered) {
            document.append(pair.firstId()).append(' ')
                    .append(pair.secondId()).append('\n');
        }
        return document.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static PossibleMatchStoreException corrupt() {
        return new PossibleMatchStoreException(
                PossibleMatchStoreException.Reason.CORRUPT_STORE);
    }
}
