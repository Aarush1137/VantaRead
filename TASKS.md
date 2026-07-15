# VantaRead Task List

## Current feature pass

- [x] Improve suggestions so a failing source does not empty the whole screen.
- [x] Prompt signed-out users on startup with email, account creation, Google, phone, and guest options.
- [x] Add a profile section for account status, sync, and reading summary.
- [x] Redesign Auth UI with Tabbed Navigation and diagnostic tools.
- [x] Refine Reactive Navigation for login/logout/guest flows.
- [x] Expand stats with richer reading, source, and offline metrics.
- [x] Improve settings with clearer sections and safer data actions.
- [x] Improve source/web-scraping reliability for main flows.
- [x] Reduce source latency with source timeouts, suggestion cache, and progressive loading.
- [x] Add source operation timeouts for search, details, chapter lists, and reader content.
- [x] Run Gradle verification after implementation.

## Later candidates

- Implement automated storage cleanup of old cached/downloaded content in `VantaStorageManager`.
- Add source-specific diagnostic tools to identify when a scraper is broken by website HTML changes.
- Replace destructive Room migration fallback with explicit migrations before production release.
