# spec-infra.md (NFR, observability, data model pointers)

This file consolidates operational requirements for MVP.

## Time & auditing

- All server times are UTC (`Instant`)
- Entities must have `createdAt` and `updatedAt` where applicable
- Use Spring Data JPA auditing (`@CreatedDate`, `@LastModifiedDate`) for auto-fill

## Request correlation

- Backend must accept `X-Request-Id` from clients
- Backend must include the same `X-Request-Id` in:
  - response headers
  - log entries for the request
- If header is absent, backend must generate it.

## Logging

- Log at least:
  - request id
  - authenticated user id (if any)
  - method + path
  - response status
  - duration
- Sensitive data:
  - never log passwords
  - avoid logging raw JWT

## Database (canonical pointer)

The authoritative MVP schema is based on `db-schema.md`. Key points:
- PostgreSQL is the source of truth
- geo positions stored as history (`user_geo_locations`) with index `(user_id, recorded_at desc)`
- markers include `expires_at` and must be filtered by active/expired rules
- squads, company_squads constraints enforce membership relations

## Background jobs (MVP)

Markers expiration:
- Background sweep every 1 minute (configurable) to detect expired markers and emit `MARKER_DELETED` events.
- Sweep also handles cleanup/archival as implementation chooses.

## Deployment defaults (MVP)

- Java 17/21 + Spring Boot 3.x
- Postgres
- Redis (optional in MVP; required later for scaling WS / caching / token revocation)
