# InternFlow — Master Build Prompt

**Use this prompt in Cursor Agent (recommended), Claude Code, or another code-aware AI IDE that can read and edit the whole repository.** It is written for an actual codebase, not just a visual mockup generator.

---

## ROLE AND QUALITY BAR

Act as a senior product designer, senior full-stack engineer, database designer, security reviewer, and QA engineer working together. Build **InternFlow**, a polished, responsive, student-first internship and early-career application command centre. This must be a real, functioning web application—not a static dashboard, not a landing-page-only mockup, and not a collection of buttons that do nothing.

The result should be credible as a serious computer-science portfolio project. Prioritise a coherent product experience, reliable core workflows, clean architecture, security, accessibility, and a visual identity that is distinctive but restrained.

I am a CS student and I know Java and C++ better than Python. **Do not use Python anywhere in the application.** Use Java for the backend. Explain major decisions in the README in beginner-friendly language. Give exact commands for Windows PowerShell and keep the project runnable locally.

## 1. PRODUCT VISION

Product name: **InternFlow**  
Product category: Student career-application workspace  
Tagline: **Turn applications into momentum.**

Target users: university students and recent graduates managing internships, graduate software-engineering roles, business/data analyst roles, technology consulting roles, and other early-career opportunities. The product must not be limited to cybersecurity roles.

Problem: Students lose track of deadlines, application statuses, assessment rounds, interview preparation, follow-up dates, role requirements, and which resume version they used. Typical spreadsheets are passive and fragmented. InternFlow should turn a student's scattered application activity into a clear next-action workflow.

Product promise: At a glance, users should be able to answer:
1. What needs my attention today?
2. Which opportunities are most urgent and which am I eligible for?
3. What stage is every application at?
4. What should I prepare before the next test or interview?
5. Which parts of my search process are working?

Do not copy a competitor's branding or UI. Learn from standard application trackers, then differentiate through a student-specific eligibility profile, clear reasons behind an opportunity-fit score, application preparation, and deadline-aware next actions.

## 2. TECH STACK — KEEP THIS CONSISTENT

### Frontend
- Current stable Next.js App Router with TypeScript.
- React; use Server Components by default and Client Components only when interactivity requires them.
- Tailwind CSS and shadcn/ui components for consistent, accessible UI primitives.
- Lucide icons.
- Recharts for charts.
- React Hook Form and Zod for validated forms.
- TanStack Query for API request caching and invalidation if it improves the implementation; avoid adding libraries without a clear reason.
- A small typed API client in one place; no scattered raw `fetch` calls throughout the UI.

### Backend
- Java 21 LTS (or another explicitly selected LTS JDK compatible with the chosen Spring Boot release).
- Spring Boot, Spring Web, Spring Security, Spring Data JPA, Bean Validation, PostgreSQL driver, Flyway migrations, and Spring Boot Actuator health checks.
- Maven wrapper (`mvnw` / `mvnw.cmd`) so users do not need a globally installed Maven.
- JUnit and Spring Boot test support. Use Mockito for isolated service tests and integration tests for important API/database behaviour.
- OpenAPI/Swagger UI for API exploration.

### Database and deployment
- PostgreSQL as the real database, hosted on Supabase or another managed PostgreSQL provider.
- Keep database credentials and JWT secrets only in backend environment variables. Never put privileged database keys or server secrets in `NEXT_PUBLIC_*` frontend variables.
- Provide a local development path using a managed PostgreSQL instance or Docker Compose. Document both accurately; do not pretend a database works without configuration.
- Frontend can deploy to Vercel or another Next.js-capable host. Backend can deploy to a Java-capable host. Document the environment variables, CORS requirements, health endpoint, and deployment sequence.

### Explicit exclusions
- No Python services, scripts, or runtime dependencies.
- No fake AI features, fabricated analytics, hardcoded success messages, or dead buttons.
- No scraping job boards in ways that violate their terms. Prefer user-entered opportunities, CSV import/export, and approved APIs/feeds when available.
- No unnecessary microservices. This is a well-structured modular monolith.

## 3. VISUAL DIRECTION: DISTINCTIVE, NOT GENERIC

Create an intentional product design system before building every screen.

