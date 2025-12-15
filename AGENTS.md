# AGENTS.md - AI Agent Guide for openhab-supla

## Project Overview

This is an **OpenHAB 5.0 binding for Supla smart home devices**, implemented as an OSGi bundle in Java 21. The binding supports two architectures: **cloud-based** (using Supla Cloud REST API) and **native server** (direct protocol communication). These implementations are strictly isolated—enforced by ArchUnit tests—to prevent architectural violations.

The project integrates Supla devices (thermostats, switches, energy monitors) into OpenHAB's home automation platform. It uses the JSupla library for protocol implementation and follows OpenHAB's handler/bridge patterns for device management.

## Repository Structure

- **`pom.xml`** - Maven build configuration (Java 21, OSGi bundle, Spotless formatting)
- **`src/main/java/pl/grzeslowski/openhab/supla/internal/`** - Production code (~8,099 lines)
  - `cloud/` - Cloud-based implementation (Supla Cloud API)
  - `server/` - Native server implementation (direct protocol)
  - `handler/` - Common handler abstractions
- **`src/main/resources/OH-INF/`** - OpenHAB metadata
  - `thing/` - Thing type definitions (XML)
  - `i18n/` - Localization (10 languages)
- **`src/test/java/`** - Test code (~2,149 lines, JUnit 5, Mockito, AssertJ)
- **`.github/workflows/`** - CI/CD pipelines (spotless, build, release)
- **`bin/`** - Utility scripts (e.g., TLS configuration)
- **`puml/`** - Architecture diagrams (PlantUML)

**Critical architectural rule**: `cloud` and `server` packages CANNOT access each other (enforced by `ArchUnitTest.java`).

## Build & Run Instructions

### Prerequisites
- Java 21 (Temurin distribution recommended)
- Maven 3.x

### Essential Commands
```bash
# ALWAYS run before committing (CI will reject unformatted code)
mvn spotless:apply

# Check code formatting
mvn spotless:check

# Run tests
mvn test

# Full build with verification (includes ArchUnit tests)
mvn verify

# Build JAR for installation
mvn clean package
```

### Installation
The JAR artifact is installed in OpenHAB via:
1. Marketplace (recommended)
2. Karaf console: `bundle:install -s url:https://github.com/magx2/openhab-supla/releases/download/supla-X.Y.Z/supla-X.Y.Z.jar`
3. Manual JAR drop in `addons/` folder

## Testing Instructions

### Test Stack
- **JUnit 5** - Test framework
- **AssertJ** - Fluent assertions (`assertThat(x).isEqualTo(y)`)
- **Mockito** - Mocking (`@Mock`, `given().willReturn()`)
- **ArchUnit** - Architecture validation (package boundaries)

### Running Tests
```bash
mvn test                  # All tests
mvn verify                # Tests + architecture validation
```

### Test Conventions
- Test files end with `Test.java`
- Use `@ExtendWith(MockitoExtension.class)` for Mockito
- Mockito uses `mock-maker-inline` (configured in `src/test/resources/mockito-extensions/`)
- **ALWAYS add tests for new features or bug fixes**

## Coding Standards & Conventions

### Code Formatting (Spotless - MANDATORY)
- **Style**: Palantir Java Format 2.43.0
- **Indentation**: Tabs (4 spaces per tab)
- **Line endings**: LF (Unix-style)
- **Import order**: Automatic
- **Command**: `mvn spotless:apply` (run BEFORE every commit)

### Lombok Usage
Heavily used throughout the codebase:
- `@Getter`, `@Data`, `@RequiredArgsConstructor`
- `@NonNull` for null checks
- `@Slf4j` for logging
- `@UtilityClass` for utility classes

### Null Safety (OpenHAB convention)
- **Class-level**: `@NonNullByDefault` (import from `org.eclipse.jdt.annotation`)
- **Field/parameter-level**: `@Nullable` for nullable elements

### Logging (SLF4J 2.0.16)
```java
// CORRECT - parameterized logging
logger.debug("Message with {} and {}", value1, value2);
logger.error("Error occurred!", exception);

// INCORRECT - string concatenation (DON'T DO THIS)
logger.debug("Message with " + value); // ❌
```

### OpenHAB Handler Patterns
- Bridge handlers extend `SuplaBridge` → `BaseBridgeHandler`
- Device handlers extend `SuplaDevice` → `BaseThingHandler`
- Override `internalInitialize()` instead of `initialize()`
- Use `InitializationException` for initialization failures
- Use `updateStatus()` for thing status changes
- Use `updateState()` for channel state updates

### Constants
- Defined in `SuplaBindingConstants.java`
- Use `ThingTypeUID` for thing types
- Use static imports for constants

### Localization
- Utility: `Localization.text(key, args...)`
- Returns format: `@text/key [arg1, arg2]`
- Supported languages: en, de, fr, it, es, pt, pl, cs, ru, uk

## Dependencies & Tooling

### Core Dependencies
- **OpenHAB 5.0.0** - Framework
- **JSupla 4.1.0** - Supla protocol implementation
- **Netty 2.0.61.Final** - Network framework (server mode only)
- **Caffeine 3.1.8** - Caching
- **Gson 2.13.1** - JSON serialization
- **OkHttp 2.7.5** - HTTP client (cloud mode)
- **BouncyCastle 1.70** - Cryptography

