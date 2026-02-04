
# Backend API Specification (на основе use-cases)

Этот документ описывает публичный backend API (REST + WebSocket) тактического приложения.
Спецификация построена на основе пользовательских сценариев (UC-1…UC-13) из `use-cases.md` и утверждённого ТЗ.

- Базовый префикс REST API: `/api/v1`
- Формат: JSON (UTF-8)
- Аутентификация: JWT через заголовок `Authorization: Bearer <jwt-token>`
- Все эндпоинты, кроме `/api/v1/auth/*`, требуют валидный JWT.
- Временные поля: ISO 8601 (`YYYY-MM-DDThh:mm:ssZ`)

---

## 1. DTO-модели

### 1.1. UserDto

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "Viking",
  "systemRole": "USER",
  "status": "ALIVE",
  "avatarIcon": "wolf_01",
  "squadId": 10
}
```

Поля:

- `id`: number — идентификатор пользователя.
- `email`: string — логин/email.
- `nickname`: string — позывной.
- `systemRole`: `"USER" | "MODERATOR" | "ADMIN"`.
- `status`: `"ALIVE" | "DEAD"` — игровой статус.
- `avatarIcon`: string — ключ иконки аватара из допустимого набора.
- `squadId`: number | null — текущий отряд пользователя (FK на `Squad`).

### 1.2. SquadSummaryDto

```json
{
  "id": 10,
  "name": "Отряд Волк",
  "description": "Описание отряда",
  "isOpen": true,
  "color": "#FF0000",
  "membersCount": 5,
  "companyId": null
}
```

Поля:

- `id`: number
- `name`: string
- `description`: string | null
- `isOpen`: boolean — открыт ли отряд для вступления.
- `color`: string — цвет отряда (hex).
- `membersCount`: number — количество участников.
- `companyId`: number | null — рота, к которой привязан отряд.

### 1.3. SquadDetailsDto

```json
{
  "id": 10,
  "name": "Отряд Волк",
  "description": "Описание отряда",
  "isOpen": true,
  "color": "#FF0000",
  "companyId": null,
  "commanderId": 1,
  "members": [
    {
      "id": 1,
      "nickname": "Viking",
      "status": "ALIVE",
      "avatarIcon": "wolf_01"
    },
    {
      "id": 2,
      "nickname": "Fox",
      "status": "DEAD",
      "avatarIcon": "fox_02"
    }
  ]
}
```

### 1.4. CompanySummaryDto

```json
{
  "id": 2,
  "name": "Рота Волга",
  "description": "Объединение северных отрядов",
  "isOpen": true,
  "squadsCount": 3
}
```

### 1.5. CompanyDetailsDto

```json
{
  "id": 2,
  "name": "Рота Волга",
  "description": "Объединение северных отрядов",
  "isOpen": true,
  "squads": [
    {
      "id": 10,
      "name": "Отряд Волк",
      "color": "#FF0000",
      "commanderId": 1,
      "membersCount": 5
    }
  ]
}
```

### 1.6. OrderDto

```json
{
  "id": 100,
  "squadId": 10,
  "authorId": 1,
  "description": "Занять высоту на холме",
  "status": "ACTIVE",
  "createdAt": "2025-01-01T12:00:00Z",
  "completedAt": null
}
```

### 1.7. MarkerTypeDto

```json
{
  "id": 1,
  "key": "ENEMY_SPOTTED",
  "name": "Обнаружен противник",
  "defaultDescription": "Контакт с противником",
  "icon": "enemy_spotted_icon",
  "defaultLifetimeSeconds": 600,
  "roleRestriction": "ANY_MEMBER",
  "canSendToCompany": true,
  "uniquenessPolicy": "NONE",
  "category": "ENEMY",
  "active": true
}
```

Поля:

- `key`: string — машинный ключ (`ENEMY_SPOTTED`, `ATTACK`, `MOVE`, `HOLD`, `MISSION_TARGET`, `RESPAWN_POINT` и т.п.).
- `roleRestriction`: `"ANY_MEMBER" | "COMMANDER_ONLY"` — кто может ставить метки этого типа.
- `canSendToCompany`: boolean — можно ли транслировать на уровень роты.
- `uniquenessPolicy`: `"NONE" | "ONE_PER_USER" | "ONE_PER_SQUAD"` — политика уникальности.
- `defaultLifetimeSeconds`: number | null — срок жизни метки по умолчанию (может быть null для «вечных» меток).
- `active`: boolean — активен ли тип метки.

### 1.8. MarkerDto

```json
{
  "id": 500,
  "markerTypeId": 1,
  "markerTypeKey": "ENEMY_SPOTTED",
  "squadId": 10,
  "companyId": 2,
  "authorId": 3,
  "lat": 59.123456,
  "lng": 30.123456,
  "description": "Контакт на холме",
  "createdAt": "2025-01-01T12:00:00Z",
  "expiresAt": "2025-01-01T12:10:00Z"
}
```

Поля MarkerDto:

- `id`: идентификатор метки.
- `markerTypeId`: идентификатор типа метки.
- `markerTypeKey`: ключ типа метки (`ENEMY_SPOTTED`, `MOVE` и т.д.).
- `squadId`: отряд, к которому относится метка.
- `companyId`: рота, если метка транслировалась на уровень роты (может быть `null`).
- `authorId`: автор метки (может быть `null`, если автор отсутствует или анонимизирован).
- `lat` / `lng`: координаты метки.
- `description`: описание, видимое пользователям.
- `createdAt`: время создания метки.
- `expiresAt`: время устаревания; активность метки определяется на клиенте (маркер считается актуальным, если `expiresAt` отсутствует или больше текущего времени).

### 1.9. GeoPositionDto

```json
{
  "userId": 3,
  "lat": 59.123456,
  "lng": 30.123456,
  "timestamp": "2025-01-01T12:00:00Z"
}
```

Используется при отдаче данных для карты.

### 1.10. ChatMessageDto (сервер → клиент)

```json
{
  "type": "CHAT_MESSAGE",
  "channelType": "SQUAD",
  "channelId": 10,
  "messageId": 1,
  "author": {
    "id": 1,
    "nickname": "Viking",
    "avatarIcon": "wolf_01"
  },
  "text": "Двигаемся к точке сбора",
  "sentAt": "2025-01-01T12:00:00Z"
}
```

### 1.11. ChatClientMessageDto (клиент → сервер)

```json
{
  "action": "SEND_MESSAGE",
  "channelType": "SQUAD",
  "text": "Двигаемся к точке сбора"
}
```

---

## 2. Auth API

> Этап реализации: 1–2 (модуль аутентификации и базовый профиль пользователя).

### 2.1. POST /api/v1/auth/register

Регистрация пользователя (UC-1).

**Request**

```json
{
  "email": "user@example.com",
  "password": "StrongPassword123",
  "nickname": "Viking"
}
```

**Response 201**

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "Viking",
  "systemRole": "USER",
  "status": "ALIVE",
  "avatarIcon": "wolf_01",
  "squadId": null
}
```

