# Finders Keepers review rules

- Prioritise correctness and security over style; Checkstyle already handles style feedback.
- Behaviour changes require relevant JUnit 5 tests, and tests must run through the Gradle Wrapper.
- Use Java 25 and the Gradle Wrapper. The authoritative gate is `./gradlew clean check release` (or `gradlew.bat clean check release` on Windows).
- Do not commit real credentials or private student or item data.
- Preserve the Student versus Desk Officer authorisation boundary as those workflows are added.
- Treat Finders Keepers as a primary-school lost-and-found application using JavaFX 25.0.4. Do not claim planned or currently unimplemented architecture as fact.