### Brand and mood
- Professional, modern, focused, trustworthy, optimistic.
- Think a polished productivity app for ambitious university students—not a template dashboard and not a neon gaming interface.
- Use generous whitespace, crisp alignment, legible typography, clear information hierarchy, subtle borders, disciplined radius, and purposeful micro-interactions.
- Do not use the generic purple/blue AI gradient as the whole identity. Avoid excessive glassmorphism, random floating blobs, over-rounded cards everywhere, decorative charts with no meaning, and oversized empty hero sections inside the authenticated app.

### Suggested palette
Define these as CSS variables/design tokens so the whole app stays consistent:
- Ink/navy for primary text and navigation: `#16233B`
- Primary blue: `#3559E8`
- Teal accent: `#12A89D`
- Warm near-white canvas: `#F6F8FB`
- White surfaces: `#FFFFFF`
- Muted text: `#64748B`
- Success: `#16845B`
- Warning: `#B7791F`
- Danger: `#C43D4B`
- Borders: `#E2E8F0`

Treat the palette as a starting point: verify contrast, create semantic tokens, and do not hard-code colors separately in every component. Use one readable sans-serif font family with distinct weights. Make data visualisations meaningful and accessible; every chart needs a title, labelled axes/legend where relevant, tooltips, and an empty state.

### Main application layout
- Responsive left sidebar on desktop with InternFlow wordmark, simple icon+label navigation, active state, and a compact user profile area.
- Mobile navigation should become a clean drawer or bottom navigation; no horizontal overflow.
- Top bar with page title/breadcrumb when useful, global search if implemented, notifications, and avatar menu.
- Main content has consistent max width, gutters, and vertical rhythm.
- The first viewport should show a clear page title, concise context, and actionable information—not just a row of giant KPI cards.

### Interaction quality
Every interactive element must work. Include focused/hover/active/disabled states, keyboard navigation, visible focus rings, helpful inline validation, loading skeletons, toast feedback, confirmation for destructive actions, and clear empty/error states. Respect `prefers-reduced-motion`. Use motion sparingly for transitions and modal/drawer entry, not for decoration.

## 4. REQUIRED ROUTES AND SCREENS

### Public/authentication
1. `/login` — clean login page, proper errors, loading state, show/hide password, and link to registration.
2. `/register` — name, email, password, password confirmation, acceptance of terms if included, field validation, and clear success/error feedback.
3. A deliberate product landing page or concise public intro page if useful. Do not spend most of the project on marketing while neglecting the authenticated application.

### Authenticated product
4. `/dashboard` — the command centre.
5. `/applications` — list/table and Kanban views, searchable and filterable.
6. `/applications/new` and `/applications/[id]` — create, view, and edit an application with complete details and activity history.
7. `/opportunities` — saved/discovered opportunities with deadline and eligibility information. Initially support manual creation and CSV import; external discovery integrations can be phased in later.
8. `/tasks` — tasks, preparation checklists, due dates, priority, and completion status.
9. `/calendar` — month/list agenda of application deadlines, assessment dates, interview dates, and follow-ups. Events must come from actual saved records.
10. `/analytics` — transparent metrics from the user's real application data.
11. `/profile` — graduation year/month, university, degree, current year/semester, target role categories, skills, locations, work-mode preferences, and other eligibility-relevant fields.
12. `/settings` — account settings and notification preferences; do not create fake settings that do not persist.

## 5. CORE FEATURES (IMPLEMENT IN THIS ORDER)

### A. Authentication and protected data
- Register, log in, log out, and fetch the current user's profile.
- Hash passwords with a modern BCrypt encoder (never plain text, never reversible encryption).
- Enforce authentication and authorisation on the server. Hiding a screen in the frontend is not access control.
- Every record must belong to a user. Every read/update/delete query must scope data to the authenticated user. Prevent IDOR: knowing another user's record ID must not allow access to that record.
- Use a documented, reasonably secure session/token design. Do not store long-lived refresh tokens in localStorage. Handle credential expiry, logout, CORS, and CSRF appropriately for the chosen cookie/token design. Explain local-development and production differences.
- Validate input on the server even when the frontend validates it too.
- Do not log passwords, tokens, secrets, or full sensitive request bodies.

