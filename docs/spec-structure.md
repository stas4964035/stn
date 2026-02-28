# spec-structure.md — Каноническая структура пакетов/каталогов проекта

Этот документ фиксирует текущую структуру каталогов (пакетов) проекта.


Важно:
- Документ фиксирует только видимую структуру каталогов.
- Имя базового Java package (например, com.k44.STN) в снимке не указано;
  структура ниже описывает поддерево под базовым пакетом.

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