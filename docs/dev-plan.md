# План разработки Tactical App (MVP)

Документ описывает последовательность разработки backend-части приложения.

## Цели плана

- идти от простого к сложному;
- вводить технологии постепенно;
- после каждого этапа система должна компилироваться и запускаться;
- этапы должны соответствовать реальным коммитам.

## Технологический стек

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Redis
- WebSocket
- JWT

## Источники требований

- `tz_tactical_app.md`
- `spec-domain.md`
- `spec-api.md`
- `spec-ws.md`
- `spec-infra.md`
- `spec-redis.md`
- `spec-structure.md`
- `V1__init_schema.sql`

---

## Этап №1 Инфраструктура проекта (Spring Boot, структура модулей)

### Задачи

1. Создать Spring Boot проект на Java 21.
2. Подключить зависимости:
    - `spring-boot-starter-web`
    - `spring-boot-starter-security`
    - `spring-boot-starter-validation`
    - `spring-boot-starter-data-jpa`
    - `flyway`
    - `postgres driver`
    - `redis client`
3. Создать базовую структуру модулей:
    - `auth`
    - `users`
    - `squads`
    - `companies`
    - `geo`
    - `markers`
    - `marker_types`
    - `orders`
    - `realtime`
    - `common`
    - `jobs`
    - `config`
4. Добавить `application.yml`.
5. Настроить profiles:
    - `dev`
    - `test`
6. Подключить `/actuator/health`.
7. Настроить логирование.
8. Добавить `Clock` bean (UTC).

### Коммиты

- `init: create spring boot project (java 21)`
- `build: add dependencies (spring web, security, jpa, flyway, redis)`
- `infra: create base package structure`
- `infra: add application.yml and profiles (dev/test)`
- `infra: configure logging and UTC clock bean`
- `infra: enable actuator health endpoint`

---

## Этап №2 База данных и миграции (Flyway)

### Задачи

1. Подключить Flyway.
2. Перенести SQL схему в `V1__init_schema.sql`.
3. Настроить PostgreSQL datasource.
4. Проверить запуск миграции.
5. Создать JPA entities:
    - `User`
    - `Squad`
    - `SquadMember`
    - `Company`
    - `Marker`
    - `MarkerType`
    - `Order`
    - `UserGeoLocation`
6. Создать Spring Data repositories.
7. Настроить JPA auditing (`createdAt`, `updatedAt`).

### Коммиты

- `db: add flyway configuration`
- `db: add V1__init_schema migration`
- `db: configure postgres datasource`
- `domain: add JPA entities (users, squads, companies, markers, orders)`
- `domain: add repositories`
- `infra: enable JPA auditing`

---

## Этап №3 Общие контракты API (ErrorResponse, validation)

### Задачи

1. Реализовать `ErrorResponse`.
2. Создать `GlobalExceptionHandler`.
3. Добавить error codes из `spec-domain`.
4. Реализовать validation errors (`details.errors[]`).
5. Добавить request logging filter.
6. Добавить поддержку `X-Request-Id`.

### Коммиты

- `api: add ErrorResponse contract`
- `api: implement GlobalExceptionHandler`
- `api: add validation error mapping`
- `infra: add request logging filter`
- `infra: implement X-Request-Id support`

---

## Этап №4 Аутентификация и пользователи

### Задачи

1. Создать DTO:
    - `RegisterRequest`
    - `LoginRequest`
    - `AuthResponse`
    - `UserProfileResponse`
2. Реализовать endpoints:
    - `POST /auth/register`
    - `POST /auth/login`
3. Реализовать password hashing.
4. Реализовать endpoints:
    - `GET /users/me`
    - `PATCH /users/me`
    - `POST /users/me/alive`
    - `POST /users/me/dead`
5. Добавить `UserService`.

### Коммиты

- `auth: add register endpoint`
- `auth: add login endpoint`
- `auth: implement password hashing`
- `users: implement GET /users/me`
- `users: implement PATCH /users/me`
- `users: implement alive/dead endpoints`

---

## Этап №5 Безопасность (JWT, SecurityFilterChain)

### Задачи

1. Реализовать JWT util.
2. Использовать claims:
    - `sub`
    - `email`
    - `role`
    - `tokenVersion`
3. Реализовать `JwtAuthenticationFilter`.
4. Настроить `SecurityFilterChain`.
5. Закрыть защищённые endpoints.
6. Проверять `accountStatus`.

### Коммиты

- `security: add JWT util`
- `security: implement JwtAuthenticationFilter`
- `security: configure SecurityFilterChain`
- `security: protect REST endpoints`
- `security: add accountStatus validation`

---

## Этап №6 Отряды (Squads) — базовые операции

### Задачи

1. Создать DTO:
    - `CreateSquadRequest`
    - `JoinSquadRequest`
    - `SquadResponse`
2. Реализовать endpoints:
    - `POST /squads`
    - `GET /squads/my`
    - `POST /squads/my/join`
    - `POST /squads/my/leave`
3. Реализовать инварианты:
    - пользователь максимум в одном отряде;
    - вступление только в открытый отряд;
    - создатель отряда становится командиром.
4. Реализовать удаление пустого отряда.

### Коммиты

- `squads: add create squad endpoint`
- `squads: implement get my squad`
- `squads: implement join squad`
- `squads: implement leave squad`
- `squads: auto delete empty squad`

---

## Этап №7 Командирские операции отряда

### Задачи

1. Реализовать endpoints:
    - `POST /squads/my/kick`
    - `POST /squads/my/transfer-commander`
    - `POST /squads/my/disband`
