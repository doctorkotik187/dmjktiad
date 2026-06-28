# AGENTS.md

Guidance for AI agents working on this codebase.

---

## What this app is

A single-purpose League of Legends stat checker. User inputs a summoner name → app fetches
recent match history from the Riot Games API → computes drake objective stats for games
where the player jungled (confirmed by Smite + `JUNGLE` position) → renders a result card
with a roast verdict.

Keep it small and dumb.

---

## Architecture

The framework is Kit: Integrant for lifecycle, reitit for routing, Ring for middleware, Selmer for templates.

Request flow:
- Route handler receives the request
- Controller orchestrates: API calls → stats computation → verdict selection → template rendering
- API layer is stateless HTTP calls, returns `{:ok data}` or `{:error reason}`
- Stats and verdict logic are pure functions — no I/O, no side effects

No database. In-memory cache only.

---

## Layer responsibilities

- **Web layer** (`web.routes.pages`, `web.controllers.jungler`): HTTP in/out, no business logic
- **Service layer** (`services.riot-api`, `services.analysis`, `services.verdict`): do the work
- **Middleware**: rate limiting, exception handling, parameter parsing
- **Cache**: simple atom-based store with TTL

Don't blur these boundaries. Routes don't compute stats. Services don't know about HTTP.

---

## Key behaviors to preserve

- Jungle game = Smite + JUNGLE position
- Players who don't jungle enough get a special verdict, not a roast
- All API functions return `{:ok data}` or `{:error reason}`
- Stats and verdict are pure functions — easy to test, no mocking needed
- Rate limiting protects the API key from abuse
- Cache prevents redundant lookups within the cooldown window

---

## No need to document

- Exact endpoint URLs or field names (read the code)
- Specific thresholds or verdict text (read the code)
- Region routing tables (read the code)
- File-by-file breakdowns (read the code)

The code is small. Read it.
