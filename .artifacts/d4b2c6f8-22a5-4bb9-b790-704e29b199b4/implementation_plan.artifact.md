# Implementation Plan - Custom Storage Path & Content Offloading

This plan introduces the ability for users to define a custom storage location (Internal or External/SD Card) for their downloaded light novels. This offloads large HTML content from the SQLite database to the file system, improving app performance and allowing users to manage their device storage better.

## User Review Required

> [!IMPORTANT]
> **Storage Permissions**: Selecting an external folder (like on an SD card) requires the user to grant persistent folder access via the Android System Picker. The app will use Scoped Storage APIs to ensure compatibility with modern Android versions.

> [!WARNING]
> **Data Migration**: When changing the storage path, existing downloaded chapters will need to be moved. I will implement an automatic migration process to ensure no data is lost.

## Proposed Changes

### Core Storage Logic

#### [NEW] [VantaStorageManager.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/data/util/VantaStorageManager.kt)
- Manage the base directory URI for novel content.
- Implement `saveChapter(novelUrl, chapterUrl, content)` and `loadChapter(novelUrl, chapterUrl)`.
- Use `DocumentFile` for Scoped Storage compatibility.
- Implement `migrateData(oldUri, newUri)` to move files.

#### [MODIFY] [ReaderPreferencesManager.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/data/prefs/ReaderPreferencesManager.kt)
- Add `storageUri` preference (stores the string representation of the URI).
- Add `setStorageUri(uri: String)`.

---

### Data Layer Integration

#### [MODIFY] [NovelRepository.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/data/repository/NovelRepository.kt)
- Update `getChapterContent` to check `VantaStorageManager` first.
- Update `fetchAndCacheChapters` and `prefetchChapter` to save content using the manager.
- Implement a background migration task that moves content from the `chapters` table to the file system.

#### [MODIFY] [ChapterDownloadWorker.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/worker/ChapterDownloadWorker.kt)
- Update `doWork` to save the downloaded HTML via `VantaStorageManager`.
- Update the DB record to set `isDownloaded = true` but leave `content = null` (referencing the file instead).

---

### UI & Settings

#### [MODIFY] [SettingsViewModel.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/ui/settings/SettingsViewModel.kt)
- Expose `currentStoragePath` StateFlow.
- Add `updateStorageLocation(uri: Uri)` and handle persistent permission requests.
- Track migration progress.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/src/main/java/com/example/vantaread/ui/settings/SettingsScreen.kt)
- Add a new "Storage" section.
- Display the current storage path (Internal vs. Custom Folder).
- Add a "Change Location" button that launches the folder picker.
- Add a progress indicator for data migration if active.

---

### Bonus Optimization: Cover Caching
- Update the storage logic to also cache novel covers in the custom path, making the entire library available offline without relying on Coil's transient cache.

## Verification Plan

### Automated Tests
- Unit tests for `VantaStorageManager` to verify file naming and path resolution.
- Test migration logic with mock URIs.

### Manual Verification
1. **Change Path**: Select a custom folder on internal storage.
2. **Download**: Download a novel and verify files appear in the selected folder.
3. **Move Path**: Change the folder again and verify that previously downloaded chapters are moved and still readable.
4. **Offline Mode**: Turn off the internet and verify that chapters stored in the custom path load correctly in the reader.
