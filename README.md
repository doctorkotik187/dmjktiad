# does my jungler know there is a dragon 🐉

> A troll app for a deeper truth: **focusing dragons wins games.**

Paste a jungler's Riot ID and find out whether they've been doing their job or just farming camps while the enemy team stacks drakes.

---

## What it does

1. **Looks up the player** by Riot ID (`name#TAG`), confirms they actually play jungle (Smite + `JUNGLE` position in recent games).
2. **Scans the last 20 matches** where they jungledand pulls dragon objective data for each game.
3. **Shows a result screen** with:
   - Drake focus rate (games where their team secured ≥ 1 drake vs total games)
   - Win/loss breakdown split by "got drakes" vs "ignored drakes"
   - Top 2–3 champions played in jungle, with game counts
   - A generated verdict sentence calibrated to how bad (or based) their drake tracking actually is
4. **Roasts accordingly.**

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Clojure |
| Framework | [Kit](https://kit-clj.github.io) (Integrant + reitit + Ring) |
| Database | SQLite (optional caching of API responses) |
| Frontend | HTMX + Selmer templates (Kit default) |
| Data source | [Riot Games API](https://developer.riotgames.com) |
| Deployment | Hetzner VPS |

---

## Data source: Riot Games API

The app uses the **official Riot Games API** — free, no scraping, no piggyback needed.

Sign up at [developer.riotgames.com](https://developer.riotgames.com) to get an API key.

**Development key**: 100 req / 2 min, expires every 24 hours. Fine for local dev.  
**Production key**: Requires an application. Submit once the app is working.

### Endpoints used

```
# 1. Resolve Riot ID → PUUID (platform-agnostic)
GET https://{region}.api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}

# 2. Get recent match IDs (filter by queue type)
GET https://{regional}.api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids
    ?queue=420    # 420 = Ranked Solo, 440 = Ranked Flex, 400 = Normal Draft, 0 = all
    &count=20

# 3. Get full match data (objectives, participants, outcomes)
GET https://{regional}.api.riotgames.com/lol/match/v5/matches/{matchId}
```

**Region routing**: account/match endpoints use routing clusters (`americas`, `europe`, `asia`, `sea`) not platform codes (`euw1`, `na1`). The player tells us their platform on input; the app maps it to the right cluster.

### Key fields from match data

```json
info.participants[n]:
  teamPosition       → "JUNGLE" (the role)
  summoner1Id        → 11 = Smite
  summoner2Id        → 11 = Smite
  championName       → "LeeSin"
  win                → true / false
  teamId             → 100 or 200

info.teams[n]:
  teamId             → 100 or 200
  objectives.dragon.kills   → number of drakes secured
  objectives.dragon.first   → got first drake (bool)
```

A "jungle game" is confirmed when: `teamPosition == "JUNGLE"` AND (`summoner1Id == 11` OR `summoner2Id == 11`).

---

## Verdict sentences (examples)

| Drake rate | Verdict |
|---|---|
| 0–20% | *"There is a dragon?! huh"* |
| 21–40% | *"Bold of you to assume they check the map."* |
| 41–60% | *"Aware of dragons. Motivated by them: unclear."* |
| 61–80% | *"Decent drake presence. Might even ping it."* |
| 81–100% | *"This jungler eats drakes for breakfast. Respect."* |

---

## Setup

### Prerequisites

- Clojure CLI (`brew install clojure` / package manager of choice)
- A Riot Games API key

### Clone and configure

```bash
git clone https://github.com/you/does-my-jungler-know-there-is-a-dragon
cd does-my-jungler-know-there-is-a-dragon

cp .env.example .env
# edit .env → set RIOT_API_KEY=RGAPI-your-key-here
```

### Run locally

```bash
clj -M:dev
# → http://localhost:3000
```

### Run tests

```bash
clj -M:test
```

---

## Project structure

```
src/clj/
  dragon/
    core.clj                 ; Integrant system entry point
    config.clj               ; env + config loading
    web/
      routes/
        pages.clj            ; GET /  →  input form
        api.clj              ; POST /check  →  result page
      controllers/
        jungler.clj          ; orchestrates lookup + analysis
      middleware/
        rate_limit.clj       ; don't get banned by Riot
    services/
      riot_api.clj           ; HTTP client wrapper for Riot endpoints
      analysis.clj           ; match data → stats (drake rate, WR, champs)
      verdict.clj            ; stats → roast string
resources/
  templates/
    home.html                ; input form (HTMX)
    result.html              ; result card
    error.html               ; "not a jungler" / "player not found"
  public/
    css/style.css
```

---

## Roadmap

- [ ] MVP: input → verify jungler → drake stats → verdict
- [ ] Region selector (or auto-detect from tagline)
- [ ] SQLite response cache (avoid re-fetching recent matches)
- [ ] Shareable result links
- [ ] "Compare two junglers" mode
- [ ] Public heatmap of drake-ignorant junglers by region (future chaos)

---

## Contributing

It's a troll app. PRs that make the roasts funnier are automatically approved.

---
