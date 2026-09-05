# operatorsJSON

A Java/Spring Boot integration-validation service originally built to automate acceptance testing for external operator integrations with a transactional JSON API.

The project is more than a collection of test scripts: it stores per-operator integration configuration, executes stateful API scenarios against client endpoints, calculates expected transactional state, validates response fields, and returns structured diagnostics with request/response/log context.

> **Project status:** historical/portfolio project. The code reflects an older production-support tool and is kept primarily as an example of automation and integration engineering. It is not intended to be deployed to the public Internet as-is.

## What it validates

The test flow covers positive and negative integration scenarios such as:

- authentication and repeated authentication;
- debit, credit and rollback flows;
- retry/idempotency behavior;
- rollback before debit and debit after rollback;
- insufficient funds;
- invalid session token and unknown user;
- negative or invalid amounts;
- missing, unknown and already-processed transaction IDs;
- invalid request signatures (HMAC/hash);
- invalid game/bet identifiers.

For every case the service can return:

- the generated request;
- the actual client response;
- a dynamically generated expected response;
- per-field validation results;
- relevant HTTP/test logs.

Validation distinguishes mandatory and optional fields and reports issues such as missing keys, wrong capitalization, incorrect data types, invalid values, unexpected error codes and formatting problems as errors or warnings.

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

## Technology

- Java 11
- Spring Boot
- Spring Web / REST
- Spring Data JPA
- PostgreSQL
- Retrofit / OkHttp
- Jackson / Gson / org.json
- Gradle

## Local configuration

No runtime credentials are stored in the repository. Copy the values from `.env.example` into your local environment and replace all placeholder values.

Required for normal authenticated operation:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
PASSWORD_HMAC_KEY
```

Optional settings include `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD`, `OUTBOUND_PROXY_URL`, `IP_CHECK_URL`, `HIBERNATE_DDL_AUTO` and `PORT`.

A bootstrap administrator is created only when `BOOTSTRAP_ADMIN_PASSWORD` is explicitly configured.

## Security note

This is an older portfolio project. Current source has been sanitized so credentials are supplied through environment variables and sensitive password/integration-key fields are not serialized in API responses or printed in object `toString()` output.

Older Git history may contain credentials that were used by retired development infrastructure. Those values must be treated as compromised and must never be reused. A full history rewrite is intentionally kept separate from the source cleanup because it changes repository history for every clone.

## What I would redesign today

The original scenario set grew organically and too much orchestration eventually accumulated in `PrepareResult`. In a new version I would split it into separate responsibilities, for example:

- scenario definitions implementing a common interface;
- request generation;
- execution/transport;
- run-state management;
- expected-state calculation;
- response validators;
- reporting/logging.

I would also replace the in-memory mutable test-state representation with typed run-state objects, make parallel execution/isolation explicit, use a modern security framework for user authentication, and add containerized test environments plus CI validation.