Ошибки: `400` (невалидные данные), `409` (email уже существует).

### 2.2. POST /api/v1/auth/login

Вход пользователя (UC-1).

**Request**

```json
{
  "email": "user@example.com",
  "password": "StrongPassword123"
}
```

**Response 200**

```json
{
  "token": "jwt-token-string",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "Viking",
    "systemRole": "USER",
    "status": "ALIVE",
    "avatarIcon": "wolf_01",
    "squadId": null
  }
}
```

Ошибки: `400`, `401`.

---

## 3. Users API

> Этап реализации: 1–2 (модуль аутентификации и базовый профиль пользователя).

### 3.1. GET /api/v1/users/me

Получить данные текущего пользователя (UserDto).

**Response 200** — `UserDto`.

Ошибки: `401`.

### 3.2. PATCH /api/v1/users/me/status

Изменить игровой статус `ALIVE/DEAD` (UC-9).

**Request**

```json
{
  "status": "DEAD"
}
```

**Response 200** — обновлённый `UserDto`.

Ошибки: `400` (невалидный статус), `401`.

### 3.3. PATCH /api/v1/users/me/avatar

Изменить аватар пользователя (UC-9).

**Request**

```json
{
  "avatarIcon": "fox_02"
}
```

**Response 200** — обновлённый `UserDto`.

