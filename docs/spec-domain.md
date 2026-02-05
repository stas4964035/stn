# spec-domain.md (Доменные правила, ошибки, security)

Этот файл объединяет:
- доменные инварианты и доступы (ACL)
- модель ошибок (единый канонический ErrorResponse)
- security правила (JWT validation, account status, политика WS)

Если любой старый документ противоречит этому файлу — **истиной считается этот файл**.

## Роли и статус аккаунта

### Роли
- `USER`
- `MODERATOR` (права модерации/админ-панели)
- `ADMIN`

### accountStatus
- `ACTIVE`
- `BLOCKED`
- `DELETED`

Правила:
- Если `accountStatus != ACTIVE`:
  - любой защищённый REST endpoint должен отвечать `403` и code `ACCOUNT_BLOCKED` или `ACCOUNT_DELETED`
  - любое WS соединение должно быть отклонено/закрыто с `ERROR` (в формате ErrorResponse)

## JWT

- Алгоритм: HS256
- Claims (MVP): `sub` = userId, `email`, `role`
- TTL токена: 24h

### Инвалидация токенов при изменении accountStatus (канон, conservative)
Требование: “токены недействительны с момента BLOCKED/DELETED”.

Правило реализации MVP:
- Каждый защищённый REST запрос MUST загружать пользователя по `sub` и проверять `accountStatus`.
- Каждое WS соединение MUST загружать пользователя по `sub` и проверять `accountStatus` при подключении.
- Для long-lived WS соединений сервер MUST закрывать сессию, если `accountStatus` меняется на не-ACTIVE.

Это делает ранее выданные токены фактически непригодными после смены статуса без введения blacklist в MVP.

## Инварианты домена (MVP)

### Отряд (Squad)
- Пользователь может состоять максимум в одном отряде.
- У отряда ровно один командир (который также участник).
- Командир может:
  - исключать участников
  - передавать роль командира
  - распускать отряд
- Правила выхода:
  - если командир выходит и участники остаются, система назначает нового командира и эмитит `BECAME_COMMANDER`
  - если выходит последний участник, отряд распускается (или удаляется — по реализации)

### Компания (Company)
- У компании ровно один командир.
- Отряд может принадлежать максимум одной компании.
- Командир компании может привязывать/отвязывать отряды и распускать компанию.

### Метки (Markers)
- Видимость метки:
  - `SQUAD` (scope = squadId)
  - `COMPANY` (scope = companyId)
  - `GLOBAL` (без scope)
- Метка активна если `expiresAt == null` ИЛИ `expiresAt > now(UTC)`.
- Listing по умолчанию: `includeExpired=false` (возвращать только активные).

### Приказы (Orders)
- Приказ принадлежит отряду.
- Статус: `CREATED | IN_PROGRESS | COMPLETED`
- При переходе в `COMPLETED` устанавливается `completedAt`.

### Геопозиции (Geo)
- `POST /geo/position` — append-only история.
- `GET /geo/positions` — возвращает **последнюю** позицию по видимым пользователям.

## Ошибки (единая каноническая модель)

### ErrorResponse
Используется в REST и WS `ERROR`.

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

### Канонические error codes (MVP)

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

Примечания:
- `details` зарезервирован под field errors (validation) и контекстные метаданные.
