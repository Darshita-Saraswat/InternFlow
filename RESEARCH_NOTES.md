# InternFlow: research findings and architecture decisions

Research checked on 15 October 2026. These notes distinguish documented facts from product recommendations.

## What existing products already do

- Teal describes job bookmarking, organising applications by stage, and role/job-description insights. (Official: https://www.tealhq.com/tools/job-tracker)
- Huntr describes tracking opportunity details such as notes, dates, tasks, locations and company data; its tracker offers a Kanban workflow and activity timeline. (Official: https://huntr.co/ and https://huntr.co/product/job-tracker)
- Simplify describes autofilling application forms, tailoring resumes, and tracking applications. (Official: https://simplify.jobs/copilot and https://simplify.jobs/job-application-tracker)

Implication: a basic job tracker with a board and reminders is not meaningfully differentiated. InternFlow should focus on student-aware eligibility, transparent fit explanations, assessment/interview preparation, and a strong next-action dashboard. This is a product hypothesis to test with student feedback, not proof of market demand.

## Stack research

- Next.js App Router is the current route/layout approach documented by the Next.js project. It uses React Server Components, Suspense, and Server Functions; choose client-side interactivity deliberately. https://nextjs.org/docs/app
- Next.js documentation separates authentication, session management, and authorization, and recommends considering authentication libraries for production-grade concerns. https://nextjs.org/docs/app/guides/authentication
- Spring Boot officially supports production Java applications and its current system requirements document lists Java 17 as the minimum for Spring Boot 4.1.1; Java 21 is an LTS JDK choice that offers broad library compatibility. https://docs.spring.io/spring-boot/documentation.html and https://docs.spring.io/spring-boot/system-requirements.html
- Spring Boot's SQL documentation supports using Spring Data JPA/Hibernate to map Java entities to relational database tables. https://docs.spring.io/spring-boot/reference/data/sql.html
- Spring Security provides authentication and access-control mechanisms. Use its recommended password-handling APIs rather than unsafe demo defaults. https://docs.spring.io/spring-security/reference/servlet/authentication/index.html
- Supabase provides a managed PostgreSQL database; its documentation recommends taking Row Level Security seriously for exposed schemas. For the planned architecture, database access should occur through the trusted Java backend, with application-level ownership enforced and no privileged key in the browser. https://supabase.com/docs/guides/database/overview and https://supabase.com/docs/guides/database/row-level-security
- shadcn/ui documents Next.js forms, validation, pending states, and component patterns; its chart components use Recharts. https://ui.shadcn.com/docs/forms/next and https://ui.shadcn.com/docs/components/base/chart
- Vercel documents zero-configuration deployment support for Next.js. https://vercel.com/docs/frameworks/full-stack/nextjs

## Java vs C++ vs Python

Recommendation: use **Java + Spring Boot** for the backend. It is a strong fit for a database-heavy web API with authentication, validation, business rules, and relational entities, and it aligns with what the student is studying. C++ can serve HTTP APIs, but would require more low-level framework and build-system choices and has less beginner-friendly ecosystem fit for this exact CRUD/product application. Python is not required.

## Base44 caveat

Base44's documented backend functions are Deno-based HTTP handlers. It is therefore not the natural single-platform choice when the requirement is a Java Spring Boot backend. To genuinely use Java, use a code-aware IDE/agent (such as Cursor or a similar tool) to build and test the frontend and backend as a code repository. Base44 can still be useful for rough UI exploration, but avoid splitting the production app between two competing backends. https://doc-sdk.base44.app/FunctionsDocs

## Security and quality priorities

- Use server-side authentication and authorisation; frontend route protection alone is not security.
- Scope every application, task, note, and event to the authenticated user.
- Use password hashing, input validation, secrets in backend environment variables, database migrations, and tests for cross-user data isolation.
- Keep chart metrics grounded in saved database records, explain denominators, and show an empty state when there is insufficient data.
- Do not claim scraped/live opportunities or AI fit predictions unless a reliable, permitted integration or evaluated model exists.
