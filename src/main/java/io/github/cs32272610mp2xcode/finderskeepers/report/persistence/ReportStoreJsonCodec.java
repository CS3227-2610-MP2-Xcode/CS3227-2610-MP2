package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemCategory;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.model.ReportType;

final class ReportStoreJsonCodec {
    private static final DateTimeFormatter INSTANT_MILLIS = new DateTimeFormatterBuilder()
            .appendInstant(3)
            .toFormatter();

    private static final List<String> REPORT_MEMBERS = List.of(
            "reportId",
            "reporterId",
            "reportType",
            "itemName",
            "category",
            "location",
            "occurrenceDate",
            "publicDescription",
            "privateIdentifyingDetail",
            "status",
            "createdAt");

    List<ItemReport> decode(byte[] document) throws InvalidStoreException {
        String json;
        try {
            json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(document))
                    .toString();
        } catch (CharacterCodingException failure) {
            throw new InvalidStoreException();
        }
        return new Parser(json).parseDocument();
    }

    byte[] encode(List<ItemReport> reports, int maximumBytes) throws InvalidReportStateException {
        BoundedUtf8 output = new BoundedUtf8(maximumBytes);
        output.ascii("{\n  \"schemaVersion\": 1,\n  \"reports\": [");
        for (int index = 0; index < reports.size(); index++) {
            if (index > 0) {
                output.ascii(",");
            }
            output.ascii("\n    {\n");
            appendReport(output, reports.get(index));
            output.ascii("\n    }");
        }
        output.ascii("\n  ]\n}\n");
        return output.toByteArray();
    }

    private static void appendReport(BoundedUtf8 output, ItemReport report)
            throws InvalidReportStateException {
        appendMember(output, "reportId", report.reportId().toString(), true);
        appendMember(output, "reporterId", report.reporterId(), false);
        appendMember(output, "reportType", report.reportType().name(), false);
        appendMember(output, "itemName", report.itemName(), false);
        appendMember(output, "category", report.category().name(), false);
        appendMember(output, "location", report.location(), false);
        appendMember(output, "occurrenceDate", report.occurrenceDate().toString(), false);
        appendMember(output, "publicDescription", report.publicDescription(), false);
        appendMember(output, "privateIdentifyingDetail", report.privateIdentifyingDetail(), false);
        appendMember(output, "status", report.status().name(), false);
        appendMember(output, "createdAt", INSTANT_MILLIS.format(report.createdAt()), false);
    }

    private static void appendMember(BoundedUtf8 output, String name, String value, boolean first)
            throws InvalidReportStateException {
        if (!first) {
            output.ascii(",\n");
        }
        output.ascii("      \"");
        output.ascii(name);
        output.ascii("\": ");
        output.jsonString(value);
    }

    static final class InvalidStoreException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    static final class InvalidReportStateException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static final class BoundedUtf8 {
        private static final char[] HEX = "0123456789abcdef".toCharArray();

        private final int maximumBytes;

        private final ByteArrayOutputStream bytes;

        BoundedUtf8(int byteLimit) {
            this.maximumBytes = byteLimit;
            this.bytes = new ByteArrayOutputStream(Math.min(byteLimit, 8192));
        }

        void ascii(String value) throws InvalidReportStateException {
            for (int index = 0; index < value.length(); index++) {
                write(value.charAt(index));
            }
        }

        void jsonString(String value) throws InvalidReportStateException {
            write('"');
            for (int index = 0; index < value.length(); index++) {
                char current = value.charAt(index);
                if (Character.isHighSurrogate(current)) {
                    if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                        throw new InvalidReportStateException();
                    }
                    writeCodePoint(Character.toCodePoint(current, value.charAt(++index)));
                } else if (Character.isLowSurrogate(current)) {
                    throw new InvalidReportStateException();
                } else {
                    writeJsonCharacter(current);
                }
            }
            write('"');
        }

        byte[] toByteArray() {
            return bytes.toByteArray();
        }

        private void writeJsonCharacter(char value) throws InvalidReportStateException {
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

        private void writeCodePoint(int codePoint) throws InvalidReportStateException {
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

        private void write(int value) throws InvalidReportStateException {
            if (bytes.size() >= maximumBytes) {
                throw new InvalidReportStateException();
            }
            bytes.write(value);
        }
    }

    private static final class Parser {
        private final String json;

        private int index;

        Parser(String document) {
            this.json = document;
        }

        List<ItemReport> parseDocument() throws InvalidStoreException {
            skipWhitespace();
            expect('{');
            Set<String> members = new HashSet<>();
            List<ItemReport> reports = null;
            boolean versionSeen = false;
            skipWhitespace();
            if (consume('}')) {
                throw new InvalidStoreException();
            }
            while (true) {
                String member = parseString();
                if (!members.add(member)) {
                    throw new InvalidStoreException();
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                switch (member) {
                    case "schemaVersion" -> {
                        parseSchemaVersion();
                        versionSeen = true;
                    }
                    case "reports" -> reports = parseReports();
                    default -> throw new InvalidStoreException();
                }
                skipWhitespace();
                if (consume('}')) {
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            skipWhitespace();
            if (index != json.length() || !versionSeen || reports == null) {
                throw new InvalidStoreException();
            }
            return List.copyOf(reports);
        }

        private void parseSchemaVersion() throws InvalidStoreException {
            expect('1');
            if (index < json.length()) {
                char next = json.charAt(index);
                if (!isWhitespace(next) && next != ',' && next != '}') {
                    throw new InvalidStoreException();
                }
            }
        }

        private List<ItemReport> parseReports() throws InvalidStoreException {
            expect('[');
            skipWhitespace();
            List<ItemReport> reports = new ArrayList<>();
            Set<UUID> identifiers = new HashSet<>();
            if (consume(']')) {
                return reports;
            }
            while (true) {
                ItemReport report = parseReport();
                if (!identifiers.add(report.reportId())) {
                    throw new InvalidStoreException();
                }
                reports.add(report);
                skipWhitespace();
                if (consume(']')) {
                    return reports;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private ItemReport parseReport() throws InvalidStoreException {
            expect('{');
            skipWhitespace();
            Map<String, String> values = new HashMap<>();
            if (consume('}')) {
                throw new InvalidStoreException();
            }
            while (true) {
                String member = parseString();
                if (!REPORT_MEMBERS.contains(member) || values.containsKey(member)) {
                    throw new InvalidStoreException();
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                values.put(member, parseString());
                skipWhitespace();
                if (consume('}')) {
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            if (values.size() != REPORT_MEMBERS.size()) {
                throw new InvalidStoreException();
            }
            return reconstruct(values);
        }

        private ItemReport reconstruct(Map<String, String> values) throws InvalidStoreException {
            try {
                String identifierText = values.get("reportId");
                UUID identifier = UUID.fromString(identifierText);
                if (!identifier.toString().equals(identifierText)) {
                    throw new IllegalArgumentException();
                }
                String dateText = values.get("occurrenceDate");
                LocalDate date = LocalDate.parse(dateText);
                if (!date.toString().equals(dateText)) {
                    throw new IllegalArgumentException();
                }
                String instantText = values.get("createdAt");
                Instant instant = Instant.parse(instantText);
                if (!INSTANT_MILLIS.format(instant).equals(instantText)) {
                    throw new IllegalArgumentException();
                }
                return new ItemReport(
                        identifier,
                        values.get("reporterId"),
                        ReportType.valueOf(values.get("reportType")),
                        values.get("itemName"),
                        ItemCategory.valueOf(values.get("category")),
                        values.get("location"),
                        date,
                        values.get("publicDescription"),
                        values.get("privateIdentifyingDetail"),
                        ReportStatus.valueOf(values.get("status")),
                        instant);
            } catch (DateTimeException | IllegalArgumentException | NullPointerException failure) {
                throw new InvalidStoreException();
            }
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
                    throw new InvalidStoreException();
                } else if (Character.isHighSurrogate(current)) {
                    if (index >= json.length() || !Character.isLowSurrogate(json.charAt(index))) {
                        throw new InvalidStoreException();
                    }
                    value.append(current).append(json.charAt(index++));
                } else {
                    value.append(current);
                }
            }
            throw new InvalidStoreException();
        }

        private void appendEscape(StringBuilder value) throws InvalidStoreException {
            if (index >= json.length()) {
                throw new InvalidStoreException();
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
                default -> throw new InvalidStoreException();
            }
        }

        private void appendUnicodeEscape(StringBuilder value) throws InvalidStoreException {
            char first = parseHexUnit();
            if (Character.isLowSurrogate(first)) {
                throw new InvalidStoreException();
            }
            if (!Character.isHighSurrogate(first)) {
                value.append(first);
                return;
            }
            if (index + 2 > json.length() || json.charAt(index) != '\\' || json.charAt(index + 1) != 'u') {
                throw new InvalidStoreException();
            }
            index += 2;
            char second = parseHexUnit();
            if (!Character.isLowSurrogate(second)) {
                throw new InvalidStoreException();
            }
            value.append(first).append(second);
        }

        private char parseHexUnit() throws InvalidStoreException {
            if (index + 4 > json.length()) {
                throw new InvalidStoreException();
            }
            int result = 0;
            for (int count = 0; count < 4; count++) {
                int digit = Character.digit(json.charAt(index++), 16);
                if (digit < 0) {
                    throw new InvalidStoreException();
                }
                result = result << 4 | digit;
            }
            return (char) result;
        }

        private void expect(char expected) throws InvalidStoreException {
            if (index >= json.length() || json.charAt(index++) != expected) {
                throw new InvalidStoreException();
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
    }
}
