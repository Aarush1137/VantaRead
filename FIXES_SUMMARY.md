# VantaRead Fixes Summary

- Unified source IDs across Library, Search, Suggestions, Settings, repository, and add-by-URL flows.
- Made Royal Road the default source and refreshed Royal Road selectors for search, suggestions, thumbnails, details, and chapters.
- Updated LightNovelPub to the working `.me` domain and refreshed its list/detail/chapter selectors.
- Reworked WTR-Lab suggestions/search parsing around the current novel-list page after the old search URL stopped working.
- Fixed add-by-URL metadata so saved novels keep real titles, covers, authors, and source IDs instead of showing unknown/blank data.
- Added cached novel metadata fallback on the detail screen so saved novels still show title/cover/synopsis if a live refresh fails.
- Made Settings source selection update the active Library/Search/Suggestions source immediately.
- Added a visible Downloads section on novel details with downloaded counts plus Next 5, Next 10, and All batch actions.
- Prevented failed/empty chapter downloads from being marked as downloaded.
- Replaced the stale instrumentation test that referenced the removed `MainScreen`.
