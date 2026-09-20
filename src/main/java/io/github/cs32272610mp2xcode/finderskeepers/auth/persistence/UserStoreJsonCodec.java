package io.github.cs32272610mp2xcode.finderskeepers.auth.persistence;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordAlgorithm;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.PasswordCredential;

/** Strict codec for the deliberately small version 1 account-store schema. */
final class UserStoreJsonCodec {
    private static final int FORMAT_VERSION = 1;

    private static final Set<String> ACCOUNT_FIELDS = Set.of(
            "userId", "username", "role", "algorithm", "iterations",
            "keyLength", "salt", "passwordHash");

    private final String input;

    private int position;

    private UserStoreJsonCodec(String jsonInput) {
        input = Objects.requireNonNull(jsonInput, "jsonInput");
    }

    static List<UserAccount> decode(String jsonInput) {
        return new UserStoreJsonCodec(jsonInput).decodeDocument();
    }

    static String encode(List<UserAccount> accounts) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"version\": ").append(FORMAT_VERSION)
                .append(",\n  \"accounts\": [");
        for (int index = 0; index < accounts.size(); index++) {
            UserAccount account = accounts.get(index);
            PasswordCredential credential = account.credential();
            if (index > 0) {
                json.append(',');
            }
            json.append("\n    {\n")
                    .append(field("userId", account.userId())).append(",\n")
                    .append(field("username", account.username())).append(",\n")
                    .append(field("role", account.role().name())).append(",\n")
                    .append(field("algorithm", credential.algorithm().storageValue())).append(",\n")
                    .append("      \"iterations\": ").append(credential.iterations()).append(",\n")
                    .append("      \"keyLength\": ").append(credential.keyLength()).append(",\n")
                    .append(field("salt", Base64.getEncoder().encodeToString(credential.salt())))
                    .append(",\n")
                    .append(field("passwordHash",
                            Base64.getEncoder().encodeToString(credential.hash())))
                    .append("\n    }");
        }
        json.append("\n  ]\n}\n");
        return json.toString();
    }

    private List<UserAccount> decodeDocument() {
        Integer version = null;
        List<UserAccount> accounts = null;
        expect('{');
        skipWhitespace();
        if (consume('}')) {
            throw invalidStore();
        }
        do {
            String name = readString();
            expect(':');
            if (name.equals("version") && version == null) {
                version = readNumber();
            } else if (name.equals("accounts") && accounts == null) {
                accounts = readAccounts();
            } else {
                throw invalidStore();
            }
            skipWhitespace();
        } while (consume(','));
        expect('}');
        skipWhitespace();
        if (position != input.length() || version == null
                || version != FORMAT_VERSION || accounts == null) {
            throw invalidStore();
        }
        validateUniqueAccounts(accounts);
        return accounts;
    }

    private List<UserAccount> readAccounts() {
        List<UserAccount> accounts = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (consume(']')) {
            return accounts;
        }
        do {
            accounts.add(readAccount());
            skipWhitespace();
        } while (consume(','));
        expect(']');
        return accounts;
    }

    private UserAccount readAccount() {
        Map<String, String> values = new HashMap<>();
        expect('{');
        skipWhitespace();
        if (consume('}')) {
            throw invalidStore();
        }
        do {
            String name = readString();
            if (!ACCOUNT_FIELDS.contains(name) || values.containsKey(name)) {
                throw invalidStore();
            }
            expect(':');
            String value;
            if (name.equals("iterations") || name.equals("keyLength")) {
                value = Integer.toString(readNumber());
            } else {
                value = readString();
            }
            values.put(name, value);
            skipWhitespace();
        } while (consume(','));
        expect('}');
        if (!values.keySet().equals(ACCOUNT_FIELDS)) {
            throw invalidStore();
        }
        try {
            PasswordCredential credential = new PasswordCredential(
                    PasswordAlgorithm.fromStorageValue(values.get("algorithm")),
                    Integer.parseInt(values.get("iterations")),
                    Integer.parseInt(values.get("keyLength")),
                    Base64.getDecoder().decode(values.get("salt")),
                    Base64.getDecoder().decode(values.get("passwordHash")));
            return new UserAccount(values.get("userId"), values.get("username"),
                    UserRole.valueOf(values.get("role")), credential);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalidStore();
        }
    }

    private String readString() {
        skipWhitespace();
        if (position >= input.length() || input.charAt(position++) != '\"') {
            throw invalidStore();
        }
        StringBuilder value = new StringBuilder();
        while (position < input.length()) {
            char character = input.charAt(position++);
            if (character == '\"') {
                return value.toString();
            }
            if (character == '\\') {
                value.append(readEscape());
            } else if (character < 0x20) {
                throw invalidStore();
            } else {
                value.append(character);
            }
        }
        throw invalidStore();
    }

    private char readEscape() {
        if (position >= input.length()) {
            throw invalidStore();
        }
        char escaped = input.charAt(position++);
        return switch (escaped) {
            case '\"', '\\', '/' -> escaped;
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape();
            default -> throw invalidStore();
        };
    }

    private char readUnicodeEscape() {
        if (position + 4 > input.length()) {
            throw invalidStore();
        }
        try {
            char value = (char) Integer.parseInt(input.substring(position, position + 4), 16);
            position += 4;
            return value;
        } catch (NumberFormatException exception) {
            throw invalidStore();
        }
    }

    private int readNumber() {
        skipWhitespace();
        int start = position;
        if (position >= input.length() || !isAsciiDigit(input.charAt(position))) {
            throw invalidStore();
        }
        if (input.charAt(position) == '0') {
            position++;
            if (position < input.length() && isAsciiDigit(input.charAt(position))) {
                throw invalidStore();
            }
        } else {
            while (position < input.length() && isAsciiDigit(input.charAt(position))) {
                position++;
            }
        }
        try {
            return Integer.parseInt(input.substring(start, position));
        } catch (NumberFormatException exception) {
            throw invalidStore();
        }
    }

    private void expect(char expected) {
        skipWhitespace();
        if (!consume(expected)) {
            throw invalidStore();
        }
    }

    private boolean consume(char expected) {
        if (position < input.length() && input.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (position < input.length() && isJsonWhitespace(input.charAt(position))) {
            position++;
        }
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static boolean isJsonWhitespace(char character) {
        return character == ' ' || character == '\t'
                || character == '\n' || character == '\r';
    }

    private static String field(String name, String value) {
        return "      \"" + name + "\": \"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static void validateUniqueAccounts(List<UserAccount> accounts) {
        Set<String> identifiers = new HashSet<>();
        Set<String> usernames = new HashSet<>();
        for (UserAccount account : accounts) {
            if (!identifiers.add(account.userId().toLowerCase(Locale.ROOT))
                    || !usernames.add(account.username().toLowerCase(Locale.ROOT))) {
                throw invalidStore();
            }
        }
    }

    private static IllegalArgumentException invalidStore() {
        return new IllegalArgumentException("Invalid local account store");
    }
}
