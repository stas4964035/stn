# Realtime Events Specification (events-spec.md)

Этот документ дополняет основные файлы спецификации backend API
(`backend-api-spec-from-use-cases.md`, `admin-moderation-api.md`) и описывает
формат и набор realtime-событий, которые сервер отправляет клиентам через WebSocket.

Документ опирается на ТЗ и use-cases проекта и фиксирует контракт для событий,
не относящихся к чат-сообщениям (чаты описаны отдельно в разделе Chat API).

---

## 1. Транспорт и общий формат события

> Относится к модулю "Чат/Realtime" и общей событийной модели.

### 1.1. Транспорт

- WebSocket endpoint: `/ws/chat` (в соответствии с разделами 10 "Chat API" и 11 "Уведомления" в `backend-api-spec-from-use-cases.md`; вариант с отдельным `/ws/events` на текущем этапе не используется, но может быть добавлен позже).
- Аутентификация: JWT (как описано в основной спецификации).
- После установления соединения клиент может получать два типа сообщений:
  - `CHAT_MESSAGE` — сообщения чатов (GLOBAL / SQUAD / COMPANY);
  - `EVENT` — системные события домена (отряды, роты, приказы, метки и т.п.).

События отправляются в те же WebSocket-соединения, что и чаты, чтобы не плодить
лишние подключения.

### 1.2. Общий формат сообщения-события

Любое событие имеет следующий «конверт»:

```json
{
  "type": "EVENT",
  "eventType": "SQUAD_MEMBER_JOINED",
  "channel": "SQUAD",
  "channelId": 10,
  "payload": { /* event-specific */ }
}
```

Поля:

- `type`: всегда `"EVENT"` для событий (чтобы отличать от `CHAT_MESSAGE`).
- `eventType`: строковый код события (см. раздел 2–5).
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
- `payload`: объект с данными конкретного события (формат ниже).

Клиент должен ориентироваться в первую очередь на `eventType` и `channel`, а поля
`payload` интерпретировать в соответствии с таблицей событий ниже.

---

## 2. События по отрядам (Squad)

> Этап реализации: после модуля "Отряды" (Squads).

### 2.1. SQUAD_MEMBER_JOINED

- **Когда генерируется:**
  - после успешного выполнения `POST /api/v1/squads/{squadId}/join`;
  - после создания отряда (`POST /api/v1/squads`) для добавления самого создателя.
- **Канал:** `SQUAD`
- **channelId:** `squadId` отряда.

**Payload:**

```json
{
  "squadId": 10,
  "user": {
    "id": 5,
    "nickname": "Fox",
    "status": "ALIVE",
    "avatarIcon": "fox_02"
  }
}
```

---

### 2.2. SQUAD_MEMBER_LEFT

- **Когда генерируется:**
  - после успешного `POST /api/v1/squads/my/leave`, если пользователь — не командир;
  - при выходе командира, если отряд не распускается, а передаёт командование дальше.
- **Канал:** `SQUAD`
- **channelId:** `squadId` отряда, который пользователь покинул.

**Payload:**

```json
{
  "squadId": 10,
  "userId": 5
}
```

---

### 2.3. SQUAD_MEMBER_KICKED

- **Когда генерируется:**
  - после успешного `POST /api/v1/squads/my/members/{userId}/kick` (командиром);
  - после успешного `POST /api/v1/admin/squads/{squadId}/members/{userId}/kick` (ADMIN/MODERATOR).
- **Канал:** `SQUAD`
- **channelId:** `squadId`.

Дополнительно отдельное событие для самого кикнутого пользователя
может быть отправлено по каналу `USER` (см. ниже).

**Payload:**

```json
{
  "squadId": 10,
  "userId": 5
}
```

---

### 2.4. SQUAD_MEMBER_KICKED_SELF

- **Когда генерируется:**
  - параллельно с `SQUAD_MEMBER_KICKED` для самого кикнутого пользователя.
- **Канал:** `USER`
- **channelId:** `userId` кикнутого пользователя.

**Payload:**

```json
{
  "squadId": 10
}
```

Клиент может использовать это событие, чтобы:
- сбросить локальное состояние «я состою в отряде»;
- вернуть пользователя на экран выбора отряда.

---

### 2.5. SQUAD_UPDATED

