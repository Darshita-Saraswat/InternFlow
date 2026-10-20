# InternFlow Implementation Plan

**Status:** Backend/database foundation, authentication, public registration/login pages, the application workflow, student profile management, and task management are implemented and verified in this checkout. Fit assessment, dashboard, calendar, analytics, and other later phases remain.

## Phase 0 Audit

**Current checkout note (2026-10-19):** The audit below records the original scaffold state. The project now has an initialized Git repository on `main`, but no commits and no tracked files; 79 project files are therefore shown as untracked. This is not an indication that those files are disposable, and none were staged or removed during this task.

Inspected the full contents of `AGENTS.md`, `MASTER_PROMPT.md`, and `RESEARCH_NOTES.md`, listed the workspace directories, searched the project folder for all files, checked Git status, and checked local tool availability.

| Area | Present | Missing / status |
| --- | --- | --- |
| Project guidance | `AGENTS.md` | No conflict with the Java/Spring Boot, PostgreSQL, and Next.js requirements. |
| Product/implementation requirements | `MASTER_PROMPT.md` | Provides detailed target scope and acceptance criteria; no code exists yet. |
| Research | `RESEARCH_NOTES.md` | Useful stack/product context; version baseline is further stated in `ARCHITECTURE.md`. |
| Application source | None | No frontend, backend, schema, tests, manifests, README, or environment examples. |
| Git | Git executable present | Current project folder is not inside a Git repository (`git status` reports not a repository). |
| Local tools | Java 21.0.12.1, Node 24.22.0, npm 11.19.1, Docker/Compose, Git | Selected runtime paths are available. PostgreSQL is running in Docker; `psql` is available inside its container. |

There is no existing Python backend or other implementation to migrate. Nothing was deleted or replaced. The only newly planned paths are the three Phase 0 documents in `docs/`.

## Proposed Repository Layout

```text
InternFlow_AI_Build_Pack/
  AGENTS.md
  MASTER_PROMPT.md
  RESEARCH_NOTES.md
  README.md
  .gitignore
  .env.example
  compose.yaml
  docs/
    PRODUCT_SPEC.md
    ARCHITECTURE.md
    IMPLEMENTATION_PLAN.md
  frontend/
    app/
    components/
    features/
    lib/
  backend/
    mvnw
    mvnw.cmd
    pom.xml
    src/main/java/
    src/main/resources/db/migration/
    src/test/java/
```

Create only the directories/files needed by the phase being implemented. Preserve all three existing instruction/research files. If this folder is the intended project root, initialize Git before implementation and make the first baseline commit only with the student's approval; no commit is part of this plan by default.

## Phased Work and Verification Gates

### Phase 1 - Design System and Frontend Shell

- Select Node 24 LTS and confirm the installed Next.js 16.4, React 19.3, and TypeScript 6.0.3 compatibility. Scaffold the frontend and lock resolved package versions.
- Define semantic CSS variables and reusable accessible primitives; use the specified palette as a starting point, not as hard-coded component colors.
- Implement app layout, responsive navigation, shared loading/error/empty states, and route placeholders with intentional destinations.
- If a preview dashboard is built at this point, label every record as demo data and keep the preview explicitly separate from real-user flows.
- **Gate:** frontend lint, typecheck, production build, and responsive/keyboard checks pass. No nonfunctional controls.

**Status (2026-10-16):** The public landing and authentication pages exist and frontend lint/build have passed, but the full reusable design system and authenticated multi-route shell were not completed as a separate phase. The applications workflow adds a small authenticated workspace header; the broader shell remains unfinished.

### Phase 2 - Java Backend Foundation

- Scaffold Java 21 / Spring Boot 4.1.1 with Maven Wrapper, PostgreSQL driver, Flyway, Spring Data JPA, validation, Actuator, OpenAPI, and tests.
- Add local configuration, environment-variable examples, and a reproducible PostgreSQL path (Docker Compose or documented local installation).
- Add versioned initial migrations and a safe health endpoint. Keep schema migration ownership in Flyway.
- **Gate:** Maven build/test against PostgreSQL; Flyway applies migrations to a fresh application schema; health endpoint exposes no secrets.

**Current implementation result (2026-10-16):** Java 21 compilation is enforced with `maven.compiler.release=21`. The `mvnw.cmd verify` gate passed against the existing PostgreSQL 18.6 container without resetting its volume. Flyway V1 and V2 were applied successfully. The schema includes `users`, `student_profiles`, `opportunities`, `applications`, `application_events`, `tasks`, and `notification_preferences`, plus the two Spring Session JDBC tables. Tests cover migration history/schema and foreign keys, public health and OpenAPI docs, protected API 401 behavior, and validation/malformed-body error responses. Hibernate uses `ddl-auto=validate`; Spring Session schema initialization is disabled so Flyway remains the schema owner. The clean goal currently hits a Windows file lock under `backend/target`, but the regular `verify` goal succeeds.

