# spec-domain.md (Domain rules, errors, security)

This file consolidates:
- domain invariants and access control (ACL)
- error model (single canonical ErrorResponse)
- security rules (JWT validation, account status, WS policy)

If any older document conflicts with this file, **this file wins**.

## Roles and account status

### Roles
- `USER`
- `MODERATOR` (admin-like permissions for moderation APIs)
- `ADMIN`

### accountStatus
- `ACTIVE`
- `BLOCKED`
- `DELETED`

Rules:
- If `accountStatus != ACTIVE`:
  - any protected REST endpoint must respond with `403` and code `ACCOUNT_BLOCKED` or `ACCOUNT_DELETED`
  - any WS connection must be rejected/closed with `ERROR` (same ErrorResponse shape)

## JWT

- Algorithm: HS256
- Claims (MVP): `sub` = userId, `login`, `role`
- Token TTL: 24h

### Token invalidation rule (canonical, conservative)
Requirement: “tokens are invalid from the moment account is BLOCKED/DELETED”.

MVP implementation rule:
- Every protected REST request MUST load user by `sub` and check `accountStatus`.
- Every WS connection MUST load user by `sub` and check `accountStatus` at connect time.
- For long-lived WS connections, server MUST close the session if `accountStatus` changes away from ACTIVE.

This makes previously issued tokens effectively unusable once the status changes, without requiring a token blacklist in MVP.

## Core domain invariants (MVP)

### Squad
- A user can be member of at most one squad at a time.
- Squad has exactly one commander (who is also a member).
- Commander can:
  - kick members
  - transfer commander role
  - disband squad
- Leaving rules:
  - if commander leaves and members remain, system must assign a new commander and emit `BECAME_COMMANDER`.
  - if last member leaves, squad is disbanded (or auto-deleted per implementation).

### Company
- Company has exactly one commander.
- A squad can belong to at most one company at a time.
- Company commander can attach/detach squads and disband company.

### Markers
- Marker visibility:
  - `SQUAD` (scope = squadId)
  - `COMPANY` (scope = companyId)
  - `GLOBAL` (no scope)
- Marker active if `expiresAt == null` OR `expiresAt > now(UTC)`.
- Listing default: `includeExpired=false` (return active only).

### Orders
- Order belongs to a squad.
- Status: `CREATED | IN_PROGRESS | COMPLETED`
- When status becomes `COMPLETED`, `completedAt` is set.

### Geo
- `POST /geo/position` is append-only history.
- `GET /geo/positions` returns **latest** per visible user.

## Errors (single canonical model)

### ErrorResponse
Used in REST responses and WS `ERROR`.

```json
{
  "timestamp": "2025-01-01T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Human readable message",
  "path": "/api/v1/...",
  "details": { }
}
```

### Canonical error codes (MVP)

Auth / user:
- `UNAUTHORIZED` (401)
- `INVALID_CREDENTIALS` (401)
- `USER_ALREADY_EXISTS` (409)
- `ACCOUNT_BLOCKED` (403)
- `ACCOUNT_DELETED` (403)

Squad / company:
- `SQUAD_NOT_FOUND` (404)
- `COMPANY_NOT_FOUND` (404)
- `ALREADY_IN_SQUAD` (409)
- `FORBIDDEN` (403)

Markers / orders:
- `MARKER_NOT_FOUND` (404)
- `ORDER_NOT_FOUND` (404)
- `INVALID_ORDER_STATUS` (400)

Generic:
- `VALIDATION_ERROR` (400)
- `CONFLICT` (409)
- `INTERNAL_ERROR` (500)

Notes:
- `details` is reserved for field errors (validation) and contextual metadata.
