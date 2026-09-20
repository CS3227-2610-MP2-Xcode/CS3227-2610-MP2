# S1-D2-02 Report Storage Format

## Purpose and public boundary

`JsonReportRepository` stores canonical `ItemReport` records in one caller-selected file. Construct it with the `Path` of that file and use it through `ReportRepository`:

- `loadAll()` returns an unmodifiable snapshot in insertion order.
- `insert(report)` appends a new Report ID and rejects a duplicate.
- `replace(targetId, replacement)` replaces the complete report in place. Report ID, Reporter ID, and Creation Time are immutable; replacement does not change ordering.

The repository owns no default data-directory policy. Application wiring must select and consistently reuse the store path. Loading a path that does not exist returns an empty snapshot and does not create a directory or file. The first successful mutation creates missing parent directories and the store.

## Version 1 document

The root is a JSON object with exactly these members:

| Property | JSON type | Meaning |
|---|---|---|
| `schemaVersion` | number | Exact integer version `1` |
| `reports` | array | Ordered report objects |

Each report object has exactly these required members:

| Property | JSON type | Encoding |
|---|---|---|
| `reportId` | string | Lowercase canonical 36-character UUID |
| `reporterId` | string | Exact Reporter ID text |
| `reportType` | string | Version-one token `LOST` or `FOUND` |
| `itemName` | string | Exact item-name text |
| `category` | string | Version-one token `OTHER` |
| `location` | string | Exact location text |
| `occurrenceDate` | string | Canonical ISO-8601 `LocalDate` |
| `publicDescription` | string | Exact public-description text |
| `privateIdentifyingDetail` | string | Exact private-detail text |
| `status` | string | Version-one token `SUBMITTED` or `UNDER_REVIEW` |
| `createdAt` | string | UTC `Instant` with exactly three fractional digits and `Z` |

All eleven report members are required and non-null. Text remains exact: the codec does not trim, normalize, reinterpret, or otherwise alter it.

## Canonical output

The writer emits UTF-8 without a byte-order mark. It uses two-space indentation, line-feed (`LF`) line endings, and one final line feed. Root members and report members are emitted in the table order above, while array order is insertion order.

JSON metacharacters and control characters are escaped. Quotation marks, reverse solidus characters, backspace, form feed, line feed, carriage return, and tab use their JSON escapes; other control characters use lowercase hexadecimal `\u00xx` escapes. Other valid Unicode code points are encoded directly as UTF-8. Unpaired UTF-16 surrogate code units are not encodable.

Canonical output is deterministic for the same ordered report snapshot. Readers must nevertheless depend on the schema rather than whitespace or member order.

## Strict reads

The reader accepts only a complete version-one document that satisfies the canonical domain contract. It rejects the store as corrupt or unsupported when it encounters, among other invalid states:

- malformed or non-shortest UTF-8, a byte-order mark, invalid JSON syntax or escapes, unpaired surrogates, or trailing content;
- a root or report member that is missing, duplicated, unknown, null, or has the wrong JSON type;
- a non-integer, non-canonical, duplicated, or unsupported schema version;
- a duplicate Report ID;
- a non-canonical UUID, date, instant, report type, category, or status token; or
- report text that violates the canonical `ItemReport` length contract.

Strictness is intentional. The store is application-owned and is not a manual-editing interface. A rejected store is never repaired, normalized, migrated, truncated, or overwritten by any repository operation.

## Resource bound

The complete stored document has an inclusive limit of 16 MiB (16,777,216 bytes). A document of exactly that size may be read or produced; a larger document is rejected. Reads retain at most the limit plus one byte so oversize detection does not require loading an arbitrarily large file. Candidate output is bounded while it is encoded, before filesystem mutation begins.

The bound provides headroom over the planning estimate of approximately 8.5 MB for 1,000 reports whose bounded text needs maximal JSON escaping. It is a byte limit, not a promised report-count capacity: Unicode encoding and escaping change the size of individual records.

An existing oversize store blocks `loadAll`, `insert`, and `replace` as corrupt or unsupported. A mutation whose candidate would exceed the limit fails as unencodable or over limit, leaving the authoritative store untouched.

## Safe mutation protocol

Each mutation performs these steps while holding the repository instance monitor:

1. Read and strictly validate the current authoritative file.
2. Apply the requested operation to an in-memory ordered snapshot.
3. Encode and bound the complete candidate document.
4. Create a randomly named temporary sibling in the target directory.
5. Write the complete candidate, force the temporary file's contents and metadata, and close it.
6. Replace the target with one `ATOMIC_MOVE` plus `REPLACE_EXISTING` operation.

There is no non-atomic fallback. If directory creation, temporary-file work, forcing, or atomic replacement is unavailable, the operation reports a storage failure. Success is reported only after the atomic move completes. Up to that point the previous target remains authoritative; a first insertion has no target to preserve.

This protocol protects against partial application-level writes. If the application process terminates before the atomic move, the previous target remains authoritative; after the move returns successfully, a later repository instance observes the complete new target while the operating system and storage remain operational. Java NIO does not provide a portable way for this implementation to force the containing directory, so sudden machine, operating-system, or storage-device failure is outside the durability guarantee.

Temporary-file deletion after failure is best effort. A leftover sibling whose name begins with `.report-store-` and ends with `.tmp` is ignored: it is never promoted, merged, or used for automatic recovery. It may contain the complete proposed report store, including private details, and therefore requires the same privacy handling as the authoritative file.

## Failures, privacy, and operational limits

Caller-visible `ReportStoreException` reasons distinguish duplicate insertion, missing replacement target, immutable-field conflict, corrupt or unsupported storage, storage or safe-replacement failure, and an unencodable or over-limit result. Diagnostics are fixed and do not include report values. A corrupt, unreadable, unsupported, non-regular, or oversize authoritative path blocks all operations and remains untouched.

Private identifying details are stored as plaintext JSON. The persistence layer does not encrypt data, configure filesystem access controls, redact the file, or decide which users may view private fields. Application services and user interfaces enforce visibility; deployment and operating-system policy protect the store and any temporary siblings.

All public operations on one `JsonReportRepository` instance are synchronized, and every operation rereads the authoritative file. The supported application shape is one process using one shared repository instance. Coordination between multiple repository instances or multiple processes is not provided and can lose updates.

Version one has no migration, backup, journal, repair, or manual-edit workflow. `ItemCategory` currently has only `OTHER`, so category replacement cannot yet express a different value. Adding an enum constant requires an explicit version-one compatibility review; renaming or removing a persisted token requires a schema-version or migration decision.
