# Realtime Events Specification (events-spec.md)

Этот документ дополняет основные файлы спецификации backend API
(`backend-api-spec-from-use-cases.md`, `admin-moderation-api.md`) и описывает
формат и **минимальный набор realtime-событий**, которые сервер отправляет клиентам через WebSocket.

Документ опирается на утверждённое ТЗ (раздел 4.8 "Уведомления и события") и фиксирует
контракт для событий, не относящихся к чат-сообщениям (чаты описаны отдельно в разделе Chat API).

---

## 1. Транспорт и общий формат события

### 1.1. Транспорт

- WebSocket endpoint: `/ws/chat`.
- Аутентификация: JWT (как описано в основной спецификации).
- После установления соединения клиент может получать два типа сообщений:
  - `CHAT_MESSAGE` — сообщения чатов (GLOBAL / SQUAD / COMPANY);
  - `EVENT` — системные события домена (отряды, роты, приказы, метки и т.п.).

События отправляются в те же WebSocket-соединения, что и чаты.

### 1.2. Общий формат сообщения-события

Любое событие имеет следующий «конверт»:

```json
{
  "type": "EVENT",
  "eventType": "JOINED_SQUAD",
  "channel": "SQUAD",
  "channelId": 10,
  "payload": { /* event-specific */ }
}
```

Поля:

- `type`: всегда `"EVENT"`.
- `eventType`: строковый код события (см. разделы 2–5).
- `channel`: область доставки события:
  - `"USER"` — событие для одного конкретного пользователя;
  - `"SQUAD"` — для всех участников отряда;
  - `"COMPANY"` — для всех участников ротных отрядов;
  - `"GLOBAL"` — для всех авторизованных пользователей (используется редко).
- `channelId`:
  - для `USER` — `userId` получателя;
  - для `SQUAD` — `squadId`;
  - для `COMPANY` — `companyId`;
  - для `GLOBAL` — `null`.
- `payload`: объект с данными конкретного события.

---

## 2. События уровня пользователя / отряда (минимальный набор из ТЗ)

Минимальный набор (строго по ТЗ):
- `SQUAD_CREATED`
- `BECAME_COMMANDER`
- `JOINED_SQUAD`
- `LEFT_SQUAD`
- `KICKED_FROM_SQUAD`
- `SQUAD_DISBANDED`

### 2.1. SQUAD_CREATED

- **Когда генерируется:** после успешного `POST /api/v1/squads`.
- **Канал:** `SQUAD`
- **channelId:** `squadId` созданного отряда.
- **Дублирование по USER:** не требуется.

**Payload (минимум):**
```json
{
  "squadId": 10
}
```

---

### 2.2. BECAME_COMMANDER

- **Когда генерируется:**
  - после успешного `POST /api/v1/squads/my/transfer-commander`;
  - при выходе текущего командира из отряда, если система автоматически назначает нового командира.
- **Доставка:** в оба канала:
  - `USER` — новому командиру;
  - `SQUAD` — всем участникам отряда.

**Вариант A (USER):**
- channel = `USER`, channelId = `newCommanderUserId`

Payload (минимум):
```json
{
  "squadId": 10
}
```

**Вариант B (SQUAD):**
- channel = `SQUAD`, channelId = `squadId`

Payload:
```json
{
  "squadId": 10,
  "oldCommanderId": 1,
  "newCommanderId": 5
}
```

---

### 2.3. JOINED_SQUAD

- **Когда генерируется:** после успешного `POST /api/v1/squads/{squadId}/join`.
- **Канал:** `SQUAD`
- **channelId:** `squadId`.

**Payload (минимум):**
```json
{
  "squadId": 10,
  "userId": 5
}
```

---

### 2.4. LEFT_SQUAD

- **Когда генерируется:** после успешного `POST /api/v1/squads/my/leave`.
- **Канал:** `SQUAD`
- **channelId:** `squadId`, который пользователь покинул.

**Payload (минимум):**
```json
{
  "squadId": 10,
  "userId": 5
}
```

---

### 2.5. KICKED_FROM_SQUAD

- **Когда генерируется:**
  - после успешного `POST /api/v1/squads/my/members/{userId}/kick`;
  - после админ/модераторского кика (если реализованы admin endpoints).
- **Доставка:** двойная:
  - `SQUAD` — всем участникам отряда (обновить состав);
  - `USER` — кикнутому пользователю (сбросить локальное состояние).

**Вариант A (SQUAD):**
- channel = `SQUAD`, channelId = `squadId`
```json
{
  "squadId": 10,
  "userId": 5
}
```

**Вариант B (USER):**
- channel = `USER`, channelId = `userId`
```json
{
  "squadId": 10
}
```

---

### 2.6. SQUAD_DISBANDED

- **Когда генерируется:**
  - после `POST /api/v1/squads/my/disband`;
  - при авто-удалении отряда, если он стал пустым.
- **Канал:** `SQUAD`
- **channelId:** `squadId`.

**Payload (минимум):**
```json
{
  "squadId": 10
}
```

---

## 3. События уровня роты (Company) (минимальный набор из ТЗ)

Минимальный набор (строго по ТЗ):
- `SQUAD_JOINED_COMPANY`
- `SQUAD_LEFT_COMPANY`
- `COMPANY_DISBANDED`

### 3.1. SQUAD_JOINED_COMPANY

- **Когда генерируется:**
  - после `POST /api/v1/companies` (создание роты включает отряд-инициатор в роту);
  - после `POST /api/v1/companies/{companyId}/join`.
