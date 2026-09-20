# S1-D2-01 Local Authentication and Role Navigation

- Status: Approved for implementation
- Owner: Developer 2
- Governing instructions: `AGENTS.md`

## Goal

Implement local username-and-password authentication so an existing user can log in and reach the interface associated with their stored role.

The completed flow is:

```text
Application start
    ↓
Login
    ↓
Local credential verification
    ↓
Authenticated session
    ├── Student interface
    └── Desk Officer interface
```

## User outcome

A user with valid local credentials can:

1. Enter a username and password.
2. Log in.
3. Reach the Student or Desk Officer interface according to the role stored in their account.
4. Log out and return to the login screen.

Invalid credentials produce a clear, generic error without revealing whether the username or password was incorrect.

## Scope

Developer 2 implements:

- Login screen with username and password fields
- Login and clear actions
- Local user-account representation
- Local user repository abstraction
- Local credential verification
- Password hashing and verification
- Authenticated in-memory session
- Role-based navigation after login
- Logout and session clearing
- Safe handling of missing, unreadable, or corrupt user storage
- A one-time local account-provisioning utility
- Focused authentication, provisioning, and navigation tests
- Authentication and role-navigation documentation

## Non-goals

This task does not implement:

- In-application user registration
- Password recovery or password changes
- Remote authentication or external identity providers
- Multi-factor authentication
- Persistent login or "remember me"
- Account administration
- Student report submission
- Desk Officer report review
- Authorization beyond routing to the stored role
- Changes to report-domain classes or validation

## Terminology

Use the canonical roles:

- `STUDENT`
- `DESK_OFFICER`

Older references to `Community Member` must not introduce a third role.

The authenticated role comes from the stored user account. A user must not select or supply their role during login.

## Functional requirements

### Login

- The application presents the login interface before either role interface.
- Username and password are required.
- Usernames are trimmed before lookup.
- Username matching is case-insensitive.
- Password matching is case-sensitive and passwords are not trimmed.
- Blank or invalid credentials produce the same generic message: `Invalid username or password.`
- A failed login does not create a session or change the current route.
- The password field masks entered characters.
- The clear action removes the username, password, and current validation message.

### Successful authentication

- A valid Student account opens the Student interface.
- A valid Desk Officer account opens the Desk Officer interface.
- Navigation uses the role stored in the authenticated account.
- Passwords, hashes, and salts are not retained in the authenticated session.
- The session contains only the minimum identity information required by the application, such as user ID, username, and role.

### Logout

- Logout clears the authenticated session.
- Logout returns to the login screen.
- After logout, role-specific screens cannot be reopened through stale navigation state.

## Credential storage

All application accounts are stored locally. The local account store must contain only the information required for authentication and routing:

- User ID
- Username
- Role
- Password hash
- Unique password salt
- Password-hashing algorithm and work-factor parameters when required for future verification or migration

Real credentials must never be exposed in source code, logs, documentation, screenshots, test output, or handoff summaries. Explicitly synthetic demo credentials may be documented in the User Guide when clearly labelled as public demonstration accounts that must not be reused for real systems.

No password, including a synthetic demo password, may be stored in plaintext in the application's credential store. Password hashing must:

- Use a dedicated password-hashing or password-based key-derivation function.
- Use a cryptographically secure, unique random salt for each account.
- Compare derived password values without ordinary string equality.
- Keep algorithm parameters with the stored credential when they are needed for verification.
- Allow the hashing implementation to be replaced without changing the login UI.

## Storage behaviour

- The user-storage path is supplied to the repository rather than hard-coded into authentication logic.
- Tests use isolated temporary storage.
- An absent user-storage file is handled as "no local accounts configured."
- An unreadable or corrupt file produces a user-safe error and does not crash the application.
- Corrupt storage is not silently replaced or overwritten.
- Error messages and logs must not include credential contents.

## Architecture boundary

The authentication design should separate:

```text
Login UI
    ↓
Authentication service
    ↓
User repository interface
    ↓
Local user repository

Authentication service
    ↓
Password hasher
```

The JavaFX login interface must not:

- Read the user-storage file directly
- Hash passwords directly
- Decide the authenticated role
- Construct role-specific application data
- Contain report submission or review behaviour

## Ownership and integration

This task stays within Developer 2's ownership defined by `AGENTS.md`.

The following require shared integration or approval from Developer 1:

- Connecting the login flow to the existing application entry point
- Changing Gradle dependencies
- Changing the initial JavaFX application shell
- Changing shared role or status definitions
- Changing release or CI configuration

Before making one of these changes, report:

1. The affected file
2. Why integration requires the change
3. The smallest proposed modification
4. Whether Developer 1 can make the modification instead

Do not create a second application entry point or duplicate shared types to bypass integration.

## Account provisioning

Provide a one-time local provisioning utility for creating the synthetic Student and Desk Officer accounts used in development and demonstration.

The utility must:

- Accept a user ID, username, role, and password.
- Permit only the `STUDENT` and `DESK_OFFICER` roles.
- Reject missing, blank, or duplicate identifiers and usernames.
- Hash the password with a unique random salt before writing the account.
- Write only the password hash, salt, and required algorithm parameters to the credential store.
- Avoid printing the supplied password, hash, or salt.
- Report success without exposing credential contents.
- Refuse to overwrite an existing account unless an explicit replacement operation is approved separately.

Provision at least one synthetic Student account and one synthetic Desk Officer account for the sprint demonstration. Their public demo usernames and passwords may be listed in the User Guide when clearly labelled as synthetic, non-secret credentials.

Normal registration and account administration remain outside this task.

## Required tests

Automated tests must cover:

- Successful Student authentication
- Successful Desk Officer authentication
- Unknown username
- Incorrect password
- Blank username
- Blank password
- Username trimming and case behaviour
- Password case sensitivity
- Stored role determines navigation
- Failed login creates no session
- Logout clears the session
- Password verification succeeds for the correct password
- Password verification fails for an incorrect password
- Separate accounts receive separate salts
- Persisted credentials do not contain plaintext passwords
- Provisioning both supported roles
- Rejecting invalid roles
- Rejecting duplicate identifiers and usernames
- Missing user-storage file
- Corrupt user-storage file
- Tests use temporary storage and do not modify real user data

UI behaviour that cannot be covered without introducing an unapproved dependency must be manually verified and recorded as such.

## Definition of done

This task is complete when:

- Valid Student and Desk Officer accounts reach visibly separate interfaces.
- Invalid credentials produce the generic login error.
- The authenticated role cannot be selected or forged through the login UI.
- Logout clears the session and returns to login.
- No plaintext password is persisted or logged.
- Missing and corrupt storage fail safely.
- The provisioning utility creates both synthetic demonstration accounts with hashed credentials.
- The User Guide identifies any published demo credentials as synthetic and non-secret.
- Every required automated test passes.
- `gradlew.bat check` or `./gradlew check` passes.
- The final Git diff remains within the approved ownership boundary.
- Any entry-point integration required from Developer 1 is identified or completed with explicit approval.
- Authentication behaviour and limitations are documented.

## Handoff evidence

The final handoff must report:

- Authentication design and storage format
- Files changed
- Automated and manual checks performed
- Test and check results
- Synthetic accounts provisioned for demonstration
- Security or usability limitations
- Integration changes requested from or approved by Developer 1
- Requirements that remain unverified
