# План реализации backend STN (согласован с ТЗ, REST/WS контрактами и спецификацией ошибок)

Дата актуализации: **2025-12-20**

## 1. Нормативная база (источник требований)

Этот план приведён в соответствие следующим документам (они считаются «источником истины» по контрактам):

- `tz_tactical_app.md` — утверждённое ТЗ (модель и бизнес-правила)
- `use-cases.md` — пользовательские сценарии UC-1…UC-13
- `backend-api-spec-from-use-cases.md` — публичный REST + WebSocket контракт
- `errors-spec.md` — единый формат ошибок (`ErrorResponse`, `ErrorCode`)
- `events-spec.md` — контракт realtime-событий поверх WebSocket
- `db-schema.md` + `V1__init_schema.sql` — базовая схема БД (как стартовая точка миграций)

> Примечание по БД: схема может расширяться в последующих миграциях (V2+), но **публичные REST/WS контракты** должны сохраняться.

---

## 2. Общие принципы реализации

### 2.1. Стек
- Java 21
- Spring Boot 3.x
- PostgreSQL
- Flyway (миграции БД)
- Spring Security (JWT)
- WebSocket (чаты + события)
- Redis Pub/Sub для маршрутизации realtime-сообщений (как указано в спецификации Chat API)

### 2.2. Версионирование API
- Базовый префикс REST API: **`/api/v1`**
- Все эндпоинты, кроме `/api/v1/auth/*`, требуют JWT в заголовке:
  - `Authorization: Bearer <jwt-token>`

### 2.3. Время
- В БД и в ответах API время хранится/отдаётся в UTC, ISO 8601 (`...Z`).
- В коде используется `TimeProvider` (из ТЗ): **`com.k44.STN.common.time.TimeProvider`**.

### 2.4. Ошибки
- Любая ошибка REST должна возвращаться в формате `ErrorResponse` из `errors-spec.md`.
- Базовые коды ошибок: `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR`.
- Поле `timestamp` **обязательно**.

### 2.5. Идентификаторы и типы
- Основные идентификаторы в API: `number` → на backend используем `Long` (BIGINT) и в JSON отдаём как число.
- UUID использовать только там, где это явно оправдано контрактом (в текущих спецификациях не требуется).

### 2.6. Тестирование (Definition of Done для каждого этапа)
- Минимум:
  - интеграционные тесты REST для публичных эндпоинтов этапа;
  - негативные тесты на ошибки (401/403/404/409/400) с проверкой `ErrorResponse`;
- Для WebSocket:
  - интеграционный тест подключения с JWT;
  - проверка доставки хотя бы одного `CHAT_MESSAGE` и одного `EVENT`.

---

## 3. Обзор этапов

0. **Core / Infrastructure** — каркас, БД (V1), время, ошибки, корреляция логов.
1. **Security & Users** — регистрация/логин, JWT, `/users/me`, статус ALIVE/DEAD и аватар.
2. **Squads** — отряды: создание/поиск/вступление/выход/управление.
3. **Companies** — роты: создание/поиск/вступление отрядом/выход, обновление параметров.
4. **Geo** — загрузка геопозиции и выдача видимых позиций.
5. **Tactical Markers** — справочник типов (чтение), создание/удаление меток, уникальность и lifetime.
6. **Orders** — создание приказов и отметка выполнения.
7. **Realtime & Chat** — WebSocket `/ws/events`, чаты + доменные события по контракту `events-spec.md`.
8. **Admin / Moderation** — операции ADMIN/MODERATOR (минимум в соответствии с references в `events-spec.md`, расширение по `admin-moderation-api.md`).

---

# Этап 0. Core / Infrastructure

## Цель
Поднять «скелет» приложения: запуск, Flyway V1, единые ошибки, время, correlationId, базовое логирование.