### B. Student profile and eligibility
Allow users to save their academic/career profile. Define a clear eligibility rules model. Examples of rules include graduation year, degree/branch, years of study, location, work mode, minimum CGPA if specified, required skills, and application deadline.

Build an **explainable fit indicator** for saved opportunities. It must not pretend to be an AI or claim certainty. Show a score or label such as `Strong fit`, `Possible fit`, or `Check requirements`, plus specific reasons:
- Matches: eligible graduation year, skill overlap, preferred location/work mode.
- Missing/unknown: a requirement the profile does not answer.
- Potential blockers: an explicit requirement that the profile does not meet.

Never label a user ineligible solely because a field is unknown. Use `Unknown` when information is missing. Make the scoring formula deterministic, documented, and testable; keep it in a backend service, not duplicated in multiple frontend components.

### C. Application tracker
Each application should support at least:
- Company and role title.
- Opportunity type (internship, graduate role, analyst role, other early-career role).
- Job/application URL.
- Source (careers page, campus placement, referral, job board, event, other).
- Work mode and location.
- Date saved, application deadline, date applied, follow-up date.
- Status with an explicit transition model: `Saved`, `Preparing`, `Applied`, `Online Assessment`, `Interview`, `Offer`, `Rejected`, `Withdrawn`.
- Priority and next action.
- Salary/stipend range as optional fields, with currency.
- Notes and required skills.
- Resume/document version reference or optional external document URL.
- Activity timeline: status changes, notes, follow-up actions, assessment/interview scheduling. Do not just overwrite the status without preserving useful history.

Provide a validated create/edit form, detail view, delete confirmation, status updates, filters, search, sorting, pagination where appropriate, and a Kanban view with drag-and-drop only if it can be implemented reliably and accessibly. A clear status dropdown is preferable to a broken drag-and-drop feature.

### D. Tasks and preparation workflow
- Create tasks linked to an application or unlinked general tasks.
- Include title, description, priority, due date/time, status, category, and optional application relation.
- Suggested categories: Resume, Research, Online Assessment, DSA Practice, Interview Preparation, Referral/Networking, Follow-up, Documents, Other.
- Provide quick completion, edit, delete, filters, overdue state, and upcoming state.
- Application detail may include a prep checklist with items that persist in the database.

### E. Dashboard that tells the user what to do next
Use real database-derived values only. Include:
- Total active applications.
- Interviews/assessments coming up.
- Upcoming deadlines in the next seven days.
- Follow-ups due or overdue.
- Application pipeline by stage.
- A concise recent-activity timeline.
- A `Focus for today` section based on actual tasks, near deadlines, and follow-ups.
- A lightweight weekly progress summary only if calculated truthfully from persisted dates.

The dashboard must be action-oriented. For example, a record with a follow-up due tomorrow should lead to that record or offer a one-click action to mark the follow-up complete. Do not show hard-coded numbers such as `12 applications` as if they belong to the user.

### F. Analytics
- Application funnel by stage.
- Application trend by week/month.
- Response rate and interview conversion rate, with denominator definitions visible or explained.
- Applications by source and by target role category.
- Time-to-response only when enough dates exist; show `Not enough data` when it cannot be calculated.
- CSV export of application records.
- Every metric must be derived from persisted data and testable. State how a metric is calculated; don't imply causation from a small sample.

### G. Alerts and calendar
- In-app notification/alert panel for overdue tasks, upcoming deadlines, and follow-ups due.
- Use date/time conventions consistently; display dates in the user's chosen timezone or clearly document the default.
- Calendar/agenda events derive from applications, tasks, assessments, interviews, and follow-ups. Avoid duplicate events for the same underlying date.
- Email notifications are an optional later phase. Do not fake sending email if no email provider is configured.

### H. Quality-of-life polish
- Useful seeded demo account/data for screenshots and first-run experience, clearly labelled as demo data and never mixed into real user accounts without consent.
- CSV import with preview, column mapping or clear expected headers, validation, duplicate handling, and an import summary.
- CSV export.
- Useful empty states with one obvious next action.
- Search and filters that combine correctly and can be cleared.
- Responsive behaviour at phone, tablet, laptop, and wide desktop widths.
- Accessible colour contrast and meaningful labels.

