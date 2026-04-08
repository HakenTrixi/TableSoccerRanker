# Table Soccer Ranker

Internal table soccer (foosball) ranking app for trixi.cz. Tracks matches, ELO ratings, player stats, and the monthly "Zlatý Bludišťák" award.

## Tech Stack

- **Backend**: Spring Boot 3.4.4, Java 21, Gradle Kotlin DSL
- **Frontend**: SvelteKit 2, Svelte 5 (runes: `$state`, `$derived`, `$effect`, `$props`), Tailwind CSS 4
- **Database**: PostgreSQL 17, Flyway migrations (V1–V9)
- **Infrastructure**: Docker Compose (4 containers: postgres, backend, frontend, nginx)
- **Auth**: Username/password (BCrypt) + Google OAuth2. First registered user gets ADMIN role.

## Project Structure

```
backend/src/main/java/com/tablesoccer/ranker/
  admin/          # Admin panel, settings, data import
  config/         # Security, CORS, OAuth2, exception handler
  dataimport/     # Excel/CSV import
  match/          # Match entity, controller, service, DTOs
  ranking/        # ELO strategy, monthly strategies, snapshots
  stats/          # Company-wide and per-player statistics
  team/           # Team formation/balancing suggestions
  user/           # User entity, auth (login/register/password)

frontend/src/
  routes/          # SvelteKit pages (+page.svelte, +layout.svelte)
  lib/api/         # API client (client.ts) and TypeScript types (types.ts)
  lib/components/  # Reusable Svelte components (charts, layout, match, ranking, ui)
```

## Development Commands

```bash
# Run everything
docker compose up -d

# Backend tests (from repo root)
cd backend && ./gradlew test

# Rebuild and deploy
docker compose build backend frontend && docker compose up -d backend frontend

# Backend only rebuild
docker compose build backend && docker compose up -d backend

# Frontend only rebuild
docker compose build frontend && docker compose up -d frontend

# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Database shell
docker exec -it tablesoccer-db psql -U tablesoccer -d tablesoccer
```

## Environment Setup

Copy `.env.example` to `.env` and set `POSTGRES_PASSWORD`. All secrets come from env vars — never hardcode in committed files.

## Code Quality Principles

Follow OOP best practices and SOLID principles in all code:

- **Single Responsibility**: Each class has one reason to change. Controllers handle HTTP, services handle business logic, repositories handle data access. Don't mix concerns.
- **Open/Closed**: Use strategy pattern (sealed interfaces) for ranking algorithms. Add new strategies without modifying existing ones.
- **Liskov Substitution**: All ranking strategy implementations must be interchangeable through their interface contracts.
- **Interface Segregation**: Keep interfaces focused — `LongTermRankingStrategy` and `MonthlyRankingStrategy` are separate, not one bloated interface.
- **Dependency Inversion**: Depend on abstractions. Services take repository interfaces, not concrete implementations. Use constructor injection.

### Additional Practices
- Package-by-feature structure (match/, ranking/, stats/, user/) — not package-by-layer.
- Prefer Java records for DTOs and value objects (immutable, concise).
- Use sealed interfaces where the set of implementations is known and fixed.
- Favor composition over inheritance.
- Keep methods short and focused. Extract private helpers for complex logic.
- Validate at system boundaries (controllers, API input) — trust internal code.
- Write tests for business logic, not for Spring wiring. Test behavior, not implementation.

## Architecture Decisions

### ELO System
- Constants: K=32, WIN_BONUS=0.2 (winner only, losers get 0), S_BASE=0.5, GOAL_RATIO_DIVISOR=20, ELO_DIFF_SCALING=400, DEFAULT_ELO=1000
- Three independent ELO ratings per player: overall, attacker, defender
- Per-match ELO metadata stored on MatchPlayer entity (eloBefore, eloAfter, eloChange, teamElo, winProbability)
- Daily ELO snapshots with upsert pattern

### Ranking Strategies
- Sealed interfaces with Strategy pattern: `LongTermRankingStrategy` (ELO, AvgGoalDiff), `MonthlyRankingStrategy` (MonthlyEloGain, MonthlyGoalsScored)
- Monthly ranking = sum of eloChange from MatchPlayer records for that month
- Team formation: 1st+4th vs 2nd+3rd by ELO ranking

### API Design
- REST endpoints under `/api/` proxied through nginx
- Server-side pagination for match listing (`Page<MatchDto>`)
- `POST /api/matches/preview` for ELO preview without persisting
- Spring Data JPA with JPQL — all queries use parameterized binding (no SQL injection risk)

### Frontend Conventions
- Svelte 5 runes only — no legacy `$:` reactive statements
- `$state()` for reactive variables, `$derived()` for computed values, `$props()` for component props
- `@const` can only be used inside `{#if}`, `{#each}`, `{:else}` blocks — not at top level in `<div>`
- API calls via `api.get()` / `api.post()` from `$lib/api/client.ts`
- SvelteKit `fetch` in `+layout.ts` (not custom api client) for SSR cookie forwarding
- Mobile-first responsive design with Tailwind breakpoints (sm:, md:)

## Testing

- Backend: JUnit 5 + Mockito. `TestDataFactory` for creating test entities.
- `TestDataFactory.createMatch()` sets `playedAt` (required since `@PrePersist` doesn't run in unit tests)
- ELO tests validated against real production spreadsheet data (EloRealDataTest.java — 17 tests, 44 matches)
- Use `@MockitoSettings(strictness = LENIENT)` for StatsService tests (complex mock setup)

## Important Gotchas

- `MatchRepository.findAllByPlayerId()` sorts ASC (chronological). This is critical — streak/form computation assumes oldest-first order.
- `$effect` must not set reactive state that it reads — causes `effect_update_depth_exceeded`. Use event-driven callbacks instead of reactive watchers for score changes.
- Svelte 5: `@const` placement is restricted. Move computation to `$derived` in `<script>` when needed at top level.
- Team colors are YELLOW and WHITE (physical foosball sides).
- Monthly Bludišťák: winner of each completed month's ELO gain leaderboard. Only completed months count.

## Ports

| Service  | Port |
|----------|------|
| nginx    | 80   |
| backend  | 8080 |
| frontend | 3000 |
| postgres | 5434 |

## Self-Improvement

This CLAUDE.md should evolve with the project. Update it when:

- A new **gotcha or bug pattern** is discovered that cost debugging time — add it to "Important Gotchas" so it never happens again.
- A new **architecture decision** is made (new strategy, new entity pattern, new API convention) — document the "why" not just the "what".
- A **dependency is upgraded** that changes behavior (e.g., Svelte version, Spring Boot version) — update the Tech Stack section.
- A new **development command** becomes commonly used — add it to Development Commands.
- A **code quality rule** is established or refined — update Code Quality Principles.
- The **project structure changes** (new packages, renamed routes) — keep the structure map accurate.
- A **test pattern** is learned (new mock setup, test data factory method) — add to Testing section.

Do NOT add ephemeral information (current bugs, in-progress tasks, temporary workarounds). This file is for durable project knowledge.
