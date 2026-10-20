# InternFlow repository instructions

- Product: student-first early-career application command centre.
- Required backend language: Java only. Never introduce Python.
- Stack: Next.js App Router + TypeScript + Tailwind/shadcn UI; Java Spring Boot REST API; PostgreSQL; Flyway; Maven; JUnit.
- Use a modular monolith. Keep controllers thin; business logic in services; request/response DTOs instead of exposing JPA entities.
- Always scope reads/updates/deletes to the authenticated user. Add regression tests for cross-user data isolation.
- Validate on client and server. Store secrets only in environment variables; never commit `.env` files.
- Build reusable responsive components. Include loading, empty, error, success, disabled, and keyboard-focus states. Every visible control must work.
- Use database-derived metrics only. Label seeded records as demo data. Do not fake AI or external integrations.
- Use Flyway migrations; don't rely on `ddl-auto=update` in production.
- Before editing, inspect the existing files. Make small, verifiable changes. Run tests/lint/build after each meaningful phase and fix failures.
- Do not claim a test, integration, deployment, or feature succeeded unless it actually did.
- Explain setup in Windows PowerShell-friendly instructions in README.md.
