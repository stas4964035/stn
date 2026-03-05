# spec-api.md (Дополнение к ТЗ: REST API)

Этот файл фиксирует REST-контракты как техническую реализацию требований из `tz_tactical_app.md`.
Он не задаёт самостоятельную продуктовую политику и не должен переопределять ТЗ.

Правило чтения:
- сначала трактуется бизнес-смысл в `tz_tactical_app.md`;
- затем этот документ уточняет форматы запросов/ответов, URL и коды статусов;
- при конфликте приоритет у `tz_tactical_app.md`, после чего контракт в этом файле должен быть обновлён.

## Конвенции

- Base path: `/api/v1`
- Content-Type: `application/json; charset=utf-8`
- Auth: `Authorization: Bearer <jwt>`
- Время: UTC, ISO-8601, `Instant` (пример: `2025-01-01T12:00:00Z`)
- ID: `long`
- Пагинация: **не в MVP**, если явно не указано.
- Ошибки: см. `spec-domain.md` (единый формат для REST и WS).
- Для `400 VALIDATION_ERROR` сервер MUST использовать `details.errors[]` в формате `{field, message, code}`.

## Аутентификация

### POST `/auth/register`
Регистрирует нового пользователя.

Request:
```json
{
  "email": "string",
  "password": "string",
  "nickname": "string"
}
```

Response `200`:
```json
{
  "token": "jwt",
  "user": {
    "id": 1,
    "email": "string",
    "nickname": "string",
    "accountStatus": "ACTIVE",
    "role": "USER",
    "avatarIcon": null,
    "isAlive": true,
    "createdAt": "2025-01-01T12:00:00Z",
    "updatedAt": "2025-01-01T12:00:00Z"
  }
}
```

Ошибки:
- `409 USER_ALREADY_EXISTS`
- `400 VALIDATION_ERROR`

### POST `/auth/email`
Аутентификация по email/password.

