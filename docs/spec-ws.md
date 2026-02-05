# spec-ws.md (Canonical WebSocket)

This is the **canonical** WebSocket contract for realtime events (MVP).
If any older document conflicts with this file, **this file wins**.

## Endpoint and auth

- Endpoint: `GET /ws/events`
- Auth:
  - preferred: `Authorization: Bearer <jwt>` on WebSocket upgrade
  - fallback: query param `token=<jwt>` (for limited clients)

If user account is not ACTIVE:
- the server must reject the connection (close during handshake or immediately after connect)
- error code: `ACCOUNT_BLOCKED` or `ACCOUNT_DELETED`

## Message types

### EVENT (server → client)
Envelope:
```json
{
  "type": "EVENT",
  "eventType": "JOINED_SQUAD",
  "channel": "SQUAD",
  "channelId": 10,
  "payload": { }
}
```

- `channel`: `"USER" | "SQUAD" | "COMPANY" | "GLOBAL"`
- `channelId`: numeric id for `USER/SQUAD/COMPANY`, omitted or null for `GLOBAL`

### SUBSCRIBE (client → server)
Client subscribes to a channel.
```json
{ "type": "SUBSCRIBE", "channel": "SQUAD", "channelId": 10 }
```

### UNSUBSCRIBE (client → server)
```json
{ "type": "UNSUBSCRIBE", "channel": "SQUAD", "channelId": 10 }
```

### ACK (server → client)
```json
{ "type": "ACK", "message": "subscribed", "channel": "SQUAD", "channelId": 10 }
```

### ERROR (server → client) — **single canonical format**
```json
{
  "type": "ERROR",
  "error": {
    "timestamp": "2025-01-01T12:00:00Z",
    "status": 403,
    "code": "ACCOUNT_BLOCKED",
    "message": "Account is blocked",
    "path": "/ws/events",
    "details": { }
  }
}
```

No alternative error formats are allowed.

## Canonical event types (MVP)

### Squad events (channel: SQUAD)

1) `SQUAD_CREATED`
Payload:
```json
{ "squadId": 10 }
```

2) `BECAME_COMMANDER`
Payload:
```json
{ "squadId": 10, "commanderId": 123 }
```

3) `JOINED_SQUAD`
Payload:
```json
{ "squadId": 10, "userId": 123 }
```

4) `LEFT_SQUAD`
Payload:
```json
{ "squadId": 10, "userId": 123 }
```

5) `KICKED_FROM_SQUAD`
Payload:
```json
{ "squadId": 10, "userId": 123, "kickedBy": 1 }
```

6) `SQUAD_DISBANDED`
Payload:
```json
{ "squadId": 10 }
```

### Company events (channel: COMPANY)

1) `SQUAD_JOINED_COMPANY`
Payload:
```json
{ "companyId": 50, "squadId": 10 }
```

2) `SQUAD_LEFT_COMPANY`
Payload:
```json
{ "companyId": 50, "squadId": 10 }
```

3) `COMPANY_DISBANDED`
Payload:
```json
{ "companyId": 50 }
```

### Marker events (channel depends on visibility)

1) `MARKER_CREATED`
Payload:
```json
{ "markerId": 900 }
```

2) `MARKER_DELETED`
Payload:
```json
{ "markerId": 900 }
```

Channel rules:
- marker visibility `SQUAD` → channel `SQUAD`, channelId=`squadId`
- `COMPANY` → channel `COMPANY`, channelId=`companyId`
- `GLOBAL` → channel `GLOBAL`

Expiration:
- If `expiresAt` is used, server must emit `MARKER_DELETED` when marker becomes inactive (background sweep).
- The payload stays the same (`markerId` only) to preserve the minimal contract.

### Order events (channel: SQUAD)

1) `ORDER_CREATED`
Payload:
```json
{ "orderId": 700, "squadId": 10 }
```

2) `ORDER_STATUS_CHANGED`
Payload:
```json
{
  "orderId": 700,
  "squadId": 10,
  "status": "IN_PROGRESS",
  "completedAt": null
}
```

## Compatibility mapping (legacy names → canonical)

If any client or scenario text uses the names below, they MUST be treated as aliases to canonical names:

- `SQUAD_MEMBER_JOINED` → `JOINED_SQUAD`
- `SQUAD_MEMBER_LEFT` → `LEFT_SQUAD`
- `COMMANDER_CHANGED` → `BECAME_COMMANDER`

No other aliases are supported in MVP.

## Subscriptions (recommended MVP behavior)

- On connect, server SHOULD auto-subscribe the user to:
  - `USER/<userId>` (optional, reserved for future)
  - the user's current `SQUAD/<squadId>` if they are in a squad
  - the user's company channel if they are company commander AND company exists

Client-driven subscribe/unsubscribe remains supported.
