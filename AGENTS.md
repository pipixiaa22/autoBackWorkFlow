# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot backend for Audio Dialogue Studio. Production code is under `src/main/java/com/ckrey/autobackworkflow`. Organize API endpoints and request DTOs in feature `api` packages, orchestration in feature `application` packages, and provider integrations in feature-specific `llm` or `provider` packages. Shared responses, exceptions, and utilities belong in `common`.

JPA-style domain records are in `domain`; MyBatis mapper interfaces are in `mapper`, with matching XML files in `src/main/resources/mapper`. Database changes go in ordered Flyway files such as `src/main/resources/db/migration/V4__add_feature.sql`. Configuration lives in `src/main/resources/application.yaml`. Place tests under the equivalent path in `src/test/java`.

## Build, Test, and Development Commands

- `./mvnw test` — compile and run the JUnit test suite.
- `./mvnw package` — run tests and assemble the executable JAR in `target/`.
- `./mvnw spring-boot:run` — start the API locally.
- `./mvnw -DskipTests compile` — fast compile-only check when tests are intentionally deferred.

Use the Maven wrapper, rather than a machine-installed Maven version.

## Coding Style & Naming Conventions

Use Java 21 and Spring conventions: four-space indentation, one public class per file, `PascalCase` classes, `camelCase` fields/methods, and `UPPER_SNAKE_CASE` constants. Keep controllers thin and return `ApiResponse`; put workflow rules in application services. Name DTO containers `*Dtos`, controllers `*Controller`, services `*ApplicationService`, and tests `*Tests`. Keep mapper interface/XML statement names aligned.

No formatter or linter is configured; format edited code consistently with nearby files and remove unused imports before committing.

## Testing Guidelines

Tests use JUnit Jupiter and live beside the code they cover. Name behavior-focused test methods, for example `rejectsInvalidRuleValues`, and cover success paths, validation failures, and provider error handling. Run `./mvnw test` before opening a pull request; add regression tests for every bug fix and for new branching behavior.

## Commit & Pull Request Guidelines

History uses short, direct Chinese summaries (for example, `skill功能添加`); keep commits focused and describe the affected feature, such as `音频任务重试逻辑`. In pull requests, explain the behavior change, schema/configuration impact, test command and result, and link the related issue. Include API request/response examples or screenshots when endpoints or client-facing behavior changes.

## Configuration & Security

Use environment variables for database, Redis, and provider credentials (`ADS_DB_*`, `DEEPSEEK_API_KEY`, `SEED_AUDIO_API_KEY`). Do not add real secrets, generated audio, or local `data/assets` output to commits. Treat Flyway migrations as immutable once shared.