### Phase 3 - Authentication and Ownership (implemented)

- Implement registration, login, logout, current-user endpoint, BCrypt, server-side session storage, CSRF behavior, and safe validation/error responses.
- Add user and profile foundations; create test fixtures that do not leak across accounts.
- **Gate:** unauthenticated requests fail; password hashes are not plaintext; cross-user application/task access tests prove read/update/delete isolation. **Verified:** registration, duplicate rejection, successful/failed login, logout invalidation, CSRF rejection and acceptance, session/CSRF cookie attributes, private endpoint rejection, BCrypt hash-at-rest, session credential erasure, and cross-user application/task read/update/delete tests pass.

**Current implementation result (2026-10-16):** Registration and login are available at `/register` and `/login`, and use the same-origin typed API client. The backend uses BCrypt, JDBC-backed Spring Security sessions, an HttpOnly `INTERNFLOW_SESSION` cookie with SameSite=Lax and environment-controlled Secure, plus a readable SameSite=Lax CSRF cookie/header pair. Passwords and session identifiers are not stored in browser localStorage. After login, the signed-in state links into the authenticated applications workspace.

### Phase 4 - Application Workflow

- Implement user-scoped opportunity/application CRUD, validation, status transitions, event history, search/filter/pagination, and typed frontend API integration.
- Build application list, create/edit, detail, and delete confirmation around real API data.
- **Gate:** CRUD and status/activity tests pass; browser workflow works against PostgreSQL; ownership tests remain green.

**Current implementation result (2026-10-16):** Added authenticated application create/list/detail/update/delete API operations using the existing V1 schema, user-scoped SQL for every read and mutation, text/status filters, pagination, request validation, and creation/status/update event history. Event rows use wall-clock occurrence timestamps so multiple changes within a transaction retain correct order. Added a same-origin typed frontend client and authenticated applications list/create/detail/edit/delete/sign-out pages. The 12 authentication/application integration tests pass, and the full backend suite passes (19 tests, 0 failures/errors/skips). A live HTTP workflow against PostgreSQL verified create (201), list/filter, detail/activity, update/status, and owner delete (204); a second account received an empty list and 404 on guessed-ID detail/update/delete, without altering the owner's record. The two uniquely generated HTTP-verification accounts were cleaned up by exact email. The existing PostgreSQL 18.6 Compose container and named volume were preserved; no migration or database reset was performed.

### Phase 5 - Profile, Tasks, Fit, Dashboard, Calendar, Analytics

- Implement one domain at a time: student profile, tasks, deterministic fit assessment, then real-record dashboard/calendar/analytics.
- Define date/time semantics and analytics denominators before presenting results.
- **Gate:** service/unit tests cover empty data, nullable dates, fit matches/unknowns/blockers, task due states, and metric calculations; UI empty/loading/error states work.

**Student profile slice (2026-10-19):** Added authenticated GET/PUT profile operations and a responsive `/profile` editor backed by the existing V1 `student_profiles` table; no migration was needed. Ownership comes only from the authenticated principal. Profile service and integration coverage adds 7 tests; the full backend suite passed with 26 tests, 0 failures, 0 errors, and 0 skipped. Frontend lint passed on Node 24, and the production build passed from an isolated copy outside OneDrive. Direct OneDrive build verification remains avoided because of the known generated-output EPERM issue.

**Task management slice:** Restored the task API, service/repository/DTOs, validation and service tests, PostgreSQL-backed CRUD/filter/ownership integration tests, and authenticated `/tasks` list/create/detail/edit UI. The existing V1 schema already enforces same-owner task/application linking, so no migration was added or changed. On Java 21, `mvnw verify` passed all 37 backend tests (0 failures, 0 errors, 0 skipped); frontend lint passed on Node 24. The production build is run from an isolated copy outside OneDrive to avoid touching the existing `.next` output.

### Phase 6 - CSV and Demo Readiness

- Add CSV export, then import preview/validation/duplicate handling and import summary.
- Add opt-in demo data isolated from real accounts; polish keyboard/mobile/accessibility behavior.
- **Gate:** malformed CSV cases are safe and summarized; exported records are user-scoped; demo records are clearly labeled and isolated.

