# changelog.md (Журнал нормализации / компиляции)

Этот журнал описывает, что было стандартизировано или разрешено при компиляции в канонические спецификации.

## Решения / разрешения

1) Канонический WS endpoint
- Выбрано: `/ws/events` для realtime событий.
- `/ws/chat` зарезервирован под будущий чат и не входит в MVP.

2) Канонический формат WS ошибок
- Выбрано: единый формат `{ "type":"ERROR", "error": <ErrorResponse> }`.
- Убрано: смешанные примеры, где `code/message` дублировались на верхнем уровне.

3) Канонические имена eventType
- Выбран канонический список:
  - `SQUAD_CREATED`, `BECAME_COMMANDER`, `JOINED_SQUAD`, `LEFT_SQUAD`, `KICKED_FROM_SQUAD`, `SQUAD_DISBANDED`
  - `SQUAD_JOINED_COMPANY`, `SQUAD_LEFT_COMPANY`, `COMPANY_DISBANDED`
  - `MARKER_CREATED`, `MARKER_DELETED`
  - `ORDER_CREATED`, `ORDER_STATUS_CHANGED`
- Добавлен явный маппинг алиасов из legacy/UC-имен:
  - `SQUAD_MEMBER_JOINED` → `JOINED_SQUAD`
  - `SQUAD_MEMBER_LEFT` → `LEFT_SQUAD`
  - `COMMANDER_CHANGED` → `BECAME_COMMANDER`

4) Инвалидация токенов при изменении accountStatus
- Канон: проверять `accountStatus` на каждом защищённом REST вызове и при WS connect; закрывать WS при смене статуса на не-ACTIVE.
- Это удовлетворяет требование “токены недействительны сразу” без blacklist в MVP.

5) Истечение меток и realtime события
- Канон: сохраняем эмит `MARKER_DELETED` при истечении через фоновой sweep.
- Payload оставлен минимальным (`markerId`) для стабильности контракта.

6) Семантика geo positions
- Канон: `GET /geo/positions` возвращает последнюю известную позицию по каждому видимому пользователю.

## Не-цели (исключено из MVP)
- Полный набор admin/moderation API beyond `PATCH /admin/users/{userId}/account-status`
- Пагинация/сортировка/фильтры beyond того, что явно указано
- Token blacklist / revocation store (Redis) — опционально позже