Ошибки: `400` (иконка вне допустимого списка), `401`.

### 3.4. GET /api/v1/avatars

(Опционально) получить список доступных аватар-иконок для UI (UC-9, шаг 2).

**Response 200**

```json
{
  "items": [
    "wolf_01",
    "fox_02",
    "bear_03"
  ]
}
```

---

## 4. Squads API (отряды)

> Этап реализации: 3 (модуль "Отряды").

### 4.1. GET /api/v1/squads

Список отрядов, с возможностью фильтрации по открытым (UC-3).

**Query-параметры:**

- `isOpen` (optional, boolean) — если `true`, только открытые (`isOpen = true`), если `false` — только закрытые.

**Response 200** — массив `SquadSummaryDto`.

### 4.2. POST /api/v1/squads

Создать отряд (UC-2).

**Request**

```json
{
  "description": "Описание отряда (опционально)"
}
```

**Семантика:**

- Генерируется имя `Отряд [животное/змея]`.
- Выбирается цвет из палитры.
- Текущий пользователь становится командиром и единственным участником.

**Response 201** — `SquadDetailsDto`.

Ошибки: `401`, `409` (пользователь уже состоит в отряде).

### 4.3. GET /api/v1/squads/my

Текущий отряд пользователя (UC-2/UC-3/UC-4).

**Response 200** — `SquadDetailsDto`.

Ошибки: `404` (пользователь не состоит в отряде).

### 4.4. POST /api/v1/squads/{squadId}/join

Вступить в указанный отряд (UC-3).

**Response 200** — `SquadDetailsDto` после вступления.

Ошибки:

- `401`
- `403` — пользователь уже состоит в отряде.
- `404` — отряд не найден.
- `409` — отряд закрыт (`isOpen = false`).

### 4.5. POST /api/v1/squads/my/leave

Выйти из текущего отряда (UC-4, выход солдата).

**Response 204** — без тела.

Семантика:

- Если после выхода отряд пуст — отряд удаляется.
- Если пользователь, покидающий отряд является его командиром и в отряде есть другие пользователи, то командиром отряда назначается выбранный случайным образом член отряда.

Ошибки: `401`, `404` (нет текущего отряда).

### 4.6. POST /api/v1/squads/my/disband

Распустить отряд (UC-4, вариант командира).

**Response 204**.

Семантика:

- Только командир может распустить отряд;
- Отряд и связанные сущности (приказы, тактические метки) удаляются;
- Все участники отвязываются от отряда.

Ошибки: `401`, `403` (не командир), `404`.

### 4.7. PATCH /api/v1/squads/my

Обновить параметры отряда командиром (UC-10).

**Request (partial)**

```json
{
  "name": "Отряд Север",
  "description": "Обновлённое описание",
  "isOpen": false
}
```

**Response 200** — обновлённый `SquadDetailsDto`.

Ошибки: `401`, `403` (не командир и не MODERATOR/ADMIN), `404`.

### 4.8. POST /api/v1/squads/my/members/{userId}/kick

Кикнуть участника из отряда (UC-10).

**Response 204** — без тела.

Семантика:

- Доступно командиру этого отряда или MODERATOR/ADMIN.
- Цель должна состоять в текущем отряде инициатора.

Ошибки: `401`, `403`, `404` (участник или отряд не найден), `409` (попытка кикнуть себя через этот эндпоинт).



### 4.9. POST /api/v1/squads/my/transfer-commander

Явная передача командования от текущего командира другому участнику отряда (manual transfer).

**Request**

```json
{
  "newCommanderUserId": 123
}
```

Поля:

- `newCommanderUserId`: number — идентификатор пользователя, который уже состоит в текущем отряде инициатора.

