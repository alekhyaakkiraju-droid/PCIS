# PCIS Frontend (WO-223 / WO-225)

React 19 SPA scaffold for the Property & Casualty Insurance System modernization,
including the shared design system and accessible component library.

## Stack

- React 19 + TypeScript (strict)
- Vite 6
- React Router 7
- OpenTelemetry browser SDK (fetch instrumentation + `X-Correlation-ID`)
- Vitest + React Testing Library + vitest-axe
- Storybook 8 (Vite) + addon-a11y
- ESLint (typescript-eslint) + Prettier

## Auth note (WO-224)

JWT access tokens are **not** stored in `localStorage` or `sessionStorage`. The API
gateway BFF completes the OIDC Authorization Code + PKCE exchange and sets
`httpOnly`, `Secure`, `SameSite=Strict` session cookies. The SPA:

- Calls `GET /api/auth/session` with `credentials: 'include'` for session state
- Starts login via `oidc-client-ts` `signinRedirect` (PKCE state only in sessionStorage)
- Posts the callback `code` to `POST /api/auth/callback` so the gateway can mint cookies
- Clears client state on `POST /api/auth/logout`

Configure via `VITE_OIDC_AUTHORITY`, `VITE_OIDC_CLIENT_ID`, `VITE_OIDC_REDIRECT_URI`,
`VITE_OIDC_POST_LOGOUT_REDIRECT_URI`, and `VITE_OIDC_SCOPE`.

Mock session fixtures for tests live in `fixtures/auth/`.

## Design system

The PCIS design system lives under `src/components/ui/`.

### Tokens

- CSS variables: `src/components/ui/tokens.css` (imported from `src/index.css`)
- TypeScript mirrors: `src/components/ui/tokens.ts`

Visual direction: deep teal primary (`#0b6e6a`), charcoal text/surfaces
(`#1a2332`), warm off-white background (`#f5f2eb`). Includes color scales,
typography, 4px spacing, radii, elevation, and breakpoints.

### Components

Import from the barrel:

```tsx
import {
  Button,
  Input,
  Select,
  TextArea,
  Badge,
  Card,
  Skeleton,
  MoneyDisplay,
  Tabs,
  Modal,
  ToastProvider,
  useToast,
  DataTable,
  tokens,
} from '@/components/ui'
```

| Component | Purpose |
| --- | --- |
| `Button` | primary / secondary / danger / ghost actions |
| `Input` | labeled text/number/date with validation states |
| `Select` | labeled select with option groups |
| `TextArea` | labeled multiline input with optional character count |
| `Badge` | claim / policy / billing status chips |
| `Card` | interactive container (header / body / footer / loading) |
| `Skeleton` | loading placeholders (rectangle / circle / text) |
| `MoneyDisplay` | locale-aware currency with exactly 2 decimals |
| `Tabs` | WAI-ARIA tabs with arrow-key navigation |
| `Modal` | focus-trapped dialog (Escape / backdrop close) |
| `Toast` | `ToastProvider` + `useToast` with `aria-live` |
| `DataTable` | sortable columns, pagination, optional row selection |

All primitives target WCAG 2.1 AA (labels, roles, focus, keyboard support).

### Storybook

```bash
npm run storybook          # http://localhost:6006
npm run build-storybook    # static build → storybook-static/
```

Stories cover variants and key states; `@storybook/addon-a11y` runs axe checks
in the Storybook UI.

## Scripts

```bash
npm install
npm run dev      # http://localhost:3000 — proxies /api/* → http://localhost:8080
npm run test
npm run lint
npm run build    # production bundle → frontend/dist/
npm run storybook
```

## Fixtures

Mock API payloads live in `fixtures/` (`sample-customer.json`, `sample-policy.json`).
Table fixtures for the design system live in `fixtures/table-data/` and are
re-exported from `src/test-fixtures/tableData.ts`.

Use `src/test/createMockFetch.ts` in Vitest suites.
