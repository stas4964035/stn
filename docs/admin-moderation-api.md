# STN — Admin / Moderation REST API (draft)

Черновик описания REST-эндпоинтов уровня ADMIN/MODERATOR, дополняющих базовый API (`backend-api-spec-from-use-cases.md`).
Документ собирает операции управления отрядами, ротами, пользователями и контентом, доступные расширенным ролям.

## Squad administration
- TODO: endpoints вида `/api/v1/admin/squads/...` для принудительного редактирования, киков и роспуска.

## Company administration
- TODO: endpoints вида `/api/v1/admin/companies/...` для управления ротами, их атрибутами и связью с отрядами.

## User moderation
- TODO: операции бана, кика, принудительной смены статуса ALIVE/DEAD.

## Orders & markers moderation
- TODO: принудительное удаление/изменение приказов и тактических меток.

## Relation to realtime events
- TODO: как админские действия транслируются в события из `events-spec.md` (каналы, payload).
