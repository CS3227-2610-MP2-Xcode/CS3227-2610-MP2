# Finders Keepers User Guide

## Current status

Finders Keepers is intended for a primary school's lost-and-found desk.
Application startup opens the local login, with Student and Desk Officer
routing and logout available after authentication. The Desk Officer route now
opens the active report-review queue backed by `data/reports.json`.

The Desk Officer route also provides the possible-match workspace and stores
officer-created relationships separately in
`data/possible-match-links.txt`.

The Student report form and personal report-history/search components are
available from the authenticated Student workspace. Reports are stored in the
project-local `data/reports.json` file. The application creates this local,
untracked runtime file after the first successful report submission.

Both workspaces also include **Claims**. A Student can submit and track an
ownership claim for an officer-created possible match; a Desk Officer can
review, approve, or reject that claim. Claims are retained separately in the
local `data/claims.json` runtime file after the first successful submission.

The Student and Desk Officer workspaces also include **Appointments**. Slots
are 30 minutes long, use Singapore time, and are served by one collection
desk. Appointment data is retained separately in the local, untracked
`data/appointments.json` runtime file.

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

#### Submit and track an ownership claim

Open **Claims** from the Student workspace. **Available matches** lists only
officer-created possible-match links where one of your lost reports is paired
with a found report. It shows your lost item and the found item's name,
category, occurrence date, and location; it does not show another person's
identity, descriptions, or private identifying details. A possible match is a
lead for review, not proof that an item belongs to you.

To submit a claim:

1. Select **Claim this found item** for the relevant found-item card.
2. Enter ownership evidence, such as a synthetic description of a distinctive
   feature. Evidence is required, is trimmed, and must be valid and at most 500
   characters.
3. Select **Review claim**, verify the displayed match summary and evidence,
   then confirm **Submit claim**.

A successful submission opens **My claims** and shows `Claim submitted for
Desk Officer review.` The claim is then **Pending review**. You can have only
one active claim involving a particular lost or found report. If you already
have an active claim for a lost item, **Available matches** directs you to that
existing claim instead of offering another submission.

**My claims** shows only your claims, newest first. Select a row to see its
reference, status, submitted/finalized time, your evidence, and the safe match
summary. The possible statuses are **Pending review**, **Approved**,
**Rejected**, and **Withdrawn**. Approval and rejection reasons are shown when
one was recorded.

You may select **Withdraw claim** only while the claim is **Pending review**.
Confirming withdrawal is final, records no withdrawal reason, and cannot be
undone. It does not alter either report or the possible-match link. Approved,
rejected, and withdrawn claims are final and cannot be decided or withdrawn
again.

Use **Refresh** to load current matches or claims, and **Retry** after an
unavailable-load message. A failure to submit or withdraw does not show a
false success. Logging out clears the on-screen claim data and unfinished
evidence text; saved claims remain available after a later login.

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

Successful Desk Officer login automatically loads the active report queue.
Reports whose status is **Submitted** or **Under review** appear in their stored
insertion order, except that both reports involved in an approved claim are
hidden from this queue. Approval does not change or delete either report. The
row summary contains only the report's Lost/Found type, item name, category,
occurrence date, and location.

Use the review queue as follows:

1. Choose **All**, **Lost**, or **Found**. **All** is selected on entry.
2. Select a row to display its complete read-only report details. The public
   description and the private identifying detail appear in separate sections;
   the private section is reserved for Desk Officer verification.

Changing filters retains a selection only while the selected report remains
visible. Without a selection, the details area asks you to select a report.
This view does not change report statuses.

An empty All queue shows `No active reports.` A Lost or Found filter with no
matching active report shows the corresponding filter-specific message. These
empty states are not storage errors.

If reports cannot be loaded, the view shows
`Reports are unavailable. Please try again.` and an explicit **Retry** button.
This unavailable state can result from either report storage or claim storage;
technical storage details and report values are not included in error messages.

Logging out removes the queue and any selected private detail. A later Desk
Officer login starts again with the All filter and reloads the report store.
Report editing, returning a report to Submitted, collection, ownership
verification, and return workflows remain outside this feature.

#### Review possible matches

Desk Officer login opens a tabbed workspace. **Report review** remains the
default tab. Open **Possible matches** to load current reports and
officer-created links.

The Suggestions list contains deterministic LOST-to-FOUND possible matches.
Rows show each report's type, item name, category, occurrence date, and
location, plus rule points and brief reasons. Rows never show Reporter ID,
descriptions, status, creation time, or private identifying detail. A score is
rule evidence only; it is not confidence and does not prove ownership.

The four rules are exact category equality (40 points), shared item-name
keyword (20), same normalized location (30), and FOUND occurring zero to seven
days after LOST (10). Category and date are mandatory. At least 70 points are
required, so a qualifying pair must also share a keyword or normalized
location. Punctuation and whitespace separate words; the workflow does not use
fuzzy, probabilistic, AI, description, or private-detail matching.

Use the workspace as follows:

1. Select a suggestion to compare both canonical reports side by side. The
   comparison is read only and shows all report fields, separates public and
   private descriptions, and labels private detail as officer-only verification
   information.
