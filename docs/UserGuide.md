# Finders Keepers User Guide

## Current status

Finders Keepers is intended for a primary school's lost-and-found desk. Application startup now opens the local login, with Student and Desk Officer routing and logout available after authentication.

The Student report form and personal report-history/search components are
available from the authenticated Student workspace. Reports are stored in the
project-local `data/reports.json` file.

## Requirements

Install Java 25 before running the project. Confirm the active version:

```bash
java --version
```

The first line should report Java 25. The included Gradle Wrapper downloads the matching Gradle distribution when first used, so a separate Gradle installation is not required.

## Start the application

On macOS or Linux:

```bash
./gradlew run
```

On Windows PowerShell or Command Prompt:

```text
gradlew.bat run
```

Both `gradlew run` and the current release JAR open the **Finders Keepers** login interface when launched from the repository root.

## Synthetic demonstration accounts

The project-local `data/demo-users.json` store contains two public, synthetic demonstration accounts:

| Role | Username | Password |
| --- | --- | --- |
| Student | `demo.student` | `Student-Demo-27!` |
| Desk Officer | `demo.officer` | `Officer-Demo-42!` |

These accounts and passwords are clearly labelled public synthetic test data. Do not reuse them for a real person, school, or system. The credential store contains salted password hashes rather than these plaintext passwords. Application startup reads the project-local store from `data/demo-users.json`. The store is not embedded in the JAR, so launch the JAR from the repository root unless a different store layout is configured later.

## Login behavior

Enter one of the usernames and passwords above and select **Log in**. Usernames ignore capitalization and surrounding spaces. Passwords are case-sensitive and are not trimmed: meaningful leading or trailing spaces are part of the password, while an empty or entirely whitespace password is rejected. Temporary password arrays used by the authentication module are cleared after each attempt. The stored account role determines which interface opens; there is no role selector.

Blank or incorrect details show `Invalid username or password.` without identifying which entry was wrong. After a failed login, the masked password field remains populated so the user can correct the attempt. A successful login clears it. **Clear** removes both fields and the message. **Log out** clears the in-memory session and returns to login.

The local store keeps its version 1 format. For safety, it accepts only
`PBKDF2WithHmacSHA256` credentials with 210,000 to 1,000,000 iterations, a
256-bit key, a 16-byte salt, and a 32-byte hash. Newly provisioned accounts use
600,000 iterations. The store must be valid UTF-8 and no larger than 1 MiB, and
it assumes one writer at a time. If any of these checks fail, authentication
reports that local accounts are unavailable rather than replacing the file.

## Build and run the packaged application

On macOS or Linux:

```bash
./gradlew release
java -jar release/FindersKeepers.jar
```

On Windows:

```text
gradlew.bat release
java -jar release\FindersKeepers.jar
```

## Test the project

Run `./gradlew test` on macOS/Linux or `gradlew.bat test` on Windows. Run `check` instead of `test` when you also want the code-quality and documentation checks.

## Role destinations

### Student

Successful Student login reaches the Student report workspace. The submission
flow is:

1. Open **Report a lost or found item**.
2. Choose **Lost** or **Found**, then enter the item name, category, location,
   date, and a public description.
3. Add a private identifying detail, such as a synthetic example of a unique
   sticker or marking. This detail is for staff verification and should not be
   copied into the public description.
4. Select **Submit report**. A valid saved report shows a confirmation with a
   report ID. The authenticated account supplies the reporter identity; there
   is no reporter-ID input for the Student to edit.

All fields are required. The date cannot be in the future. Validation messages
appear beside the relevant fields, including missing report type, item name,
category, location, date, public description, or private identifying detail.
The item name can contain at most 100 characters, the location at most 120,
and each description at most 500. Text containing only spaces is not accepted.
If storage fails, the form shows a safe retry/help message and keeps the
entered values so they can be submitted again. It does not show file paths or
technical error details. **Clear** removes all editable values, validation
messages, and feedback without submitting a report. A successful submission
also clears the editable fields.

The private identifying detail is not shown in the confirmation, and the
reporter identity comes from the signed-in session rather than user-entered
text. Use synthetic descriptions in demonstrations and do not enter real
children's identifying information.