## 0.1. Создание проекта и зависимостей
Задачи:
- Maven-проект (Spring Boot 3.x, Java 21).
- Базовые зависимости:
  - `spring-boot-starter-webmvc`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security` (минимально, без бизнес-правил)
  - `org.postgresql:postgresql`(runtime)
  - `org.flywaydb:flyway-core`
  - `org.flywaydb:flyway-database-postgresql` (runtime, для поддержки Postgres)
  - `spring-boot-starter-test`
  - `spring-boot-starter-webmvc-test`
  - (при наличии security-тестов) `spring-security-test`
  - (опционально) `lombok`

Результат: приложение стартует, `mvn test` проходит.

## 0.2. Структура пакетов
Требование из ТЗ: `TimeProvider` находится в `com.k44.STN.common.time`.

Рекомендуемый каркас (без навязывания «микросервисности»):
```text
com.k44.STN
  common.time            (TimeProvider и связанные утилиты времени)
  core                   (ошибки, web-фильтры, конфиги, общие утилиты)
  auth                   (JWT, auth endpoints)
  users                  (профиль пользователя)
  squads                 (отряды)
  companies              (роты)
  geo                    (геолокации)
  markers                (типы меток и метки)
  orders                 (приказы)
  realtime               (WS, чат, доставка EVENT)
  admin                  (admin/moderation endpoints)
