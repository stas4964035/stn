# Redis Specification

Redis используется как высокоскоростной слой временных данных
и механизм координации между backend-инстансами.

Redis **не является источником истины**.

Источник истины всех доменных данных системы — PostgreSQL.

Redis используется для:

- WebSocket event bus
- token revocation
- кэширования часто читаемых данных
- хранения актуального состояния карты
- distributed locks

---

# Namespace ключей Redis

Все ключи Redis должны использовать единый namespace.

Формат ключа:

stn:{env}:{context}:{entity}:{id?}:{aspect?}

где:

- `stn` — код проекта
- `env` — среда (`dev`, `test`, `stage`, `prod`)
- `context` — функциональная область
- `entity` — тип данных
- `id` — идентификатор сущности
- `aspect` — дополнительный аспект ключа

Пример:

stn:prod:auth:jwt-revoked:{jti}

---

# Схема ключей Redis

## Auth

`stn:{env}:auth:user-token-version:{userId}`

Данный ключ хранит текущую версию JWT пользователя.

Каждый JWT содержит claim tokenVersion.

Если значение tokenVersion в JWT отличается
от значения Redis key auth:user-token-version:{userId},
токен считается недействительным.

## WebSocket

```stn:{env}:ws:pubsub:user:{userId}  
stn:{env}:ws:pubsub:squad:{squadId}  
stn:{env}:ws:pubsub:company:{companyId}  
stn:{env}:ws:pubsub:global
```
```
stn:{env}:ws:user-sessions:{userId}  
stn:{env}:ws:session-meta:{sessionId}
```
## Cache

```stn:{env}:cache:marker-type:{markerTypeId}  
stn:{env}:cache:marker-types:all
```
## Membership

```stn:{env}:membership:user-squad:{userId}  
stn:{env}:membership:user-company:{userId}  
stn:{env}:membership:squad-members:{squadId}  
stn:{env}:membership:company-squads:{companyId}
```
## Visibility

`stn:{env}:visibility:user-visible-users:{userId}`

## Geo

`stn:{env}:geo:user-last-pos:{userId}`

## Map

```stn:{env}:map:active-markers:squad:{squadId}  
stn:{env}:map:active-markers:company:{companyId}  
stn:{env}:map:marker:{markerId}
```
## Marker uniqueness

```stn:{env}:marker-unique:user:{userId}:type:{markerTypeId}  
stn:{env}:marker-unique:squad:{squadId}:type:{markerTypeId}
```
## Distributed locks

`stn:{env}:lock:marker-sweep`

---

# Таблица использования ключей

| Ключ | Тип Redis | TTL                                   | Кто пишет | Кто читает |
|-----|-----|---------------------------------------|-----|-----|
| auth:user-token-version:{userId} | string | без TTL                               | auth service | auth middleware |
| ws:user-sessions:{userId} | set | пока активны WS                       | websocket service | websocket service |
| ws:session-meta:{sessionId} | hash | пока жива сессия                      | websocket service | websocket service |
| cache:marker-type:{markerTypeId} | hash | долгий TTL                            | backend cache layer | backend services |
| cache:marker-types:all | string/json | долгий TTL                            | backend cache layer | backend services |
| membership:user-squad:{userId} | string | без TTL                               | membership service | visibility / geo |
| membership:user-company:{userId} | string | без TTL                               | membership service | visibility / geo |
| membership:squad-members:{squadId} | set | без TTL                               | membership service | visibility / map |
| membership:company-squads:{companyId} | set | без TTL                               | membership service | visibility / map |
| visibility:user-visible-users:{userId} | set | без TTL                               | visibility service | geo / map |
| geo:user-last-pos:{userId} | hash | ограниченный TTL(2 × update interval) | geo service | map service |
| map:active-markers:squad:{squadId} | set | до истечения маркеров                 | marker service | map service |
| map:active-markers:company:{companyId} | set | до истечения маркеров                 | marker service | map service |
| map:marker:{markerId} | hash | до expiresAt                          | marker service | map service |
| marker-unique:user:{userId}:type:{markerTypeId} | string | lifetime маркера                      | marker service | marker service |
| marker-unique:squad:{squadId}:type:{markerTypeId} | string | lifetime маркера                      | marker service | marker service |
| lock:marker-sweep | string | короткий TTL                          | scheduler | scheduler |

---

# Правила кэширования

Используется схема **cache-aside**.

Алгоритм чтения:

1. backend проверяет Redis
2. если данные есть — возвращает их
3. если данных нет — читает PostgreSQL
4. результат записывается в Redis
5. результат возвращается клиенту

---

# Правила инвалидции кэша

Кэш должен инвалидироваться при изменении
авторитетных данных в PostgreSQL.

Инвалидация выполняется backend после успешной записи.

Примеры:

### marker type изменён

инвалидируется:

cache:marker-type:{id}  
cache:marker-types:all

---

### membership изменён

инвалидируется:

membership:user-squad:{userId}  
membership:squad-members:{squadId}  
visibility:user-visible-users:{userId}

---

### company membership изменён

инвалидируется:

membership:user-company:{userId}  
membership:company-squads:{companyId}  
visibility:user-visible-users:{userId}

---

### маркер создан

обновляется:

map:marker:{markerId}  
map:active-markers:*  
marker-unique:*

---

### маркер удалён или истёк

удаляется:

map:marker:{markerId}  
marker-unique:*

обновляется:

map:active-markers:*

---

### новая геопозиция

обновляется:

geo:user-last-pos:{userId}

---

# События, обновляющие Redis

## Membership

события:

- USER_ASSIGNED_TO_SQUAD
- USER_REMOVED_FROM_SQUAD
- SQUAD_ASSIGNED_TO_COMPANY
- SQUAD_REMOVED_FROM_COMPANY

обновляют:

membership:*  
visibility:*

---

## Visibility

пересчитывается при:

- изменении squad membership
- изменении company membership
- изменении роли пользователя

обновляет:

visibility:user-visible-users:{userId}

---

## Map state

события:

- MARKER_CREATED
- MARKER_DELETED
- MARKER_EXPIRED
- GEO_POSITION_UPDATED

обновляют:

map:*  
geo:*

---

# Distributed Locks

Redis используется для предотвращения
параллельного выполнения фоновых задач.

Пример:

lock:marker-sweep

Перед выполнением задачи backend должен:

1. попытаться получить lock
2. выполнить задачу
3. освободить lock

Если lock уже существует — задача не выполняется.

---

# Ограничения использования Redis

Redis **не должен использоваться как постоянное хранилище**.

Все авторитетные данные должны храниться в PostgreSQL.

Redis используется только для:

- cache
- coordination
- realtime state
- revocation
- distributed locks

---

# Правило изменения Redis-схемы

Добавление нового Redis-ключа допускается только при выполнении условий:

1. ключ добавлен в раздел **"Схема ключей Redis"**
2. ключ добавлен в таблицу **"Таблица использования ключей"**
3. указан тип Redis
4. указан TTL
5. указано кто пишет и кто читает ключ

Добавление Redis-ключей без обновления документации запрещено.