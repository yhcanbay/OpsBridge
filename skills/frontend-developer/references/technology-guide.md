# React decisions

Research checked: 2026-11-04. Package imports and requirements must match the actual installed version.

| Concern | Useful starting point | Add complexity only when |
|---|---|---|
| Routing | BrowserRouter, Routes, Outlet, Navigate, URL parameters | Loaders/actions or server rendering are an actual requirement |
| HTTP | Shared Axios instance with timeout, normalized errors and explicit domain services | Multiple backends need independent client configurations |
| Authentication | Context/provider and a protected layout | A broader state model justifies another store |
| Server state | A focused hook with cancellation and explicit states | Shared caching, invalidation and retries justify a query library |
| Forms | Controlled fields and API field errors | Complex schemas or form performance justify a form library |
| Formatting | Repository Prettier configuration | Existing conventions already define another formatter |

Do not confuse the router's UI protection with authorization. Keep side effects out of render. Route parameters must be handled when a requested project is absent; do not silently display another project's details. A failed initial session request due to connectivity should offer retry, not be silently swallowed as a successful logout.

Primary sources:

- [React Router modes](https://reactrouter.com/start/modes) and [routing](https://reactrouter.com/start/declarative/routing): matching URLs, nested layouts and navigation; consult the version selector for the installed major.
- [Axios instance](https://axios-http.com/docs/instance), [interceptors](https://axios-http.com/docs/interceptors) and [cancellation](https://axios-http.com/docs/cancellation): shared transport configuration and request lifecycle.
- [React custom hooks](https://react.dev/learn/reusing-logic-with-custom-hooks): reusable behavior and state ownership.
- [React effect synchronization](https://react.dev/learn/synchronizing-with-effects): cleanup and synchronization boundaries.
- [Prettier options](https://prettier.io/docs/options): consistent checked-in formatting.