## 6. DATA MODEL AND BACKEND ARCHITECTURE

Use a modular monolith with clear package boundaries. Suggested Java packages:
- `auth`
- `user`
- `profile`
- `opportunity`
- `application`
- `task`
- `calendar`
- `analytics`
- `notification`
- `common` (error handling, configuration, shared types)

Keep controllers thin. Use DTOs for request/response payloads; do not expose JPA entities directly. Separate controller, service, repository, DTO, entity, validation, and mapping responsibilities where it improves clarity. Avoid building abstractions for their own sake.

Suggested core database entities:
- `users`
- `student_profiles` (one-to-one with user)
- `opportunities`
- `applications`
- `application_events`
- `tasks`
- `task_checklist_items` (only if needed)
- `notification_preferences`

An application may reference an opportunity, but users must be able to track an application even if the opportunity record is minimal. Define ownership consistently and use foreign keys, indexes, uniqueness where appropriate, timestamps, and constraints. Store dates/times in a robust format; avoid mixing local dates and timestamps carelessly. Add indexes for user ownership, statuses, deadlines, and common filters where helpful.

Database changes must use versioned Flyway migrations. Do not rely solely on `ddl-auto=update` in production. Include a safe development configuration and explain how schema migrations are applied.

### API conventions
- Use `/api/v1/...` endpoint prefixes.
- Use consistent status codes and structured error responses (`ProblemDetail` or a documented equivalent).
- Validate payloads with Bean Validation and return actionable field errors.
- Use pagination for lists that can grow.
- Use DTOs and explicit enums for application status and task priority.
- Add OpenAPI documentation.
- Create a health endpoint, and avoid exposing secrets in health diagnostics.
- Do not return stack traces or internal implementation details to users.

## 7. FRONTEND COMPONENT AND STATE ARCHITECTURE

Organise components by domain instead of writing the entire app in one page file. Suggested structure:
- `app/(auth)/...`
- `app/(app)/dashboard/...`
- `app/(app)/applications/...`
- `components/layout/...`
- `components/ui/...`
- `features/applications/...`
- `features/tasks/...`
- `features/profile/...`
- `lib/api/...`
- `lib/validation/...`
- `lib/formatters/...`
- `lib/types/...`

Use reusable design-system components and consistent form patterns. Keep server/client boundaries deliberate. Type API payloads, represent loading/error/empty states explicitly, and invalidate/refetch the right data after mutations. Do not make an all-in-one `page.tsx` file containing the whole product.

## 8. REQUIRED TESTING AND ACCEPTANCE CRITERIA

The application is not complete just because it compiles. Add tests and manually verify the main user journeys.

### Backend tests
- Registration and login success/failure.
- Password hashing is not plain text.
- Unauthenticated requests to protected endpoints are rejected.
- User A cannot read/update/delete User B's applications or tasks, even if User A guesses their IDs.
- Application creation, update, status transitions, and validation.
- Task creation, completion, due-date handling, and validation.
- Dashboard analytics reflect database contents, including an empty database and nullable dates.
- Fit-score/eligibility logic has unit tests for matching, missing information, and explicit blockers.
- CSV import rejects malformed rows safely and reports the result.

### Frontend checks
- Registration/login/logout.
- Add, edit, and delete an application.
- Search/filter by company, status, role type, and deadline.
- Update status and observe the dashboard/analytics refresh.
- Create/complete/edit a task.
- Dashboard action items link to real records.
- Empty/loading/error states do not crash.
- Layout works on phone and desktop.
- Forms have labels, keyboard support, and clear errors.

### Build/quality commands
- Frontend lint and production build must pass.
- Backend test suite must pass.
- Do not hide failing tests or silence compiler/linter failures with blanket suppressions.
- Add GitHub Actions CI for frontend lint/build and Java tests if feasible.
- Include `.env.example` files with placeholder values only. Ensure `.env`, credentials, and build artefacts are ignored by Git.

## 9. DOCUMENTATION AND DEMO READINESS

