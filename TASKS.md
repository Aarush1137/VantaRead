# VantaRead Task List

## Current feature pass

- [x] Improve suggestions so a failing source does not empty the whole screen.
- [x] Prompt signed-out users on startup with email, account creation, Google, phone, and guest options.
- [x] Add a profile section for account status, sync, and reading summary.
- [x] Expand stats with richer reading, source, and offline metrics.
- [x] Improve settings with clearer sections and safer data actions.
- [x] Improve source/web-scraping reliability for main flows.
- [x] Reduce source latency with source timeouts, suggestion cache, and progressive loading.
- [x] Add source operation timeouts for search, details, chapter lists, and reader content.
- [x] Run Gradle verification after implementation.

## Later candidates

- Add full Google OAuth setup documentation once release Firebase credentials are finalized.
- Replace destructive Room migration fallback with explicit migrations before production release.
- Add source health telemetry so broken scrapers can be surfaced inside suggestions/search.
