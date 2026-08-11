# @PreAuthorize mutation guard (Semgrep)

PCIS enforces **deny-by-default authorization** on every financial mutation
endpoint. The custom Semgrep rule `require-preauthorize-on-mutations` is the
build-time gate that blocks merges when a mutating Spring MVC handler lacks an
explicit `@PreAuthorize` permission expression.

## What the rule checks

The rule flags **public controller methods** annotated with any of:

| HTTP verb | Spring annotation |
|-----------|-------------------|
| POST | `@PostMapping` |
| PUT | `@PutMapping` |
| PATCH | `@PatchMapping` |
| DELETE | `@DeleteMapping` |

Each flagged method must also carry **`@PreAuthorize(...)`** on the same method
(before the method signature). Read-only handlers (`@GetMapping`, etc.) are out
of scope.

### Compliant example (from `audit-svc`)

```java
@PostMapping("/events")
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasAuthority('audit:write')")
public AuditEventResponse recordEvent(@RequestBody AuditEventRequest request) {
  return auditEventService.recordEvent(request);
}
```

Other annotations (`@Operation`, `@ResponseStatus`, …) may appear between the
mapping and `@PreAuthorize`; the rule only requires that `@PreAuthorize` is
present on the method before the signature.

### Non-compliant example

```java
@PostMapping("/roles")
public RoleResponse createRole(@RequestBody CreateRoleRequest request) {
  return roleService.create(request);
}
```

Semgrep reports:

```text
Mutating controller handler `createRole` is missing `@PreAuthorize`.
```

## Repository layout

```text
semgrep/
  rules/require-preauthorize-on-mutations.yaml   # rule definition
  tests/
    positive/   # fixtures that MUST trigger the rule (3+)
    negative/   # fixtures that MUST NOT trigger (3+)
docs/authz/preauthorize-rule-guide.md            # this guide
```

## Local development

### Prerequisites

Install [Semgrep](https://semgrep.dev/docs/getting-started/) (1.x). The rule
uses standard Java pattern matching and does not require Semgrep Pro.

### Run rule unit tests

```bash
make test-semgrep
# or
semgrep --test --config semgrep/rules semgrep/tests
```

All tests must pass before opening a PR. Positive fixtures use `//
ruleid: require-preauthorize-on-mutations`; negative fixtures use `// ok:
require-preauthorize-on-mutations`.

### Scan production services

```bash
make lint-semgrep
# or
semgrep --config semgrep/rules --error services/
```

The scan exits non-zero on any finding (`--error`), which matches the intended
`scan:semgrep` pipeline gate described in
`docs/modernization/Architecture_Options.md`.

## Fixing violations

1. Identify the permission string from the service authorization inventory (or
   define one following `{domain}:{resource}:{action}` — e.g. `authz:role:write`).
2. Add `@PreAuthorize("hasAuthority('…')")` on the mutating handler.
3. Ensure the service enables method security (`@EnableMethodSecurity` or
   equivalent) and issues JWT authorities that satisfy the expression.
4. Add or extend integration tests for 401 / 403 / 200 on the endpoint.

## CI integration

Until Forge Shipping `scan:semgrep` is wired (see WO-051), the Makefile targets
`test-semgrep` and `lint-semgrep` provide local and ad-hoc CI parity:

| Target | Purpose |
|--------|---------|
| `test-semgrep` | Validates rule fixtures (`semgrep --test`) |
| `lint-semgrep` | Scans `services/` for live violations |

Add `make test-semgrep` to pre-commit or PR checks alongside existing `make
lint` targets.

## Permission naming guidance

Align permission strings with the authz-svc model:

- **Domain prefix** — service area (`audit`, `authz`, `claims`, …)
- **Resource** — entity acted upon (`role`, `event`, `payment`, …)
- **Action** — verb (`read`, `write`, `delete`, `approve`, …)

Examples: `audit:write`, `authz:role:write`, `claims:payment:approve`.

## Related work

- **WO-166** — Semgrep rule and fixtures (this document)
- **Deny-by-default security** — `authz-svc` / `audit-svc` `SecurityConfig`
- **Pipeline gate** — `scan:semgrep` in Forge Shipping (Architecture Options)

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Rule fires on a `@GetMapping` handler | Unlikely — GET is excluded | Verify the mapping annotation |
| Rule misses an unguarded POST | Handler not in a `class` body pattern | Ensure standard `@RestController` layout |
| Tests fail after rule edit | Fixture comment mismatch | Update `ruleid` / `ok` annotations |
| `semgrep: command not found` | CLI not installed | `brew install semgrep` or pip install |

For questions, comment on **WO-166** in Forge.