```

> Важно: главный класс Spring Boot разместить в `com.k44.STN` (или настроить `scanBasePackages="com.k44.STN"`), чтобы компоненты из `common.*` сканировались автоматически.

## 0.3. Время (`TimeProvider`)
Задачи:
- `com.k44.STN.common.time.TimeProvider`:
  - метод `Instant now()` (UTC).
- Реализация `SystemTimeProvider` как Spring Bean.
- Унифицировать сериализацию времени в DTO (ISO 8601, `Z`).

## 0.4. Flyway: V1__init_schema.sql (базовая схема)
Задачи:
- Сформировать V1, включающую (как минимум):
  - `avatar_icons`
  - `users`
  - `companies`
  - `squads`
  - `orders`
  - `tactical_marker_types`
  - `tactical_markers`
  - `user_geo_locations`
- Зафиксировать:
  - PK/FK, `ON DELETE` политики (как описано в `db-schema.md`);
  - индексы по «горячим» полям (`is_open`, `squad_id`, `company_id`, и т.п.);
  - ENUM/псевдо-enum стратегии (типизированные ENUM в Postgres или `varchar + check`).

Результат: при старте приложения накатывается V1, схема готова для дальнейших этапов.

## 0.5. Механизм ошибок (строго по `errors-spec.md`)
Задачи:
- `ErrorCode` (только базовые коды на текущем этапе):
  - `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR`.
- `ErrorResponse`:
  - `code: string` (обязательное)
  - `message: string` (обязательное)
  - `details: object | null` (опционально)
  - `timestamp: string` (обязательное, ISO 8601 UTC)
- `@ControllerAdvice`:
  - `MethodArgumentNotValidException`, `BindException`, `ConstraintViolationException` → HTTP 400 + `BAD_REQUEST` + `details.fieldErrors[]`
  - `HttpMessageNotReadableException` → HTTP 400 + `BAD_REQUEST`
  - `NoHandlerFoundException` (если включено) → HTTP 404 + `NOT_FOUND`
  - все прочие → HTTP 500 + `INTERNAL_ERROR` (логировать stack trace)
- Для Spring Security:
  - `AuthenticationEntryPoint` → HTTP 401 + `UNAUTHORIZED`
  - `AccessDeniedHandler` → HTTP 403 + `FORBIDDEN`

## 0.6. Correlation ID и логирование
Задачи:
- Фильтр (OncePerRequestFilter):
  - принимает `X-Request-Id` (если валиден) или генерирует новый;
  - помещает идентификатор в MDC под ключом `requestId`;
  - возвращает `X-Request-Id` в ответ.
  - гарантирует очистку MDC в `finally`.
- Логирование запросов:
  - формат логов: text (key=value);
  - метод, путь, статус, durationMs, requestId.
  - политика в prod: errors+slow
    - логировать запросы со статусом >= 400 и/или durationMs >= SLOW_THRESHOLD_MS;
    - по умолчанию SLOW_THRESHOLD_MS = 500 ms;
  - поля access log: `method`, `path`, `status`, `durationMs`, `requestId`.
  
Примечание: дублирование requestId в ErrorResponse (body) не требуется — достаточно заголовка X-Request-Id.
## 0.7. Dev-only debug endpoints (не часть публичного контракта)
Задачи (опционально, чтобы ускорять проверку инфраструктуры):
- Профиль `dev`:
  - `GET /internal/debug/time` → `{ "serverTime": "..." }`
  - `GET /internal/debug/error?type=validation|notfound|internal` → тест маппинга ошибок
- Интеграционные тесты на них (в `dev`/`test` профиле).

### Критерии завершения Этапа 0
- Приложение стартует, накатывает Flyway V1.
- `TimeProvider` доступен как бин.
- Любой контроллер возвращает ошибки строго по `errors-spec.md`.
- В логе есть requestId, а в ответе — `X-Request-Id`.

---

# Этап 1. Security & Users

## Цель
Реализовать UC-1 и UC-9: регистрация/логин, JWT, профиль пользователя, смена статуса ALIVE/DEAD и аватара.

## 1.1. Модель `User` и связь с `avatar_icons`
Требования:
- REST контракт (`backend-api-spec-from-use-cases.md`, разделы 2–3):
  - `email`, `nickname`, `systemRole`, `status (ALIVE/DEAD)`, `avatarIcon`, `squadId`.
- ТЗ:
  - при регистрации `systemRole = USER`, `status = ALIVE`, `avatarIcon` выбирается случайно из допустимого набора.

Задачи:
- `User` (JPA):
  - `email` (unique, not null)
  - `passwordHash` (not null)
  - `nickname` (not null)
  - `systemRole` (`USER|MODERATOR|ADMIN`)
  - `status` (`ALIVE|DEAD`) — игровой статус (UC-9)
  - `avatarIcon` → FK на `avatar_icons.key` (not null)
  - `squad` → nullable FK на `squads.id`

> Если текущая V1-схема использует иной enum/значения для `users.status`, ввести корректировку через миграцию V2, сохранив публичный контракт API.

## 1.2. JWT и SecurityConfig
Задачи:
- JWT:
  - subject = userId
  - claim `role` = `systemRole`
- Security:
  - `/api/v1/auth/*` → `permitAll`
  - всё остальное `/api/v1/**` → `authenticated`
- Настроить единый формат ошибок для 401/403 (через custom entry point/handler, см. Этап 0.5).

## 1.3. Auth API (строго по контракту)
Эндпоинты:
- `POST /api/v1/auth/register`
  - Request: `{ email, password, nickname }`
  - Response 200: `{ token, user }` (как в `spec-api.md`)

- `POST /api/v1/auth/login`
  - Request: `{ email, password }`
  - Response 200: `{ token, user: UserDto }`
  - Ошибки: 400, 401

## 1.4. Users API (строго по контракту)
Эндпоинты:
- `GET /api/v1/users/me` → `UserDto`
- `PATCH /api/v1/users/me/status` (UC-9) → `UserDto`
  - Request: `{ status: "ALIVE" | "DEAD" }`
- `PATCH /api/v1/users/me/avatar` (UC-9) → `UserDto`
  - Request: `{ avatarIcon: string }`
- `GET /api/v1/avatars` (опционально) → `{ items: string[] }`

## 1.5. Realtime события пользователя (опционально, но согласовано с `events-spec.md`)
Задачи:
- При смене статуса → `USER_STATUS_CHANGED`
- При смене аватара → `USER_AVATAR_CHANGED`
Каналы доставки: как описано в `events-spec.md` (SQUAD и/или COMPANY).

## 1.6. Тесты
Минимум:
- register → login → GET `/users/me`
- 401 без токена на `/users/me`
- PATCH status/avatar
- ошибки в формате `ErrorResponse` (400/401/409)

### Критерии завершения Этапа 1
- Контракты Auth/Users реализованы полностью.
- 401/403/400/409 возвращают `ErrorResponse` с `timestamp`.

---

# Этап 2. Squads

## Цель
Реализовать UC-2/UC-3/UC-4/UC-10 и REST контракт раздела 4 (`backend-api-spec-from-use-cases.md`).

## 2.1. Модель `Squad` и членство
Требования:
- Членство пользователя в отряде реализовано через `users.squad_id` (см. `db-schema.md`).

Задачи:
- `Squad`:
  - `name` (генерируется при создании)
  - `description` (nullable)
  - `isOpen` (default true)
  - `color` (из фиксированной палитры)
  - `company` (nullable)
  - `commander` (FK на `users.id`, not null)

## 2.2. Доменные правила
- Пользователь может состоять не более чем в одном отряде.
- Создатель отряда становится командиром.
- Только командир (или ADMIN/MODERATOR где указано контрактом) может:
  - обновлять параметры (`PATCH /squads/my`)
  - кикать (`POST /squads/my/members/{userId}/kick`)
  - передавать командование (`POST /squads/my/transfer-commander`)
  - распускать (`POST /squads/my/disband`)
- При выходе последнего участника отряд удаляется (UC-4).

## 2.3. REST API (строго по контракту)
Эндпоинты:
- `GET /api/v1/squads` (query `isOpen`)
- `POST /api/v1/squads` → 201 `SquadDetailsDto`
- `GET /api/v1/squads/my` → 200 `SquadDetailsDto` (404 если нет отряда)
- `POST /api/v1/squads/{squadId}/join` → 200 `SquadDetailsDto`
- `POST /api/v1/squads/my/leave` → 204
- `POST /api/v1/squads/my/disband` → 204
- `PATCH /api/v1/squads/my` → 200 `SquadDetailsDto`
- `POST /api/v1/squads/my/members/{userId}/kick` → 204
- `POST /api/v1/squads/my/transfer-commander` → 200 `SquadDetailsDto`
  - Request: `{ newCommanderUserId: number }`

## 2.4. Realtime события (строго по `events-spec.md`)
Задачи: генерируем события согласно events-spec.md (канонический контракт)

## 2.5. Тесты
Сценарии:
- Создание отряда → командир = создатель, user.squadId заполнен
- Вступление второго пользователя в открытый отряд
- Выход / распуск / кик / transfer commander
- Ошибки 403/404/409 в формате `ErrorResponse`

### Критерии завершения Этапа 2
- Полный lifecycle отрядов работает строго по контракту.
- События по отрядам генерируются по `events-spec.md`.

---

# Этап 3. Companies

## Цель
Реализовать UC-5/UC-11 и REST контракт раздела 5.

## 3.1. Модель `Company`
ТЗ: отдельной роли «командир роты» нет; управляют ротой командиры входящих отрядов.

Задачи:
- `Company`:
  - `name` (генерируется при создании)
  - `description`
  - `isOpen` (default true)
- Связь: `squads.company_id` (FK), один отряд максимум в одной роте.

## 3.2. Доменные правила
- Создать роту может только командир своего отряда.
- Вступить/выйти из роты может только командир отряда.
- Если после выхода отряда в роте не осталось отрядов — рота удаляется.

## 3.3. REST API (строго по контракту)
Эндпоинты:
- `GET /api/v1/companies` (query `isOpen`)
- `GET /api/v1/companies/my` → 200 `CompanyDetailsDto` (404 если нет отряда/роты)
- `POST /api/v1/companies` → 201 `CompanyDetailsDto`
- `POST /api/v1/companies/{companyId}/join` → 200 `CompanyDetailsDto`
- `PATCH /api/v1/companies/my` → 200 `CompanyDetailsDto`
- `POST /api/v1/companies/my/leave` → 204

## 3.4. Realtime события (строго по `events-spec.md`)
События:
- `COMPANY_CREATED`
- `COMPANY_UPDATED`
- `COMPANY_SQUAD_JOINED`
- `COMPANY_SQUAD_LEFT`
- `COMPANY_DISBANDED` (как минимум для админ-операции; опционально при авто-удалении)

## 3.5. Тесты
- Создание роты командиром
- Присоединение другого отряда к роте
- Обновление параметров роты
- Выход отряда и авто-удаление роты при пустоте
- Негативные 401/403/404/409

### Критерии завершения Этапа 3
- Роты работают поверх отрядов строго по контракту.
- События по ротам соответствуют `events-spec.md`.

---

# Этап 4. Geo

## Цель
Реализовать UC-8 и REST контракт раздела 9.

## 4.1. Модель GeoLocation
ТЗ допускает «последняя точка» или «история», но REST контракт описывает запись каждой точки.

Задачи:
- `GeoLocation` (таблица `user_geo_locations`):
  - `user_id`
  - `lat`, `lon`
  - `recorded_at`/`timestamp` (UTC)

## 4.2. REST API (строго по контракту)
- `POST /api/v1/geo/position` → 204
  - Request: `{ lat, lon, mode: "AUTO"|"MANUAL" }`
  - Семантика: **создаём новую запись** координат пользователя.
- `GET /api/v1/geo/positions` → 200 `GeoPositionDto[]`
  - Видимость:
    - все члены своего отряда;
    - командиры отрядов той же роты.

## 4.3. Тесты
- Запись геопозиции и выборка видимых позиций для отрядного состава
- Проверка правил видимости по роте

### Критерии завершения Этапа 4
- Геопозиции обновляются и выдаются по правилам видимости.

---

# Этап 5. Tactical Markers

## Цель
Реализовать UC-6/UC-12 и REST контракт разделов 7–8.

## 5.1. TacticalMarkerType
ТЗ фиксирует поля:
- `key`, `name`, `defaultDescription`, `icon`
- `defaultLifetimeSeconds` (nullable)
- `roleRestriction` ∈ {`ANY_MEMBER`, `COMMANDER_ONLY`}
- `canSendToCompany`
- `uniquenessPolicy` ∈ {`NONE`, `ONE_PER_USER`, `ONE_PER_SQUAD`}
- `category` (optional), `active`

Задачи:
- Чтение активных типов через `GET /api/v1/marker-types`.
- Инициализация seed-данных типов (миграция Vx) согласно ТЗ.

> Управление типами (CRUD, деактивация) — в Этапе 8 (ADMIN).

## 5.2. TacticalMarker
Задачи:
- `TacticalMarker`:
  - `markerTypeId` (FK)
  - `squadId` (FK, not null)
  - `companyId` (nullable, если sendToCompany)
  - `authorId` (nullable по БД, но в домене обычно известен)
  - `lat`, `lon`
  - `description`
  - `createdAt`
  - `expiresAt` (nullable)

## 5.3. REST API (строго по контракту)
- `GET /api/v1/marker-types` → `MarkerTypeDto[]`
- `GET /api/v1/markers?includeExpired=false` → `MarkerDto[]`
- `POST /api/v1/markers` → 201 `MarkerDto`
  - Request: `{ markerTypeKey, lat, lon, description?, sendToCompany? }`
  - Семантика:
    - проверка `active` и `roleRestriction`
    - `sendToCompany` учитывается только если `canSendToCompany=true` и отряд в роте
    - применение `uniquenessPolicy` (удаление/вытеснение активных меток)
    - установка `expiresAt` по `defaultLifetimeSeconds`
- `DELETE /api/v1/markers/{markerId}` → 204 (автор или ADMIN/MODERATOR)

## 5.4. Realtime события (строго по `events-spec.md`)
- `MARKER_CREATED`
- `MARKER_DELETED` (включая истечение lifetime и вытеснение уникальностью)

## 5.5. Тесты
- Создание/видимость меток отрядных и ротных
- Уникальность `ONE_PER_USER` и `ONE_PER_SQUAD`
- Истечение `expiresAt` (минимум на уровне выборки includeExpired=false)
- 401/403/404/409 по контракту

### Критерии завершения Этапа 5
- Метки работают по правилам ТЗ и контрактам REST/WS.

---

# Этап 6. Orders

## Цель
Реализовать UC-7 и REST контракт раздела 6.

## 6.1. Модель `Order`
Требования:
- Статусы: `ACTIVE`, `COMPLETED`
- `completedAt` выставляется при переводе в `COMPLETED`

## 6.2. REST API (строго по контракту)
- `GET /api/v1/orders?activeOnly=false` → `OrderDto[]` (404 если пользователь без отряда)
- `POST /api/v1/orders` → 201 `OrderDto` (только командир отряда)
- `PATCH /api/v1/orders/{orderId}/status` → 200 `OrderDto`
  - Request: `{ status: "COMPLETED" }`
  - Доступ: командир отряда или ADMIN/MODERATOR

## 6.3. Realtime события
- `ORDER_CREATED`
- `ORDER_STATUS_CHANGED`

## 6.4. Тесты
- Создание приказа командиром, видимость членам отряда
- Завершение приказа командиром (и негативные проверки)
- Ошибки 401/403/404/409

### Критерии завершения Этапа 6
- Приказы реализованы по контракту, события приходят по WS.

---

# Этап 7. Realtime & Chat

## Цель
Реализовать UC-13 и доставку доменных событий по `events-spec.md` через один WebSocket endpoint. WS errors использовать схему ErrorResponse из errors-spec.md (timestamp обязателен, details опционален)

## 7.1. WebSocket endpoint и аутентификация (строго по контракту)
- Endpoint: **`/ws/events`**
- Аутентификация: JWT
  - в query `token` (предпочтительно, как в спецификации) или заголовке upgrade

## 7.2. Каналы и правила доступа
Доступные каналы (как в спецификации):
- `GLOBAL` — любой аутентифицированный пользователь
- `SQUAD` — если пользователь в отряде
- `COMPANY` — если отряд пользователя в роте

Служебное сообщение после подключения:
- `CHANNELS_READY` с `availableChannels`.

## 7.3. Сообщения чата (строго по контракту)
Клиент → сервер:
```json
{ "action": "SEND_MESSAGE", "channelType": "SQUAD", "text": "..." }
```

Сервер → клиент:
- `CHAT_MESSAGE` (см. `backend-api-spec-from-use-cases.md`, раздел 1.10)

## 7.4. События домена
Сервер → клиент:
- `EVENT` в «конверте» из `events-spec.md`:
  - `type=EVENT`, `eventType`, `channel`, `channelId`, `payload`

Маршрутизация событий:
- `SQUAD` → все участники squadId
- `COMPANY` → все участники companyId
- `USER` → конкретный userId

## 7.5. Внутренний Pub/Sub (Redis) для WS
Требование из спецификации: внутренний Pub/Sub (Redis).
Задачи:
- Подключить Redis.
- Ввести внутренние топики (пример):
  - `chat:global`
  - `chat:squad:{squadId}`
  - `chat:company:{companyId}`
  - `events:user:{userId}`, `events:squad:{squadId}`, `events:company:{companyId}`
- WS gateway подписывает соединение на нужные топики и транслирует в сокет.

## 7.6. Тесты
- Подключение к `/ws/events` с JWT
- Отправка `SEND_MESSAGE` и получение `CHAT_MESSAGE`
- Доставка одного `EVENT` (например `ORDER_CREATED`) в нужный канал

### Критерии завершения Этапа 7
- WS работает как единая точка для чатов и событий, соответствует контрактам.

---

# Этап 8. Admin / Moderation

## Цель
Реализовать минимально необходимые операции ADMIN/MODERATOR, которые:
1) согласуются с references в `events-spec.md`,  
2) не ломают публичные контракты базового API.

> Документ `admin-moderation-api.md` помечен как draft. На этом этапе реализуем минимум, а затем фиксируем контракт админки отдельным согласованием.

## 8.1. Безопасность
- Роли:
  - `MODERATOR`, `ADMIN`
- Ограничения доступа:
  - `/api/v1/admin/**` только для `MODERATOR|ADMIN`
- Ошибки 401/403 в формате `ErrorResponse`.

## 8.2. Минимальный набор эндпоинтов (по ссылкам из `events-spec.md`)
Squads:
- `PATCH /api/v1/admin/squads/{squadId}` → принудительное обновление (генерирует `SQUAD_UPDATED`)
- `POST /api/v1/admin/squads/{squadId}/members/{userId}/kick` → кик (генерирует `SQUAD_MEMBER_KICKED` + `SQUAD_MEMBER_KICKED_SELF`)
- `POST /api/v1/admin/squads/{squadId}/disband` → роспуск (генерирует `SQUAD_DISBANDED`)

Companies:
- `PATCH /api/v1/admin/companies/{companyId}` → принудительное обновление (генерирует `COMPANY_UPDATED`)
- `POST /api/v1/admin/companies/{companyId}/disband` → принудительный роспуск (генерирует `COMPANY_DISBANDED`)

Markers:
- `DELETE /api/v1/admin/markers/{markerId}` (или эквивалентная операция) → `MARKER_DELETED`

Users (минимум):
- (опционально) `PATCH /api/v1/admin/users/{userId}/status` → принудительная смена ALIVE/DEAD с событием `USER_STATUS_CHANGED`

## 8.3. Управление типами меток (ADMIN)
Из ТЗ: типы меток «конфигурируемы ADMIN».
Задачи:
- CRUD или минимум:
  - активация/деактивация типа,
  - изменение параметров (`roleRestriction`, `canSendToCompany`, `uniquenessPolicy`, `defaultLifetimeSeconds`).
- Любые изменения типов должны быть безопасны для уже созданных меток.

## 8.4. Тесты
- 403 для обычного пользователя на `/api/v1/admin/**`
- Успешные операции MODERATOR/ADMIN
- Проверка генерации соответствующих `EVENT` (по мере покрытия)

### Критерии завершения Этапа 8
- Админские операции доступны только нужным ролям и транслируются в WS события.
- Базовые игровые сценарии не требуют админки, но админка позволяет «чинить состояние».

---

## 4. Итоговый результат проекта (после Этапа 8)
Backend покрывает все ключевые сценарии из ТЗ и `use-cases.md`:
- регистрация/логин, профиль, статус и аватар;
- отряды и роты;
- приказы;
- тактические метки и типы;
- геопозиции;
- WebSocket чаты и события;
- минимальная админка/модерация.

