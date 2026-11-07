---
name: frontend-developer
description: Build or refactor React applications with routing, HTTP services, authentication state, accessible components, and readable code. Use for frontend implementation or review, not backend-only or database-only tasks.
---

# Frontend Developer

Prefer understandable components and named operations over compressed JSX or clever expressions. Format source consistently. Separate route composition, layouts, pages, reusable components, hooks, context and HTTP services when those responsibilities exist; avoid empty folders and unnecessary global state libraries.

- Inspect React, Vite and router versions first. Use React Router for URL navigation; distinguish declarative, data and framework modes rather than mixing incompatible examples. Respect an existing `react-router-dom` convention when compatible.
- For a shared HTTP layer, use an Axios instance when interceptors, common errors, timeout and cancellation improve the application. Native fetch remains valid for small independent calls. Components call named domain services rather than carrying transport configuration.
- Centralize authentication state in an appropriate provider. Route guards improve navigation; backend authorization remains required. Preserve the intended destination after login and distinguish an expired session from a network failure.
- For session authentication, handle CSRF token acquisition and renewal in the HTTP/auth layer. Do not store passwords or session credentials in localStorage. Do not retry POST/PUT/DELETE automatically after an ambiguous failure.
- Extract hooks around a coherent behavior, not simply to hide a long file. Handle loading, empty, error and success states, including cancellation and stale responses after route changes.
- Keep DTO vocabulary and capability labels outside form components. Use labeled fields, accessible errors, native dialog behavior and meaningful button states. Avoid component exports that force unrelated components to import a form for constants.
- Add format tooling that the project can run. Verify navigation/authentication and HTTP protocol changes with focused behavioral tests; a successful build alone does not prove form interaction or browser behavior.

Read [technology choices and sources](references/technology-guide.md) when selecting router mode, HTTP client or state handling. Keep the user's chosen stack and deployment model unless a change is requested.
