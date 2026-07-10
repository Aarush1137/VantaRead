# Build and Publish APK to GitHub

This plan outlines the steps to build the VantaRead Android APK and push it to GitHub. Based on the request "as in in packages", I will create a `packages/` directory in the repository to store the APK and also set up a GitHub Action for automated releases.

## User Review Required

> [!IMPORTANT]
> Since no signing key is provided, I will build the **Debug** version of the APK. This APK can be installed on devices for testing but is not suitable for Play Store distribution.

- Do you have a specific signing key you'd like to use for a **Release** build?
- Is the `packages/` folder the intended location, or did you mean the "GitHub Packages" registry (which usually hosts library artifacts)?

## Proposed Changes

### Build System

#### [:app/build.gradle.kts](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/app/build.gradle.kts)
- (No changes needed for building, but I can add `maven-publish` if requested for the Packages registry).

### Repository Structure

#### [NEW] [packages/VantaRead-debug.apk](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/packages/VantaRead-debug.apk)
- The built APK will be stored here and pushed to GitHub.

### Automation (Optional)

#### [NEW] [.github/workflows/release.yml](file:///C:/Users/aarus/.gemini/antigravity/scratch/VantaRead/.github/workflows/release.yml)
- Create a GitHub Action to automatically build and upload the APK to GitHub Releases on tag creation.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the build is successful.
- Check for the existence of the APK in `app/build/outputs/apk/debug/`.

### Manual Verification
- Verify the `packages/` directory contains the APK.
- Run `git status` to ensure the APK is tracked (not ignored).
- Run `git push` to verify the APK reaches GitHub.