### Phase 7 - Final QA and Deployment Preparation

- Add CI for frontend checks and Java tests if the repository hosting is set up.
- Write the actual README setup/deployment steps, environment variable reference, migration/health instructions, limitations, and roadmap.
- Verify CORS/proxy/session-cookie configuration and deployment sequence for the chosen hosts.
- **Gate:** full frontend lint/typecheck/build and backend test suite pass; manually verify the main user journeys; report only checks that actually ran. Deployment is not claimed until deployed and verified.

## Required Quality Gates Across Phases

- Make small, reviewable changes and inspect each target file before editing it.
- Run the narrowest relevant test/build immediately after each meaningful change, then the phase gate.
- Keep Java as the only backend language; no Python runtime, service, or dependency.
- Add a regression test for each new user-scoped data operation.
- Keep secrets out of source control and browser-visible environment variables.
- Do not mark routes/features complete when they are placeholders, hardcoded metrics, or unverified integrations.
- Report changed files, actual commands/results, known limitations, and the next concrete step at each phase end.

## Software and Tools to Prepare

Required or recommended for the planned Windows workflow:

- **JDK 21 LTS:** Temurin 21.0.12.1 is selected at `C:\Users\HP\AppData\Local\Programs\InternFlowTools\jdk-21`; select it for backend commands rather than relying on the default PATH JDK.
- **Node.js 24 LTS:** Node 24.22.0 is available at `C:\Users\HP\AppData\Local\Programs\InternFlowTools\node-v24.22.0-win-x64`.
- **npm:** npm 11.19.1 is available with the selected Node installation; use the frontend lockfile for reproducible installs.
- **Docker Desktop:** Docker 29.7.2 and Compose v5.4.0 are available; the PostgreSQL service is running and healthy.
- **PostgreSQL 18:** PostgreSQL 18.6 is active in the Compose container. Use `docker compose exec -T postgres psql ...` if a host `psql` is not on PATH.
- **Git:** executable is present, but initialize a repository in the intended project root before implementation if version control is desired.
- **VS Code:** current editor is VS Code. Recommended extensions: Extension Pack for Java and Spring Boot Extension Pack; optional PostgreSQL/SQL tooling. Extensions are helpful, not runtime requirements.
- **Maven:** no global installation needed; the project must include and use `mvnw.cmd`.
- A modern browser for manual responsive/accessibility checks.

The actual installed Java and Node patch versions should be rechecked after selection. Docker CLI availability does not prove that its daemon is running. No software was installed as part of Phase 0.

## Questions / Blockers Before Coding

1. Confirm that the nested `InternFlow_AI_Build_Pack` folder is the intended repository root. It currently contains the three project files and no `.git` directory.
2. Confirm whether to initialize Git in that folder (recommended before implementation). No approval is needed to prepare the docs; creating a repository is outside this Phase 0 change.
3. Select the local PostgreSQL setup: Docker Desktop/Compose (recommended) or a native PostgreSQL 18 installation. The Docker daemon has not been verified.
4. Confirm permission to install/select JDK 21 and Node 24, or provide the managed versions the project should target. Java 27 and Node 22 are currently on PATH.
5. Phase 1 must verify TypeScript 7.0 with Next.js 16.4 during scaffold/build. If incompatible, use the newest stable TypeScript version explicitly supported by Next.js and record the resolved version in the lockfile.

These were Phase 0 setup questions and are now resolved: the nested folder is the project root, Git exists without any commits, Java 21/Node 24 are selected, and PostgreSQL runs in Docker Compose. Port 8080 is occupied by a local Oracle TNS listener; run Spring Boot on an alternate port (for example 8081) and set frontend `BACKEND_API_URL` accordingly.

## Frontend audit follow-up (2026-10-16)

`npm audit` reports five high-severity dependency-path findings but one underlying advisory: `braces` GHSA-vfj7-8cjw-p6xm, affecting versions `<=3.0.3` for stack exhaustion on deeply nested patterns (CVSS 7.5). The path is `eslint-config-next` → `@next/eslint-plugin-next` → `fast-glob` → `micromatch` → `braces`. The installed `braces` 3.0.3 is still the newest registry release; the audit fix downgrades `eslint-config-next` to 14.2.35, incompatible with Next.js 16. No force fix or downgrade was used. These packages are in the development-only lint chain, not the production dependency set. Recheck for a compatible upstream patch before release.

`npm audit --omit=dev` reports zero production dependency vulnerabilities. TypeScript 6.0.3 is within the installed TypeScript-ESLint peer range and passes both frontend lint and production build.