2. Review all four component outcomes and their rule points. Selecting or
   refreshing a pair changes neither report nor link state.
3. Select **Link as Possible Match** only when the pair should remain recorded
   for officer review. Success appears only after the separate relationship is
   saved. Linking does not change either report or confirm ownership.
4. Select a row under **Linked possible matches** and use **Unlink Possible
   Match** to remove only that relationship. Unlinking does not assert that the
   items differ and does not affect another link.
5. Use **Refresh** for a new authoritative report/link load. Use **Retry** when
   loading is unavailable. There is no automatic polling or retry.

A report may appear in several links. Linked pairs stay available for Unlink
even if they later stop qualifying or one report is unavailable. The workspace
distinguishes no eligible LOST-to-FOUND pair, no qualifying pair, all qualifying
pairs already linked, no linked relationships, and unavailable storage.
Failed Link or Unlink keeps clearly labelled last-known state and never shows
success.

Logging out removes suggestions, comparisons, reasons, Reporter IDs, and
private details from the screen. Durable links remain stored. A later Desk
Officer login performs a fresh load and restores no previous selection.

#### Review Claims

Open **Claims** from the Desk Officer workspace. **Pending review** lists
pending claims from oldest to newest. Select a claim to view the claimant,
ownership evidence, claim timestamps, and the current full lost and found
reports, including their private identifying details. Treat the evidence and
private details as officer-only verification information.

To decide a selected pending claim:

1. Review the claim and both reports.
2. Enter an optional approval reason, or a required rejection reason. A reason
   that is provided must be valid and at most 500 characters.
3. Select **Approve** or **Reject**, then confirm the final action.

If **Reject** is selected without a valid reason, the screen shows a validation
message and does not open the confirmation or save a decision.

Approval or rejection is final and moves the claim out of **Pending review**.
A rejected claim always has a reason; an approved claim can omit one. If
another decision has already finalized the claim, the workspace refreshes its
current final status instead of recording a second decision. Decisions are
disabled if either current report cannot be loaded.

Open **Claim history** to view final claims. Use **All**, **Approved**,
**Rejected**, or **Withdrawn** to filter the history; it is ordered newest
finalized first. Selecting a history entry shows its retained claim details
and any available current reports. An approved claim removes both of its
reports from the active **Report review** queue, but does not change the
reports themselves or a possible-match relationship.

Use **Refresh** for a current load and **Retry** after an unavailable-load
message. If a decision cannot be saved, the claim remains pending and the
application reports no success. Logging out clears claim and report details
from the screen; saved claim decisions remain available after a later login.

### Student appointments

Open **Appointments** after a Desk Officer has approved one of your Claims.
Pending, rejected, and withdrawn Claims cannot be used for booking. Select an
approved Claim, select an available 30-minute slot, and choose **Book selected
slot**. A successful confirmation displays the Claim reference and Singapore
date/time. If no approved Claim or future slot is available, the screen shows
an empty-state explanation rather than an error.

Only one appointment can use a slot. The booking operation checks the current
Claim approval and slot availability again when it saves, so a stale screen
cannot create a second booking. If another Student takes the slot first,
refresh and choose another slot.

Select an active appointment to reschedule it to another available slot or to
cancel it. Rescheduling keeps the appointment history while moving the same
booking to a new slot. Cancellation releases the slot; you may book again for
the same approved Claim. Changes are not accepted after the slot has started.

The history list includes cancelled appointments, no-shows, and closed cases.
Storage locations and officer-only custody details are never shown to Students.

### Desk Officer appointments and custody

Open **Appointments** and enter a future start as `yyyy-MM-dd HH:mm` in
Singapore time. Starts must be on the hour or half-hour. Select **Create
30-minute slot**. Overlapping enabled slots are rejected because the service
has one collection desk. An unbooked future slot can be disabled; a booked
slot cannot be silently removed.

Select a booked case to record its storage location and mark custody ready.
At the appointment, choose **Confirm collection** to record the collection
time and officer action. Then choose **Mark item returned**, followed by **Close
case**. Invalid orderings are rejected and do not change stored data. If the
Student does not attend, use **NO_SHOW** after the slot has ended; the slot is
released and the Student may book another appointment.

**NO_SHOW** is for a missed appointment, not a step in returning an item. It
cannot be recorded before the 30-minute slot ends or after collection has been
confirmed. If the screen says the custody order is invalid, record a storage
location and mark the item ready before confirming collection. Confirm
collection before marking the item returned, and mark it returned before
closing the case.

The officer audit history records booking, rescheduling, cancellation,
no-show, custody, collection, return, and closure events. Officer identities
are visible in this officer-only history. Evidence, report descriptions, and
storage locations are not copied into audit-event text.

## Troubleshooting

- If Gradle cannot start, confirm that Java 25 is installed and that `java --version` reports the expected version.
- If a command is not found, run it from the Xcode project directory and use the platform-specific wrapper command above.
- If a release artifact is missing, run the `release` command again and check `release/FindersKeepers.jar` after it completes.
- If a documented workflow is absent, treat it as unimplemented and check the project status before reporting a defect.
- If login reports that local accounts are unavailable, do not edit or replace the credential store. Ask the project team to inspect or restore it.
