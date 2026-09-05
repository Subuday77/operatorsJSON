# operatorsJSON

A Java/Spring Boot integration-validation service originally built to automate acceptance testing for external operator integrations with a transactional JSON API.

The project is more than a collection of test scripts: it stores per-operator integration configuration, executes stateful API scenarios against client endpoints, calculates expected transactional state, validates response fields, and returns structured diagnostics with request/response/log context.

> **Project status:** historical/portfolio project. The code reflects an older production-support tool and is kept primarily as an example of automation and integration engineering. It is not intended to be deployed to the public Internet as-is.

## What it validates

The test flow covers positive and negative integration scenarios such as authentication, debit/credit/rollback flows, retry and idempotency behavior, insufficient funds, invalid tokens/users/amounts, missing or reused transaction IDs, invalid request signatures and invalid game/bet identifiers.

For every case the service can return the generated request, the actual client response, a dynamically generated expected response, per-field validation results and relevant HTTP/test logs. Validation distinguishes mandatory and optional fields and reports missing keys, wrong capitalization, data-type errors, invalid values, unexpected error codes and formatting problems as errors or warnings.

## Architecture

```text
                     REST / UI
                        |
                        v
                 Spring Controllers
                  /             \
                 /               \
      user/operator management   test execution
              |                       |
              v                       v
        DAO / Spring Data        scenario engine
              |                       |
              v                       v
          PostgreSQL           request builders
                                      |
                                      v
                             Retrofit / OkHttp
                                      |
                                      v
                              External client API
                                      |
                   +------------------+------------------+
                   |                                     |
                   v                                     v
           state calculation                     response validation
                   |                                     |
                   +------------------+------------------+
                                      |
                                      v
                          expected vs actual result
                           + diagnostics + logs
```

### Persistent vs runtime integration data

`Operator` stores stable integration configuration such as the client URL, endpoint names and request-signing key. `OperatorsDynamicConfig` stores values discovered or changed during a run, such as session token, UID, currency, round information and balance state.

The execution engine keeps state across scenarios so later tests can validate results against the sequence of earlier debit/credit/rollback operations instead of treating every HTTP call as an isolated assertion.

## Account model

The original application used four access levels:

- `0` — operator/client account;
- `1` — KAM account;
- `2` — Integrator/administrator;
- `3` — recovery administrator.

The recovery account is intentionally not a normal superuser. It can manage only level-2 Integrator accounts, allowing an administrator to be recreated, re-enabled, renamed, deleted or have a configured temporary password restored after an operational failure. It cannot access operator configuration or execute integration tests.

The legacy Angular client keeps the HMAC-derived login credential returned by `/login` in memory and sends that value on subsequent requests. For compatibility, that credential is still serialized in authenticated login responses. Integration signing keys are also returned to authorized users because the legacy UI allows Integrators to view and edit operator configuration. Sensitive values are redacted from `toString()` output and must not be written to application logs.

## Technology

- Java 11
- Spring Boot
- Spring Web / REST
- Spring Data JPA
- PostgreSQL
- Retrofit / OkHttp
- Jackson / Gson / org.json
- Gradle
- GitHub Actions

## Local configuration

No runtime credentials are stored in the current source tree. Copy the values from `.env.example` into your local environment and replace all placeholders.

Required for normal authenticated operation:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
PASSWORD_HMAC_KEY
```

Optional settings include `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD`, `RESET_PASSWORD_LEVEL_0`, `RESET_PASSWORD_LEVEL_1`, `RESET_PASSWORD_LEVEL_2`, `OUTBOUND_PROXY_URL`, `IP_CHECK_URL`, `HIBERNATE_DDL_AUTO` and `PORT`.

A recovery administrator is created only when `BOOTSTRAP_ADMIN_PASSWORD` is explicitly configured. Password-reset endpoints use only reset values configured in the environment; no default passwords are hardcoded in source.

## CI

Pull requests and pushes to `master` run `./gradlew clean test`. Tests use an in-memory H2 database so CI does not require production database credentials.

## Security note

This is an older portfolio project. Current source externalizes database, proxy, bootstrap and HMAC configuration and redacts credentials, signing keys and runtime tokens from object logging.

The authentication protocol itself is legacy and would be replaced in a new implementation with Spring Security, session/token-based authentication and dedicated API DTOs rather than exposing persistence entities directly.

Older Git history may contain credentials that were used by retired development infrastructure. Those values must be treated as compromised and must never be reused. A full history rewrite is intentionally separate from the source cleanup because it changes repository history for every clone.

## Frontend

The original Angular UI is maintained separately. The backend repository still contains a historical gitlink named `frontend`; it has no usable `.gitmodules` entry and is not required for the backend build. It should be removed from Git history with normal Git tooling rather than treated as a working submodule.

## What I would redesign today

The original scenario set grew organically and too much orchestration eventually accumulated in `PrepareResult`. In a new version I would split it into separate responsibilities:

- scenario definitions implementing a common interface;
- request generation;
- execution/transport;
- per-run state management;
- expected-state calculation;
- response validators;
- reporting/logging.

I would also replace the mutable global test-state representation with typed per-run objects, make parallel execution and isolation explicit, use `BigDecimal` for money, move authentication and authorization to Spring Security, use explicit request/response DTOs, add containerized integration-test environments and expand CI beyond the current context/build check.
