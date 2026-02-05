# spec-ws.md (Канонический WebSocket)

Это **канонический** контракт WebSocket для realtime-событий (MVP).
Если любой старый документ противоречит этому файлу — **истиной считается этот файл**.

## Endpoint и auth

- Endpoint: `GET /ws/events`
- Auth:
  - предпочтительно: `Authorization: Bearer <jwt>` при WebSocket upgrade
  - fallback: query param `token=<jwt>` (для ограниченных клиентов)

Если `accountStatus != ACTIVE`:
- сервер должен отклонить соединение (на handshake или сразу после connect)
- код ошибки: `ACCOUNT_BLOCKED` или `ACCOUNT_DELETED`

## Типы сообщений

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
- `channelId`: числовой id для `USER/SQUAD/COMPANY`, опущен или `null` для `GLOBAL`

### SUBSCRIBE (client → server)
Подписка клиента на канал.
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

### ERROR (server → client) — **единый канонический формат**
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

Альтернативные форматы ошибок не допускаются.

## Канонические eventType (MVP)

### События отряда (канал: SQUAD)

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

### События компании (канал: COMPANY)

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

### События меток (канал зависит от visibility)

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

Правила выбора канала:
- visibility `SQUAD` → channel `SQUAD`, channelId=`squadId`
- `COMPANY` → channel `COMPANY`, channelId=`companyId`
- `GLOBAL` → channel `GLOBAL`

Истечение:
- Если используется `expiresAt`, сервер должен эмитить `MARKER_DELETED`, когда метка становится неактивной (фоновый sweep).
- Payload остаётся минимальным (`markerId`), чтобы сохранить стабильность контракта.

### События приказов (канал: SQUAD)

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

## Маппинг совместимости (legacy names → canonical)

Если клиент или текст сценариев использует имена ниже, они **должны трактоваться как алиасы** к каноническим именам:

- `SQUAD_MEMBER_JOINED` → `JOINED_SQUAD`
- `SQUAD_MEMBER_LEFT` → `LEFT_SQUAD`
- `COMMANDER_CHANGED` → `BECAME_COMMANDER`

Другие алиасы в MVP не поддерживаются.

## Подписки (рекомендуемое поведение MVP)

- При connect сервер SHOULD auto-subscribe пользователя на:
  - `USER/<userId>` (optional, резерв на будущее)
  - текущий `SQUAD/<squadId>`, если пользователь в отряде
  - канал компании, если пользователь — командир компании И компания существует

При этом client-driven subscribe/unsubscribe остаётся поддержан.
