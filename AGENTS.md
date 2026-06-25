# AGENTS.md

Guidance for AI agents (and humans) working on this codebase.

---

## What this app is

A single-purpose League of Legends stat checker. User inputs a summoner name → app fetches
recent match history from the Riot Games API → computes drake objective stats for games
where the player jungled (confirmed by Smite + `JUNGLE` position) → renders a result card
with a roast verdict.

That's it. Keep it small and dumb.

---

## Architecture overview

```
Request
  └─► pages.clj (GET /)        → renders input form
  └─► pages.clj (POST /check)  → redirects to result page
  └─► pages.clj (GET /summoners/:region/:name)
          └─► jungler.clj      → orchestrates the flow
                  ├─► riot_api.clj    → HTTP calls to Riot
                  ├─► analysis.clj    → crunches the numbers
                  └─► verdict.clj     → picks the roast string
          └─► result.html template
```

The framework is [Kit](https://kit-clj.github.io): Integrant for system lifecycle,
reitit for routing, Ring middleware stack, Selmer for templates.

---

## Key features

- **200 recent ranked matches** fetched per player (2 pages of 100)
- **Jungle filtering**: only games with Smite + JUNGLE position count
- **Drake stats**: control rate, first drake rate, avg drakes per game, drake differential
- **Win rate splits**: WR with/without drakes, WR by drake tier (0, 1-2, 3, 4+)
- **Top champions**: most-played jungle champs with game counts and percentages
- **Verdict system**: 7 special overrides + primary tier based on drake control percentage
- **Rank display**: shows tier, rank, LP, and W/L record
- **Caching**: in-memory cache per player, 1-hour cooldown on refresh
- **Rate limiting**: 30 requests/hour per IP
- **"Not a jungler" page**: shown when player has < 40% jungle games in recent history
- **Bookmarkable URLs**: `/summoners/{region}/{name}-{tag}`

---

## Riot API integration

### Region routing

| Platform code | Routing cluster |
|---|---|
| `na1`, `br1`, `la1`, `la2` | `americas` |
| `euw1`, `eun1`, `tr1`, `ru` | `europe` |
| `kr`, `jp1` | `asia` |
| `oc1`, `ph2`, `sg2`, `th2`, `tw2`, `vn2` | `sea` |

Account lookup and match endpoints use the **cluster** (`europe.api.riotgames.com`).
Summoner and league endpoints use the **platform** (`euw1.api.riotgames.com`).

### HTTP client

Use `hato` (included with Kit). Pass the API key as a header:
```
X-Riot-Token: RGAPI-your-key-here
```

Read the key from the environment — never hardcode it.

### Error handling

| HTTP status | Meaning | Action |
|---|---|---|
| 200 | OK | proceed |
| 400 | Bad request | show "invalid Riot ID" to user |
| 403 | Forbidden | API key expired (dev keys last 24h) — log it |
| 404 | Not found | show "player not found" to user |
| 429 | Rate limited | retry after `Retry-After` header seconds |
| 5xx | Riot outage | show generic error |

Return either `{:ok data}` or `{:error reason}` from every API function.

---

## Key namespaces

| Namespace | Responsibility |
|---|---|
| `doctorkotik.dmjktiad.services.riot-api` | All HTTP calls to Riot. No business logic. |
| `doctorkotik.dmjktiad.services.analysis` | Pure functions: match data → stats map. No I/O. |
| `doctorkotik.dmjktiad.services.verdict` | Pure function: stats map → verdict string. |
| `doctorkotik.dmjktiad.web.controllers.jungler` | Orchestrates: calls api, analysis, verdict. Returns data for template. |
| `doctorkotik.dmjktiad.web.routes.pages` | Serves all pages (home, result, not-jungler). |
| `doctorkotik.dmjktiad.cache` | In-memory cache (atom). |
| `doctorkotik.dmjktiad.web.middleware.rate-limit` | IP-based rate limiter. |

Keep `analysis` and `verdict` as pure functions with no side effects — they're easy to test.

---

## Templates

All templates extend `base.html` which provides:
- HTML boilerplate
- CSS and JS includes
- Header (title + subtitle)
- Loading overlay
- Footer (legal + GitHub links)

Key templates:
- `home.html` — search form
- `result.html` — player stats card
- `not-jungler.html` — "this is not a jungler" page

---

## Deployment

Hetzner VPS. Uberjar deployed as a systemd service.

Build: `clj -T:build uber`  
Run: `java -jar target/dmjktiad.jar`