Write a strong README containing:
- Product overview and the specific student problem it solves.
- Feature list and screenshots section.
- Architecture diagram (Mermaid is fine) showing Next.js → Spring Boot REST API → PostgreSQL.
- Technology decisions and what I learned.
- Prerequisites and exact Windows PowerShell setup steps.
- Environment variables with examples that contain no real secrets.
- Commands to run tests, lint, build, and start each service.
- Database setup and migration instructions.
- API docs URL and health endpoint.
- Deployment guide and common troubleshooting.
- Known limitations and a realistic roadmap.

Use demo data to make a strong screenshot/demo. Include a few applications at different stages, a near deadline, an interview, and tasks so charts and action panels are not empty. Keep this data seeded deterministically in a demo-only mechanism; never expose one user's private data to another.

## 10. IMPLEMENTATION STRATEGY — DO NOT BUILD A GIANT UNTESTED MONOLITHIC PATCH

Work incrementally in these phases. At the start of each phase, inspect the existing repo, summarise what is already present, and state which files will change. After implementation, run the relevant tests/build, fix errors, and report the actual command output or honestly state what could not be run.

**Phase 0 — Audit and plan**
- Inspect all existing files and current working tree.
- If this repo contains the previous Python/FastAPI backend, plan a clean migration to Java Spring Boot. Do not leave two active backends or keep Python as a hidden dependency.
- Create `docs/PRODUCT_SPEC.md`, `docs/ARCHITECTURE.md`, and `docs/IMPLEMENTATION_PLAN.md`.
- Identify risks and choose exact compatible dependency versions.

**Phase 1 — Design system and frontend shell**
- Implement visual tokens, app shell, sidebar, top bar, responsive navigation, typography, buttons, inputs, cards, dialogs, toasts, skeletons, empty states, and route placeholders.
- Make a high-quality dashboard and application-list screen using clearly labelled demo data only until the backend is connected.
- Ensure every visible navigation link leads somewhere intentional.

**Phase 2 — Java backend foundation**
- Set up Spring Boot, Maven wrapper, config profiles, PostgreSQL connection, Flyway, global exception handling, validation, OpenAPI, and health endpoint.
- Add users/profile/application/task entities and migrations.
- Build and test the first endpoints before implementing all features.

**Phase 3 — Authentication and ownership security**
- Implement register/login/logout/current-user flows, password hashing, authorization, and user-scoped repository/service queries.
- Add tests for cross-user isolation before proceeding.

**Phase 4 — End-to-end application workflow**
- Connect the frontend to the typed API client.
- Implement real application CRUD, detail/activity view, search/filter/sort, and dashboard stats.
- Test the complete flow with the database.

**Phase 5 — Tasks, profile, eligibility, calendar, analytics**
- Implement one feature area at a time, with tests and responsive UI.
- Do not fake calendar or analytic data.

**Phase 6 — Import/export, demo data, visual refinement**
- Add reliable CSV import/export, demo mode, accessible polish, keyboard checks, mobile refinements, and micro-interactions.

**Phase 7 — Final QA and deployment**
- Run the full backend test suite and frontend lint/build.
- Fix the highest-impact bugs first.
- Verify all environment variables and CORS configuration.
- Write the README and a truthful deployment checklist.
- Do not claim deployment, tests, integrations, or features were completed unless they actually were.

## 11. RULES FOR YOUR WORK AS THE CODING AGENT

- Do not overwrite or delete files without inspecting them first.
- Do not generate the entire project in one huge unreviewed response.
- Do not silently change the selected tech stack.
- Do not use mock data after the real API integration is complete, except in explicit demo/preview mode.
- Do not build controls that look interactive but have no action.
- Do not invent real opportunities, real salary information, or company eligibility rules. Demo data must be labelled.
- Do not describe the fit score as AI unless an actual, evaluated AI component exists.
- If a requirement is ambiguous, make the safest reasonable choice and document it instead of blocking on trivial questions.
- If a build/test fails, investigate and fix the cause; do not simply claim it works.
- Keep the architecture understandable to a student and avoid premature microservices.
- End each phase with: changed files, what works, commands run and their real results, known limitations, and the exact next step.

## FIRST TASK

Start with **Phase 0**. Inspect the repository and make a concrete migration/build plan for the Java-based InternFlow described above. Do not implement every feature yet. Then begin Phase 1 only after the plan is written and the directory structure is clear. Preserve useful work when possible, but remove the Python backend once its Java replacement is ready and verified. Never use Python.