**Response 200** — обновлённый `SquadDetailsDto` с новым `commanderId`.

Пример:

```json
{
  "id": 10,
  "name": "Отряд Волк",
  "description": "Описание отряда",
  "isOpen": true,
  "color": "#FF0000",
  "companyId": null,
  "commanderId": 123,
  "members": [
    {
      "id": 1,
      "nickname": "Viking",
      "status": "ALIVE",
      "avatarIcon": "wolf_01"
    },
    {
      "id": 123,
      "nickname": "Fox",
      "status": "ALIVE",
      "avatarIcon": "fox_02"
    }
  ]
}
```

**Семантика:**

1. По JWT определяется текущий пользователь и его отряд.
2. Проверяется, что инициатор является командиром этого отряда (или имеет роль ADMIN/MODERATOR, если разрешено форс-переназначение сверху).
3. Проверяется, что `newCommanderUserId` существует и состоит в этом же отряде.
4. Проверяется, что `newCommanderUserId` не совпадает с текущим командиром.
5. Обновляется поле `commanderId` у отряда.
6. (Опционально) публикуется realtime‑событие `COMMANDER_CHANGED` в отрядный/ротный каналы.

**Ошибки:**

- `401` — пользователь не авторизован.
- `403` — инициатор не является командиром и не имеет прав ADMIN/MODERATOR.
- `404` — отряд не найден, у пользователя нет отряда или `newCommanderUserId` не найден/не состоит в отряде.
- `409` — попытка передать командование текущему командиру (нет изменения состояния).

---

## 5. Companies API (роты)

> Этап реализации: 4 (модуль "Роты").

### 5.1. GET /api/v1/companies

Список рот (UC-5).

**Query-параметры:**

- `isOpen` (optional, boolean) — если `true`, только `isOpen = true`.

**Response 200** — массив `CompanySummaryDto`.

### 5.2. GET /api/v1/companies/my

Рота текущего отряда пользователя (UC-5, UC-11).

**Response 200** — `CompanyDetailsDto`.

Ошибки: `401`, `404` (нет отряда или отряд не состоит в роте).

### 5.3. POST /api/v1/companies

Создать роту (UC-5).

**Request**

```json
{
  "description": "Объединение северных отрядов",
  "isOpen": true
}
```

Семантика:

- Генерируется имя `Рота [крупная река/водоём в РФ]`.
- Отряд командира-инициатора становится первым отрядом роты.

**Response 201** — `CompanyDetailsDto`.

Ошибки: `401`, `403` (не командир отряда), `404` (пользователь без отряда), `409` (отряд уже в роте).

### 5.4. POST /api/v1/companies/{companyId}/join

Вступление отряда в существующую роту (UC-5).

**Response 200** — `CompanyDetailsDto` после вступления.

Ошибки: `401`, `403` (не командир), `404` (рота не найдена или нет отряда), `409` (отряд уже в роте или рота закрыта).

### 5.5. PATCH /api/v1/companies/my

Обновление параметров роты (UC-11).

**Request (partial)**

```json
{
  "name": "Рота Север",
  "description": "Обновлённое описание роты",
  "isOpen": false
}
```

**Response 200** — обновлённый `CompanyDetailsDto`.

Ошибки: `401`, `403` (не командир отряда роты и не MODERATOR/ADMIN), `404`.

### 5.6. POST /api/v1/companies/my/leave

Выход отряда из роты (UC-11).

**Response 204** — без тела.

Семантика:

- Отряд удаляется из `Company.squads` и отвязывается от роты;
- Если в роте не осталось отрядов — рота удаляется.

Ошибки: `401`, `403` (не командир), `404` (нет отряда или он не в роте).

---

## 6. Orders API (приказы)

> Этап реализации: модуль "Приказы".

### 6.1. GET /api/v1/orders

Список приказов текущего отряда (UC-7).

**Query-параметры:**

- `activeOnly` (optional, boolean, default `false`) — если `true`, только `ACTIVE`.

**Response 200** — массив `OrderDto`.

Ошибки: `401`, `404` (пользователь не состоит в отряде).