- **Доставка:** в оба канала:
  - `COMPANY` — всем участникам роты;
  - `SQUAD` — отряду, который вошёл.

**Вариант A (COMPANY):**
- channel = `COMPANY`, channelId = `companyId`
```json
{
  "companyId": 3,
  "squadId": 10
}
```

**Вариант B (SQUAD):**
- channel = `SQUAD`, channelId = `squadId`
```json
{
  "companyId": 3,
  "squadId": 10
}
```

---

### 3.2. SQUAD_LEFT_COMPANY

- **Когда генерируется:** после `POST /api/v1/companies/my/leave`.
- **Доставка:** в оба канала:
  - `COMPANY` — всем участникам роты;
  - `SQUAD` — отряду, который вышел.

**Вариант A (COMPANY):**
- channel = `COMPANY`, channelId = `companyId`
```json
{
  "companyId": 3,
  "squadId": 10
}
```

**Вариант B (SQUAD):**
- channel = `SQUAD`, channelId = `squadId`
```json
{
  "companyId": 3,
  "squadId": 10
}
```

---

### 3.3. COMPANY_DISBANDED

- **Когда генерируется:**
  - при авто-удалении роты, если она стала пустой;
  - при принудительном роспуске (ADMIN/MODERATOR), если реализован соответствующий эндпоинт.
- **Канал:** `COMPANY`
- **channelId:** `companyId`.

**Payload (минимум):**
```json
{
  "companyId": 3
}
```

---

## 4. События по тактическим меткам (Markers) (минимальный набор из ТЗ)

Минимальный набор (строго по ТЗ):
- `MARKER_CREATED`
- `MARKER_DELETED` (включая автоматическое истечение)

### 4.1. MARKER_CREATED

- **Когда генерируется:** после `POST /api/v1/markers`.
- **Доставка:**
  - если `sendToCompany = true` и метка реально стала ротной — событие отправляется в `COMPANY`;
  - иначе — событие отправляется в `SQUAD`.

`marker` в payload — это тот же `MarkerDto`, который возвращают REST эндпоинты `/api/v1/markers`.

**Вариант A (SQUAD, sendToCompany != true):**
- channel = `SQUAD`, channelId = `squadId`
```json
{
  "marker": {
    "id": 500,
    "markerTypeId": 1,
    "markerTypeKey": "ENEMY_SPOTTED",
    "squadId": 10,
    "companyId": null,
    "authorId": 5,
    "lat": 59.123456,
    "lng": 30.123456,
    "description": "Контакт на холме",
    "createdAt": "2025-01-01T12:00:00Z",
    "expiresAt": "2025-01-01T12:10:00Z"
  }
}
```

**Вариант B (COMPANY, sendToCompany = true):**
- channel = `COMPANY`, channelId = `companyId`
```json
{
  "marker": {
    "id": 501,
    "markerTypeId": 1,
    "markerTypeKey": "ENEMY_SPOTTED",
    "squadId": 10,
    "companyId": 3,
    "authorId": 5,
    "lat": 59.123456,
    "lng": 30.123456,
    "description": "Контакт на холме",
    "createdAt": "2025-01-01T12:00:00Z",
    "expiresAt": "2025-01-01T12:10:00Z"
  }
}
```

---

### 4.2. MARKER_DELETED

- **Когда генерируется:**
  - после `DELETE /api/v1/markers/{markerId}`;
  - при автоматическом истечении `expiresAt`;
  - при вытеснении по `uniquenessPolicy`.
- **Канал:** определяется тем, куда была адресована метка:
  - `SQUAD` — для отрядных меток;
  - `COMPANY` — для ротных меток.

**Payload (минимум):**
```json
{
  "markerId": 500,
  "squadId": 10,
  "companyId": 3
}
```

---

## 5. События по приказам (Orders) (минимальный набор из ТЗ)

Минимальный набор (строго по ТЗ):
- `ORDER_CREATED`
- `ORDER_STATUS_CHANGED`

### 5.1. ORDER_CREATED

- **Когда генерируется:** после `POST /api/v1/orders`.
- **Канал:** `SQUAD`
- **channelId:** `squadId`.

`order` в payload — это тот же `OrderDto`, который возвращают REST эндпоинты `/api/v1/orders`.

**Payload:**
```json
{
  "order": {
    "id": 100,
    "squadId": 10,
    "authorId": 1,
    "description": "Занять высоту",
    "status": "ACTIVE",
    "createdAt": "2025-01-01T12:00:00Z",
    "completedAt": null
  }
}
```

---

### 5.2. ORDER_STATUS_CHANGED

- **Когда генерируется:** после `PATCH /api/v1/orders/{orderId}/status`.
- **Канал:** `SQUAD`
- **channelId:** `squadId` приказа.

**Payload (минимум):**
```json
{
  "orderId": 100,
  "squadId": 10,
  "oldStatus": "ACTIVE",
  "newStatus": "COMPLETED",
  "completedAt": "2025-01-01T12:05:00Z"
}
```

---

## 6. Замечания по развитию спецификации

1. Набор `eventType`, описанный выше, **строго соответствует минимальному набору событий из ТЗ** (раздел 4.8).
2. Любые новые `eventType` добавляются только через обновление ТЗ или отдельное согласование контракта.
3. Общий формат `EVENT` и принцип каналов `USER/SQUAD/COMPANY/GLOBAL` сохраняются.
4. Админские действия (ADMIN/MODERATOR) должны транслироваться теми же `eventType`, что и пользовательские,
   если это не меняет семантику события.
