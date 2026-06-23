# AGENTS.md

Guidance for AI agents (and humans) working on this codebase.

---

## What this app is

A single-purpose League of Legends stat checker. User inputs a Riot ID → app fetches
recent match history from the Riot Games API → computes drake objective stats for games
where the player jungledconfirmed by Smite + `JUNGLE` position → renders a result card
with a roast verdict.

That's it. Keep it small and dumb.

---

## Architecture overview

```
Request
  └─► pages.clj (GET /)        → renders input form
  └─► api.clj (POST /check)
          └─► jungler.clj      → orchestrates the flow
                  ├─► riot_api.clj    → HTTP calls to Riot
                  ├─► analysis.clj    → crunches the numbers
                  └─► verdict.clj     → picks the roast string
          └─► result.html template
```

The framework is [Kit](https://kit-clj.github.io): Integrant for system lifecycle,
reitit for routing, Ring middleware stack, Selmer for templates.

---

## Data flow

### Step 1 — Resolve player

Input: `"PlayerName#EUW"` (user splits name + tag, or we parse the `#`)

```
POST /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}
→ { puuid, gameName, tagLine }
```

Store `puuid`. All subsequent calls use this.

### Step 2 — Fetch match IDs

```
GET /lol/match/v5/matches/by-puuid/{puuid}/ids?count=20&queue=420
```

`queue=420` = Ranked Solo/Duo. Consider accepting `queue=0` (all queues) as a fallback
or user toggle later. Returns a list of match ID strings.

### Step 3 — Fetch each match

```
GET /lol/match/v5/matches/{matchId}
```

One request per match. With 20 matches and a dev key (100 req/2min) this is fine.
For production, add a small `(Thread/sleep 50)` between calls or implement proper
token bucket rate limiting in `rate_limit.clj`.

### Step 4 — Filter to jungle games

A match counts as a "jungle game" for the target player when:
- `participant.teamPosition == "JUNGLE"`
- AND (`participant.summoner1Id == 11` OR `participant.summoner2Id == 11`)
  (11 = Smite spell ID)

Discard any match where neither condition holds. Player might flex other roles.

### Step 5 — Extract drake data

For each confirmed jungle game:
```clojure
{:match-id     "EUW1_12345"
 :win          true            ; participant.win
 :team-id      100             ; participant.teamId
 :drake-kills  3               ; teams[team-id].objectives.dragon.kills
 :drake-first  true            ; teams[team-id].objectives.dragon.first
 :champion     "LeeSin"        ; participant.championName
 :game-duration 1823}          ; info.gameDuration (seconds)
```

### Step 6 — Compute stats (`analysis.clj`)

```clojure
{:total-games    20
 :jungle-games   18            ; after filtering
 :drake-rate     0.61          ; games where team got >= 1 drake / jungle-games
 :win-with-drakes   0.73       ; WR in games with >= 1 drake
 :win-without-drakes 0.31      ; WR in games with 0 drakes
 :top-champs     [["LeeSin" 8] ["Kayn" 5] ["Hecarim" 3]]
 :verdict-key    :aware}       ; see verdict.clj
```

### Step 7 — Pick verdict (`verdict.clj`)

Map `drake-rate` to a key, key to a string. Keep the verdict strings in a data map,
not scattered in logic:

```clojure
(def verdicts
  {:clueless   "There is a dragon?! huh"
   :suspicious "Bold of you to assume they check the map."
   :aware      "Aware of dragons. Motivated by them: unclear."
   :decent     "Decent drake presence. Might even ping it."
   :based      "This jungler eats drakes for breakfast. Respect."})
```

---

## Riot API integration (`riot_api.clj`)

### Region routing

The user must specify their region. Riot uses two tiers of routing:

| Platform code | Routing cluster |
|---|---|
| `na1`, `br1`, `la1`, `la2` | `americas` |
| `euw1`, `eun1`, `tr1`, `ru` | `europe` |
| `kr`, `jp1` | `asia` |
| `oc1`, `ph2`, `sg2`, `th2`, `tw2`, `vn2` | `sea` |

Account lookup and match endpoints use the **cluster** (`europe.api.riotgames.com`).
Keep a lookup map in `riot_api.clj`.

### HTTP client

Use `hato` or `clj-http` (whichever Kit includes). Pass the API key as a header:
```
X-Riot-Token: RGAPI-your-key-here
```

Read the key from the environment via `config.clj` — never hardcode it.

### Error handling

| HTTP status | Meaning | Action |
|---|---|---|
| 200 | OK | proceed |
| 400 | Bad request | show "invalid Riot ID" to user |
| 403 | Forbidden | API key expired (dev keys last 24h) — log it |
| 404 | Not found | show "player not found" to user |
| 429 | Rate limited | retry after `Retry-After` header seconds |
| 5xx | Riot outage | show generic error |

Return either `{:ok data}` or `{:error reason}` from every API function. Let the
controller decide what to show the user.

---

## Key namespaces

| Namespace | Responsibility |
|---|---|
| `dragon.services.riot-api` | All HTTP calls to Riot. No business logic here. |
| `dragon.services.analysis` | Pure functions: match maps → stats map. No I/O. |
| `dragon.services.verdict` | Pure function: stats map → verdict string. |
| `dragon.web.controllers.jungler` | Orchestrates: calls api, analysis, verdict. Handles errors. Returns data for template. |
| `dragon.web.routes.pages` | Serves the home page (GET /). |
| `dragon.web.routes.api` | Handles form submit (POST /check). Calls controller. |

Keep `analysis` and `verdict` as pure functions with no side effects — they're easy
to test in isolation.

---

## Templates

Two main templates in `resources/templates/`:

`home.html` — dead simple form:
- Text input: Riot ID (`Name#TAG`)
- Region dropdown (EUW, NA, KR, etc.)
- Submit (HTMX `hx-post="/check"` swapping `#result` div)

`result.html` — the card:
- Player name + top 3 champ icons (use DDragon CDN for assets)
- Drake rate stat (big number, like "61%")
- WR with drakes vs without (two smaller numbers)
- Verdict sentence (big, centered, slightly dramatic)
- "Check another" button

Champion icons URL pattern:
```
https://ddragon.leagueoflegends.com/cdn/{version}/img/champion/{ChampionName}.png
```
Get latest version from:
```
https://ddragon.leagueoflegends.com/api/versions.json
```

---

## What NOT to build (for now)

- **No user accounts.** Stateless by design. Every lookup is fresh.
- **No leaderboards.** Not worth the complexity.
- **No real-time data.** Match history only. Riot's live game API is a different beast.
- **No database writes** until caching is actually needed (profile the API latency first).
- **No authentication.** It's a troll app.

---

## Testing approach

`analysis.clj` and `verdict.clj` are pure functions — test them with sample match data
fixtures. No mocking needed.

`riot_api.clj` functions should be tested against recorded HTTP responses (use
`clj-http-fake` or equivalent) to avoid hitting Riot's API in CI.

Controller tests: test the happy path, the "not a jungler" path, and the 404 path.

---

## Dev tips

- Riot dev API keys expire every 24 hours. Keep `.env` up to date.
- The DDragon CDN version needs refreshing at the start of each patch. Consider
  fetching it at startup and storing in the Integrant system state.
- `(Thread/sleep 1200)` between match fetches is safe and avoids all rate limit
  headaches on a dev key. Remove / tune for production.
- The match endpoint is slow (~200–400ms each). 20 matches = ~5–8 seconds. Show a
  loading state via HTMX.

---

## Deployment

Hetzner VPS. Uberjar deployed as a systemd service. See the server setup notes in
the repo wiki for SSH hardening, non-root user config, and reverse proxy (caddy or nginx).

Build: `clj -T:build uber`  
Run: `java -jar target/dragon.jar`
