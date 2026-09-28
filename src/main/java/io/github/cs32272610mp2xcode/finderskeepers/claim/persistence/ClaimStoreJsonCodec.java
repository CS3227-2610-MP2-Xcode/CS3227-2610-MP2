package io.github.cs32272610mp2xcode.finderskeepers.claim.persistence;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.model.Claim;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimLedger;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimStatus;

/** Strict, size-bounded JSON codec for the complete Claim store. */
final class ClaimStoreJsonCodec {
    private static final DateTimeFormatter INSTANT_FORMAT =
            new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    private static final List<String> CLAIM_MEMBERS = List.of(
            "claimId", "claimantUserId", "lostReportId", "foundReportId",
            "ownershipEvidence", "status", "submittedAt", "terminalAt",
            "decisionReason");

    /**
     * Decodes and validates one complete Claim-store document.
     *
     * @param document UTF-8 JSON document
     * @return validated Claims in stored order
     * @throws InvalidStoreException if the document is malformed, unsupported, or inconsistent
     */
    List<Claim> decode(byte[] document) throws InvalidStoreException {
        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(document)).toString();
        } catch (CharacterCodingException failure) {
            throw new InvalidStoreException(false);
        }
        List<Claim> claims = new Parser(json).parseDocument();
        try {
            ClaimLedger.from(claims);
        } catch (IllegalArgumentException | NullPointerException failure) {
            throw new InvalidStoreException(false);
        }
        return claims;
    }

    /**
     * Serializes validated Claims without exceeding the configured limit.
     *
     * @param claims Claims to persist
     * @param maximumBytes largest permitted encoded document
     * @return UTF-8 JSON document
     * @throws InvalidClaimStateException if the state or encoded document is invalid
     */
    byte[] encode(List<Claim> claims, int maximumBytes)
            throws InvalidClaimStateException {
        try {
            ClaimLedger.from(claims);
        } catch (IllegalArgumentException | NullPointerException failure) {
            throw new InvalidClaimStateException();
        }
        BoundedUtf8 output = new BoundedUtf8(maximumBytes);
        output.ascii("{\n  \"schemaVersion\": 1,\n  \"claims\": [");
        for (int index = 0; index < claims.size(); index++) {
            if (index > 0) {
                output.ascii(",");
            }
            output.ascii("\n    {\n");
            appendClaim(output, claims.get(index));
            output.ascii("\n    }");
        }
        output.ascii("\n  ]\n}\n");
        return output.toByteArray();
    }

    private static void appendClaim(BoundedUtf8 output, Claim claim)
            throws InvalidClaimStateException {
        appendStringMember(output, "claimId", claim.claimId().value().toString(), true);
        appendStringMember(output, "claimantUserId", claim.claimantUserId(), false);
        appendStringMember(output, "lostReportId", claim.lostReportId().toString(), false);
        appendStringMember(output, "foundReportId", claim.foundReportId().toString(), false);
        appendStringMember(output, "ownershipEvidence", claim.ownershipEvidence(), false);
        appendStringMember(output, "status", claim.status().storedName(), false);
        appendStringMember(output, "submittedAt", formatInstant(claim.submittedAt()), false);
        appendOptionalMember(output, "terminalAt", claim.terminalAt().map(
                ClaimStoreJsonCodec::formatInstant));
        appendOptionalMember(output, "decisionReason", claim.decisionReason());
    }

    private static void appendStringMember(BoundedUtf8 output, String name,
            String value, boolean first) throws InvalidClaimStateException {
        if (!first) {
            output.ascii(",\n");
        }
        output.ascii("      \"");
        output.ascii(name);
        output.ascii("\": ");
        output.jsonString(value);
    }

    private static void appendOptionalMember(BoundedUtf8 output, String name,
            Optional<String> value) throws InvalidClaimStateException {
        output.ascii(",\n      \"");
        output.ascii(name);
        output.ascii("\": ");
        if (value.isPresent()) {
            output.jsonString(value.orElseThrow());
        } else {
            output.ascii("null");
        }
    }

    private static String formatInstant(Instant instant) {
        return INSTANT_FORMAT.format(instant);
    }

    /** Signals that stored bytes do not represent a supported, valid Claim store. */
    static final class InvalidStoreException extends Exception {
        private static final long serialVersionUID = 1L;

        private final boolean unsupportedVersion;

        /**
         * Creates a classified store-validation failure.
         *
         * @param versionUnsupported whether the schema version is unsupported
         */
        InvalidStoreException(boolean versionUnsupported) {
            unsupportedVersion = versionUnsupported;
        }

        /**
         * Reports whether the failure was caused by an unsupported schema version.
         *
         * @return {@code true} for an unsupported schema version
         */
        boolean unsupportedVersion() {
            return unsupportedVersion;
        }
    }

    /** Signals that in-memory Claim state cannot be serialized safely. */
    static final class InvalidClaimStateException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    /** Writes escaped UTF-8 while enforcing the store's byte limit incrementally. */
    private static final class BoundedUtf8 {
        private static final char[] HEX = "0123456789abcdef".toCharArray();

        private final int maximumBytes;

        private final ByteArrayOutputStream bytes;

        BoundedUtf8(int byteLimit) {
            maximumBytes = byteLimit;
            bytes = new ByteArrayOutputStream(Math.min(byteLimit, 8192));
        }

        void ascii(String value) throws InvalidClaimStateException {
            for (int index = 0; index < value.length(); index++) {
                write(value.charAt(index));
            }
        }

        void jsonString(String value) throws InvalidClaimStateException {
            write('"');
            for (int index = 0; index < value.length(); index++) {
                char current = value.charAt(index);
                if (Character.isHighSurrogate(current)) {
                    if (index + 1 >= value.length()
                            || !Character.isLowSurrogate(value.charAt(index + 1))) {
                        throw new InvalidClaimStateException();
                    }
                    writeCodePoint(Character.toCodePoint(current, value.charAt(++index)));
                } else if (Character.isLowSurrogate(current)) {
                    throw new InvalidClaimStateException();
                } else {
                    writeJsonCharacter(current);
                }
            }
            write('"');
        }

        byte[] toByteArray() {
            return bytes.toByteArray();
        }

        private void writeJsonCharacter(char value) throws InvalidClaimStateException {
            switch (value) {
                case '"' -> ascii("\\\"");
                case '\\' -> ascii("\\\\");
                case '\b' -> ascii("\\b");
                case '\f' -> ascii("\\f");
                case '\n' -> ascii("\\n");
                case '\r' -> ascii("\\r");
                case '\t' -> ascii("\\t");
                default -> {
                    if (value < 0x20) {
                        ascii("\\u00");
                        write(HEX[(value >>> 4) & 0x0f]);
                        write(HEX[value & 0x0f]);
                    } else {
                        writeCodePoint(value);
                    }
                }
            }
        }

        private void writeCodePoint(int codePoint) throws InvalidClaimStateException {
            if (codePoint <= 0x7f) {
                write(codePoint);
            } else if (codePoint <= 0x7ff) {
                write(0xc0 | codePoint >>> 6);
                write(0x80 | codePoint & 0x3f);
            } else if (codePoint <= 0xffff) {
                write(0xe0 | codePoint >>> 12);
                write(0x80 | codePoint >>> 6 & 0x3f);
                write(0x80 | codePoint & 0x3f);
            } else {
                write(0xf0 | codePoint >>> 18);
                write(0x80 | codePoint >>> 12 & 0x3f);
                write(0x80 | codePoint >>> 6 & 0x3f);
                write(0x80 | codePoint & 0x3f);
            }
        }

        private void write(int value) throws InvalidClaimStateException {
            if (bytes.size() >= maximumBytes) {
                throw new InvalidClaimStateException();
            }
            bytes.write(value);
        }
    }

    /** Minimal strict JSON parser used to reject ambiguous Claim-store input. */
    private static final class Parser {
        private final String json;

        private int index;

        Parser(String document) {
            json = document;
        }

        List<Claim> parseDocument() throws InvalidStoreException {
            skipWhitespace();
            expect('{');
            Set<String> members = new HashSet<>();
            List<Claim> claims = null;
            boolean versionSeen = false;
            skipWhitespace();
            if (consume('}')) {
                corrupt();
            }
            while (true) {
                String member = parseString();
                if (!members.add(member)) {
                    corrupt();
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                switch (member) {
                    case "schemaVersion" -> {
                        parseSchemaVersion();
                        versionSeen = true;
                    }
                    case "claims" -> claims = parseClaims();
                    default -> corrupt();
                }
                skipWhitespace();
                if (consume('}')) {
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            skipWhitespace();
            if (index != json.length() || !versionSeen || claims == null) {
                corrupt();
            }
            return List.copyOf(claims);
        }

        private void parseSchemaVersion() throws InvalidStoreException {
            int start = index;
            if (index >= json.length() || !isAsciiDigit(json.charAt(index))) {
                corrupt();
            }
            if (json.charAt(index) == '0') {
                index++;
                if (index < json.length() && isAsciiDigit(json.charAt(index))) {
                    corrupt();
                }
            } else {
                while (index < json.length() && isAsciiDigit(json.charAt(index))) {
                    index++;
                }
            }
            String token = json.substring(start, index);
            if (!"1".equals(token)) {
                throw new InvalidStoreException(true);
            }
            if (index < json.length()) {
                char next = json.charAt(index);
                if (!isWhitespace(next) && next != ',' && next != '}') {
                    corrupt();
                }
            }
        }

        private List<Claim> parseClaims() throws InvalidStoreException {
            expect('[');
            skipWhitespace();
            List<Claim> claims = new ArrayList<>();
            if (consume(']')) {
                return claims;
            }
            while (true) {
                claims.add(parseClaim());
                skipWhitespace();
                if (consume(']')) {
                    return claims;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private Claim parseClaim() throws InvalidStoreException {
            expect('{');
            skipWhitespace();
            Map<String, Optional<String>> values = new HashMap<>();
            if (consume('}')) {
                corrupt();
            }
            while (true) {
                String member = parseString();
                if (!CLAIM_MEMBERS.contains(member) || values.containsKey(member)) {
                    corrupt();
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                values.put(member, nullable(member) ? parseNullableString()
                        : Optional.of(parseString()));
                skipWhitespace();
                if (consume('}')) {
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            if (values.size() != CLAIM_MEMBERS.size()) {
                corrupt();
            }
            return reconstruct(values);
        }

        private static boolean nullable(String member) {
            return "terminalAt".equals(member) || "decisionReason".equals(member);
        }

        private Optional<String> parseNullableString() throws InvalidStoreException {
            if (json.startsWith("null", index)) {
                index += 4;
                return Optional.empty();
            }
            return Optional.of(parseString());
        }

        private Claim reconstruct(Map<String, Optional<String>> values)
                throws InvalidStoreException {
            try {
                UUID claimUuid = canonicalUuid(required(values, "claimId"));
                UUID lostUuid = canonicalUuid(required(values, "lostReportId"));
                UUID foundUuid = canonicalUuid(required(values, "foundReportId"));
                Instant submitted = canonicalInstant(required(values, "submittedAt"));
                Optional<Instant> terminal = values.get("terminalAt")
                        .map(Parser::canonicalInstant);
                return Claim.restore(ClaimId.of(claimUuid),
                        required(values, "claimantUserId"), lostUuid, foundUuid,
                        required(values, "ownershipEvidence"),
                        ClaimStatus.fromStoredName(required(values, "status")),
                        submitted, terminal, values.get("decisionReason"));
            } catch (DateTimeException | IllegalArgumentException
                    | NullPointerException failure) {
                throw new InvalidStoreException(false);
            }
        }

        private static String required(Map<String, Optional<String>> values, String name) {
            return values.get(name).orElseThrow();
        }

        private static UUID canonicalUuid(String text) {
            UUID value = UUID.fromString(text);
            if (!value.toString().equals(text)) {
                throw new IllegalArgumentException("Noncanonical UUID.");
            }
            return value;
        }

        private static Instant canonicalInstant(String text) {
            Instant value = Instant.from(INSTANT_FORMAT.parse(text));
            if (!formatInstant(value).equals(text)) {
                throw new IllegalArgumentException("Noncanonical time.");
            }
            return value;
        }

        private String parseString() throws InvalidStoreException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < json.length()) {
                char current = json.charAt(index++);
                if (current == '"') {
                    return value.toString();
                }
                if (current == '\\') {
                    appendEscape(value);
                } else if (current < 0x20 || Character.isLowSurrogate(current)) {
                    corrupt();
                } else if (Character.isHighSurrogate(current)) {
                    if (index >= json.length()
                            || !Character.isLowSurrogate(json.charAt(index))) {
                        corrupt();
                    }
                    value.append(current).append(json.charAt(index++));
                } else {
                    value.append(current);
                }
            }
            corrupt();
            return "";
        }

        private void appendEscape(StringBuilder value) throws InvalidStoreException {
            if (index >= json.length()) {
                corrupt();
            }
            switch (json.charAt(index++)) {
                case '"' -> value.append('"');
                case '\\' -> value.append('\\');
                case '/' -> value.append('/');
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> appendUnicodeEscape(value);
                default -> corrupt();
            }
        }

        private void appendUnicodeEscape(StringBuilder value) throws InvalidStoreException {
            char first = parseHexUnit();
            if (Character.isLowSurrogate(first)) {
                corrupt();
            }
            if (!Character.isHighSurrogate(first)) {
                value.append(first);
                return;
            }
            if (index + 2 > json.length() || json.charAt(index) != '\\'
                    || json.charAt(index + 1) != 'u') {
                corrupt();
            }
            index += 2;
            char second = parseHexUnit();
            if (!Character.isLowSurrogate(second)) {
                corrupt();
            }
            value.append(first).append(second);
        }

        private char parseHexUnit() throws InvalidStoreException {
            if (index + 4 > json.length()) {
                corrupt();
            }
            int result = 0;
            for (int count = 0; count < 4; count++) {
                int digit = asciiHexValue(json.charAt(index++));
                if (digit < 0) {
                    corrupt();
                }
                result = result << 4 | digit;
            }
            return (char) result;
        }

        private static int asciiHexValue(char value) {
            if (value >= '0' && value <= '9') {
                return value - '0';
            }
            if (value >= 'a' && value <= 'f') {
                return value - 'a' + 10;
            }
            if (value >= 'A' && value <= 'F') {
                return value - 'A' + 10;
            }
            return -1;
        }

        private static boolean isAsciiDigit(char value) {
            return value >= '0' && value <= '9';
        }

        private void expect(char expected) throws InvalidStoreException {
            if (index >= json.length() || json.charAt(index++) != expected) {
                corrupt();
            }
        }

        private boolean consume(char expected) {
            if (index < json.length() && json.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < json.length() && isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private static boolean isWhitespace(char value) {
            return value == ' ' || value == '\t' || value == '\n' || value == '\r';
        }

        private static void corrupt() throws InvalidStoreException {
            throw new InvalidStoreException(false);
        }
    }
}
