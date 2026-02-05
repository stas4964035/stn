# spec-api.md (Канонический REST API)

Это **канонический** контракт REST API для Tactical App backend (MVP).
Если любой старый документ противоречит этому файлу — **истиной считается этот файл**.

## Конвенции

- Base path: `/api/v1`
- Content-Type: `application/json; charset=utf-8`
- Auth: `Authorization: Bearer <jwt>`
- Время: UTC, ISO-8601, `Instant` (пример: `2025-01-01T12:00:00Z`)
- ID: `long`
- Пагинация: **не в MVP**, если явно не указано.
- Ошибки: см. `spec-domain.md` (единый формат для REST и WS).

## Аутентификация

### POST `/auth/register`
Регистрирует нового пользователя.

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

Ошибки:
- `409 USER_ALREADY_EXISTS`
- `400 VALIDATION_ERROR`

### POST `/auth/login`
Аутентификация по login/password.

Request:
```json
{ "login": "string", "password": "string" }
```

Response `200` — тот же формат, что и у `/auth/register`.

Ошибки:
- `401 INVALID_CREDENTIALS`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`
- `400 VALIDATION_ERROR`

## Пользователи

### GET `/users/me`
Возвращает профиль текущего пользователя.

Response `200`: `UserDto` (как поле `user` в ответах auth).

Ошибки:
- `401 UNAUTHORIZED`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`

### PATCH `/users/me`
Обновляет профиль (поля MVP).

Request:
```json
{
  "displayName": "string",
  "avatarUrl": "string|null"
}
```

Response `200`: `UserDto`

