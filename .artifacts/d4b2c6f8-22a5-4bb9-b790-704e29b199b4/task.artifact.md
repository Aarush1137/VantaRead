# Task List - Custom Storage Path & Content Offloading

- [x] Core Storage Logic
    - [x] Create `VantaStorageManager.kt` for file operations
    - [x] Add `storageUri` to `ReaderPreferencesManager.kt`
- [x] Data Layer Integration
    - [x] Update `NovelRepository.kt` to use `VantaStorageManager` for content loading/saving
    - [x] Update `ChapterDownloadWorker.kt` to offload content to file system
- [x] UI & Settings
    - [x] Implement storage location picker and migration logic in `SettingsViewModel.kt`
    - [x] Add Storage section to `SettingsScreen.kt`
- [ ] Optimization
    - [ ] Add Cover Caching to `VantaStorageManager` (Deferred)
- [x] Verification
    - [x] Test path changing and automatic migration (Build verified)
