# Finders Keepers Domain

Finders Keepers records lost and found belongings for a primary school and supports the people responsible for reporting and reviewing them.

## Language

**Student**: A primary-school student who reports a lost or found belonging and checks its progress. _Avoid_: Community Member, reporter

**Desk Officer**: A school staff member who reviews reports and coordinates safe item collection. _Avoid_: administrator, staff user

**Item Report**: The canonical, immutable record of one lost-or-found report, including its identity, descriptions, status, and creation time. _Avoid_: persistence DTO, report record, temporary report

**Report ID**: The globally unique identity of an Item Report. It remains unchanged for the life of the report. _Avoid_: database ID, row ID

**Reporter ID**: The stable identity of the Student who owns an Item Report. It remains unchanged when the report is updated. _Avoid_: username, display name

**Report Store**: The authoritative local collection of Item Reports preserved across application runs. _Avoid_: cache, backup, database