Request:
```json
{ "email": "string", "password": "string" }
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
  "nickname": "string",
  "avatarIcon": "string|null"
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
  "description": null,  
  "commanderId": 1,
  "companyId": null,
  "isOpen": true,
  "color": "red",
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
  "description": null,
  "commanderId": 1,
  "companyId": null,
  "isOpen": true,
  "color": "red",
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
Вступить в существующий открытый отряд (isOpen = true).

Условия вступления:
- пользователь не состоит в другом отряде;
- отряд существует;
- `isOpen = true`.

Request:
```json
{ "squadId": 10 }
```

Response `200`: `SquadDto`

WS событие: `JOINED_SQUAD` (канал `SQUAD`, channelId=`squadId`).

Ошибки:
- `404 SQUAD_NOT_FOUND`
- `409 ALREADY_IN_SQUAD`
- `403 FORBIDDEN` (если `isOpen = false`)
- `400 VALIDATION_ERROR`

### POST `/squads/my/leave`
Выйти из текущего отряда.

Response `200`:
```json
{ "left": true }
```

WS событие: `LEFT_SQUAD` (канал `SQUAD`, channelId=`squadId`).

Примечания:
- Если командир выходит и остаются участники, система назначает нового командира и эмитит `BECAME_COMMANDER`.
- Если выходит последний участник, отряд удаляется.
  - В этом случае сервер должен:
    - эмитить `LEFT_SQUAD` (как обычно);
    - затем эмитить `SQUAD_DISBANDED` (канал `SQUAD`, channelId=`squadId`);
    - удалить все приказы (`Orders`) данного отряда (каскад).
  - Если отряд состоял в компании — применяются те же WS эффекты, что и при отвязке/удалении:
    - `SQUAD_LEFT_COMPANY` (канал `COMPANY`, channelId=`companyId`);
    - и, если после этого в компании не осталось отрядов, `COMPANY_DISBANDED`.

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

Примечания:
- При распуске отряда сервер должен удалить все приказы (`Orders`) данного отряда (каскад).
- Если отряд состоял в компании, сервер должен эмитить `SQUAD_LEFT_COMPANY` (канал `COMPANY`, `channelId=companyId`) и, если компания стала пустой, `COMPANY_DISBANDED`.

## Компании (Companies)

### POST `/companies`
Создать компанию может только командир отряда. Управление компанией осуществляют командиры отрядов, входящих в компанию.
- `isOpen` — определяет, может ли отряд вступать в компанию.

Request:
```json
{ 
  "name": "string",
  "isOpen": true
}
```

Response `200`:

```json
{
  "id": 50,
  "name": "string",
  "description": null,
  "isOpen": true,
  "createdAt": "2025-01-01T12:00:00Z",
  "updatedAt": "2025-01-01T12:00:00Z"
}
```
Ошибки:
- `403 FORBIDDEN` (если не командир отряда)
- `404 SQUAD_NOT_FOUND` (если пользователь не в отряде)

### GET `/companies/my`
Возвращает компанию, в которую входит отряд текущего пользователя,
или `404`, если:
- пользователь не состоит в отряде, или
- отряд пользователя не состоит в компании.

Response `200`: `CompanyDto` + список отрядов:
```json
{
  "id": 50,
  "name": "string",
  "description": null,
  "isOpen": true,
  "squads": [
    { "id": 10, "name": "string", "commanderId": 2 }
  ],
  "createdAt": "2025-01-01T12:00:00Z",
  "updatedAt": "2025-01-01T12:00:00Z"
}
```

### POST `/companies/my/add-squad`
Привязать отряд к компании (командир отряда).
Отряд может быть добавлен только если `isOpen = true`.
Если `isOpen = false`, сервер возвращает `403 FORBIDDEN`.

Request:
```json
{ "squadId": 10 }
```

Response `200`:
```json
{ "added": true }
```

WS события: `SQUAD_JOINED_COMPANY` (канал `COMPANY`, channelId=`companyId`).

### POST `/companies/my/remove-squad`
Отвязать отряд от компании(командир отряда, входящего в компанию и являющийся командиром этого отряда).

Request:
```json
{ "squadId": 10 }
```

Response `200`:
```json
{ "removed": true }
```
Правило: если после отвязки в компании не осталось отрядов, компания удаляется.


WS события:
- `SQUAD_LEFT_COMPANY` (канал `COMPANY`, channelId=`companyId`).
- Если после отвязки в компании не осталось отрядов и компания удаляется — дополнительно эмитить `COMPANY_DISBANDED` (канал `COMPANY`, `channelId=companyId`)

### POST `/companies/my/disband`
Распустить компанию (только командир отряда, в случае если этот отряд единственный в компании).

Response `200`:
```json
{ "disbanded": true }
```

WS событие: `COMPANY_DISBANDED` (канал `COMPANY`, `channelId=companyId`).

## Типы тактических меток (TacticalMarkerType)

Тип метки определяет правила создания и поведения меток.

### TacticalMarkerTypeDto

```json
{
  "id": 1,
  "key": "ENEMY_SPOTTED",
  "name": "Enemy spotted",
  "defaultDescription": "string|null",
  "icon": "string",
  "defaultLifetimeSeconds": 600,
  "roleRestriction": "ANY_MEMBER|COMMANDER_ONLY",
  "canSendToCompany": true,
  "uniquenessPolicy": "NONE|ONE_PER_USER|ONE_PER_SQUAD",
  "active": true
}
```
### GET `/marker-types`
Возвращает список активных типов меток.

Response `200`:
```json
{
  "items": [ /* TacticalMarkerTypeDto */ ]
}
```
### POST `/admin/marker-types`
Создание нового типа метки (только ADMIN).

### PATCH `/admin/marker-types/{id}`
Изменение типа метки (только ADMIN).

## Метки (Markers)

Видимость метки правилами `TacticalMarkerType` и флагом `sendToCompany`.

### POST `/markers`
Создать метку.

Request:
```json
{
  "markerTypeId": 1,
  "lat": 56.123456,
  "lon": 24.123456,
  "description": "string|null",
  "sendToCompany": true,
  "expiresAt": "2025-01-01T14:00:00Z|null"
}
```

Правила:

- Автор должен состоять в отряде.
- Метка всегда создаётся в рамках отряда автора.
- Если `sendToCompany = true`,
  сервер проверяет:
    - что отряд состоит в компании;
    - что тип метки допускает отправку в компанию
      (`TacticalMarkerType.canSendToCompany = true`).
  Если отряд автора не состоит в компании — вернуть
  `404 COMPANY_NOT_FOUND`.
- Eсли `expiresAt` передан → использовать его:
- иначе если `defaultLifetimeSeconds` у типа задан → вычислить `expiresAt = nowUtc + defaultLifetimeSeconds`
- иначе `expiresAt=null`

Response `200`:
```json
{
  "id": 900,
  "markerTypeId": 1,
  "creatorId": 1,
  "squadId": 10,
  "companyId": 50,
  "lat": 56.123456,
  "lon": 24.123456,
  "description": null,
  "expiresAt": null,
  "createdAt": "2025-01-01T12:00:00Z"
}
```
companyId заполняется только если sendToCompany = true.

WS событие: `MARKER_CREATED` (канал зависит от companyId; см. `spec-ws.md`).

Ошибки:
- `404 COMPANY_NOT_FOUND` (если `sendToCompany=true`, но у отряда автора нет компании)
- `403 FORBIDDEN` (если `sendToCompany=true`, но `canSendToCompany=false`)
- `400 VALIDATION_ERROR`

### GET `/markers`
Список меток, видимых текущему пользователю.

Query params:
- `includeExpired` = `true|false` (default `false`)

Response `200`:
```json
{ "items": [ /* MarkerDto */ ] }
```

### DELETE `/markers/{markerId}`
Удалить метку (**деактивировать**, append-only).

Семантика (канон):
- Запись метки физически **не удаляется**.
- Удаление = сервер устанавливает `expiresAt = nowUtc` (UTC `Instant`).
- Повторный вызов для уже неактивной метки должен быть идемпотентным
(поведение по статусу/ответу — на усмотрение реализации MVP, но без физического удаления).

Response `200`:
```json
{ "deleted": true }
```
WS событие: `MARKER_DELETED` (обязательное).
- Эмитится в `SQUAD/<squadId>` и, если `sendToCompany=true`, дополнительно в `COMPANY/<companyId>`
- Эмиссия должна происходить как при деактивации через этот endpoint, так и при истечении `expiresAt` (sweep).



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

Пользователь видит:
- последнюю позицию всех участников своего отряда;
- последнюю позицию командиров всех отрядов,
  входящих в ту же компанию, что и его отряд.


  Если пользователь не состоит в отряде —
  видимость ограничивается только его собственной позицией.
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

### POST `/admin/marker-types`
Создать новый тип тактической метки (только ADMIN).

Request:
```json
{
  "key": "ENEMY_SPOTTED",
  "name": "Enemy spotted",
  "defaultDescription": "string|null",
  "icon": "string",
  "defaultLifetimeSeconds": 600,
  "roleRestriction": "ANY_MEMBER|COMMANDER_ONLY",
  "canSendToCompany": true,
  "uniquenessPolicy": "NONE|ONE_PER_USER|ONE_PER_SQUAD",
  "active": true
}
```
Response `200`:
```json
{
  "id": 1,
  "key": "ENEMY_SPOTTED",
  "name": "Enemy spotted",
  "defaultDescription": null,
  "icon": "string",
  "defaultLifetimeSeconds": 600,
  "roleRestriction": "ANY_MEMBER",
  "canSendToCompany": true,
  "uniquenessPolicy": "NONE",
  "active": true
}
```

Ошибки:
- 400 VALIDATION_ERROR
- 409 MARKER_TYPE_ALREADY_EXISTS
- 403 FORBIDDEN
- 401 UNAUTHORIZED

### PATCH `/admin/marker-types/{id}`

Изменить существующий тип метки (только ADMIN).

Request (частичное обновление):
```json
{
  "name": "string|null",
  "defaultDescription": "string|null",
  "icon": "string|null",
  "defaultLifetimeSeconds": 300,
  "roleRestriction": "ANY_MEMBER|COMMANDER_ONLY",
  "canSendToCompany": false,
  "uniquenessPolicy": "NONE|ONE_PER_USER|ONE_PER_SQUAD",
  "active": false
}
```
Response `200`: `TacticalMarkerTypeDto`

Ошибки:
- 404 MARKER_TYPE_NOT_FOUND
- 400 VALIDATION_ERROR
- 403 FORBIDDEN
- 401 UNAUTHORIZED

### GET `/admin/marker-types`

Возвращает список всех типов меток (включая `active=false`).

Response `200`:
```json
{ "items": [ /* TacticalMarkerTypeDto */ ] }
