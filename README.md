# api-automation

A production-ready, layered API automation framework built with **Java 17 + Rest Assured + TestNG + Allure**.

## Features
- Config-driven execution (`dev` / `qa` / `stage`) with JVM/env overrides.
- Thread-safe token cache with expiry-aware refresh (`TokenManager`).
- Centralized `RequestBuilder` (base URL, headers, auth, correlation ID, timeouts, logging, Allure attachment).
- Retry-on-5xx request handler with exponential backoff, and test-level retry listener.
- `BaseService` offering reusable GET/POST/PUT/PATCH/DELETE helpers.
- Workflow layer for API chaining — tests contain zero HTTP logic.
- `UserDataFactory` powered by Java Faker; JSON-based external test data support.
- JSON schema validation and reusable assertion utilities (status, response time, fields, headers).
- Log4j2 with per-thread correlation IDs emitted in every log line.
- Allure reporting with request/response attachments.
- TestNG parallel execution + smoke/regression grouping.
- GitHub Actions CI pipeline.

## Project structure
```
api-automation/
├── pom.xml
├── testng.xml
├── .github/workflows/ci.yml
├── src/main/java/com/automation/api/
│   ├── config/          # ConfigManager
│   ├── core/            # RequestBuilder, BaseService, RetryHandler, ApiResponse
│   ├── auth/            # TokenManager
│   ├── services/        # UserService (example)
│   ├── workflows/       # UserWorkflow (business-level chains)
│   ├── datafactory/     # UserDataFactory (Faker-based)
│   ├── models/          # User, AuthRequest, AuthResponse
│   ├── constants/       # Endpoints, Headers
│   ├── utils/           # JsonUtils, SchemaValidator, AssertionUtils, LogUtils, CorrelationIdUtil, FileUtils
│   └── exceptions/      # FrameworkException, ApiException
├── src/test/java/com/automation/api/
│   ├── tests/           # BaseTest, UserApiTests
│   └── listeners/       # TestListener, RetryAnalyzer, RetryListener
└── src/test/resources/
    ├── config.properties
    ├── config-dev.properties
    ├── config-qa.properties
    ├── config-stage.properties
    ├── log4j2.xml
    ├── allure.properties
    ├── schemas/user-schema.json
    ├── testdata/users.json
    └── suites/{smoke,regression}.xml
```

## Run
```bash
# Default (qa env, root testng.xml)
mvn clean test

# Switch environment
mvn clean test -Denv=stage

# Smoke suite
mvn clean test -Psmoke -Denv=qa

# Regression suite
mvn clean test -Pregression -Denv=qa
```

Secrets (`AUTH_USERNAME`, `AUTH_PASSWORD`, `AUTH_CLIENT_ID`, `AUTH_CLIENT_SECRET`, `AUTH_STATIC_TOKEN`)
should be passed via environment variables, never committed.

## Reports
```bash
mvn allure:serve        # opens interactive report
mvn allure:report       # writes to target/allure-report
```
