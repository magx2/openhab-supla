# AI Contributor Guide

Welcome to the Supla ↔ OpenHAB binding. Follow these rules when making changes:

## Scope
These instructions apply to the entire repository unless a more specific `AGENTS.md` is added in a subdirectory.

## Project Overview
- This is a Java 21 project providing an OpenHAB binding for Supla (both cloud and native/server modes). Maven is the build system (`pom.xml`).
- The binding is an OpenHAB add-on built on the OSGi architecture; the bundle configuration and packaging live in the root `pom.xml`.
- Core code lives under `src/main/java/pl/grzeslowski/openhab/supla/internal/` with separate packages for cloud and server implementations; tests are under `src/test/java/`.
- Configuration and OpenHAB metadata live in `src/main/resources` (e.g., `OH-INF`).

## Coding Guidelines
- Prefer Lombok annotations already used in the codebase (e.g., `@Getter`) instead of manually writing boilerplate.
- Maintain OpenHAB's null-safety conventions: annotate classes with `@NonNullByDefault` and use `@Nullable` where appropriate.
- Follow existing logging style with SLF4J (`logger.debug("message {}", value);`), avoiding string concatenation in log statements.
- Keep handler initialization and configuration validation consistent with existing patterns in `...internal.server` and `...internal.cloud` packages.
- Update documentation (e.g., `README.md`) when user-facing behavior or setup changes.

## Testing & Validation
- Before committing, format the codebase with Spotless: `mvn spotless:apply`.
- Run the Maven test suite from the repository root before committing: `mvn test`.
- If you add new features or bug fixes, include or update JUnit 5 tests under `src/test/java` when feasible.

## Pull Requests & Commits
- Keep commits focused and descriptive.
- In the final PR body, summarize key changes and mention test coverage (commands executed) when applicable.
