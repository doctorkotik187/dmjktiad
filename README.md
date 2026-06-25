# does my jungler know there is a dragon

> A troll app for a deeper truth: **focusing dragons wins games.**

Paste a jungler's Riot ID and find out whether they've been doing their job or just farming camps while the enemy team stacks drakes.

---

## What it does

1. **Looks up the player** by Riot ID (`name#TAG`), confirms they actually play jungle (Smite + `JUNGLE` position in recent games).
2. **Scans the last 200 matches** where they jungled and pulls dragon objective data for each game.
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
| Frontend | Selmer templates + vanilla JS (no frameworks) |
| Data source | [Riot Games API](https://developer.riotgames.com) |
| Deployment | Hetzner VPS, uberjar as systemd service |

---

## Data source: Riot Games API

The app uses the **official Riot Games API** — no scraping, no piggyback needed.

Sign up at [developer.riotgames.com](https://developer.riotgames.com) to get an API key.

**Development key**: 100 req / 2 min, expires every 24 hours. Fine for local dev.  
**Production key**: Requires an application. Submit once the app is working.

### Endpoints used

```
# 1. Resolve Riot ID → PUUID (regional routing)
GET https://{region}.api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}

# 2. Get recent ranked match IDs
GET https://{regional}.api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids?count=200&queue=420

# 3. Get full match data (objectives, participants, outcomes)
GET https://{regional}.api.riotgames.com/lol/match/v5/matches/{matchId}

# 4. Get summoner data (profile icon, level)
GET https://{platform}.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/{puuid}

# 5. Get ranked league data (tier, rank, LP, W/L)
GET https://{platform}.api.riotgames.com/lol/league/v4/entries/by-summoner/{summonerId}
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
git clone https://github.com/doctorkotik187/dmjktiad
cd dmjktiad

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

### Build and deploy

```bash
clj -T:build uber
java -jar target/dmjktiad.jar
```

---
