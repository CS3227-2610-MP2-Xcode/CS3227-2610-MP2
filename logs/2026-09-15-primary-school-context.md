# Primary-school context update — 2026-09-15

## Goal

Update Finders Keepers from a general community lost-and-found concept to a
lost-and-found application designed for primary schools.

## Decisions

- Rename the child-facing `Community Member` role to `Student`.
- Define a Student as a primary-school student who reports lost or found
  belongings and checks report updates.
- Retain the `Desk Officer` role and define it as the school staff member who
  reviews reports and coordinates safe item collection.
- Keep both role workflows clearly marked as planned until they are
  implemented.

## Affected areas

The application placeholder, role metadata, automated setup test, README, user
guide, developer guide, and reflection context were updated. The release JAR
must be regenerated after the source change.

## Verification

The Java 25 `clean check release` gate passed, including both JUnit tests,
Checkstyle, Javadoc, JaCoCo reporting, and universal JAR verification. The exact
regenerated `release/FindersKeepers.jar` also completed its macOS ARM64 startup
smoke test. Human review and Windows/Linux runtime verification remain pending.