### 6.2. POST /api/v1/orders

Создать приказ (UC-7, командир).

**Request**

```json
{
  "description": "Занять высоту на холме"
}
```

**Response 201** — `OrderDto`.

Ошибки: `401`, `403` (не командир отряда), `404` (нет отряда).

### 6.3. PATCH /api/v1/orders/{orderId}/status

Отметить приказ как выполненный (UC-7).

**Request**

```json
{
  "status": "COMPLETED"
}
```

**Response 200** — обновлённый `OrderDto`.

Ошибки: `401`, `403`, `404`, `409` (статус уже COMPLETED, если трактуем как конфликт).

---

## 7. Marker Types API

> Этап реализации: модуль "Тактические метки".

### 7.1. GET /api/v1/marker-types

Получить список активных типов меток (для UI) (UC-6, UC-12).

**Response 200** — массив `MarkerTypeDto`.

Ошибки: `401`.

---

## 8. Markers API (тактические метки)

> Этап реализации: модуль "Тактические метки".

### 8.1. GET /api/v1/markers

Получить метки, видимые текущему пользователю для карты (UC-6, UC-12).

**Query-параметры:**

- `includeExpired` (optional, boolean, default `false`).

**Семантика видимости:**

- Если пользователь в отряде:
  - все отрядные метки этого отряда;
  - ротные метки командиров других отрядов роты (если `companyId` совпадает).
- Если не в отряде — можно вернуть `[]`.

**Response 200** — массив `MarkerDto`.

Ошибки: `401`.

### 8.2. POST /api/v1/markers

Создать тактическую метку (UC-6, UC-12).

**Request**

```json
{
  "markerTypeKey": "ENEMY_SPOTTED",
  "lat": 59.123456,
  "lng": 30.123456,
  "description": "Контакт на холме",
  "sendToCompany": true
}
```

Поля:

- `markerTypeKey`: string — ключ типа метки (`ENEMY_SPOTTED`, `ATTACK`, `MOVE`, ...).
- `lat`, `lng`: number — координаты.
- `description`: string | null — описание; если null, использовать `defaultDescription`.
- `sendToCompany`: boolean | null — флаг «сделать метку ротной», учитывается только если тип позволяет и отряд состоит в роте.

**Семантика:**

- Находим `TacticalMarkerType` по `markerTypeKey`, проверяем `active` и `roleRestriction`.
- Если `sendToCompany = true` и `canSendToCompany = true` и отряд в роте — устанавливаем `companyId`.
- Применяем `uniquenessPolicy`:
  - `ONE_PER_USER` — удаляем/деактивируем активные метки такого типа от этого автора.
  - `ONE_PER_SQUAD` — удаляем/деактивируем активные метки такого типа этого отряда.
- Создаём `TacticalMarker` с `createdAt` и `expiresAt` (если задано).

**Response 201** — `MarkerDto`.

Ошибки: `401`, `403` (не в отряде или нарушен `roleRestriction`), `404` (тип метки не найден/неактивен), `409` (возможные коллизии политики уникальности, если считаем их ошибкой).

### 8.3. DELETE /api/v1/markers/{markerId}

Удалить метку (создатель или MODERATOR/ADMIN).

**Response 204** — без тела.

Ошибки: `401`, `403`, `404`.

---

## 9. GeoLocation API

> Этап реализации: модуль "Геопозиции".

### 9.1. POST /api/v1/geo/position

Обновить текущую геопозицию пользователя (UC-8).

**Request**

```json
{
  "lat": 59.123456,
  "lng": 30.123456,
  "mode": "AUTO"
}
```

Поля:

- `lat`, `lng`: number — координаты.
- `mode`: `"AUTO" | "MANUAL"` — текущий режим (для логирования/аналитики, необязательно хранить).

**Response 204** — без тела.

Семантика:

- Для каждого входящего запроса создаём новую запись `GeoLocation` для пользователя (`userId`, `lat`, `lng`, `timestamp = now`).

Ошибки: `401`, `400` (невалидные координаты).

