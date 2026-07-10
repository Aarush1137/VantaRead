# VantaRead Fixes Summary

- Unified source IDs across Library, Search, Suggestions, Settings, repository, and add-by-URL flows.
- Made Royal Road the default source and refreshed Royal Road selectors for search, suggestions, thumbnails, details, and chapters.
- Updated LightNovelPub to the working `.me` domain and refreshed its list/detail/chapter selectors.
- Reworked WTR-Lab suggestions/search parsing around the current novel-list page after the old search URL stopped working.
- Converted Royal Road, LightNovelPub, and WTR-Lab away from hidden WebView scraping for list/search/detail/chapter paths and onto direct HTTP parsing where the sites are reachable.
- Added automatic Search/Suggestions fallback to Royal Road when the selected source returns no results or throws an error.
- Added source IDs onto novel search/suggestion results so tapping a result opens it with the correct scraper instead of a stale active source.
- Made repository detail/chapter loading detect the source from the URL host, so old saved items are less likely to open with the wrong source.
- Added LightNovelPub search fallback that filters its popular/list page when the site search endpoint returns empty results.
- Added WTR-Lab chapter-list fallback that builds chapter URLs from the page chapter count when chapter links are not rendered in the HTML.
- Fixed add-by-URL metadata so saved novels keep real titles, covers, authors, and source IDs instead of showing unknown/blank data.
- Added cached novel metadata fallback on the detail screen so saved novels still show title/cover/synopsis if a live refresh fails.
- Made Settings source selection update the active Library/Search/Suggestions source immediately.
- Added a visible Downloads section on novel details with downloaded counts plus Next 5, Next 10, and All batch actions.
- Added a dedicated Downloads tab that lists locally downloaded chapters, opens them in the reader, and lets you remove downloaded chapter content.
- Prevented failed/empty chapter downloads from being marked as downloaded.
- Replaced the stale instrumentation test that referenced the removed `MainScreen`.
