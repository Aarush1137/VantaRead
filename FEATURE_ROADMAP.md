# VantaRead Feature Roadmap

## Implemented in this update

- Firebase email/password and phone authentication screens.
- Bookmark-only cloud sync with stable document IDs, stale-cloud cleanup, and safe batching for large libraries.
- Firestore rules that isolate each user's bookmark collection.
- Offline chapter download constraints and duplicate-work prevention.
- Reader paragraph rendering, text-to-speech controls, auto-scroll, progress indicator, tap navigation, and pull-to-next-chapter gesture.
- Downloads screen with queued/running state, per-novel progress, removal, and Android notifications.
- Firebase-safe guest mode: builds without local Firebase configuration no longer crash, while configured builds use the local, git-ignored `google-services.json` for sign-in.
- Account flow cleanup: email validation, password reset, cancellable phone verification, explicit Firebase availability, and reliable logout routing.
- Compact navigation: Library, Search, Suggestions, Downloads, and a More sheet that keeps History, Statistics, Profile/cloud sync, and Settings reachable without crowding the bottom bar.

## Next feature pass: reader

1. **Chapter picker and search** — searchable chapter list, unread/read filtering, and a direct chapter-number jump.
2. **Reader annotations** — highlights, notes, and saved passages stored per chapter with a searchable notebook view.
3. **Reader bookmarks** — quick page bookmarks with named labels and one-tap return from the chapter toolbar.
4. **Better reading controls** — per-novel overrides for font, theme, spacing, brightness, and reading direction.
5. **Reader resilience** — prefetch adjacent chapters, show a retry card instead of raw errors, and preserve position across orientation/process recreation.
6. **Accessibility** — semantic labels for tap zones, larger adjustable controls, configurable line height, and high-contrast reader presets.

## Next feature pass: downloads and library

1. **Download manager actions** — pause, resume, retry failed jobs, reorder queue, and delete a whole novel's offline cache at once.
2. **Storage dashboard** — total offline size, per-novel usage, stale-download cleanup, and a configurable storage limit.
3. **Offline cover cache** — cache thumbnails and metadata with chapters so the library remains polished without a network connection.
4. **Smart download rules** — download the next unread chapters only on Wi-Fi/charging, with per-novel rules and a daily limit.
5. **Library organization** — tags, custom shelves, pinning, advanced sort/filter, and a reading-status tracker.
6. **Backup and restore** — export/import the local library, progress, settings, and annotations as a user-controlled file.

## Next feature pass: discovery and reliability

1. **Source health dashboard** — live status, last successful refresh, retry controls, and clear fallback explanations.
2. **Unified search** — search all active sources in parallel, group results by source, and let readers choose a preferred result.
3. **Release-safe source monitoring** — parser fixtures and scheduled checks to detect source HTML/API changes before users do.
4. **Conflict-aware cloud sync** — two-way progress/history sync with timestamps and a clear conflict choice when devices disagree.
5. **Modern Google sign-in migration** — replace the deprecated Google Sign-In client with Credential Manager / Google Identity Services.

## UI polish candidates

1. **Adaptive layouts** for tablets, foldables, and landscape reading.
2. **Shared loading and empty states** with retry actions across Search, Suggestions, Downloads, History, and Profile.
3. **Motion polish** for shelf changes, download progress, and reader chapter transitions, respecting reduced-motion preferences.
4. **Profile improvements** including connected-provider badges, sync time, storage summary, and account-security actions.
5. **Navigation customization** so readers can pin their preferred More destinations to the primary bar.

- Conflict-aware two-way cloud sync for progress and reading history.
- Offline-first cover caching and a storage management screen.
- Per-source health checks with user-visible fallback explanations.
- Chapter search, bookmarks/notes, and highlights in the reader.
- Import/export of a local library backup.
- Accessibility pass: scalable controls, screen-reader labels, and high-contrast presets.
