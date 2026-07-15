# Implementation Plan - Auth Fixes, TTS Voices, and Reader Bookmarks

The user reports that all sign-in methods are "stuck" and requests more voices and new features (Reader Bookmarks).

## User Review Required

> [!IMPORTANT]
> **Firebase Configuration**: If sign-in is "stuck" without an error message, it often indicates a network issue or a missing/misconfigured `google-services.json`. I will improve error reporting and timeout handling to make this clearer.

## Proposed Changes

### Auth Fixes

#### [MODIFY] [AuthRepository.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/data/repository/AuthRepository.kt)
- Update `signIn`, `signUp`, and `signInWithCredential` to rethrow `CancellationException`. This ensures that `withTimeout` in the ViewModel triggers correctly.

#### [MODIFY] [AuthViewModel.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/ui/auth/AuthViewModel.kt)
- Improve error messages for timeouts.
- Ensure `isLoading` is consistently reset even on unexpected failures.
- Add more granular logging for debugging sign-in steps.

---

### TTS Improvements

#### [MODIFY] [ReaderViewModel.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/ui/reader/ReaderViewModel.kt)
- Add a "Premium/Network" voice filter or better grouping in `refreshTtsVoices`.
- Store the selected voice more reliably in preferences.

#### [MODIFY] [ReaderScreen.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/ui/reader/ReaderScreen.kt)
- Improve the voice selection menu to include search or categorization if the list is long.

---

### New Feature: Reader Bookmarks

#### [NEW] [BookmarkEntity.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/data/db/BookmarkEntity.kt)
- Define a new entity for per-chapter bookmarks (page/paragraph index, label, timestamp).

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/data/db/AppDatabase.kt)
- Add the new Bookmark table.

#### [MODIFY] [NovelRepository.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/data/repository/NovelRepository.kt)
- Add methods to save/delete/fetch chapter-specific bookmarks.

#### [MODIFY] [ReaderViewModel.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/ui/reader/ReaderViewModel.kt)
- Expose a flow of bookmarks for the current chapter.
- Add `addBookmark(paragraphIndex: Int, label: String)` and `deleteBookmark(id: Long)`.

#### [MODIFY] [ReaderScreen.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/ui/reader/ReaderScreen.kt)
- Add a "Bookmark" icon to the HUD.
- Show a list of bookmarks for the current chapter in a side sheet or bottom sheet.
- Allow one-tap navigation to a bookmarked paragraph.

## Verification Plan

### Automated Tests
- `gradlew :app:testDebugUnitTest` to verify repository and ViewModel logic.

### Manual Verification
- Deploy to device and test all sign-in methods (Google, Email, Phone).
- Verify that timeout error appears after 30 seconds if network is blocked.
- Test adding and navigating to bookmarks in the reader.
- Test switching between different TTS voices.
