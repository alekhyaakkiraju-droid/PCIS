# PCIS Frontend (WO-223)

React 19 SPA scaffold for the Property & Casualty Insurance System modernization.

## Stack

- React 19 + TypeScript (strict)
- Vite 6
- React Router 7
- OpenTelemetry browser SDK (fetch instrumentation + `X-Correlation-ID`)
- Vitest + React Testing Library
- ESLint (typescript-eslint) + Prettier

## Auth note

JWT access tokens are **not** stored in `localStorage`. The API gateway sets
`httpOnly`, `Secure`, `SameSite=Strict` cookies. Frontend code must not embed
secrets or API keys.

## Scripts

```bash
npm install
npm run dev      # http://localhost:3000 — proxies /api/* → http://localhost:8080
npm run test
npm run lint
npm run build    # production bundle → frontend/dist/
```

## Fixtures

Mock API payloads live in `fixtures/` (`sample-customer.json`, `sample-policy.json`).
Use `src/test/createMockFetch.ts` in Vitest suites.