- **Когда генерируется:**
  - после `PATCH /api/v1/squads/my`;
  - после `PATCH /api/v1/admin/squads/{squadId}` (принудительные изменения).
- **Канал:** `SQUAD`
- **channelId:** `squadId`.

**Payload:**

```json
{
  "squad": {
    "id": 10,
    "name": "Отряд Север",
    "description": "Обновлённое описание",
    "isOpen": false,
    "color": "#00FF00",
    "companyId": 3,
    "commanderId": 1
  }
}
```

Клиент может обновить локальный кеш данных отряда (название, описание, цвет и т.п.).

---

### 2.6. COMMANDER_CHANGED

- **Когда генерируется:**
  - после `POST /api/v1/squads/my/transfer-commander`;
  - при выходе командира из отряда, если система автоматически назначает нового.
- **Канал:** `SQUAD`
- **channelId:** `squadId`.

**Payload:**

```json
{
  "squadId": 10,
  "oldCommanderId": 1,
  "newCommanderId": 5
}
```

Клиент может, например, обновить отображение значка командира в списке участников.

---

### 2.7. SQUAD_DISBANDED

- **Когда генерируется:**
  - после успешного `POST /api/v1/squads/my/disband`;
  - после `POST /api/v1/admin/squads/{squadId}/disband`.
- **Канал:** `SQUAD`
- **channelId:** `squadId` (подписки на этот канал после события теряют смысл).

Дополнительно может дублироваться по каналу `USER` для каждого участника.

**Payload:**

```json
{
  "squadId": 10
}
```

Клиент должен:
- очистить локальное состояние отряда;
- вернуть пользователя на экран выбора/создания отряда.

---

## 3. События по ротам (Company)

> Этап реализации: после модуля "Роты" (Companies).

### 3.1. COMPANY_CREATED

- **Когда генерируется:**
  - после `POST /api/v1/companies` (командир создаёт роту для своего отряда).
- **Канал:** `SQUAD`
- **channelId:** `squadId` отряда создателя.

**Payload:**

```json
{
  "company": {
    "id": 3,
    "name": "Рота Волга",
    "description": "Объединение отрядов",
    "isOpen": true
  }
}
```

---

### 3.2. COMPANY_UPDATED

- **Когда генерируется:**
  - после `PATCH /api/v1/companies/my`;
  - после `PATCH /api/v1/admin/companies/{companyId}`.
- **Канал:** `COMPANY`
- **channelId:** `companyId`.

**Payload:**

```json
{
  "company": {
    "id": 3,
    "name": "Рота Север",
    "description": "Обновлённое описание",
    "isOpen": false
  }
}
```

---

### 3.3. COMPANY_SQUAD_JOINED

- **Когда генерируется:**
  - после успешного `POST /api/v1/companies/{companyId}/join` (вступление отряда).
- **Канал:** `COMPANY`
- **channelId:** `companyId`.

**Payload:**

```json
{
  "companyId": 3,
  "squad": {
    "id": 10,
    "name": "Отряд Волк",
    "color": "#FF0000",
    "commanderId": 1,
    "membersCount": 5
  }
}
```

---

### 3.4. COMPANY_SQUAD_LEFT

- **Когда генерируется:**
  - после `POST /api/v1/companies/my/leave` (выход отряда);
  - после принудительного удаления отряда из роты через админ-операцию.
- **Канал:** `COMPANY`
- **channelId:** `companyId`.

**Payload:**

```json
{
  "companyId": 3,
  "squadId": 10
}
```

---

### 3.5. COMPANY_DISBANDED

- **Когда генерируется:**
  - после `POST /api/v1/admin/companies/{companyId}/disband`;
  - опционально — если последняя рота распущена пользователем (в сценариях, где это возможно).
- **Канал:** `COMPANY`
- **channelId:** `companyId`.

**Payload:**

```json
{
  "companyId": 3
}
```

Участники отрядов, входивших в роту, должны перестать считать себя частью роты
и удалить ротный чат/ротные метки из UI.

---

## 4. События по приказам (Orders)

> Этап реализации: после модуля "Приказы" (Orders).

### 4.1. ORDER_CREATED

- **Когда генерируется:**
  - после успешного `POST /api/v1/orders` (командир создаёт приказ).
- **Канал:** `SQUAD`
- **channelId:** `squadId` отряда, к которому относится приказ.

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

