package io.github.cs32272610mp2xcode.finderskeepers.appointment.persistence;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AppointmentStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEvent;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEventId;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.AuditEventType;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CaseStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionAppointment;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionCase;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CollectionSlot;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.CustodyStatus;
import io.github.cs32272610mp2xcode.finderskeepers.appointment.model.SlotId;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;
import io.github.cs32272610mp2xcode.finderskeepers.claim.model.ClaimId;

/** Strict JSON codec for the complete appointment store. */
final class AppointmentStoreJsonCodec {
    private static final String VERSION = "schemaVersion";

    /**
     * Decodes and validates one complete appointment-store document.
     *
     * @param document UTF-8 JSON document
     * @return validated slots and cases
     * @throws InvalidStoreException if the document is malformed, unsupported, or inconsistent
     */
    DecodedStore decode(byte[] document) throws InvalidStoreException {
        String text;
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(document));
            text = decoded.toString();
        } catch (CharacterCodingException failure) {
            throw new InvalidStoreException(false, "Invalid UTF-8 document.");
        }
        try {
            Object root = new Parser(text).parse();
            Map<String, Object> object = object(root);
            requireKeys(object, Set.of(VERSION, "slots", "cases"));
            if (number(object, VERSION) != 1L) {
                throw new InvalidStoreException(true, "Unsupported schema version.");
            }
            List<CollectionSlot> slots = parseSlots(array(object, "slots"));
            List<CollectionCase> cases = parseCases(array(object, "cases"));
            validateCrossRecordState(slots, cases);
            return new DecodedStore(slots, cases);
        } catch (InvalidStoreException failure) {
            throw failure;
        } catch (IllegalArgumentException | IndexOutOfBoundsException | NullPointerException
                 | DateTimeException failure) {
            throw new InvalidStoreException(false, failure);
        }
    }

    /**
     * Serializes validated appointment state without exceeding the configured limit.
     *
     * @param slots slots to persist
     * @param cases collection cases to persist
     * @param maximumBytes largest permitted encoded document
     * @return UTF-8 JSON document
     * @throws InvalidStateException if the state is inconsistent or the document is too large
     */
    byte[] encode(List<CollectionSlot> slots, List<CollectionCase> cases, int maximumBytes)
            throws InvalidStateException {
        try {
            validateCrossRecordState(slots, cases);
            StringBuilder output = new StringBuilder();
            output.append("{\n  \"schemaVersion\": 1,\n  \"slots\": [");
            for (int index = 0; index < slots.size(); index++) {
                if (index > 0) {
                    output.append(',');
                }
                output.append('\n');
                appendSlot(output, slots.get(index));
            }
            output.append("\n  ],\n  \"cases\": [");
            for (int index = 0; index < cases.size(); index++) {
                if (index > 0) {
                    output.append(',');
                }
                output.append('\n');
                appendCase(output, cases.get(index));
            }
            output.append("\n  ]\n}\n");
            byte[] bytes = output.toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > maximumBytes) {
                throw new InvalidStateException();
            }
            return bytes;
        } catch (IllegalArgumentException | NullPointerException failure) {
            throw new InvalidStateException();
        }
    }

    private static void appendSlot(StringBuilder out, CollectionSlot slot) {
        out.append("    {");
        member(out, "slotId", slot.slotId().value().toString());
        member(out, "startsAt", slot.startsAt().toString());
        member(out, "enabled", slot.enabled());
        member(out, "createdAt", slot.createdAt().toString());
        member(out, "createdByOfficerId", slot.createdByOfficerId());
        member(out, "disabledAt", slot.disabledAt().map(Instant::toString).orElse(null));
        member(out, "disabledByOfficerId", slot.disabledByOfficerId().orElse(null));
        removeTrailingComma(out);
        out.append("\n    }");
    }

    private static void appendCase(StringBuilder out, CollectionCase caseState) {
        out.append("    {");
        member(out, "claimId", caseState.claimId().value().toString());
        member(out, "studentUserId", caseState.studentUserId());
        member(out, "status", caseState.status().storedName());
        member(out, "custodyStatus", caseState.custodyStatus().storedName());
        member(out, "storageLocation", caseState.storageLocation().orElse(null));
        member(out, "closedAt", caseState.closedAt().map(Instant::toString).orElse(null));
        out.append("\n      \"appointments\": [");
        for (int index = 0; index < caseState.appointments().size(); index++) {
            if (index > 0) {
                out.append(',');
            }
            out.append("\n        ");
            appendAppointment(out, caseState.appointments().get(index));
        }
        out.append("\n      ],\n      \"auditEvents\": [");
        for (int index = 0; index < caseState.auditEvents().size(); index++) {
            if (index > 0) {
                out.append(',');
            }
            out.append("\n        ");
            appendEvent(out, caseState.auditEvents().get(index));
        }
        out.append("\n      ]\n    }");
    }

    private static void appendAppointment(StringBuilder out, CollectionAppointment appointment) {
        out.append('{');
        member(out, "appointmentId", appointment.appointmentId().value().toString());
        member(out, "claimId", appointment.claimId().value().toString());
        member(out, "studentUserId", appointment.studentUserId());
        member(out, "slotId", appointment.slotId().value().toString());
        member(out, "status", appointment.status().storedName());
        member(out, "bookedAt", appointment.bookedAt().toString());
        member(out, "cancelledAt", appointment.cancelledAt().map(Instant::toString).orElse(null));
        member(out, "noShowAt", appointment.noShowAt().map(Instant::toString).orElse(null));
        member(out, "collectionConfirmedAt", appointment.collectionConfirmedAt()
                .map(Instant::toString).orElse(null));
        removeTrailingComma(out);
        out.append("\n        }");
    }

    private static void appendEvent(StringBuilder out, AuditEvent event) {
        out.append('{');
        member(out, "eventId", event.eventId().value().toString());
        member(out, "eventType", event.eventType().storedName());
        member(out, "occurredAt", event.occurredAt().toString());
        member(out, "actorUserId", event.actorUserId());
        member(out, "actorRole", event.actorRole().name());
        member(out, "appointmentId", event.appointmentId().map(value ->
                value.value().toString()).orElse(null));
        member(out, "sourceSlotId", event.sourceSlotId().map(value ->
                value.value().toString()).orElse(null));
        member(out, "targetSlotId", event.targetSlotId().map(value ->
                value.value().toString()).orElse(null));
        removeTrailingComma(out);
        out.append("\n        }");
    }

    private static void member(StringBuilder out, String name, Object value) {
        out.append("\n      \"").append(name).append("\": ");
        if (value == null) {
            out.append("null");
        } else if (value instanceof Boolean || value instanceof Number) {
            out.append(value);
        } else {
            appendString(out, value.toString());
        }
        out.append(',');
    }

    private static void appendString(StringBuilder out, String value) {
        out.append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (current < 0x20) {
                        out.append(String.format("\\u%04x", (int) current));
                    } else {
                        out.append(current);
                    }
                }
            }
        }
        out.append('"');
    }

    private static void removeTrailingComma(StringBuilder out) {
        if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
            out.setLength(out.length() - 1);
        }
    }

    private static List<CollectionSlot> parseSlots(List<Object> values)
            throws InvalidStoreException {
        List<CollectionSlot> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> object = object(value);
            requireKeys(object, Set.of("slotId", "startsAt", "enabled", "createdAt",
                    "createdByOfficerId", "disabledAt", "disabledByOfficerId"));
            result.add(CollectionSlot.restore(SlotId.of(uuid(string(object, "slotId"))),
                    instant(string(object, "startsAt")), bool(object, "enabled"),
                    instant(string(object, "createdAt")), string(object, "createdByOfficerId"),
                    optionalInstant(object, "disabledAt"),
                    optionalString(object, "disabledByOfficerId")));
        }
        return result;
    }

    private static List<CollectionCase> parseCases(List<Object> values)
            throws InvalidStoreException {
        List<CollectionCase> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> object = object(value);
            requireKeys(object, Set.of("claimId", "studentUserId", "status", "custodyStatus",
                    "storageLocation", "closedAt", "appointments", "auditEvents"));
            List<CollectionAppointment> appointments = parseAppointments(
                    array(object, "appointments"));
            List<AuditEvent> events = parseEvents(array(object, "auditEvents"));
            result.add(CollectionCase.restore(ClaimId.of(uuid(string(object, "claimId"))),
                    string(object, "studentUserId"),
                    CaseStatus.fromStoredName(string(object, "status")),
                    CustodyStatus.fromStoredName(string(object, "custodyStatus")),
                    optionalString(object, "storageLocation"), appointments, events,
                    optionalInstant(object, "closedAt")));
        }
        return result;
    }

    private static List<CollectionAppointment> parseAppointments(List<Object> values)
            throws InvalidStoreException {
        List<CollectionAppointment> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> object = object(value);
            requireKeys(object, Set.of("appointmentId", "claimId", "studentUserId", "slotId",
                    "status", "bookedAt", "cancelledAt", "noShowAt",
                    "collectionConfirmedAt"));
            result.add(CollectionAppointment.restore(
                    AppointmentId.of(uuid(string(object, "appointmentId"))),
                    ClaimId.of(uuid(string(object, "claimId"))),
                    string(object, "studentUserId"), SlotId.of(uuid(string(object, "slotId"))),
                    AppointmentStatus.fromStoredName(string(object, "status")),
                    instant(string(object, "bookedAt")), optionalInstant(object, "cancelledAt"),
                    optionalInstant(object, "noShowAt"),
                    optionalInstant(object, "collectionConfirmedAt")));
        }
        return result;
    }

    private static List<AuditEvent> parseEvents(List<Object> values)
            throws InvalidStoreException {
        List<AuditEvent> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> object = object(value);
            requireKeys(object, Set.of("eventId", "eventType", "occurredAt", "actorUserId",
                    "actorRole", "appointmentId", "sourceSlotId", "targetSlotId"));
            result.add(new AuditEvent(AuditEventId.of(uuid(string(object, "eventId"))),
                    AuditEventType.fromStoredName(string(object, "eventType")),
                    instant(string(object, "occurredAt")), string(object, "actorUserId"),
                    UserRole.valueOf(string(object, "actorRole")),
                    optionalAppointmentId(object, "appointmentId"),
                    optionalSlotId(object, "sourceSlotId"),
                    optionalSlotId(object, "targetSlotId")));
        }
        return result;
    }

    private static void validateCrossRecordState(List<CollectionSlot> slots,
            List<CollectionCase> cases) {
        Set<UUID> slotIds = new HashSet<>();
        for (CollectionSlot slot : slots) {
            if (!slotIds.add(slot.slotId().value())) {
                throw new IllegalArgumentException("Duplicate slot ID.");
            }
        }
        for (int first = 0; first < slots.size(); first++) {
            CollectionSlot left = slots.get(first);
            if (!left.enabled()) {
                continue;
            }
            for (int second = first + 1; second < slots.size(); second++) {
                CollectionSlot right = slots.get(second);
                if (right.enabled() && overlaps(left, right)) {
                    throw new IllegalArgumentException("Enabled slots overlap.");
                }
            }
        }
        Set<UUID> claimIds = new HashSet<>();
        Set<UUID> appointmentIds = new HashSet<>();
        Set<UUID> activelyOccupiedSlotIds = new HashSet<>();
        Set<UUID> eventIds = new HashSet<>();
        for (CollectionCase caseState : cases) {
            if (!claimIds.add(caseState.claimId().value())) {
                throw new IllegalArgumentException("Duplicate case Claim ID.");
            }
            for (CollectionAppointment appointment : caseState.appointments()) {
                if (!appointmentIds.add(appointment.appointmentId().value())) {
                    throw new IllegalArgumentException("Duplicate appointment ID.");
                }
                if (!slotIds.contains(appointment.slotId().value())) {
                    throw new IllegalArgumentException("Appointment references an unknown slot.");
                }
                if (appointment.status() == AppointmentStatus.BOOKED
                        && !activelyOccupiedSlotIds.add(appointment.slotId().value())) {
                    throw new IllegalArgumentException("A slot has multiple active appointments.");
                }
            }
            for (AuditEvent event : caseState.auditEvents()) {
                if (!eventIds.add(event.eventId().value())) {
                    throw new IllegalArgumentException("Duplicate audit event ID.");
                }
            }
        }
    }

    private static boolean overlaps(CollectionSlot left, CollectionSlot right) {
        return left.startsAt().isBefore(right.endsAt())
                && right.startsAt().isBefore(left.endsAt());
    }

    private static Map<String, Object> object(Object value) throws InvalidStoreException {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new InvalidStoreException(false, "Expected object.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) raw;
        return result;
    }

    private static List<Object> array(Map<String, Object> object, String name)
            throws InvalidStoreException {
        Object value = object.get(name);
        if (!(value instanceof List<?> raw)) {
            throw new InvalidStoreException(false, "Expected array.");
        }
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) raw;
        return result;
    }

    private static void requireKeys(Map<String, Object> object, Set<String> expected)
            throws InvalidStoreException {
        if (!object.keySet().equals(expected)) {
            throw new InvalidStoreException(false, "Unexpected object members: "
                    + object.keySet() + " expected " + expected);
        }
    }

    private static String string(Map<String, Object> object, String name)
            throws InvalidStoreException {
        Object value = object.get(name);
        if (!(value instanceof String text)) {
            throw new InvalidStoreException(false, "Expected string: " + name);
        }
        return text;
    }

    private static long number(Map<String, Object> object, String name)
            throws InvalidStoreException {
        Object value = object.get(name);
        if (!(value instanceof Long number)) {
            throw new InvalidStoreException(false, "Expected number: " + name);
        }
        return number;
    }

    private static boolean bool(Map<String, Object> object, String name)
            throws InvalidStoreException {
        Object value = object.get(name);
        if (!(value instanceof Boolean result)) {
            throw new InvalidStoreException(false, "Expected boolean: " + name);
        }
        return result;
    }

    private static Instant instant(String value) throws InvalidStoreException {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException failure) {
            throw new InvalidStoreException(false, "Invalid instant.");
        }
    }

    private static Optional<String> optionalString(Map<String, Object> object, String name)
            throws InvalidStoreException {
        Object value = object.get(name);
        if (value == null) {
            return Optional.empty();
        }
        if (!(value instanceof String text)) {
            throw new InvalidStoreException(false, "Expected optional string: " + name);
        }
        return Optional.of(text);
    }

    private static Optional<Instant> optionalInstant(Map<String, Object> object, String name)
            throws InvalidStoreException {
        return optionalString(object, name).map(value -> {
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException failure) {
                throw new IllegalArgumentException(failure);
            }
        });
    }

    private static UUID uuid(String value) throws InvalidStoreException {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new InvalidStoreException(false, "Invalid UUID.");
        }
    }

    private static Optional<AppointmentId> optionalAppointmentId(Map<String, Object> object,
            String name) throws InvalidStoreException {
        return optionalString(object, name).map(value -> AppointmentId.of(uuidUnchecked(value)));
    }

    private static Optional<SlotId> optionalSlotId(Map<String, Object> object, String name)
            throws InvalidStoreException {
        return optionalString(object, name).map(value -> SlotId.of(uuidUnchecked(value)));
    }

    private static UUID uuidUnchecked(String value) {
        return UUID.fromString(value);
    }

    /**
     * Validated state reconstructed from one store document.
     *
     * @param slots decoded collection slots
     * @param cases decoded collection cases
     */
    record DecodedStore(List<CollectionSlot> slots, List<CollectionCase> cases) {
    }

    /** Signals that stored bytes do not represent a supported, valid appointment store. */
    static final class InvalidStoreException extends Exception {
        private static final long serialVersionUID = 1L;

        private final boolean unsupportedVersion;

        /**
         * Creates a store-validation failure with a standard message.
         *
         * @param unsupported whether the schema version is unsupported
         */
        InvalidStoreException(boolean unsupported) {
            this(unsupported, "Invalid appointment store.");
        }

        /**
         * Creates a store-validation failure caused by invalid decoded state.
         *
         * @param unsupported whether the schema version is unsupported
         * @param cause internal validation failure
         */
        InvalidStoreException(boolean unsupported, Throwable cause) {
            super(cause);
            unsupportedVersion = unsupported;
        }

        /**
         * Creates a store-validation failure with a safe diagnostic message.
         *
         * @param unsupported whether the schema version is unsupported
         * @param message safe diagnostic message
         */
        InvalidStoreException(boolean unsupported, String message) {
            super(message);
            unsupportedVersion = unsupported;
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

    /** Signals that in-memory appointment state cannot be serialized safely. */
    static final class InvalidStateException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    /** Minimal strict JSON parser used to avoid accepting ambiguous store input. */
    private static final class Parser {
        private final String input;

        private int index;

        Parser(String source) {
            input = source;
        }

        Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != input.length()) {
                throw new IllegalArgumentException("Trailing JSON data.");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                throw new IllegalArgumentException("Missing JSON value.");
            }
            return switch (input.charAt(index)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                if (result.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate JSON member.");
                }
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            List<Object> result = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return result.toString();
                }
                if (current == '\\') {
                    if (index >= input.length()) {
                        throw new IllegalArgumentException("Unterminated escape.");
                    }
                    char escaped = input.charAt(index++);
                    result.append(switch (escaped) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'u' -> parseUnicodeEscape();
                        default -> throw new IllegalArgumentException("Unknown escape.");
                    });
                } else {
                    if (current < 0x20) {
                        throw new IllegalArgumentException("Control character in string.");
                    }
                    result.append(current);
                }
            }
            throw new IllegalArgumentException("Unterminated string.");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                throw new IllegalArgumentException("Invalid unicode escape.");
            }
            int value = Integer.parseInt(input.substring(index, index + 4), 16);
            index += 4;
            return (char) value;
        }

        private Object parseNumber() {
            int start = index;
            while (index < input.length() && "-+0123456789.eE".indexOf(input.charAt(index)) >= 0) {
                index++;
            }
            String value = input.substring(start, index);
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, index)) {
                throw new IllegalArgumentException("Invalid JSON literal.");
            }
            index += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                String found = index >= input.length() ? "<eof>"
                        : Character.toString(input.charAt(index));
                throw new IllegalArgumentException("Expected '" + expected + "' at "
                        + index + " but found " + found);
            }
        }
    }
}
