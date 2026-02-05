# spec-api.md (Canonical REST API)

This is the **canonical** REST API contract for the Tactical App backend (MVP).
If any older document conflicts with this file, **this file wins**.

## Conventions

- Base path: `/api/v1`
- Content-Type: `application/json; charset=utf-8`
- Auth: `Authorization: Bearer <jwt>`
- Time: UTC, ISO-8601, `Instant` (e.g. `2025-01-01T12:00:00Z`)
- IDs: `long`
- Pagination: **not in MVP** unless explicitly specified.
- Errors: see `spec-domain.md` / `errors` section (single format for REST and WS).

## Authentication

### POST `/auth/register`
Registers a new user.

Request:
```json
{
  "login": "string",
  "password": "string",
  "displayName": "string"
}
```

Response `200`:
```json
{
  "token": "jwt",
  "user": {
    "id": 1,
    "login": "string",
    "displayName": "string",
    "accountStatus": "ACTIVE",
    "role": "USER",
    "avatarUrl": null,
    "isAlive": true,
    "createdAt": "2025-01-01T12:00:00Z",
    "updatedAt": "2025-01-01T12:00:00Z"
  }
}
```

Errors:
- `409 USER_ALREADY_EXISTS`
- `400 VALIDATION_ERROR`

### POST `/auth/login`
Authenticates by login/password.

Request:
```json
{ "login": "string", "password": "string" }
```

Response `200` same shape as `/auth/register`.

Errors:
- `401 INVALID_CREDENTIALS`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`
- `400 VALIDATION_ERROR`

## Users

### GET `/users/me`
Returns current user profile.

Response `200`: `UserDto` (same as in auth response `user`)

Errors:
- `401 UNAUTHORIZED`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`

### PATCH `/users/me`
Updates profile (MVP fields).

Request:
```json
{
  "displayName": "string",
  "avatarUrl": "string|null"
}
```

Response `200`: `UserDto`

Errors:
- `400 VALIDATION_ERROR`
- `401 UNAUTHORIZED`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`

### POST `/users/me/alive`
Set `isAlive=true`.

Response `200`: `UserDto`

### POST `/users/me/dead`
Set `isAlive=false`.

Response `200`: `UserDto`

## Squads

### POST `/squads`
Creates a squad. Creator becomes commander and member.

Request:
```json
{ "name": "string" }
```

Response `200`:
```json
{
  "id": 10,
  "name": "string",
  "commanderId": 1,
  "companyId": null,
  "createdAt": "2025-01-01T12:00:00Z",
  "updatedAt": "2025-01-01T12:00:00Z"
}
```

Emits WS event: `SQUAD_CREATED` (channel `SQUAD`, channelId=`squadId`).

Errors:
- `400 VALIDATION_ERROR`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`

### GET `/squads/my`
Returns squad where current user is member, or `404` if none.

Response `200`: `SquadDto` as above, plus members:
```json
{
  "id": 10,
  "name": "string",
  "commanderId": 1,
  "companyId": null,
  "members": [
    { "userId": 1, "joinedAt": "2025-01-01T12:00:00Z" }
  ],
  "createdAt": "2025-01-01T12:00:00Z",
  "updatedAt": "2025-01-01T12:00:00Z"
}
```

Errors: `404 SQUAD_NOT_FOUND` if user is not in a squad.

### POST `/squads/my/join`
Join existing squad by invite code.

Request:
```json
{ "inviteCode": "string" }
```

Response `200`: `SquadDto`

Emits WS event: `JOINED_SQUAD` (channel `SQUAD`, channelId=`squadId`).

Errors:
- `404 SQUAD_NOT_FOUND`
- `409 ALREADY_IN_SQUAD`
- `403 FORBIDDEN` (e.g., squad full if such rule exists) or `400 VALIDATION_ERROR`

### POST `/squads/my/leave`
Leave current squad.

Response `200`:
```json
{ "left": true }
```

Emits WS event: `LEFT_SQUAD` (channel `SQUAD`, channelId=`squadId`).

Notes:
- If commander leaves and system auto-assigns a new commander, emits `BECAME_COMMANDER`.

### POST `/squads/my/kick`
Commander kicks a member.

Request:
```json
{ "userId": 123 }
```

Response `200`:
```json
{ "kicked": true }
```

Emits WS event: `KICKED_FROM_SQUAD` (channel `SQUAD`, channelId=`squadId`).

### POST `/squads/my/transfer-commander`
Transfer commander role to another member.

Request:
```json
{ "userId": 123 }
```

Response `200`:
```json
{ "commanderId": 123 }
```

Emits WS event: `BECAME_COMMANDER` (channel `SQUAD`, channelId=`squadId`).

### POST `/squads/my/disband`
Disband current squad (commander only).

Response `200`:
```json
{ "disbanded": true }
```

Emits WS event: `SQUAD_DISBANDED` (channel `SQUAD`, channelId=`squadId`).

## Companies

### POST `/companies`
Create company. Creator becomes commander of the company.

Request:
```json
{ "name": "string" }
```

Response `200`:
```json
{
  "id": 50,
  "name": "string",
  "commanderId": 1,
  "createdAt": "2025-01-01T12:00:00Z",
  "updatedAt": "2025-01-01T12:00:00Z"
}
```