2. Реализовать правила:
    - commander ACL;
    - передача командования;
    - роспуск отряда.

### Коммиты

- `squads: implement kick member`
- `squads: implement transfer commander`
- `squads: implement disband squad`
- `squads: add commander permission checks`

---

## Этап №8 Компании (Companies)

### Задачи

1. Реализовать endpoints:
    - `POST /companies`
    - `GET /companies/my`
    - `POST /companies/my/add-squad`
    - `POST /companies/my/remove-squad`
    - `POST /companies/my/disband`
2. Реализовать правила:
    - вступление отряда в компанию;
    - выход отряда из компании;
    - удаление пустой компании.

### Коммиты

- `companies: add create company endpoint`
- `companies: implement get my company`
- `companies: implement add squad to company`
- `companies: implement remove squad from company`
- `companies: implement disband company`

---

## Этап №9 Администрирование и типы меток

### Задачи

1. Реализовать endpoints:
    - `PATCH /admin/users/{userId}/account-status`
    - `POST /admin/marker-types`
    - `PATCH /admin/marker-types/{id}`
2. Добавить seed начальных типов меток.

### Коммиты

- `admin: implement user account-status management`
- `marker-types: implement admin CRUD`
- `marker-types: add initial seed data`

---

## Этап №10 Метки (Markers)

### Задачи

1. Реализовать endpoints:
    - `POST /markers`
    - `GET /markers`
    - `DELETE /markers/{id}`
2. Реализовать правила:
    - `roleRestriction`;
    - `sendToCompany`;
    - `expiresAt`;
    - `uniquenessPolicy = ONE_PER_USER`.

### Коммиты

- `markers: implement create marker`
- `markers: implement list markers`
- `markers: implement delete marker`
- `markers: enforce ONE_PER_USER policy`
- `markers: implement marker expiration logic`

---

## Этап №11 Приказы (Orders)

### Задачи

1. Реализовать endpoints:
    - `POST /orders`
    - `GET /orders`
    - `PATCH /orders/{id}/status`
2. Реализовать статусную модель:
    - `CREATED`
    - `IN_PROGRESS`
    - `COMPLETED`

### Коммиты

- `orders: implement create order`
- `orders: implement list orders`
- `orders: implement order status transition`
- `orders: enforce order state machine`

---

## Этап №12 Геолокация (Geo)

### Задачи

1. Реализовать endpoints:
    - `POST /geo/position`
    - `GET /geo/positions`
2. Реализовать правила:
    - append-only история;
    - visibility rules.

### Коммиты

- `geo: implement position update endpoint`
- `geo: implement get visible positions`
- `geo: add visibility rules`

---

## Этап №13 Realtime (WebSocket)

### Задачи

1. Реализовать endpoint:
    - `/ws/events`
2. Реализовать:
    - JWT handshake;
    - subscription model.
3. Начать эмитить события:
    - `MARKER_CREATED`
    - `MARKER_DELETED`
    - `ORDER_CREATED`
    - `ORDER_STATUS_CHANGED`

### Коммиты

- `ws: implement websocket endpoint`
- `ws: add JWT authentication`
- `ws: implement subscription model`
- `ws: emit marker events`
- `ws: emit order events`

---

## Этап №14 Redis (cache + tokenVersion)

### Задачи

1. Подключить Redis.
2. Реализовать `tokenVersion` storage.
3. Добавить cache для marker types.
4. Добавить cache последней геопозиции пользователя.

### Коммиты

- `redis: configure redis connection`
- `redis: implement tokenVersion storage`
- `redis: add marker type cache`
- `redis: add geo last position cache`

---

## Этап №15 Redis Event Bus

### Задачи

1. Реализовать Redis Pub/Sub.
2. Связать WebSocket events с Redis.
3. Поддержать несколько backend-инстансов.

### Коммиты

- `redis: implement pub/sub event bus`
- `realtime: publish domain events to redis`
- `realtime: subscribe instances to redis channels`

---

## Этап №16 Фоновые задачи

### Задачи

1. Реализовать marker expiration sweep.
2. Добавить Redis distributed lock.
3. Эмитить события удаления меток.

### Коммиты

- `jobs: implement marker expiration sweep`
- `redis: add distributed lock for jobs`
- `jobs: emit marker deleted events`

---

## Этап №17 Тестирование и стабилизация

### Задачи

1. Добавить integration tests для:
    - `auth`
    - `squads`
    - `companies`
    - `markers`
    - `orders`
    - `geo`
2. Добавить WebSocket tests.
3. Добавить Redis cache tests.
4. Добавить JWT invalidation tests.

### Коммиты

- `tests: add integration tests for auth`
- `tests: add integration tests for squads`
- `tests: add integration tests for markers`
- `tests: add websocket event tests`
- `tests: add redis cache tests`

---

## Порядок прохождения этапов

1. Инфраструктура
2. База данных
3. Общие API-контракты
4. Auth и Users
5. Security
6. Squads
7. Commander actions
8. Companies
9. Admin и Marker Types
10. Markers
11. Orders
12. Geo
13. WebSocket
14. Redis cache и tokenVersion
15. Redis event bus
16. Background jobs
17. Тестирование и стабилизация

---

## Принцип выполнения плана

После завершения каждого этапа должно быть выполнено следующее:

- проект компилируется;
- приложение запускается;
- миграции применяются корректно;
- изменения зафиксированы отдельными коммитами;
- следующий этап не требует отката архитектуры предыдущего.