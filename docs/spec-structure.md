# spec-structure.md — Дополнение к ТЗ: структура пакетов/каталогов проекта

Этот документ фиксирует рекомендуемую структуру кодовой базы для реализации требований из `tz_tactical_app.md`.
Он описывает организацию модулей и ответственность пакетов, но не подменяет собой функциональные требования ТЗ.

Правило приоритета:
- если структура мешает реализации требований ТЗ, корректируется структура и этот файл;
- ТЗ остаётся источником истины по бизнес-поведению.

## 1) Каноническое дерево каталогов

```
.
├── admin
│   ├── api
│   └── application
├── auth
│   ├── api
│   ├── application
│   └── domain
├── common
│   ├── error
│   ├── persistence
│   ├── security
│   ├── time
│   └── web
├── companies
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── geo
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── jobs
├── markers
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── marker_types
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── orders
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── realtime
│   ├── events
│   └── ws
├── squads
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
└── users
├── application
├── domain
└── persistence
```

## 2) Фиксированные соглашения по назначению подпакетов

- api: REST/DTO/контроллеры (входные адаптеры).
- application: use-cases / application services / orchestration.
- domain: доменная модель, доменные правила, доменные сервисы.
- persistence: JPA entities, repositories, маппинг и инфраструктура хранения.
- realtime/events: публикация/моделирование realtime-событий.
- realtime/ws: WebSocket endpoint и обработчики сообщений.
- common/*: общий слой (ошибки, безопасность, время, веб-утилиты, общая persistence-инфраструктура).

## 3) Исключения структуры (как есть)

- users: отсутствует подпакет api.
- admin: отсутствуют domain и persistence.
- jobs: подкаталоги отсутствуют (в текущем снимке).