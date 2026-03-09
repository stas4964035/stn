# Tactical App — Documentation Map

Документация проекта разделена на один канонический документ и несколько технических спецификаций.

## Канонический документ

- tz_tactical_app.md — продуктовый канон MVP

Он описывает:
- бизнес-логику
- правила домена
- роли
- поведение системы

Все остальные документы уточняют реализацию.

## Технические спецификации

- spec-domain.md — доменные инварианты, ACL, модель ошибок
- spec-api.md — REST API контракты
- spec-ws.md — WebSocket и realtime события
- spec-infra.md — инфраструктура, NFR, эксплуатация
- spec-redis.md — использование Redis
- spec-structure.md — структура backend-кода

## Правило приоритета

1. tz_tactical_app.md
2. spec-domain.md
3. остальные spec-*

## Порядок чтения

1. tz_tactical_app.md
2. spec-domain.md
3. spec-api.md
4. spec-ws.md
5. spec-infra.md
6. spec-redis.md
7. spec-structure.md

## Границы ответственности документов

tz_tactical_app.md
— продуктовая логика и функциональные требования.

spec-domain.md
— формальные доменные инварианты, ACL, security и модель ошибок.

spec-api.md
— HTTP-контракты REST API.

spec-ws.md
— протокол WebSocket и события realtime.

spec-infra.md
— инфраструктурные требования и эксплуатационные ограничения.

spec-redis.md
— схема ключей Redis и правила кэширования.

spec-structure.md
— структура backend-кода.

Если спецификация конфликтует с ТЗ — исправляется спецификация.