Ошибки:
- `400 VALIDATION_ERROR`
- `401 UNAUTHORIZED`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`

### POST `/users/me/alive`
Установить `isAlive=true`.

Response `200`: `UserDto`

### POST `/users/me/dead`
Установить `isAlive=false`.

Response `200`: `UserDto`

## Отряды (Squads)

### POST `/squads`
Создаёт отряд. Создатель становится командиром и участником.

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

WS событие: `SQUAD_CREATED` (канал `SQUAD`, channelId=`squadId`).

Ошибки:
- `400 VALIDATION_ERROR`
- `403 ACCOUNT_BLOCKED | ACCOUNT_DELETED`

### GET `/squads/my`
Возвращает отряд, в котором состоит текущий пользователь, или `404`, если отряда нет.

Response `200`: `SquadDto` + список участников:
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

Ошибки:
- `404 SQUAD_NOT_FOUND` (если пользователь не состоит в отряде)

### POST `/squads/my/join`
Вступить в существующий отряд по invite-коду.

Request:
```json
{ "inviteCode": "string" }
```

Response `200`: `SquadDto`

WS событие: `JOINED_SQUAD` (канал `SQUAD`, channelId=`squadId`).

Ошибки:
- `404 SQUAD_NOT_FOUND`
- `409 ALREADY_IN_SQUAD`
- `403 FORBIDDEN` (например, если будет правило “отряд заполнен”) или `400 VALIDATION_ERROR`

### POST `/squads/my/leave`
Выйти из текущего отряда.

Response `200`:
```json
{ "left": true }
```

WS событие: `LEFT_SQUAD` (канал `SQUAD`, channelId=`squadId`).

Примечания:
- Если командир выходит и остаются участники, система назначает нового командира и эмитит `BECAME_COMMANDER`.

### POST `/squads/my/kick`
Командир исключает участника.

Request:
```json
{ "userId": 123 }
```

Response `200`:
```json
{ "kicked": true }
```

WS событие: `KICKED_FROM_SQUAD` (канал `SQUAD`, channelId=`squadId`).

### POST `/squads/my/transfer-commander`
Передать роль командира другому участнику.

Request:
```json
{ "userId": 123 }
```

Response `200`:
```json
{ "commanderId": 123 }
```

WS событие: `BECAME_COMMANDER` (канал `SQUAD`, channelId=`squadId`).

### POST `/squads/my/disband`
Распустить текущий отряд (только командир).

Response `200`:
```json
{ "disbanded": true }
```

WS событие: `SQUAD_DISBANDED` (канал `SQUAD`, channelId=`squadId`).

## Компании (Companies)

### POST `/companies`
Создать компанию. Создатель становится командиром компании.

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
Возвращает компанию, где текущий пользователь является командиром (MVP),
или `404`, если компании нет.

Response `200`: `CompanyDto` + список отрядов:
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
Привязать отряд к компании (только командир компании).

Request:
```json
{ "squadId": 10 }
```

Response `200`:
```json
{ "added": true }
```

WS событие: `SQUAD_JOINED_COMPANY` (канал `COMPANY`, channelId=`companyId`).

### POST `/companies/my/remove-squad`
Отвязать отряд от компании.

Request:
```json
{ "squadId": 10 }
```

Response `200`:
```json
{ "removed": true }
```

WS событие: `SQUAD_LEFT_COMPANY` (канал `COMPANY`, channelId=`companyId`).

### POST `/companies/my/disband`
Распустить компанию (только командир).

Response `200`:
```json
{ "disbanded": true }
```

WS событие: `COMPANY_DISBANDED` (канал `COMPANY`, channelId=`companyId`).

## Метки (Markers)

Видимость метки определяется `visibility` и scope-полями.

### POST `/markers`
Создать метку.

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

Правила:
- Для `SQUAD` должно быть задано `squadId`.
- Для `COMPANY` должно быть задано `companyId`.
- Для `GLOBAL` оба поля `squadId` и `companyId` должны быть `null`.

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

WS событие: `MARKER_CREATED` (канал зависит от visibility; см. `spec-ws.md`).

### GET `/markers`
Список меток, видимых текущему пользователю.

Query params:
- `includeExpired` = `true|false` (default `false`)

Response `200`:
```json
{ "items": [ /* MarkerDto */ ] }
```

### DELETE `/markers/{markerId}`
Удалить метку.

Response `200`:
```json
{ "deleted": true }
```

WS событие: `MARKER_DELETED` (канал зависит от visibility/scope).

## Приказы (Orders)

### POST `/orders`
Создать приказ в рамках отряда.

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

WS событие: `ORDER_CREATED` (канал `SQUAD`, channelId=`squadId`).

### GET `/orders`
Список приказов.

Query params:
- `squadId` (required)
- `status` (optional)
- `includeCompleted` (optional)

Response `200`:
```json
{ "items": [ /* OrderDto */ ] }
```

### PATCH `/orders/{orderId}/status`
Изменить статус приказа.

Request:
```json
{ "status": "CREATED|IN_PROGRESS|COMPLETED" }
```

Response `200`: `OrderDto`

WS событие: `ORDER_STATUS_CHANGED` (канал `SQUAD`, channelId=`squadId`).

## Геопозиции (Geo)

### POST `/geo/position`
Сохранить позицию текущего пользователя (append-only история).

Request:
```json
{ "lat": 56.123456, "lon": 24.123456 }
```

Response `200`:
```json
{ "saved": true, "recordedAt": "2025-01-01T12:00:00Z" }
```

### GET `/geo/positions`
Возвращает **последнюю** известную позицию для каждого видимого пользователя.

Response `200`:
```json
{
  "items": [
    { "userId": 1, "lat": 56.1, "lon": 24.1, "recordedAt": "2025-01-01T12:00:00Z" }
  ]
}
```

Видимость (MVP):
- Пользователи в одном отряде видимы друг другу.
- Командир компании может видеть участников отрядов, привязанных к компании (если реализовано в MVP).

## Admin (MVP)

### PATCH `/admin/users/{userId}/account-status`
Изменить `accountStatus` пользователя (только ADMIN/MODERATOR).

Request:
```json
{ "accountStatus": "ACTIVE|BLOCKED|DELETED" }
```

Response `200`:
```json
{ "userId": 123, "accountStatus": "BLOCKED" }
```

Эффекты:
- Любой защищённый REST-запрос этого пользователя должен возвращать `403 ACCOUNT_BLOCKED|ACCOUNT_DELETED`.
- WS соединения этого пользователя должны быть отклонены/закрыты.