### Build Plugins
- **maven-compiler-plugin 3.13.0** - Java 21 compilation with Lombok
- **bnd-maven-plugin 7.0.0** - OSGi bundle generation
- **spotless-maven-plugin 2.43.0** - Code formatting (critical!)
- **maven-surefire-plugin 3.2.5** - Test execution

## Rules for AI Agents

### ✅ DO
- **ALWAYS** run `mvn spotless:apply` before committing
- **ALWAYS** run `mvn test` before committing
- **ALWAYS** read files before modifying them
- Use Lombok annotations for boilerplate (e.g., `@Getter`, `@RequiredArgsConstructor`)
- Use parameterized logging with SLF4J
- Add `@NonNullByDefault` to new classes
- Use `@Nullable` for nullable fields/parameters
- Add/update tests for new features or bug fixes
- Follow existing handler patterns in `cloud` and `server` packages
- Respect the architectural boundary between `cloud` and `server` packages
- Update README.md when user-facing behavior changes

### ❌ DON'T
- **NEVER** commit without running `mvn spotless:apply`
- **NEVER** violate the cloud/server package boundary (ArchUnit will fail)
- **NEVER** use string concatenation in log statements
- **NEVER** guess at file contents—read them first
- **NEVER** add unnecessary abstractions or over-engineer solutions
- **NEVER** create new files when editing existing ones suffices
- **NEVER** add error handling for scenarios that cannot happen
- **NEVER** add comments, docstrings, or type annotations to unchanged code
- **NEVER** use bash `cat`/`grep`/`sed`—use dedicated Read/Grep/Edit tools

## Safe Modification Guidelines

### Architecture Boundaries
- **Cloud package** (`internal.cloud.*`) uses Supla Cloud REST API
  - Dependencies: `jsupla-api`, OkHttp, Gson
  - **CANNOT** use: `jsupla-server`, Netty, protocol-java
- **Server package** (`internal.server.*`) uses native Supla protocol
  - Dependencies: `jsupla-server`, Netty, protocol-java
  - **CANNOT** use: `jsupla-api`, OkHttp

Violations cause `ArchUnitTest` failures.

### Handler Lifecycle
1. Constructor - dependency injection
2. `initialize()` - triggers `internalInitialize()`
3. `internalInitialize()` - custom initialization logic (override this)
4. `updateStatus()` - set thing status (ONLINE, OFFLINE, etc.)
5. `handleCommand()` - process user commands
6. `dispose()` - cleanup

### Adding New Device Support
1. Define thing type in `src/main/resources/OH-INF/thing/thing-types.xml`
2. Add channel definitions with OpenHAB standard item types
3. Create handler class extending `SuplaDevice` or `BaseThingHandler`
4. Implement `internalInitialize()` and `handleCommand()`
5. Add discovery support in `CloudDiscovery` or `ServerDiscoveryService`
6. Add tests in `src/test/java/`
7. Update README.md with device details

### Configuration Changes
- Thing configs: `src/main/resources/OH-INF/thing/*.xml`
- Channel types: `thing-types.xml`
- Localization: `src/main/resources/OH-INF/i18n/*.properties`
- Use `<advanced>true</advanced>` for advanced parameters

## What to Ask Before Making Changes

### Ask the user if:
1. **Adding new device support**: Which specific Supla device model?
2. **Architectural changes**: Should this be cloud-based, server-based, or both?
3. **API rate limits**: What is the acceptable API call frequency for cloud mode?
4. **Breaking changes**: Is backward compatibility required for existing configurations?
5. **Localization**: Which languages need translation updates?
6. **Security changes**: Are self-signed certificates acceptable for server mode?
7. **Dependencies**: Is upgrading OpenHAB/JSupla versions acceptable?

### Do NOT ask about:
- Code formatting style (use Spotless)
- Test framework (use JUnit 5 + Mockito)
- Logging style (use SLF4J parameterized logging)
- Build tool (use Maven)

## CI/CD Pipeline

### GitHub Actions Workflows
- **maven-main.yml** (on push to master):
  - Runs `mvn spotless:apply` and auto-commits if needed
  - Runs `mvn spotless:check verify`
  - Fails if formatting was required
- **maven-pr.yml** (on PRs):
  - Runs `mvn spotless:check verify`
- **release.yml** (manual):
  - Runs `mvn release:prepare release:perform`
  - Uploads JAR to GitHub releases

**Key takeaway**: Spotless formatting is enforced—unformatted code will fail CI.

## Additional Notes

### SSL/TLS Configuration
- Some devices use TLS 1.0/1.1 (disabled by default in Java 8+)
- Enable via `bin/update-java-tls.sh` or manual `java.security` edit
- Docker: Set `CRYPTO_POLICY=unlimited` environment variable

### Logging Configuration
- Debug logs: Add loggers to `log4j2.xml` in OpenHAB's `userdata/etc/`
- Use `[guid=%X{guid}]` pattern for device-specific log filtering
- Utility: `GuidLogger.attachGuid()` to attach device GUID to MDC

### Gateway Devices
- Gateway devices require `Supla Native Server Bridge` thing (not device thing)
- Sub-devices appear automatically in discovery after gateway connection
- GUID and Auth Key must be copied from device to bridge configuration

---

**Last Updated**: Auto-generated for AI agents. For user documentation, see `README.md`.