### 4.2. ORDER_STATUS_CHANGED

- **Когда генерируется:**
  - после `PATCH /api/v1/orders/{orderId}/status` (командиром или ADMIN/MODERATOR).
- **Канал:** `SQUAD`
- **channelId:** `squadId` приказа.

**Payload:**

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

## 5. События по тактическим меткам (Markers)

> Этап реализации: после модуля "Тактические метки" (Markers).

### 5.1. MARKER_CREATED

- **Когда генерируется:**
  - после `POST /api/v1/markers` (любым участником или командиром, в зависимости от типа метки).
- **Канал:**
  - для чисто отрядных меток — `SQUAD` с `channelId = squadId`;
  - для меток с `sendToCompany = true` — событие может дублироваться:
    - в `SQUAD` (отряд-автор);
    - и в `COMPANY` (все отряды роты), если метка действительно ротная.

`marker` в payload — это тот же `MarkerDto`, который возвращают REST эндпоинты `/api/v1/markers`.

**Payload:**

```json
{
  "marker": {
    "id": 500,
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

### 5.2. MARKER_DELETED

- **Когда генерируется:**
  - после `DELETE /api/v1/markers/{markerId}` (автором или ADMIN/MODERATOR);
  - при автоматическом истечении `expiresAt` (для типов меток с lifetime);
  - при применении политики уникальности (`uniquenessPolicy`), когда новая метка
    того же типа вытесняет старую.

- **Канал:**
  - `SQUAD` — для отрядных меток;
  - `COMPANY` — для ротных меток.

**Payload:**

```json
{
  "markerId": 500,
  "squadId": 10,
  "companyId": 3
}
```

Клиент должен удалить метку с карты.

---

## 6. События по пользователю (статус/аватар)

> Этап реализации: после модулей "Security & Users" и "Профиль пользователя".

Эти события не строго обязательны, но могут использоваться для более «живого» UI,
чтобы не опрашивать REST при каждом изменении.

### 6.1. USER_STATUS_CHANGED

- **Когда генерируется:**
  - после `PATCH /api/v1/users/me/status` (смена ALIVE/DEAD пользователем);
  - опционально — после модераторской операции, если она будет добавлена.
- **Канал:**
  - `SQUAD` — для текущего отряда пользователя (если есть);
  - опционально — `COMPANY`, если пользователь — командир и важен для ротного видимого состояния.

**Payload:**

```json
{
  "userId": 5,
  "status": "DEAD"
}
```

---

### 6.2. USER_AVATAR_CHANGED

- **Когда генерируется:**
  - после `PATCH /api/v1/users/me/avatar`.
- **Канал:**
  - `SQUAD` — для текущего отряда пользователя;
  - `COMPANY` — если пользователь — командир или, в целом, логика позволяет.

**Payload:**

```json
{
  "userId": 5,
  "avatarIcon": "fox_02"
}
```

---

## 7. Замечания по развитию спецификации

1. Набор `eventType`, описанный выше, покрывает основные сценарии из ТЗ и use-cases:
   - изменение состава и параметров отрядов/рот;
   - приказы;
   - тактические метки;
   - базовые изменения статуса/аватара.
2. Для первых релизов (после реализации модулей Users/Squads/Companies/Orders/Markers и базового WebSocket-слоя) достаточно минимального подмножества событий, сгруппированного по модулям:
   - модуль "Отряды" (Squads): `SQUAD_MEMBER_JOINED`, `SQUAD_MEMBER_LEFT`, `COMMANDER_CHANGED`, `SQUAD_DISBANDED`;
   - модуль "Роты" (Companies): `COMPANY_SQUAD_JOINED`, `COMPANY_SQUAD_LEFT`, `COMPANY_DISBANDED`;
   - модуль "Приказы" (Orders): `ORDER_CREATED`, `ORDER_STATUS_CHANGED`;
   - модуль "Тактические метки" (Markers): `MARKER_CREATED`, `MARKER_DELETED`.
3. При добавлении новых бизнес-функций рекомендуется:
   - расширять этот документ новыми `eventType`, привязанными к соответствующим модулям/этапам;
   - сохранять общий формат `EVENT` и принцип каналов `USER/SQUAD/COMPANY/GLOBAL`.
На стороне реализации перечень событий может быть реализован как enum/константы,
с централизованной точкой генерации событий из доменных сервисов.