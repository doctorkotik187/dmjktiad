# does my jungler know there is a dragon

> A troll app for a deeper truth: **focusing dragons wins games.**

Paste a jungler's Riot ID and find out whether they've been doing their job or just farming camps while the enemy team stacks drakes.

---

## What it does

1. Looks up the player by Riot ID, confirms they actually play jungle (Smite + `JUNGLE` position in recent games).
2. Scans recent ranked matches where they jungled and pulls dragon objective data for each game.
3. Shows a result screen with drake stats, win rate splits, top champions, and a roast verdict.
4. Handles edge cases: not enough jungle games, not a jungler at all, API errors.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Clojure |
| Framework | Kit (Integrant + reitit + Ring) |
| Frontend | Selmer templates + vanilla JS |
| Data source | Riot Games API |
| Deployment | VPS, with a simple docker compose |

---

## Setup

- Clojure CLI + a Riot Games API key (get one at developer.riotgames.com)
- Copy `.env.example` to `.env` and set your `RIOT_API_KEY`
- `clj -M:dev` → http://localhost:3000
- `clj -M:test` → run tests
- `clj -T:build uber` → build, then `java -jar target/dmjktiad.jar`

---

## Architecture

User input → resolve account → fetch match IDs → fetch match details → filter jungle games → compute drake stats → render result.

The code is organized into thin layers:
- **web layer**: routes and controllers handle HTTP
- **service layer**: pure functions for API calls, stats computation, verdict selection
- **cache**: in-memory, per-player with cooldown
- **middleware**: rate limiting, error handling

No ORM, no database, no frontend framework. Just HTTP calls and arithmetic.

---

## Key behaviors

- Only ranked solo queue games count
- Jungle = Smite + JUNGLE position
- Players below a jungle-game threshold get a "not a jungler" verdict instead of a roast
- Results are cached; refresh respects a cooldown window
- Rate limited per IP to protect the Riot API key