#### View and search personal reports

Open **My reports** from the Student workspace.
The list contains only reports submitted by the signed-in Student and orders
them from newest to oldest. Each result shows the item name, whether the item
was lost or found, its category and occurrence date, its public description,
and a readable status such as **Submitted** or **Under review**.

Use the search box to search the item name or public description. Search ignores
capitalisation and surrounding spaces and matches part of a word or phrase.
Press Enter or select **Search** to apply it. A blank search shows every personal
report. Select **Clear search** to remove the query and restore the full list.

Synthetic examples:

- Searching for `pencil` finds an item named **Blue pencil case**.
- Searching for `WHITE ZIPPER` finds a report whose public description says
  `Blue case with a white zipper.`
- Searching for `umbrella` shows **No reports match your search** when none of
  the signed-in Student's item names or public descriptions contain that text.
- A Student who has not submitted a report sees
  **You have not submitted any reports yet** instead of a search-results message.

Private identifying details are never included in this history or searched.
Another Student's reports are also excluded, even if their item name or public
description matches the query. If report storage cannot be read safely, the
screen shows a retry/help message instead of incorrectly claiming the history
is empty.

## Report domain rules (Sprint 1)

The report domain is the shared contract used by the Student submission form,
personal report history, and future Desk Officer workflows.

Every report has a generated UUID report ID, the case-sensitive reporter ID, a lost-or-found type, item name, category, location, occurrence date, public description, private identifying detail, status, and creation time. New reports start with status `SUBMITTED`. The creation time is recorded as a UTC `Instant` with millisecond precision.

The public description is suitable for information that can help people recognise an item. The private identifying detail is kept separately for safe verification by a Desk Officer; it must not be copied into the public description, displayed in public summaries, or exposed in logs.

### Accepted values and limits

- Type: `LOST` or `FOUND`.
- Category: `STATIONERY`, `BOOKS`, `CLOTHING`, `BAGS`, `WATER_BOTTLES`, `ELECTRONICS`, `SPORTS_EQUIPMENT`, `PERSONAL_ITEMS`, or `OTHER`.
- Status: `SUBMITTED` or `UNDER_REVIEW`. Students do not choose this value;
  every new report starts as `SUBMITTED`.
- Reporter ID: 1–128 nonblank Unicode code points.
- Item name: 1–100 nonblank Unicode code points.
- Location: 1–120 nonblank Unicode code points.
- Public description: 1–500 nonblank Unicode code points.
- Private identifying detail: 1–500 nonblank Unicode code points.
- Occurrence date: `yyyy-MM-dd` (for example, `2026-09-19`), today or earlier.

All fields are required. Missing values and blank text are different validation errors, but both are rejected. Enum values are stored and parsed using their exact uppercase names; values such as `Lost` or `under_review` are invalid.

### Examples

Valid synthetic example:

```text
Type: LOST
Item name: Blue pencil case
Category: STATIONERY
Location: Library, shelf 2
Occurrence date: 2026-09-18
Public description: Blue case with a white zipper.
Private identifying detail: [entered privately; not reproduced in this guide]
```

Invalid examples:

- Missing location: rejected because every report needs a location.
- Item name `   `: rejected because blank text is not a name.
- Occurrence date `2026-09-20` when today is `2026-09-19`: rejected because future dates are not allowed.
- Type `STOLEN`: rejected because only `LOST` and `FOUND` are supported.
- Category `VEHICLE`: rejected because it is not an accepted category.
- A description longer than 500 code points: rejected with a field-specific length message.

### Desk Officer

Successful Desk Officer login reaches a distinct Desk Officer home. Report review and collection features remain planned for later sprints.

## Troubleshooting

- If Gradle cannot start, confirm that Java 25 is installed and that `java --version` reports the expected version.
- If a command is not found, run it from the Xcode project directory and use the platform-specific wrapper command above.
- If a release artifact is missing, run the `release` command again and check `release/FindersKeepers.jar` after it completes.
- If a documented workflow is absent, treat it as unimplemented and check the project status before reporting a defect.
- If login reports that local accounts are unavailable, do not edit or replace the credential store. Ask the project team to inspect or restore it.