### 9.2. GET /api/v1/geo/positions

Получить геопозиции игроков для карты (UC-8 + ТЗ).

**Response 200**

```json
[
  {
    "userId": 3,
    "lat": 59.123456,
    "lng": 30.123456,
    "timestamp": "2025-01-01T12:00:00Z"
  }
]
```

Семантика видимости:

- Пользователь видит:
  - всех членов своего отряда;
  - командиров отрядов, входящих в ту же роту.

Ошибки: `401`.

---

## 10. Chat API (WebSocket)

> Этап реализации: модуль "Чат/Realtime".

Чаты реализуются через WebSocket и внутренний Pub/Sub (Redis). Истории сообщений нет, только realtime (UC-13).

### 10.1. Endpoint

- URL: `/ws/chat`
- Протокол: WebSocket
- Аутентификация: JWT в query-параметре `token` или в заголовке при апгрейде.

Пример: `/ws/chat?token=<jwt>`.

### 10.2. Подключение и подписка

При установлении соединения:

1. Сервер валидирует JWT, определяет `userId`.
2. Определяет доступные каналы:
   - `GLOBAL` — всегда;
   - `SQUAD` — если пользователь в отряде;
   - `COMPANY` — если отряд в роте.
3. Подписывает соединение на внутренние каналы (например, `chat:global`, `chat:squad:{squadId}`, `chat:company:{companyId}`).

Сервер может отправить служебное сообщение:

```json
{
  "type": "CHANNELS_READY",
  "availableChannels": ["GLOBAL", "SQUAD", "COMPANY"]
}
```

### 10.3. Сообщения клиент → сервер

#### 10.3.1. Отправка сообщения в чат

```json
{
  "action": "SEND_MESSAGE",
  "channelType": "SQUAD",
  "text": "Двигаемся к точке сбора"
}
```

Поля:

- `action`: `"SEND_MESSAGE"`.
- `channelType`: `"GLOBAL" | "SQUAD" | "COMPANY"`.
- `text`: string — текст сообщения.

`channelId` клиент не указывает — сервер выводит его из контекста пользователя (текущий отряд/рота).

#### 10.3.2. Ping (опционально)

```json
{
  "action": "PING"
}
```

Сервер отвечает `{"type": "PONG"}`.

### 10.4. Сообщения сервер → клиент

#### 10.4.1. Чат-сообщение

```json
{
  "type": "CHAT_MESSAGE",
  "channelType": "COMPANY",
  "channelId": 2,
  "messageId": 1,
  "author": {
    "id": 1,
    "nickname": "Viking",
    "avatarIcon": "wolf_01"
  },
  "text": "Рота, готовность 5 минут",
  "sentAt": "2025-01-01T12:00:00Z"
}
```

#### 10.4.2. Ошибка

```json
{
  "type": "ERROR",
  "code": "FORBIDDEN",
  "message": "You are not allowed to send messages to this channel"
}
```

### 10.5. Особенности отсутствия истории

- При подключении **не** отдаются старые сообщения.
- Клиент видит только сообщения, поступившие после установления текущего соединения.
- При переподключении история не восстанавливается с сервера (можно хранить локально на клиенте).
- При выходе из отряда/роты подписки пересоздаются, и пользователь перестаёт получать сообщения соответствующих чатов.

---

## 11. Уведомления (общие замечания)

> Относится к модулю "Чат/Realtime" и общей событийной модели.

Многие UC предполагают отправку уведомлений через realtime‑каналы (создание/роспуск отряда, смена командира, новые приказы, статус приказов, новые метки и т.п.).

Архитектурно:

- Можно использовать отдельный WebSocket‑endpoint `/ws/events` или переиспользовать `/ws/chat` с другим `type` (`EVENT` vs `CHAT_MESSAGE`).
- Формат событий может быть унифицированным:

```json
{
  "type": "EVENT",
  "eventType": "SQUAD_MEMBER_JOINED",
  "payload": { ... }
}
```

Детальная спецификация событий может быть описана отдельным документом, когда будет реализован базовый функционал API.
