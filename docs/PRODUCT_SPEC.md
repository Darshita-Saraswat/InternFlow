# InternFlow Product Specification

**Status:** Product scope, backend foundation, authentication, public registration/login flows, the persisted application workflow, student profile management, and task management are implemented. Profile and task workflows use the existing Flyway V1 schema and are covered by PostgreSQL-backed integration tests. Dashboard and other later product areas remain unfinished.

## Product Summary

InternFlow is a student-first workspace for organizing internships, graduate roles, and other early-career applications. Its promise is: **Turn applications into momentum.** It turns a scattered search into a visible queue of applications, deadlines, preparation tasks, and useful next actions.

The primary users are university students and recent graduates. The product is not limited to one industry or degree program. Its differentiator is a student-aware profile, explainable opportunity-fit checks, preparation workflows, and a dashboard grounded in the user's saved records. The differentiation is a product hypothesis to validate with student feedback, not an established market finding.

## User Problem and Outcomes

Students need one reliable place to answer:

1. What needs attention today?
2. Which opportunities have approaching deadlines, and what requirements do I match or still need to check?
3. What stage is each application in?
4. What should I prepare before the next assessment, interview, or follow-up?
5. What does my saved application history actually show?

The product should make these answers actionable: a dashboard item should open the relevant record or perform a real, clearly confirmed action.

## Product Principles

- Persisted user data is the source of truth. Never show invented metrics, success states, integrations, or AI claims.
- Missing profile information is `Unknown`, not a reason to mark a student ineligible.
- Explain fit assessments with specific matches, unknowns, and explicit blockers. The rules are deterministic and documented; this is not AI.
- Every private record belongs to one account. The backend enforces ownership on every read and mutation.
- Keep the interface professional, responsive, accessible, and consistent. All visible controls need real behavior and loading, empty, error, success, disabled, and keyboard-focus states where applicable.
- Keep the code understandable to a student: a modular monolith, clear boundaries, small changes, and tests around security and business rules.

## Initial Product Scope

### Required account and profile workflows

- Register, log in, log out, and retrieve the current account/profile.
- Hash passwords with BCrypt and protect private APIs server-side.
- Collect relevant profile details: university, degree, graduation date/year, study year, target role categories, skills, preferred locations, and work modes.

### Applications and opportunities

- Create and manage opportunities and applications, with an application usable even when no complete opportunity record exists.
- Record company, role, type, URL, source, location/work mode, saved/deadline/applied/follow-up dates, status, priority, next action, optional compensation, skills, notes, and a resume/document reference.
- Support statuses `Saved`, `Preparing`, `Applied`, `Online Assessment`, `Interview`, `Offer`, `Rejected`, and `Withdrawn`.
- Preserve a useful activity history for status changes and important application events.
- Provide searchable, filterable application lists and an accessible status control. Drag-and-drop is not required for the first usable version.
- Provide opportunity-fit explanations that clearly identify matches, unknown fields, and explicit blockers.

### Tasks, dashboard, calendar, and analytics

- Create, edit, complete, and delete general tasks or tasks linked to an application. Include priority, category, due date, status, and overdue/upcoming states.
- Derive dashboard counts, near-term deadlines, follow-ups, pipeline, recent activity, and focus actions from the authenticated user's database records.
- Derive calendar/agenda entries only from persisted application dates, tasks, assessments, interviews, and follow-ups; avoid duplicate representations of the same underlying date.
- Derive analytics from saved records. Explain metric denominators, show insufficient-data states, and avoid implying causation.

### Import, export, and account settings

- Export a user's application data as CSV.
- Add CSV import only after core CRUD is dependable; show a preview, validate rows, define duplicate behavior, and summarize results.
- Persist settings that are presented as configurable. Email and external job-board integrations are deferred until a real provider/API is selected and tested.
- Optional demo data must be opt-in, clearly labeled, and isolated from real user data.

## Routes

Public routes: `/login`, `/register`, and a concise public introduction if useful. Authenticated routes: `/dashboard`, `/applications`, `/applications/new`, `/applications/[id]`, `/opportunities`, `/tasks`, `/calendar`, `/analytics`, `/profile`, and `/settings`.

Routes should be delivered in stages. A route is not considered complete until its controls work against real persisted data or it is explicitly presented as an unfinished placeholder during development.

## Success and Acceptance Criteria

- A student can create an account, sign in, add/edit/delete an application, and record an activity/status change.
- A user cannot read, update, or delete another user's applications or tasks, including by guessing an ID.
- A student can manage a task and see its real due/completion state reflected in relevant views.
- Empty accounts show useful empty states and zero/insufficient-data metrics rather than seeded-looking fabricated numbers.
- Fit explanations distinguish an explicit requirement mismatch from unavailable profile data.
- Dashboard actions navigate to real records; analytics/calendar values trace back to persisted records.
- Forms have server-side validation; important journeys and ownership rules have automated tests.
- Frontend lint/production build and backend tests pass before a phase is reported complete.
- Phone and desktop layouts remain usable, with keyboard access and visible focus.

## Explicit Non-Goals for the Initial Release

- Python services, scripts, or application dependencies.
- Microservices, job-board scraping, fabricated analytics, fake AI, or placeholder integrations presented as working.
- Sending email before an email service is configured and verified.
- Treating a UI-only authorization check as data security.
- Building every route and feature in one unreviewed change.

## Implementation Snapshot (2026-10-16)

The project has a Next.js 16.4.1 frontend and a Java 21 / Spring Boot 4.1.1 backend. Flyway V1 creates users, student profiles, opportunities, applications, application events, tasks, and notification preferences; V2 adds Spring Session JDBC storage. Registration/login pages call the real API. Backend auth provides BCrypt registration, login, logout, current-account lookup, CSRF protection, and JDBC-backed sessions. The applications API and authenticated frontend implement persisted CRUD, search/status filters, pagination, and status-change history, with ownership taken from the authenticated principal. The profile API and responsive `/profile` page read and update the authenticated user's academic and career preferences without duplicating account identity fields. The task API and `/tasks` pages provide persisted CRUD, completion, filtering, sorting, deadlines, text search, and optional application linking. Task ownership and linked-application ownership are enforced by the authenticated principal and same-owner database relationship. Application, profile, and task integration tests run against PostgreSQL. Dashboard, fit assessment, analytics, calendar, and opportunities remain unfinished in this checkout.