# changelog.md (Normalization / compilation log)

This changelog describes what was standardized or resolved during compilation into canonical specs.

## Decisions / resolutions

1) Canonical WS endpoint
- Chosen: `/ws/events` for realtime events.
- `/ws/chat` is reserved for future chat and is not part of MVP.

2) Canonical WS error format
- Chosen: single format `{ "type":"ERROR", "error": <ErrorResponse> }`.
- Removed: mixed examples where `code/message` were duplicated at the top level.

3) Canonical eventType names
- Chosen canonical list from events specification:
  - `SQUAD_CREATED`, `BECAME_COMMANDER`, `JOINED_SQUAD`, `LEFT_SQUAD`, `KICKED_FROM_SQUAD`, `SQUAD_DISBANDED`
  - `SQUAD_JOINED_COMPANY`, `SQUAD_LEFT_COMPANY`, `COMPANY_DISBANDED`
  - `MARKER_CREATED`, `MARKER_DELETED`
  - `ORDER_CREATED`, `ORDER_STATUS_CHANGED`
- Added explicit alias mapping for legacy UC names:
  - `SQUAD_MEMBER_JOINED` → `JOINED_SQUAD`
  - `SQUAD_MEMBER_LEFT` → `LEFT_SQUAD`
  - `COMMANDER_CHANGED` → `BECAME_COMMANDER`

4) Token invalidation on accountStatus change
- Canonical rule: check `accountStatus` on every protected REST call and at WS connect; close WS when status changes away from ACTIVE.
- This satisfies “tokens invalid immediately” without introducing a blacklist in MVP.

5) Marker expiration vs realtime events
- Canonical rule: keep `MARKER_DELETED` emission on expiration by using a background sweep.
- Payload kept minimal (`markerId`) to preserve contract.

6) Geo positions semantics
- Canonical rule: `GET /geo/positions` returns the latest known position per visible user.

## Non-goals (kept out of MVP)
- Full admin/moderation suite beyond `PATCH /admin/users/{userId}/account-status`
- Pagination, sorting, filtering beyond what is explicitly stated
- Token blacklist / revocation store (Redis) — optional later