### GET `/companies/my`
Returns company where current user is commander (MVP),
or `404` if none.

Response `200`: `CompanyDto` plus squads list:
```json
{
  "id": 50,
  "name": "string",
  "commanderId": 1,
  "squads": [
    { "id": 10, "name": "string", "commanderId": 2 }
  ],
  "createdAt": "2025-01-01T12:00:00Z",
  "updatedAt": "2025-01-01T12:00:00Z"
}
```

### POST `/companies/my/add-squad`
Attach a squad to the company (company commander only).

Request:
```json
{ "squadId": 10 }
```

Response `200`:
```json
{ "added": true }
```

Emits WS event: `SQUAD_JOINED_COMPANY` (channel `COMPANY`, channelId=`companyId`).

### POST `/companies/my/remove-squad`
Detach squad from company.

Request:
```json
{ "squadId": 10 }
```

Response `200`:
```json
{ "removed": true }
```

Emits WS event: `SQUAD_LEFT_COMPANY` (channel `COMPANY`, channelId=`companyId`).

### POST `/companies/my/disband`
Disband company (commander only).

Response `200`:
```json
{ "disbanded": true }
```

Emits WS event: `COMPANY_DISBANDED` (channel `COMPANY`, channelId=`companyId`).

## Markers

Marker visibility is defined by `visibility` and scope fields.

### POST `/markers`
Create marker.

Request:
```json
{
  "title": "string",
  "description": "string|null",
  "lat": 56.123456,
  "lon": 24.123456,
  "visibility": "SQUAD|COMPANY|GLOBAL",
  "squadId": 10,
  "companyId": 50,
  "expiresAt": "2025-01-01T14:00:00Z|null"
}
```

Rules:
- For `SQUAD` visibility, `squadId` must be set.
- For `COMPANY` visibility, `companyId` must be set.
- For `GLOBAL`, both `squadId` and `companyId` must be null.

Response `200`:
```json
{
  "id": 900,
  "creatorId": 1,
  "title": "string",
  "description": null,
  "lat": 56.123456,
  "lon": 24.123456,
  "visibility": "SQUAD",
  "squadId": 10,
  "companyId": null,
  "expiresAt": null,
  "createdAt": "2025-01-01T12:00:00Z"
}
```

Emits WS event: `MARKER_CREATED` (channel depends on visibility; see `spec-ws.md`).

### GET `/markers`
List markers visible to current user.

Query params:
- `includeExpired` = `true|false` (default `false`)

Response `200`:
```json
{ "items": [ /* MarkerDto */ ] }
```

### DELETE `/markers/{markerId}`
Delete marker.

Response `200`:
```json
{ "deleted": true }
```

Emits WS event: `MARKER_DELETED` (channel depends on visibility/scope).

## Orders

### POST `/orders`
Create order inside squad.

Request:
```json
{
  "squadId": 10,
  "title": "string",
  "text": "string"
}
```

Response `200`:
```json
{
  "id": 700,
  "squadId": 10,
  "creatorId": 1,
  "title": "string",
  "text": "string",
  "status": "CREATED|IN_PROGRESS|COMPLETED",
  "createdAt": "2025-01-01T12:00:00Z",
  "updatedAt": "2025-01-01T12:00:00Z",
  "completedAt": null
}
```

Emits WS event: `ORDER_CREATED` (channel `SQUAD`, channelId=`squadId`).

### GET `/orders`
List orders.

Query params:
- `squadId` (required)
- `status` (optional)
- `includeCompleted` (optional)

Response `200`:
```json
{ "items": [ /* OrderDto */ ] }
```

### PATCH `/orders/{orderId}/status`
Update order status.

Request:
```json
{ "status": "CREATED|IN_PROGRESS|COMPLETED" }
```

Response `200`: `OrderDto`

Emits WS event: `ORDER_STATUS_CHANGED` (channel `SQUAD`, channelId=`squadId`).

## Geo

### POST `/geo/position`
Store current user position (append-only history).

Request:
```json
{ "lat": 56.123456, "lon": 24.123456 }
```

Response `200`:
```json
{ "saved": true, "recordedAt": "2025-01-01T12:00:00Z" }
```

### GET `/geo/positions`
Returns **latest** known position for each visible user.

Response `200`:
```json
{
  "items": [
    { "userId": 1, "lat": 56.1, "lon": 24.1, "recordedAt": "2025-01-01T12:00:00Z" }
  ]
}
```

Visibility (MVP):
- Users in same squad are visible to each other.
- Company commander can see squad members of squads attached to the company (if implemented in MVP).

## Admin (MVP)

### PATCH `/admin/users/{userId}/account-status`
Change user `accountStatus` (ADMIN/MODERATOR only).

Request:
```json
{ "accountStatus": "ACTIVE|BLOCKED|DELETED" }
```

Response `200`:
```json
{ "userId": 123, "accountStatus": "BLOCKED" }
```

Effects:
- Any protected REST request by that user must return `403 ACCOUNT_BLOCKED|ACCOUNT_DELETED`.
- WS connections for that user must be rejected/closed.
