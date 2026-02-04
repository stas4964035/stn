# STN — Security Specification (JWT / Spring Security)

Версия: 0.1 (draft)

## 1. Область действия

- Этот документ фиксирует **конвенции JWT** и **контракт безопасности** для backend (REST + WebSocket).
- Публичный REST-префикс: `/api/v1`.
- Аутентификация: JWT через заголовок `Authorization: Bearer <jwt-token>`.
- Все эндпоинты, кроме `/api/v1/auth/*`, требуют валидный JWT.
- WebSocket `/ws/chat`: JWT передаётся либо в query-параметре `token`, либо в заголовке при upgrade.

## 2. Тип токена

- Используется **только access token** (refresh token отсутствует).
- Истечение access token: **24 часа**.

## 3. Алгоритм и ключ

- Алгоритм подписи: **HS256** (HMAC-SHA256).
- Секрет хранится как **base64-строка** в конфигурации и при использовании декодируется в байты.
- Минимальная длина ключа (после base64-decode): **32 байта (256 бит)**.

## 4. Claims (payload)

Обязательные:

- `sub` — идентификатор пользователя (userId). Рекомендуемый формат: строковое представление `User.id`.
- `iat` — время выдачи токена (UTC).
- `exp` — время истечения токена (UTC).
- `role` — роль пользователя (`USER` | `MODERATOR` | `ADMIN`) из `systemRole`.

Опционально (пока решение не принято):

- `iss` — issuer (строка-идентификатор выпускающей стороны).  
  Если `issuer` **не задан** в конфигурации, claim `iss` **не добавляется** и при валидации **не проверяется**.

## 5. Источники конфигурации и имена свойств

На текущем этапе источник — `application.yml`.

Рекомендуемые свойства:

```yaml
stn:
  security:
    jwt:
      secret-base64: "<base64-secret>"   # обязательно
      access-ttl: "PT24H"                # обязательно (Duration)
      issuer: ""                         # опционально (пусто = не использовать iss)
      clock-skew: "PT60S"                # опционально (Duration), по умолчанию 60s
```

Примечания:
- `access-ttl` и `clock-skew` — стандартные `java.time.Duration`, предпочтительно в ISO-8601 виде (`PT24H`, `PT60S`).
- Если `issuer` пустой — поведение “issuer disabled”.

## 6. Передача токена

### 6.1. REST

- Клиент обязан передавать токен в заголовке:
  - `Authorization: Bearer <jwt-token>`

### 6.2. WebSocket `/ws/chat`

Допустимы оба варианта (как в API-спеке):
- query-параметр `token` (например `/ws/chat?token=...`)
- заголовок `Authorization: Bearer <jwt-token>` при upgrade

## 7. Валидация токена

При обработке защищённого запроса (всё, кроме `/api/v1/auth/*`):

- Проверить наличие токена.
- Проверить подпись HS256 (по `secret-base64`).
- Проверить `exp` (с учётом `clock-skew`).
- (Опционально) проверить `iss`, если включён.
- Извлечь `sub` → userId и `role` → GrantedAuthority.

## 8. Ошибки безопасности

Формат ошибок — **строго `ErrorResponse`** из `errors-spec.md`.

- Если JWT отсутствует / невалиден / просрочен → HTTP **401** + `code=UNAUTHORIZED`.
- Если JWT валиден, но прав недостаточно (роль) → HTTP **403** + `code=FORBIDDEN`.

## 9. Account status / блокировки

В БД присутствует `account_status` (`ACTIVE` | `BLOCKED` | `DELETED`). Текущая документация **не фиксирует** поведение API для `BLOCKED/DELETED` (например, 401 vs 403).

Решение нужно отдельно, иначе реализация будет произвольной.

## 10. Request ID (observability)

Request ID и логирование определяются `observability.md`:
- вход/выход: заголовок `X-Request-Id`,
- MDC key: `requestId`,
- дублирование `requestId` в теле `ErrorResponse` не требуется.

